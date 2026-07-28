package com.amz.ios.launcher.config;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;


public class AppearanceActivity extends SettingBaseActivity {

    private TopTitlebar mTitlebar;
    private IOSPreference mDesktopStylePref;
    private IOSPreference mDesktopGridPref;
    private IOSPreference mDrawerStylePref;

    private View mDrawerSubTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appearance_settings_activity);
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

        final Resources res = getResources();
        String desktopStyleSummary = res.getString(R.string.home_screen_standard);

        mDesktopStylePref = (IOSPreference) findViewById(R.id.dekstop_style);
        mDesktopStylePref.setSummary(desktopStyleSummary);
        mDesktopStylePref.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_DESKTOP_STYLE_CHANGE_ENABLE, true) ? View.VISIBLE : View.GONE);
        mDesktopStylePref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(DesktopStyleActivity.class);
            }
        });

        int checkedItem = Settings.getDesktopGrid(mContext);
        final String[] gridTitles = mContext.getResources().getStringArray(R.array.desktop_grid_layout_entries);

        mDesktopGridPref = (IOSPreference) findViewById(R.id.desktop_grid);
        mDesktopGridPref.setSummary(gridTitles[checkedItem]);
        mDesktopGridPref.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_DESKTOP_GRID_CHANGE_ENABLE, true) ? View.VISIBLE : View.GONE);
        mDesktopGridPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDesktopGridChooser();
            }
        });

        mDrawerSubTitle = findViewById(R.id.drawer_subtitle);
        mDrawerStylePref = (IOSPreference) findViewById(R.id.drawer_style);
        final int displayMode = Settings.getAllAppsDisplayMode(mContext);

        mDrawerStylePref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AppearanceActivity.this, DrawerStyleActivity.class);
                startActivityForResult(intent, DrawerStyleActivity.REQUEST_CODE);
            }
        });

        mDrawerSubTitle.setVisibility(View.GONE);
        mDrawerStylePref.setVisibility(View.GONE);

    }

    protected void showDesktopGridChooser() {
        int checkedItem = Settings.getDesktopGrid(AppearanceActivity.this);
        final AlertDialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog)
                .setTitle(R.string.desktop_grid_layout_title)
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setSingleChoiceItems(R.array.desktop_grid_layout_entries, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.setDesktopGrid(mContext, which);
                LauncherRouter.launch(mContext);
                dialog.dismiss();
            }
        });

        // Create the dialog
        dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final int displayMode = Settings.getAllAppsDisplayMode(mContext);
    }
}
