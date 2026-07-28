package com.amz.ios.launcher.config;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.widget.SmoothCheckBox;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.views.TopTitlebar;


public class DesktopStyleActivity extends SettingBaseActivity implements View.OnClickListener {
    private TopTitlebar mTitlebar;

    private View mStandardLayout;
    private View mDrawerLayout;
    private SmoothCheckBox mStandardCheckbox;
    private SmoothCheckBox mDrawerCheckbox;
    private ImageView mStandardPreview;
    private ImageView mDrawerPreview;

    private Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desktop_style_setting_activity);
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

        View standardItem = findViewById(R.id.standard);
        mStandardLayout = standardItem.findViewById(R.id.title_layout);
        mStandardCheckbox = (SmoothCheckBox) standardItem.findViewById(R.id.checkbox);
        mStandardPreview = (ImageView) standardItem.findViewById(R.id.preview);
        ((CustomTextView) standardItem.findViewById(R.id.title)).setText(res.getString(R.string.home_screen_standard));
        ((CustomTextView) standardItem.findViewById(R.id.desc)).setText(res.getString(R.string.home_screen_standard_des));

        View drawerItem = findViewById(R.id.drawer);
        mDrawerLayout = drawerItem.findViewById(R.id.title_layout);
        mDrawerCheckbox = (SmoothCheckBox) drawerItem.findViewById(R.id.checkbox);
        mDrawerPreview = (ImageView) drawerItem.findViewById(R.id.preview);
        ((CustomTextView) drawerItem.findViewById(R.id.title)).setText(res.getString(R.string.home_screen_drawer));
        ((CustomTextView) drawerItem.findViewById(R.id.desc)).setText(res.getString(R.string.home_screen_drawer_des));

        mStandardPreview.setImageResource(R.drawable.standard_style_preview);
        mDrawerPreview.setImageResource(R.drawable.drawer_style_preview);
        mStandardLayout.setOnClickListener(this);
        mDrawerLayout.setOnClickListener(this);
        mStandardCheckbox.setOnClickListener(this);
        mDrawerCheckbox.setOnClickListener(this);


        updateCheckState(false);
    }


    @Override
    public void onClick(View v) {
        mStandardCheckbox.toggle(true);
        mDrawerCheckbox.toggle(true);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showChangeConfirm();
            }
        }, 100);
    }


    private void showChangeConfirm() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog);
        builder.setTitle(R.string.program_restart)
                .setMessage(R.string.program_restart_confirm)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        applySettingChange();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateCheckState(true);
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }


    private void applySettingChange() {
        LauncherRouter.restartLauncher(this);
    }

    private void updateCheckState(boolean animate) {
        mDrawerCheckbox.setChecked(false, animate);
        mStandardCheckbox.setChecked(true, animate);
    }
}
