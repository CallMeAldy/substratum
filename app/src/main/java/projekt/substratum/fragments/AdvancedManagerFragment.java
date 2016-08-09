package projekt.substratum.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.OverlayManagerAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.OverlayManager;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AdvancedManagerFragment extends Fragment {

    private RecyclerView.Adapter mAdapter;
    private MaterialSheetFab materialSheetFab;
    private ArrayList<String> activated_overlays, disabled_overlays, all_overlays;
    private SharedPreferences prefs;
    private RelativeLayout relativeLayout;
    private ViewGroup root;
    private List<OverlayManager> overlaysList;
    private FloatingActionMenu floatingActionButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean swipeRefreshing;
    private MaterialProgressBar progressBar;
    private RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        root = (ViewGroup) inflater.inflate(R.layout.advanced_manager_fragment, null);
        relativeLayout = (RelativeLayout) root.findViewById(R.id.no_overlays_enabled);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlays_recycler_view);

        View sheetView = root.findViewById(R.id.fab_sheet);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = getContext().getColor(R.color.fab_menu_background_card);
        int fabColor = getContext().getColor(R.color.colorAccent);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        floatingActionButton = (FloatingActionMenu) root.findViewById(R
                .id.apply_fab);
        floatingActionButton.hide();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(floatingActionButton, sheetView, overlay,
                    sheetColor, fabColor);
        }

        LayoutReloader layoutReloader = new LayoutReloader();
        layoutReloader.execute("");

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshing = true;
                LayoutReloader layoutReloader = new LayoutReloader();
                layoutReloader.execute("");
            }
        });

        TextView disable_selected = (TextView) root.findViewById(R.id.disable_selected);
        if (!References.checkOMS())
            disable_selected.setText(getString(R.string.fab_menu_uninstall));
        if (disable_selected != null)
            disable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    materialSheetFab.hideSheet();
                    if (References.checkOMS()) {
                        String data = "om disable";
                        List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                                .getOverlayManagerList();
                        for (int i = 0; i < overlayList.size(); i++) {
                            OverlayManager overlay = overlayList.get(i);
                            if (overlay.isSelected()) {
                                data = data + " " + overlay.getName();
                            }
                        }
                        if (!prefs.getBoolean("systemui_recreate", false) &&
                                data.contains("systemui")) {
                            data = data + " && pkill -f com.android.systemui";
                        }
                        Toast toast = Toast.makeText(getContext(), getString(R
                                        .string.toast_disabled),
                                Toast.LENGTH_LONG);
                        toast.show();
                        if (References.isPackageInstalled(getContext(),
                                "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putExtra("om-commands", data);
                            getContext().sendBroadcast(runCommand);
                        } else {
                            Root.runCommand(data);
                        }
                    } else {
                        String current_directory;
                        if (References.inNexusFilter()) {
                            current_directory = "/system/overlay/";
                        } else {
                            current_directory = "/system/vendor/overlay/";
                        }

                        for (int i = 0; i < overlaysList.size(); i++) {
                            if (overlaysList.get(i).isSelected()) {
                                Log.e("overlays", overlaysList.get(i).getName());
                                Root.runCommand("mount -o rw,remount /system");
                                Root.runCommand("rm -r " + current_directory +
                                        overlaysList.get(i).getName() + ".apk");
                            }
                        }

                        // Since we had to parse the directory to process the recyclerView,
                        // reparse it to notifyDataSetChanged

                        activated_overlays.clear();
                        overlaysList.clear();
                        if (References.inNexusFilter()) {
                            current_directory = "/system/overlay/";
                        } else {
                            current_directory = "/system/vendor/overlay/";
                        }

                        File currentDir = new File(current_directory);
                        String[] listed = currentDir.list();
                        for (int i = 0; i < listed.length; i++) {
                            if (listed[i].substring(listed[i].length() - 4).equals(".apk")) {
                                activated_overlays.add(listed[i].substring(0,
                                        listed[i].length() - 4));
                            }
                        }

                        Collections.sort(activated_overlays);

                        for (int i = 0; i < activated_overlays.size(); i++) {
                            OverlayManager st = new OverlayManager(getContext(),
                                    activated_overlays.get(i), true);
                            overlaysList.add(st);
                        }

                        mAdapter.notifyDataSetChanged();

                        Toast toast2 = Toast.makeText(getContext(), getString(R
                                        .string.toast_disabled6),
                                Toast.LENGTH_SHORT);
                        toast2.show();
                        AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(getContext());
                        alertDialogBuilder
                                .setTitle(getString(R.string.legacy_dialog_soft_reboot_title));
                        alertDialogBuilder
                                .setMessage(getString(R.string.legacy_dialog_soft_reboot_text));
                        alertDialogBuilder
                                .setPositiveButton(android.R.string.ok, new DialogInterface
                                        .OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Root.runCommand("reboot");
                                    }
                                });
                        alertDialogBuilder.setCancelable(false);
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }
            });

        TextView enable_selected = (TextView) root.findViewById(R.id.enable_selected);
        if (enable_selected != null)
            enable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    materialSheetFab.hideSheet();
                    String data = "om enable";
                    List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                            .getOverlayManagerList();
                    for (int i = 0; i < overlayList.size(); i++) {
                        OverlayManager overlay = overlayList.get(i);
                        if (overlay.isSelected()) {
                            data = data + " " + overlay.getName();
                        }
                    }
                    if (!prefs.getBoolean("systemui_recreate", false) &&
                            data.contains("systemui")) {
                        data = data + " && pkill -f com.android.systemui";
                    }
                    Toast toast = Toast.makeText(getContext(), getString(R
                                    .string.toast_enabled),
                            Toast.LENGTH_LONG);
                    toast.show();
                    if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", data);
                        getContext().sendBroadcast(runCommand);
                    } else {
                        Root.runCommand(data);
                    }
                }
            });

        TextView uninstall_selected = (TextView) root.findViewById(R.id.uninstall);
        if (uninstall_selected != null)
            uninstall_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    materialSheetFab.hideSheet();
                    String data = "";
                    List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                            .getOverlayManagerList();
                    for (int i = 0; i < overlayList.size(); i++) {
                        OverlayManager overlay = overlayList.get(i);
                        if (data.length() == 0) {
                            if (overlay.isSelected()) {
                                data = "pm uninstall " + overlay.getName();
                            }
                        } else {
                            if (overlay.isSelected()) {
                                data = data + " && pm uninstall " + overlay.getName();
                            }
                        }
                    }
                    if (!prefs.getBoolean("systemui_recreate", false) &&
                            data.contains("systemui")) {
                        data = data + " && om refresh && pkill -f com.android.systemui";
                    } else {
                        data += " && om refresh";
                    }
                    Toast toast = Toast.makeText(getContext(), getString(R
                                    .string.toast_uninstalling),
                            Toast.LENGTH_LONG);
                    toast.show();
                    if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", data);
                        getContext().sendBroadcast(runCommand);
                    } else {
                        Root.runCommand(data);
                    }
                }
            });

        return root;
    }

    private class LayoutReloader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mAdapter = new OverlayManagerAdapter(overlaysList);
            mRecyclerView.setAdapter(mAdapter);

            if (activated_overlays.size() == 0) {
                floatingActionButton.hide();
                relativeLayout.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                floatingActionButton.show();
                relativeLayout.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
            if (!prefs.getBoolean("manager_disabled_overlays", true) || !References.checkOMS()) {
                LinearLayout enable_view = (LinearLayout) root.findViewById(R.id.enable);
                enable_view.setVisibility(View.GONE);
            }
            if (swipeRefreshing) {
                swipeRefreshing = false;
                swipeRefreshLayout.setRefreshing(false);
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            overlaysList = new ArrayList<>();
            activated_overlays = new ArrayList<>();
            disabled_overlays = new ArrayList<>();
            all_overlays = new ArrayList<>();

            if (References.checkOMS()) {
                Process nativeApp = null;
                try {
                    nativeApp = Runtime.getRuntime().exec("om list");

                    try (OutputStream stdin = nativeApp.getOutputStream();
                         InputStream stderr = nativeApp.getErrorStream();
                         InputStream stdout = nativeApp.getInputStream();
                         BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
                        String line;

                        stdin.write(("ls\n").getBytes());
                        stdin.write("exit\n".getBytes());

                        while ((line = br.readLine()) != null) {
                            if (line.length() > 0) {
                                if (line.contains("[x]")) {
                                    activated_overlays.add(line.substring(8));
                                } else if (line.contains("[ ]")) {
                                    disabled_overlays.add(line.substring(8));
                                }
                            }
                        }

                        try (BufferedReader br1 = new BufferedReader(
                                new InputStreamReader(stderr))) {
                            while ((line = br1.readLine()) != null) {
                                Log.e("AdvancedManagerFragment", line);
                            }
                        }
                    }
                } catch (IOException ioe) {
                    Log.e("PriorityListFragment", "There was an issue regarding loading the " +
                            "priorities of each overlay.");
                } finally {
                    if (nativeApp != null) {
                        // destroy the Process explicitly
                        nativeApp.destroy();
                    }
                }

                if (prefs.getBoolean("manager_disabled_overlays", true)) {
                    all_overlays = new ArrayList<>(activated_overlays);
                    all_overlays.addAll(disabled_overlays);

                    Collections.sort(all_overlays);

                    for (int i = 0; i < all_overlays.size(); i++) {
                        if (disabled_overlays.contains(all_overlays.get(i))) {
                            OverlayManager st = new OverlayManager(getContext(),
                                    all_overlays.get(i), false);
                            overlaysList.add(st);
                        } else if (activated_overlays.contains(all_overlays.get(i))) {
                            OverlayManager st = new OverlayManager(getContext(),
                                    all_overlays.get(i), true);
                            overlaysList.add(st);
                        }
                    }
                } else {
                    all_overlays = new ArrayList<>(activated_overlays);
                    all_overlays.addAll(disabled_overlays);

                    Collections.sort(all_overlays);

                    for (int i = 0; i < all_overlays.size(); i++) {
                        if (activated_overlays.contains(all_overlays.get(i))) {
                            OverlayManager st = new OverlayManager(getContext(),
                                    all_overlays.get(i), true);
                            overlaysList.add(st);
                        }
                    }
                }
            } else {
                // At this point, the object is an RRO formatted check
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = "/system/overlay/";
                } else {
                    current_directory = "/system/vendor/overlay/";
                }

                File currentDir = new File(current_directory);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    String[] listed = currentDir.list();
                    for (int i = 0; i < listed.length; i++) {
                        if (listed[i].substring(listed[i].length() - 4).equals(".apk")) {
                            activated_overlays.add(listed[i].substring(0, listed[i].length() - 4));
                        }
                    }
                    Collections.sort(activated_overlays);
                    for (int i = 0; i < activated_overlays.size(); i++) {
                        OverlayManager st = new OverlayManager(getContext(), activated_overlays.get
                                (i), true);
                        overlaysList.add(st);
                    }
                }
            }
            return null;
        }
    }
}