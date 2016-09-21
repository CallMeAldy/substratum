package projekt.substratum.adapters;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.model.ThemeInfo;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private ArrayList<ThemeInfo> information;

    public DataAdapter(ArrayList<ThemeInfo> information) {
        this.information = information;
    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card,
                viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        viewHolder.theme_name.setText(information.get(i).getThemeName());
        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        if (information.get(i).getPluginVersion() != null) {
            viewHolder.plugin_version.setText(information.get(i).getPluginVersion());
        } else {
            viewHolder.plugin_version.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getSDKLevels() != null) {
            viewHolder.theme_apis.setText(information.get(i).getSDKLevels());
        } else {
            viewHolder.theme_apis.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getThemeVersion() != null) {
            viewHolder.theme_version.setText(information.get(i).getThemeVersion());
        } else {
            viewHolder.theme_version.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getThemeReadyVariable() == null) {
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (information.get(i).getThemeReadyVariable().equals("all")) {
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else if (information.get(i).getThemeReadyVariable().equals("ready")) {
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (information.get(i).getThemeReadyVariable().equals("stock")) {
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else {
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        }

        viewHolder.tbo.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        new AlertDialog.Builder(information.get(i)
                                .getContext())
                                .setMessage(R.string.tbo_description)
                                .setPositiveButton(R.string.tbo_dialog_proceed,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    String playURL =
                                                            information.get(i).getContext()
                                                                    .getString(R.string
                                                                            .tbo_theme_ready_url);
                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setData(Uri.parse(playURL));
                                                    information.get(i).getContext()
                                                            .startActivity(intent);
                                                } catch (ActivityNotFoundException
                                                        activityNotFoundException) {
                                                    //
                                                }
                                            }
                                        })
                                .setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        })
                                .setCancelable(true)
                                .show();
                    }
                }
        );

        viewHolder.two.setOnClickListener(new View.OnClickListener() {
                                              public void onClick(View v) {
                                                  new AlertDialog.Builder(information.get(i)
                                                          .getContext())
                                                          .setMessage(R.string.two_description)
                                                          .setCancelable(true)
                                                          .show();
                                              }
                                          }
        );

        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        viewHolder.imageView.setImageDrawable(information.get(i).getThemeDrawable());
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView theme_name;
        TextView theme_author;
        TextView theme_apis;
        TextView theme_version;
        TextView plugin_version;
        ImageView imageView;
        ImageView tbo;
        ImageView two;

        public ViewHolder(View view) {
            super(view);
            theme_name = (TextView) view.findViewById(R.id.theme_name);
            theme_author = (TextView) view.findViewById(R.id.theme_author);
            theme_apis = (TextView) view.findViewById(R.id.api_levels);
            theme_version = (TextView) view.findViewById(R.id.theme_version);
            plugin_version = (TextView) view.findViewById(R.id.plugin_version);
            imageView = (ImageView) view.findViewById(R.id.theme_preview_image);
            tbo = (ImageView) view.findViewById(R.id.theme_ready_indicator);
            two = (ImageView) view.findViewById(R.id.theme_unready_indicator);
        }
    }
}