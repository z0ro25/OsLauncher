package com.amz.ios.launcher.config;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;

public class EffectSettingsActivity extends SettingBaseActivity {

    private TopTitlebar mTitlebar;
    private IOSPreference mAppAnimPref;
    private IOSPreference mDropDelAnimPref;
    private IOSPreference mWallpaperScrollPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.effect_settings_activity);
        setupViews();
    }

    private void setupViews() {
        mTitlebar = (TopTitlebar) findViewById(R.id.titlebar);
        mTitlebar.setDividerVisible(true);
        mTitlebar.setBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        mAppAnimPref = (IOSPreference) findViewById(R.id.app_trans_anim);
        mAppAnimPref.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_DESKTOP_TRANS_ANIM_ENABLE) ? View.VISIBLE : View.GONE);
        mAppAnimPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppAnimChooser();
            }
        });

        mDropDelAnimPref = (IOSPreference) findViewById(R.id.delete_anim);
        mDropDelAnimPref.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_DELETE_EFFECT_ENABLE) ? View.VISIBLE : View.GONE);
        mDropDelAnimPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAnimChooser();
            }
        });

        mWallpaperScrollPref = (IOSPreference) findViewById(R.id.wallpaper_scroll);
        mWallpaperScrollPref.setChecked(Settings.isWallpaperScrollEnabled(this));
        mWallpaperScrollPref.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.setWallpaperScrollEnabled(mContext, isChecked);
            }
        });
    }


    protected void showAppAnimChooser() {
        int checkedItem = Settings.getAppAnimationStyle(mContext);
        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog)
                .setTitle(R.string.pending_anim_title)
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setSingleChoiceItems(R.array.pending_transition_entries, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.setAppAnimationStyle(mContext, which);
                dialog.dismiss();
            }
        });

        // Create the dialog
        dialog = builder.create();
        dialog.show();

    }

    protected void showDeleteAnimChooser() {
        int checkedItem = Settings.getDropTargetAnimStyle(mContext);
        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog)
                .setTitle(R.string.drop_delete_anim_title)
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setSingleChoiceItems(R.array.drop_delete_anim_entries, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.setDropTargetAnimStyle(mContext, which);
                dialog.dismiss();
            }
        });

        // Create the dialog
        dialog = builder.create();
        dialog.show();

    }

}
