package com.amz.ios.launcher.config;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.update.BaseUpdateClient;
import com.amz.ios.ioslite.common.update.VersionUpdateManager;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.ioslite.common.util.SharedUtil;
import com.amz.ios.ioslite.common.util.TimeUtil;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherProvider;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;
import com.amz.ios.launcher.config.ActionPickerFragment;

/**
 * Created by server on 16-11-30.
 */
public class LauncherSettingActivity extends SettingBaseActivity implements LauncherBackupHelper.OnBackupListener {
    private TopTitlebar mTitlebar;

    private LauncherBackupHelper mBackupHelper;

    private IOSPreference mAppearancePre;
    private IOSPreference mIconDesignPre;

    private IOSPreference mGeneralSettingPre;
    private IOSPreference mEffectPre;
    private IOSPreference mGesturePre;

    private IOSPreference mBackupPre;
    private IOSPreference mRestorePre;
    private IOSPreference mResetPre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUseExitAnim = false;

        mBackupHelper = new LauncherBackupHelper(this);
        mBackupHelper.setOnBackupListener(this);

        AnalyticsDelegate.onLauncherSettingsEvent(this, UMEventConstants.LAUNCHERSETTINGS_ENTER);
        setContentView(R.layout.launcher_setting_activity);
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

        mAppearancePre = (IOSPreference) findViewById(R.id.settings_appearance);
        mAppearancePre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsDelegate.onLauncherSettingsEvent(LauncherSettingActivity.this, UMEventConstants.LAUNCHERSETTINGS_OUTLOOK_CLICK);
                startActivity(new Intent(LauncherSettingActivity.this, AppearanceActivity.class));
            }
        });

        mIconDesignPre = (IOSPreference) findViewById(R.id.settings_icon_design);
        mIconDesignPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IconLayoutActivity.class);
            }
        });

        mGeneralSettingPre = (IOSPreference) findViewById(R.id.settings_general);
        mGeneralSettingPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherSettingActivity.this, FeatureSettingActivity.class));
            }
        });


        mEffectPre = (IOSPreference) findViewById(R.id.settings_effect);
        mEffectPre.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_DESKTOP_EFFECT_ENABLE) ? View.VISIBLE : View.GONE);
        mEffectPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherSettingActivity.this, EffectSettingsActivity.class));
            }
        });

        mGesturePre = (IOSPreference) findViewById(R.id.settings_gesture);
        mGesturePre.setVisibility(Partner.getBoolean(mContext, Partner.FEATURE_DESKTOP_GESTURE_ENABLE, true) ? View.VISIBLE : View.GONE);
        mGesturePre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(GestureSettingActivity.class);
            }
        });

        mBackupPre = (IOSPreference) findViewById(R.id.settings_backup);
        long lastedBackupTime = mBackupHelper.getLastestRestoreTime();
        if (lastedBackupTime > 0) {
            mBackupPre.setSummary(getString(R.string.setting_backup_desktop_time) + " : " +
                    TimeUtil.localizatedTime(lastedBackupTime,
                            getString(R.string.preYear),
                            getString(R.string.preMonth),
                            getString(R.string.preDay),
                            getString(R.string.preHour),
                            getString(R.string.preMinute),
                            getString(R.string.preSecond)));
        }

        mBackupPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBackupHelper.doBackup();
            }
        });

        mRestorePre = (IOSPreference) findViewById(R.id.settings_restore);
        mRestorePre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBackupHelper.isBackupDbExists()) {
                    Toast.makeText(mContext, mContext.getString(R.string.launcher_settings_restore_no_data), Toast.LENGTH_SHORT).show();
                } else {
                    showRestoreConfirmDialog();
                }
            }
        });

        mResetPre = (IOSPreference) findViewById(R.id.settings_reset);
        mResetPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirmDialog();
            }
        });
    }


    protected void onResume() {
        super.onResume();
        if (!PermissionUtil.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showPermissoinRationDialog();
        }
    }

    protected void onDestroy() {
        AnalyticsDelegate.onLauncherSettingsEvent(this, UMEventConstants.LAUNCHERSETTINGS_EXIT);
        super.onDestroy();
    }

    private void updateBackupPref() {
        long lastedBackupTime = mBackupHelper.getLastestRestoreTime();
        if (lastedBackupTime > 0) {
            mBackupPre.setSummary(getString(R.string.setting_backup_desktop_time) + " : " +
                    TimeUtil.localizatedTime(lastedBackupTime,
                            getString(R.string.preYear),
                            getString(R.string.preMonth),
                            getString(R.string.preDay),
                            getString(R.string.preHour),
                            getString(R.string.preMinute),
                            getString(R.string.preSecond)));
        } else {
            mBackupPre.setSummary(getString(R.string.setting_backup_desktop_summary));
        }
    }

    private void showResetConfirmDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog);
        builder.setTitle(R.string.desktop_reset)
                .setMessage(R.string.desktop_reset_confirm)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetSettings();
                        LauncherRouter.resetLauncher(mContext);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    private void showRestoreConfirmDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog);
        builder.setTitle(R.string.setting_category_restore)
                .setMessage(R.string.launcher_settings_restore_confirm)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBackupHelper.doRestore();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    private void showPermissoinRationDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, R.style.LauncherAlertDialog);
        builder.setTitle(R.string.backup_permission_title)
                .setMessage(R.string.backup_permission_ration)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionUtil.requestPermissions(LauncherSettingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    @Override
    public void onBackup(boolean success) {
        if (success) {
            updateBackupPref();
        }
    }

    @Override
    public void onRestore(boolean success) {
        if (success) {
            LauncherRouter.restartLauncher(this);
        }
    }

    private void resetSettings() {
        LauncherAppState.getLauncherProvider().clearDatabase();
//        //Scroll effect, MagicThumb style
//        Settings.setWorkspaceScrollEffect(mContext, Partner.getString(mContext, Partner.DEF_WORKSPACE_SCROLL_EFFECT));
//        Settings.setWorkspaceMagicThumb(mContext, Partner.getString(mContext, Partner.DEF_WORKSPACE_MAGIC_THUMB));

        //Icon, Text Size
        Settings.setWorkspaceIconSizeScale(mContext, 1.0f);
        Settings.setWorkspaceTextSizeScale(mContext, 1.0f);
        Settings.setWorkspaceLabelColor(mContext, 0xFFFFFFFF);

        //General Settings
        Settings.setDesktopLoopEnable(mContext, Partner.getBoolean(mContext, Partner.DEF_DESKTOP_LOOP_ENABLE));
        Settings.setDesktopAlignEnable(mContext, Partner.getBoolean(mContext, Partner.DEF_DESKTOP_AUTO_ALIGN_ENABLE));
        Settings.setNewAppsCategoryEnable(mContext, Partner.getBoolean(mContext, Partner.DEF_CATEGORY_NEW_APPS));
        Settings.setSearchBarEnabled(mContext, Partner.getBoolean(mContext, Partner.FEATURE_SEARCH_BOX_ENABLE,true));

        //Effect Settings
        Settings.setAppAnimationStyle(mContext, Partner.getInteger(mContext, Partner.DEF_PENDING_ANIM_STYLE));
        Settings.setWallpaperScrollEnabled(mContext, Partner.getBoolean(mContext, Partner.DEF_WALLPAPER_SCROLL_ENABLED));
        Settings.setDropTargetAnimStyle(mContext, Partner.getBoolean(mContext, Partner.FEATURE_DELETE_EFFECT_ENABLE)?1:0);

        //Gesture Settings
        GestureEventModel.AlarmShowItemInfo alarmInfo = new GestureEventModel.AlarmShowItemInfo(mContext);
        GestureEventModel.updateGestureEvent(GestureEventModel.GESTURE_SWIPE_DOWN, alarmInfo.getURI(), alarmInfo.getDescription());

        GestureEventModel.SearchShowItemInfo searchInfo = new GestureEventModel.SearchShowItemInfo(mContext);
        GestureEventModel.updateGestureEvent(GestureEventModel.GESTURE_SWIPE_UP, searchInfo.getURI(), searchInfo.getDescription());

        Settings.setSleepModeEnabled(mContext, Partner.getBoolean(mContext, Partner.DEF_SLEEP_MODE_ENABLE));
    }
}
