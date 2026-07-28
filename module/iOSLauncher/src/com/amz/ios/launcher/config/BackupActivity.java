package com.amz.ios.launcher.config;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.ioslite.common.util.TimeUtil;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;

public class BackupActivity extends SettingBaseActivity implements LauncherBackupHelper.OnBackupListener {
    private TopTitlebar mTitlebar;

    private LauncherBackupHelper mBackupHelper;

    private IOSPreference mBackupPre;
    private IOSPreference mResorePre;
    private IOSPreference mResetPre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackupHelper = new LauncherBackupHelper(this);
        mBackupHelper.setOnBackupListener(this);
        setContentView(R.layout.backup_setting_activity);
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

        mBackupPre = (IOSPreference) findViewById(R.id.settings_backup);
        mBackupPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBackupHelper.doBackup();
            }
        });

        mResorePre = (IOSPreference) findViewById(R.id.settings_import);
        mResorePre.setOnClickListener(new View.OnClickListener() {
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

        updateBackupPref();
    }

    private void updateBackupPref() {
        long lastedBackupTime = mBackupHelper.getLastestRestoreTime();
        if (lastedBackupTime > 0) {
            mBackupPre.setSummary(getString(R.string.setting_backup_desktop_time) + " : " +
                    TimeUtil.formatTime(lastedBackupTime));
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
                        PermissionUtil.requestPermissions(BackupActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
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

    protected void onResume() {
        super.onResume();
        if (!PermissionUtil.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showPermissoinRationDialog();
        }
    }
}
