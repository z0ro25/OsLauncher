package com.amz.ios.launcher.config;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;

public class NotificationMgrActivity extends SettingBaseActivity {
    private TopTitlebar mTitlebar;

    private IOSPreference mDefHomePre;
    private IOSPreference mLowBatteryPre;
    private IOSPreference mLowMemoryPre;
    private IOSPreference mNewThemePre;
    private IOSPreference mNewWallpaperPre;

    private ContentResolver mResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_manage_activity);
        mResolver = getContentResolver();
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

        mDefHomePre = (IOSPreference) findViewById(R.id.default_home);
        mDefHomePre.setChecked(IOSSettings.getBoolean(mResolver, IOSSettings.Notify.NOTIFY_DEFAULT_HOME));
        mDefHomePre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                IOSSettings.putBoolean(mResolver, IOSSettings.Notify.NOTIFY_DEFAULT_HOME, isChecked);
            }
        });

        mLowBatteryPre = (IOSPreference) findViewById(R.id.low_battery);
        mLowBatteryPre.setChecked(IOSSettings.getBoolean(mResolver, IOSSettings.Notify.NOTIFY_LOW_BATTERY));
        mLowBatteryPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                IOSSettings.putBoolean(mResolver, IOSSettings.Notify.NOTIFY_LOW_BATTERY, isChecked);
            }
        });

        mLowMemoryPre = (IOSPreference) findViewById(R.id.low_memory);
        mLowMemoryPre.setChecked(IOSSettings.getBoolean(mResolver, IOSSettings.Notify.NOTIFY_LOW_MEMORY));
        mLowMemoryPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                IOSSettings.putBoolean(mResolver, IOSSettings.Notify.NOTIFY_LOW_MEMORY, isChecked);
            }
        });

        mNewThemePre = (IOSPreference) findViewById(R.id.new_theme);
        mNewThemePre.setChecked(IOSSettings.getBoolean(mResolver, IOSSettings.Notify.NOTIFY_NEW_THEME));
        mNewThemePre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                IOSSettings.putBoolean(mResolver, IOSSettings.Notify.NOTIFY_NEW_THEME, isChecked);
            }
        });

        mNewWallpaperPre = (IOSPreference) findViewById(R.id.new_wallpaper);
        mNewWallpaperPre.setChecked(IOSSettings.getBoolean(mResolver, IOSSettings.Notify.NOTIFY_NEW_WALLPAPER));
        mNewWallpaperPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                IOSSettings.putBoolean(mResolver, IOSSettings.Notify.NOTIFY_NEW_WALLPAPER, isChecked);
            }
        });

        mNewThemePre.setVisibility(View.GONE);
        mNewWallpaperPre.setVisibility(View.GONE);

    }

}
