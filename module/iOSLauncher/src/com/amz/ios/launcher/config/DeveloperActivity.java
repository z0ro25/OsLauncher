package com.amz.ios.launcher.config;


import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CompoundButton;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.debug.DeveloperRouter;
import com.amz.ios.ioslite.common.preference.SettingBaseActivity;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.ioslite.common.view.InfoItemLayout;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.parser.AppCategoryModel;
import com.amz.ios.launcher.views.IOSPreference;
import com.amz.ios.launcher.views.TopTitlebar;

public class DeveloperActivity extends SettingBaseActivity {
    private TopTitlebar mTitlebar;

    private InfoItemLayout mBuildType;
    private InfoItemLayout mBuildCustomer;
    private InfoItemLayout mBuildChannel;
    private InfoItemLayout mBuildTime;

    private InfoItemLayout mImei;
    private InfoItemLayout mImsi;
    private InfoItemLayout mBrand;

    private InfoItemLayout mCustomConfigPk;

    private IOSPreference mAdControllPref;
    private IOSPreference mAdSourcePref;
    private IOSPreference mMemoryPref;
    private IOSPreference mAppCategoryPref;
    private IOSPreference mBuildWorkspace;
    private IOSPreference mDumpAllApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.developer_activity);
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

        mBuildType = (InfoItemLayout) findViewById(R.id.build_type);
        mBuildCustomer = (InfoItemLayout) findViewById(R.id.build_customer);
        mBuildChannel = (InfoItemLayout) findViewById(R.id.build_channel);
        mBuildTime = (InfoItemLayout) findViewById(R.id.build_time);

        mImei = (InfoItemLayout) findViewById(R.id.device_imei);
        mImsi = (InfoItemLayout) findViewById(R.id.device_imsi);
        mBrand = (InfoItemLayout) findViewById(R.id.device_brand);

        mCustomConfigPk = (InfoItemLayout) findViewById(R.id.custom_config_pkg);

        mMemoryPref = (IOSPreference) findViewById(R.id.memoryTracker);
        mMemoryPref.setChecked(Settings.isMemWatcherEnabled(this));
        mMemoryPref.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.setMemWatcherEnabled(mContext, isChecked);
            }
        });

        mBuildWorkspace = (IOSPreference) findViewById(R.id.build_workspace);
        mBuildWorkspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LauncherAppState.getInstance().getModel().dumpWorkspace();
                mBuildWorkspace.setSummary(Environment.getExternalStorageDirectory() + "/launcher/" + GridConfig.getGridWorkspaceXmlName(mContext));
            }
        });

        mDumpAllApps = (IOSPreference) findViewById(R.id.dump_allapps);
        mDumpAllApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LauncherAppState.getInstance().getModel().dumpAllAppsToLocalData();
                mDumpAllApps.setSummary(FileUtil.getRootFilesDir() + "/allapps.txt");
            }
        });

        mAppCategoryPref = (IOSPreference) findViewById(R.id.appCategory);
        mAppCategoryPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppCategoryModel.getInstance().requestDbFromServer();
            }
        });

        mAdControllPref = (IOSPreference) findViewById(R.id.advertise_controll);
        mAdControllPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeveloperRouter.startAdControllActivity(mContext);
            }
        });

        mAdSourcePref = (IOSPreference) findViewById(R.id.advertise_source);
        mAdSourcePref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeveloperRouter.startAdSourceActivity(mContext);
            }
        });

        showVersionDetail();
        showDeviceDetail();
        showConfigDetail();
    }

    private void showVersionDetail() {
        mBuildType.setDescription(getBuildType());
        mBuildCustomer.setDescription(DeviceInfoUtil.getCustomer(this));
        mBuildChannel.setDescription(DeviceInfoUtil.getChannel(this));
        mBuildTime.setDescription(BuildUtil.getCompileTime(this));
    }

    private void showDeviceDetail() {
        mImei.setDescription(DeviceInfoUtil.getImei(this));
        mImsi.setDescription(DeviceInfoUtil.getImsi(this));
        mBrand.setDescription(DeviceInfoUtil.getBrand());
    }

    private void showConfigDetail() {
        if (BuildUtil.isCustomerBuild()) {
            mCustomConfigPk.setDescription(Partner.getPackageName(this));
        } else {
            mCustomConfigPk.setVisibility(View.GONE);
        }

    }


    private String getBuildType() {
        String buildType = "";
        if (BuildUtil.BUILD_MODE == BuildUtil.Mode.CUSTOMER_CN) {
            buildType = getString(R.string.build_mode_customer_cn);
        } else if (BuildUtil.BUILD_MODE == BuildUtil.Mode.CUSTOMER_HW) {
            buildType = getString(R.string.build_mode_customer_hw);
        } else if (BuildUtil.BUILD_MODE == BuildUtil.Mode.PUBLIC_CN) {
            buildType = getString(R.string.build_mode_public_cn);
        } else if (BuildUtil.BUILD_MODE == BuildUtil.Mode.PUBLIC_HW) {
            buildType = getString(R.string.build_mode_public_hw);
        } else {
            buildType = getString(R.string.build_error);
        }
        return buildType + (BuildUtil.DEBUG ? "(Debug)" : "(Release)");
    }

}
