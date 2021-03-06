package projekt.substratum.fragments;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substrate.LetsGetStarted;
import projekt.substratum.R;
import projekt.substratum.adapters.DataAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.ThemeInfo;
import projekt.substratum.util.CacheCreator;
import projekt.substratum.util.ReadOverlays;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class BootAnimationsFragment extends Fragment {

    private final int THEME_INFORMATION_REQUEST_CODE = 1;
    private HashMap<String, String[]> substratum_packages;
    private RecyclerView recyclerView;
    private Map<String, String[]> map;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<ApplicationInfo> list;
    private DataAdapter adapter;
    private View cardView;
    private ViewGroup root;
    private String selected_theme_name;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = (ViewGroup) inflater.inflate(R.layout.home_fragment, null);

        mContext = getActivity();

        substratum_packages = new HashMap<>();
        recyclerView = (RecyclerView) root.findViewById(R.id.theme_list);
        cardView = root.findViewById(R.id.no_entry_card_view);
        cardView.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            try {
                                                String playURL = getString(R.string
                                                        .search_play_store_url);
                                                Intent i = new Intent(Intent.ACTION_VIEW);
                                                i.setData(Uri.parse(playURL));
                                                startActivity(i);
                                            } catch (ActivityNotFoundException
                                                    activityNotFoundException) {
                                                //
                                            }
                                        }
                                    }
        );
        cardView.setVisibility(View.GONE);

        // Create it so it uses a recyclerView to parse substratum-based themes

        PackageManager packageManager = mContext.getPackageManager();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);

        LayoutLoader layoutLoader = new LayoutLoader();
        layoutLoader.execute("");

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);

        ArrayList<ThemeInfo> themeInfos = prepareData();
        adapter = new DataAdapter(themeInfos);

        // Assign adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onSingleTapUp(MotionEvent e) {
                            return true;
                        }
                    });

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && gestureDetector.onTouchEvent(e)) {
                    // RecyclerView Clicked item value
                    SharedPreferences prefs = getContext().getSharedPreferences(
                            "substratum_state", Context.MODE_PRIVATE);
                    if (!prefs.contains("is_updating")) prefs.edit()
                            .putBoolean("is_updating", false).apply();
                    if (!prefs.getBoolean("is_updating", true)) {
                        int position = rv.getChildAdapterPosition(child);

                        // Process fail case if user uninstalls an app and goes back an activity
                        if (References.isPackageInstalled(getContext(),
                                map.get(map.keySet().toArray()[position].toString())[1])) {

                            File checkSubstratumVerity = new File(getContext().getCacheDir()
                                    .getAbsoluteFile() + "/SubstratumBuilder/" +
                                    getThemeName(map.get(map.keySet().toArray()[position]
                                            .toString())[1]).replaceAll("\\s+", "")
                                            .replaceAll("[^a-zA-Z0-9]+", "") + "/substratum.xml");
                            selected_theme_name = map.get(
                                    map.keySet().toArray()[position].toString())[1];
                            if (checkSubstratumVerity.exists()) {
                                launchTheme(getThemeName(selected_theme_name), position);
                            } else {
                                new SubstratumThemeUpdate(selected_theme_name, position).execute();
                            }
                        } else {
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .toast_uninstalled),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            refreshLayout();
                        }
                    } else {
                        Toast toast = Toast.makeText(getContext(), getString(R.string
                                        .background_updating_toast),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                refreshLayout();
            }
        });
        return root;
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataName) != null) {
                    if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                        return appInfo.metaData.getString(References.metadataName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    private void getSubstratumPackages(Context context, String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);

            Context otherContext = getContext().createPackageContext(package_name, 0);
            AssetManager am = otherContext.getAssets();
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataName) != null) {
                    if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                        try {
                            String[] stringArray = am.list("");
                            if (Arrays.asList(stringArray).contains("bootanimation")) {
                                String[] data = {appInfo.metaData.getString
                                        (References.metadataAuthor),
                                        package_name};
                                substratum_packages.put(appInfo.metaData.getString
                                        (References.metadataName), data);
                            }
                        } catch (Exception e) {
                            Log.e("SubstratumLogger", "Unable to find package identifier");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
    }

    private ArrayList<ThemeInfo> prepareData() {

        ArrayList<ThemeInfo> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeInfo themeInfo = new ThemeInfo();
            themeInfo.setThemeName(map.keySet().toArray()[i].toString());
            themeInfo.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeInfo.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themeInfo.setThemeDrawable(
                    References.grabPackageHeroImage(mContext, map.get(map.keySet().toArray()[i]
                            .toString())[1]));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (prefs.getBoolean("show_template_version", false)) {
                themeInfo.setPluginVersion(References.grabPackageTemplateVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeInfo.setSDKLevels(References.grabThemeAPIs(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeInfo.setThemeVersion(References.grabThemeVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            } else {
                themeInfo.setPluginVersion(null);
                themeInfo.setSDKLevels(null);
                themeInfo.setThemeVersion(null);
            }
            themeInfo.setContext(mContext);
            themes.add(themeInfo);
        }
        return themes;
    }

    private void refreshLayout() {
        MaterialProgressBar materialProgressBar = (MaterialProgressBar) root.findViewById(R.id
                .progress_bar_loader);
        materialProgressBar.setVisibility(View.VISIBLE);
        PackageManager packageManager = mContext.getPackageManager();
        list.clear();
        recyclerView.setAdapter(null);
        substratum_packages = new HashMap<>();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            getSubstratumPackages(mContext, packageInfo.packageName);
        }

        doCleanUp cleanUp = new doCleanUp();
        cleanUp.execute("");

        if (substratum_packages.size() == 0) {
            cardView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);
        ArrayList<ThemeInfo> themeInfos = prepareData();
        adapter = new DataAdapter(themeInfos);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
        materialProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getInt(
                "uninstalled", THEME_INFORMATION_REQUEST_CODE) == THEME_INFORMATION_REQUEST_CODE) {
            prefs.edit().putInt("uninstalled", 0).commit();
            refreshLayout();
        }
        super.onResume();
    }

    private void launchTheme(String theme_name, int position) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            long currentDateAndTime = Long.parseLong(sdf.format(new Date()));

            String parse1_themeName = theme_name.replaceAll("\\s+", "");
            String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

            SharedPreferences prefs = mContext.getSharedPreferences(
                    "filter_state", Context.MODE_PRIVATE);
            long saved_time = prefs.getLong(parse2_themeName + "_saved_time", 0);

            if (currentDateAndTime > saved_time && String.valueOf(currentDateAndTime).length() ==
                    String.valueOf(saved_time).length() && saved_time != 0 &&
                    !References.isPackageInstalled(getContext(),
                            References.lp_package_identifier)) {
                LetsGetStarted.initialize(getContext(),
                        map.get(map.keySet().toArray()[position].toString())[1],
                        !References.checkOMS(), "bootanimation", References.DEBUG, saved_time);
            } else {
                long checker = LetsGetStarted.initialize(getContext(),
                        map.get(map.keySet().toArray()[position].toString())[1],
                        !References.checkOMS(), "bootanimation", References.DEBUG, saved_time);
                if (checker > -1) {
                    prefs.edit().putLong(
                            parse2_themeName + "_saved_time", currentDateAndTime).apply();
                } else {
                    Toast toast = Toast.makeText(getContext(),
                            getString(R.string
                                    .information_activity_pirated_toast),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        } catch (Exception ex) {
            Toast toast = Toast.makeText(getContext(),
                    getString(R.string
                            .information_activity_upgrade_toast),
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private class LayoutLoader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            refreshLayout();
            if (substratum_packages.size() == 0) {
                cardView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                cardView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                for (ApplicationInfo packageInfo : list) {
                    getSubstratumPackages(mContext, packageInfo.packageName);
                }
            } catch (Exception e) {
                // Exception
            }
            return null;
        }
    }

    private class doCleanUp extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            List<String> state1 = ReadOverlays.main(1);  // Overlays with non-existent targets
            for (int i = 0; i < state1.size(); i++) {
                Log.e("OverlayCleaner", "Target APK not found for \"" + state1.get(i) + "\" and " +
                        "will " +
                        "be removed.");
                Root.runCommand("pm uninstall " + state1.get(i));
            }
            return null;
        }
    }

    private class SubstratumThemeUpdate extends AsyncTask<Void, Integer, String> {

        private ProgressDialog progress;
        private int position;
        private String theme_name;
        private Boolean launch;

        public SubstratumThemeUpdate(String strValue, int intValue) {
            this.position = intValue;
            this.theme_name = strValue;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);

            String parse = String.format(mContext.getString(R.string.on_demand_updating_text),
                    getThemeName(selected_theme_name));

            progress.setTitle(mContext.getString(R.string.on_demand_updating_title));
            progress.setMessage(parse);
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            Toast toast = Toast.makeText(getContext(), getString(R.string
                            .background_updated_toast),
                    Toast.LENGTH_SHORT);
            Toast toast2 = Toast.makeText(getContext(), getString(R.string
                            .background_updated_toast_cancel),
                    Toast.LENGTH_SHORT);
            if (launch) {
                toast.show();
                // At this point, we can safely assume that the theme has successfully extracted
                launchTheme(getThemeName(theme_name), position);
            } else {
                toast2.show();
                // We don't want this cache anymore, delete it from the system completely
                new CacheCreator().wipeCache(mContext, theme_name);
            }
        }

        @Override
        protected String doInBackground(Void... Params) {
            launch = new CacheCreator().initializeCache(mContext, theme_name);
            return null;
        }
    }
}