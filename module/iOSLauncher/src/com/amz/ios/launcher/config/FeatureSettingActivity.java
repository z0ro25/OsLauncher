package com.amz.ios.launcher.config;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.assembly.LeftCustomContentUtil;
import com.amz.ios.launcher.assembly.SearchWidgetUtil;
import com.amz.ios.launcher.notification.NotificationListener;
import com.amz.ios.launcher.util.SettingsObserver;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;

public class FeatureSettingActivity extends SettingBaseActivity {
    private TopTitlebar mTitlebar;

    private IOSPreference mDesktopLoop;
    private IOSPreference mDesktopAlignPre;
    private IOSPreference mSmartCategoryPre;
    private IOSPreference mLeftPagePre;
    private IOSPreference mSearchbarPre;
    private IOSPreference mRecommendAppPre;
    private IOSPreference mFloatViewPre;
    private IOSPreference mAppmgrpre;
    private IOSPreference mNotification;

    private CustomTextView mMsgCategory;

    private IconBadgingObserver mIconBadgingObserver;
    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";

    public static final String EXTRA_NEWSPAGE_WIDGET_STATE = "ioslite.intent.extra.NEWSPAGE_WIDGET_STATE";
    public static final String ACTION_NEWSPAGE_WIDGET_STATE = "ioslite.intent.action.NEWSPAGE_WIDGET_STATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feature_setting_activity);
        setupViews();
        requestSwitch();
    }

    private void requestSwitch() {
//        NetworkManager.handleNetConnect(getApplicationContext());
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

        mDesktopLoop = (IOSPreference) findViewById(R.id.desktop_loop);
        mDesktopLoop.setChecked(Settings.isDesktopLoopEnable(this));
        mDesktopLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.setDesktopLoopEnable(mContext, isChecked);
            }
        });

        if (!Partner.getBoolean(mContext, Partner.FEATURE_DESKTOP_LOOP_ENABLE)) {
            mDesktopLoop.setVisibility(View.GONE);
        }

        mNotification = (IOSPreference) findViewById(R.id.notification_badging);

        if (!Utilities.ATLEAST_OREO) {
            mNotification.setVisibility(View.GONE);
        } else if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
            mNotification.setVisibility(View.GONE);
        } else {
            // Listen to system notification badge settings while this UI is active.
            mIconBadgingObserver = new IconBadgingObserver(
                    mNotification, getContentResolver(), getFragmentManager());
            mIconBadgingObserver.register(NotificationListener.NOTIFICATION_BADGING, NOTIFICATION_ENABLED_LISTENERS);
        }

        mDesktopAlignPre = (IOSPreference) findViewById(R.id.desktop_auto_align);
        mDesktopAlignPre.setChecked(Settings.isDesktopAlignEnable(this));
        mDesktopAlignPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.setDesktopAlignEnable(mContext, isChecked);
            }
        });

        mSmartCategoryPre = (IOSPreference) findViewById(R.id.apps_smart_category);
        mSmartCategoryPre.setChecked(Settings.isNewAppsCategoryEnable(this));
        mSmartCategoryPre.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_CATEGORY_NEW_APPS_ENABLE) ? View.VISIBLE : View.GONE);
        mSmartCategoryPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.setNewAppsCategoryEnable(mContext, isChecked);
            }
        });

        mLeftPagePre = (IOSPreference) findViewById(R.id.settings_leftpage);
        mLeftPagePre.setVisibility(Settings.isNewsPageSwitchEnable(mContext) && LeftCustomContentUtil.hasCustomContentToLeft(mContext) ? View.VISIBLE : View.GONE);
        mLeftPagePre.setChecked(Settings.isLeftPageEnabled(FeatureSettingActivity.this));
        mLeftPagePre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AnalyticsDelegate.onLauncherSettingsEvent(FeatureSettingActivity.this, UMEventConstants.LAUNCHERSETTINGS_HEADNEWS_CLICK);
                Settings.setLeftPageEnabled(FeatureSettingActivity.this, isChecked);
                sendBroadcast(isChecked);
            }
        });

        mRecommendAppPre = (IOSPreference) findViewById(R.id.settings_apprecommend);
        mRecommendAppPre.setVisibility(Settings.isFolderDiscoveryFeatureEnable(mContext) ? View.VISIBLE : View.GONE);
        mRecommendAppPre.setChecked(Settings.isFolderDiscoveryUserEnable(FeatureSettingActivity.this));
        mRecommendAppPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AnalyticsDelegate.onLauncherSettingsEvent(FeatureSettingActivity.this, UMEventConstants.LAUNCHERSETTINGS_APPRECOMMEND_CLICK);
                Settings.setFolderDiscoveryUserEnable(mContext, isChecked);
            }
        });

        mSearchbarPre = (IOSPreference) findViewById(R.id.settings_searchbar);
        mSearchbarPre.setVisibility(SearchWidgetUtil.hasSearchWidget(mContext) ? View.VISIBLE : View.GONE);//xiaopeng VISIBLE
        mSearchbarPre.setChecked(SearchWidgetUtil.shouldShow(mContext));
        mSearchbarPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AnalyticsDelegate.onLauncherSettingsEvent(FeatureSettingActivity.this, UMEventConstants.LAUNCHERSETTINGS_SHOWSEARCHBOX_CLICK);
                Settings.setSearchBarEnabled(FeatureSettingActivity.this, isChecked);
            }
        });

        mFloatViewPre = (IOSPreference) findViewById(R.id.settings_float_view);
        mFloatViewPre.setVisibility(Partner.getBoolean(this, Partner.FEATURE_IOS_KNOW_ENABLE) ? View.VISIBLE : View.GONE);
        mFloatViewPre.setChecked(Settings.isSwingViewEnabled(FeatureSettingActivity.this));
        mFloatViewPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AnalyticsDelegate.onLauncherSettingsEvent(FeatureSettingActivity.this, UMEventConstants.LAUNCHERSETTINGS_SHOWIOSKNOW_CLICK);
                Settings.setSwingViewEnabled(FeatureSettingActivity.this, isChecked);
            }
        });

        mAppmgrpre = (IOSPreference) findViewById(R.id.settings_appmgr);
        mAppmgrpre.setVisibility(false&&Partner.getBoolean(this, Partner.FEATURE_APP_MANAGER_ENABLE) ? View.VISIBLE : View.GONE);
        mAppmgrpre.setChecked(Settings.isAppmgrShow(FeatureSettingActivity.this));
        mAppmgrpre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.setAppmgrShow(FeatureSettingActivity.this, isChecked);
            }
        });

        mMsgCategory = (CustomTextView) findViewById(R.id.message_category);
        mMsgCategory.setVisibility(View.GONE);

    }

    private void sendBroadcast(boolean isChecked){
        Intent intent = new Intent(ACTION_NEWSPAGE_WIDGET_STATE);
        intent.putExtra(EXTRA_NEWSPAGE_WIDGET_STATE, isChecked);
        intent.setPackage("com.ios.widget.newspage");
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        if (mIconBadgingObserver != null) {
            mIconBadgingObserver.unregister();
            mIconBadgingObserver = null;
        }
        super.onDestroy();
    }

    private static class IconBadgingObserver extends SettingsObserver.Secure
            implements CompoundButton.OnCheckedChangeListener {

        private final IOSPreference mBadgingPref;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;
        private CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent("android.settings.NOTIFICATION_SETTINGS")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(":settings:fragment_args_key", NotificationListener.NOTIFICATION_BADGING);
                mBadgingPref.getContext().startActivity(intent);
            }
        };

        public IconBadgingObserver(IOSPreference badgingPref, ContentResolver resolver,
                                   FragmentManager fragmentManager) {
            super(resolver);
            mBadgingPref = badgingPref;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            int summary = enabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;

            boolean serviceEnabled = true;
            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        android.provider.Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mBadgingPref.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
                if (!serviceEnabled) {
                    summary = R.string.title_missing_notification_access;
                }
            }

            mBadgingPref.setOnCheckedChangeListener(serviceEnabled ? mCheckedChangeListener : this);
            mBadgingPref.setSummary(mBadgingPref.getContext().getString(summary));
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            new NotificationAccessConfirmation().show(mFragmentManager, "notification_access");
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access,
                    context.getString(R.string.derived_app_name));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_notification_access)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.title_change_settings, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ComponentName cn = new ComponentName(getActivity(), NotificationListener.class);
            Bundle showFragmentArgs = new Bundle();
            showFragmentArgs.putString(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString());

            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString())
                    .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, showFragmentArgs);
            getActivity().startActivity(intent);
        }
    }
}
