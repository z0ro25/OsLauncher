package com.amz.ios.iossettings;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amz.ios.ioslite.common.CommonActivity;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.launcher.LauncherWallpaperManager;
import com.amz.ios.ioslite.common.util.FunctionUtil;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.ioslite.common.util.PhoneStateUtil;
import com.amz.ios.ioslite.common.util.Sputil;
import com.amz.ios.launcher.views.CustomTextView;
import com.ios.ioslite.odm.R;
//import com.zhuoyi.security.batterysave.BS_MainActivity;

import static android.content.Intent.ACTION_VIEW;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;


public class SettingsActivity extends CommonActivity implements OnClickListener, OnLongClickListener {
    /**
     * 监听广播action
     */
    private final String NOTIFICATION_LIGHT_STATES = "notification_follow_up_status";
    private final String TORCH_LIGHT_STATES = "torch_widget_view_status";
    private final String SETTINGS_LIGHT_STATES = "settings_follow_up_status";
    private final String NOTIFICATION_CLOSE_CAMERA = "close_camera";
    private final String NOTIFICATION_DATA_REFRESH = "com.ios.notification.data.refresh";
    private final String ACTION_ANY_DATA_STATE = "android.intent.action.ANY_DATA_STATE";
    private final String ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private final String fileName = "torchfile";

    /**
     * 界面控件
     */
    private ImageView mAirplaneModeIv;
    private ImageView mDataIv;
    private ImageView mWifiIv;
    private CustomTextView mWifiTv;

    private ImageView mRingtoneIv;
    private ImageView mHotSpotIv;
    private ImageView mLocationIv;

    private ImageView mAutoRotateIv;
    private ImageView mBluetoothIv;
    private ImageView mPowerSavingIv;

    private ImageView mBrightIv;
    private CustomTextView mBrightTv;
    private ImageView mAutoBrightnessIv;
    private ImageView mTorchIv;

    private ImageView mAppSettingsIv;
    private ImageView mSystemSettingsIv;
    private ImageView mLauncherSettingsIv;

    private FrameLayout mFlBackground;
    private FrameLayout mFlBackground2;
    /**
     * 设备管理对象
     */
    private WifiManager wifiManager;
    private AudioManager audioManager;
    private SettingsObserver mObserver;
    private LightnessObserver mLightnessObserver;
    private SettingsBrightnessDialog mBrightnessDialog;
    private Context mContext;
    private final String lightKey = "isLight";
    private final static String UNKNOWN_SSID = "<unknown ssid>";
    private FunctionUtil mFunctionUtils;
    private PhoneStateUtil mPhoneStateUtils;
    private int mMinBright;
    private final int MESSAGE_TORCH = 0x11;
    private final int MESSAGE_WIFI_NAME = 0x12;
    private final int MESSAGE_DATA = 0x13;
    @RequiresApi(Build.VERSION_CODES.M)
    private CameraManager mCameraManager;
    @RequiresApi(Build.VERSION_CODES.M)
    private CameraManager.TorchCallback mTorchCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsDelegate.onSettingsEvent(getApplicationContext(),UMEventConstants.SETTINGS_ENTER);
        AnalyticsDelegate.onSettingsEvent(getApplicationContext(),UMEventConstants.SETTINGS_ACCESS);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.settings_main);
        applyBlur();
        mContext = getApplicationContext();
        init();
        initShortClick();
        initLongClick();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(ACTION_ANY_DATA_STATE);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(ACTION_WIFI_AP_STATE_CHANGED);
        filter.addAction(SETTINGS_LIGHT_STATES);
        registerReceiver(mSettingsReceiver, filter);
        mFunctionUtils = FunctionUtil.getInstance(mContext);
        mPhoneStateUtils = mFunctionUtils.getPhoneStateUtils();
        mObserver = new SettingsObserver(new Handler());
        mObserver.startObserver();
        mLightnessObserver = new LightnessObserver(new Handler());
        mLightnessObserver.startObserver();
        mIsLightOpen = Sputil.getTorchState(mContext, lightKey, fileName);
        mMinBright = mFunctionUtils.getMinBright();
        initImageView();

        if (!Partner.getBoolean(getApplicationContext(), Partner.FEATURE_TORCH_ENABLE)) {
            View view = findViewById(R.id.settings_torch);
            view.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
        }

        if (!Partner.getBoolean(getApplicationContext(), Partner.FEATURE_BATTERY_SAVE_ENABLE)) {
            View view = findViewById(R.id.settings_save_power);
            view.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
        }

        if (!Partner.getBoolean(getApplicationContext(), Partner.FEATURE_WIFI_AP_ENABLE)) {
            View view = findViewById(R.id.settings_hotpot);
            view.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
        }

        if (!Partner.getBoolean(getApplicationContext(), Partner.FEATURE_APP_MANAGER_ENABLE)) {
            findViewById(R.id.settings_application).setVisibility(View.GONE);
        }

        if (!mFunctionUtils.havaLightSensor() || !Partner.getBoolean(getApplicationContext(), Partner.FEATURE_AUTO_LIGHT_ENABLE)) {
            View view = findViewById(R.id.settings_auto_light);
            view.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
        } else {
            setTorchChangeListener();
        }
    }

    private void setTorchChangeListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mCameraManager == null) {
                mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            }
            if (mTorchCallback == null) {
                mTorchCallback = new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(@NonNull String cameraId, final boolean enabled) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mIsLightOpen = enabled;
                                setTorchIv();
                            }
                        });
                    }
                };
            }
            mCameraManager.registerTorchCallback(mTorchCallback, new Handler());
        }
    }

    private class LightnessObserver extends ContentObserver {
        ContentResolver mResolver;

        public LightnessObserver(Handler handler) {
            super(handler);
            mResolver = mContext.getContentResolver();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setBrightTv();
        }

        public void startObserver() {
            mResolver.registerContentObserver(Settings.System
                            .getUriFor(Settings.System.SCREEN_BRIGHTNESS), false,
                    this);
        }

        public void stopObserver() {
            mResolver.unregisterContentObserver(this);
        }
    }

    /**
     * 监听修改屏幕亮度和屏幕旋转方向
     */
    private class SettingsObserver extends ContentObserver {
        ContentResolver mSettingsResolver;

        public SettingsObserver(Handler handler) {
            super(handler);
            mSettingsResolver = mContext.getContentResolver();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setBrightIv();
            setAutoRatateIv();
        }

        public void startObserver() {
            mSettingsResolver.registerContentObserver(Settings.System
                            .getUriFor(SCREEN_BRIGHTNESS_MODE), false,
                    this);
            mSettingsResolver.registerContentObserver(Settings.System
                            .getUriFor(Settings.System.ACCELEROMETER_ROTATION), false,
                    this);
        }

        public void stopObserver() {
            mSettingsResolver.unregisterContentObserver(this);
        }
    }

    @Override
    protected void onResume() {
        mHandler.sendEmptyMessage(MESSAGE_DATA);
        super.onResume();
    }


    private void init() {
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mLightThread = new LightThread();

        LinearLayout mAriplaneLl = (LinearLayout) findViewById(R.id.settings_ariplane);
        LinearLayout mDataLl = (LinearLayout) findViewById(R.id.settings_data);
        LinearLayout mWifiLl = (LinearLayout) findViewById(R.id.settings_wifi);
        LinearLayout mRingLl = (LinearLayout) findViewById(R.id.settings_ring);
        LinearLayout mHotpotLl = (LinearLayout) findViewById(R.id.settings_hotpot);
        LinearLayout mLocationLl = (LinearLayout) findViewById(R.id.settings_location);
        LinearLayout mRotateLl = (LinearLayout) findViewById(R.id.settings_rotate);
        LinearLayout mBluetoothLl = (LinearLayout) findViewById(R.id.settings_bluetooth);
        LinearLayout mSavePowerLl = (LinearLayout) findViewById(R.id.settings_save_power);
        LinearLayout mScreenLightLl = (LinearLayout) findViewById(R.id.settings_screen_light);
        LinearLayout mAutoLightLl = (LinearLayout) findViewById(R.id.settings_auto_light);
        LinearLayout mTorchLl = (LinearLayout) findViewById(R.id.settings_torch);
        LinearLayout mApplicationLl = (LinearLayout) findViewById(R.id.settings_application);
        LinearLayout mSystemSettingLl = (LinearLayout) findViewById(R.id.settings_system_setting);
        LinearLayout mLauncherSettingLl = (LinearLayout) findViewById(R.id.settings_launcher_setting);


        mAirplaneModeIv = (ImageView) mAriplaneLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mAriplaneLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_airplane_mode_text);
        mDataIv = (ImageView) mDataLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mDataLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_traffic_data_text);
        mWifiIv = (ImageView) mWifiLl.findViewById(R.id.setting_image_view);
        mWifiTv = (CustomTextView) mWifiLl.findViewById(R.id.setting_text_view);
        mWifiTv.setText(R.string.settings_wifi_text);

        mRingtoneIv = (ImageView) mRingLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mRingLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_ringtone_text);
        mHotSpotIv = (ImageView) mHotpotLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mHotpotLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_settings_hot_spot);
        mLocationIv = (ImageView) mLocationLl.findViewById(R.id.setting_image_view);
        mLocationIv.setImageResource(R.drawable.location_close_selector);
        ((CustomTextView) mLocationLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_location_mode_text);

        mAutoRotateIv = (ImageView) mRotateLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mRotateLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_auto_rotate_text);
        mBluetoothIv = (ImageView) mBluetoothLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mBluetoothLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_blue_touch_text);
        mPowerSavingIv = (ImageView) mSavePowerLl.findViewById(R.id.setting_image_view);
        mPowerSavingIv.setImageResource(R.drawable.power_saving_open_selector);
        ((CustomTextView) mSavePowerLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_power_saving_text);

        mBrightIv = (ImageView) mScreenLightLl.findViewById(R.id.setting_image_view);
        mBrightTv = (CustomTextView) mScreenLightLl.findViewById(R.id.setting_text_view);
        mBrightTv.setText(R.string.settings_brightness_text);
        mAutoBrightnessIv = (ImageView) mAutoLightLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mAutoLightLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_auto_brightness_text);
        mTorchIv = (ImageView) mTorchLl.findViewById(R.id.setting_image_view);
        ((CustomTextView) mTorchLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_flash_light_text);

        mAppSettingsIv = (ImageView) mApplicationLl.findViewById(R.id.setting_image_view);
        mAppSettingsIv.setImageResource(R.drawable.app_manager_selector);
        ((CustomTextView) mApplicationLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_application_management_text);
        mSystemSettingsIv = (ImageView) mSystemSettingLl.findViewById(R.id.setting_image_view);
        mSystemSettingsIv.setImageResource(R.drawable.system_settings_selector);
        ((CustomTextView) mSystemSettingLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_settings_text);
        mLauncherSettingsIv = (ImageView) mLauncherSettingLl.findViewById(R.id.setting_image_view);
        mLauncherSettingsIv.setImageResource(R.drawable.launcher_settings_selector);
        ((CustomTextView) mLauncherSettingLl.findViewById(R.id.setting_text_view)).setText(R.string.settings_desk);

        mFlBackground = (FrameLayout) findViewById(R.id.fl_background);
        mFlBackground2 = (FrameLayout) findViewById(R.id.fl_background2);
    }

    private void initShortClick() {
        mAirplaneModeIv.setOnClickListener(this);
        mDataIv.setOnClickListener(this);
        mWifiIv.setOnClickListener(this);

        mRingtoneIv.setOnClickListener(this);
        mHotSpotIv.setOnClickListener(this);
        mLocationIv.setOnClickListener(this);

        mAutoRotateIv.setOnClickListener(this);
        mBluetoothIv.setOnClickListener(this);
        mPowerSavingIv.setOnClickListener(this);

        mBrightIv.setOnClickListener(this);
        mAutoBrightnessIv.setOnClickListener(this);
        mTorchIv.setOnClickListener(this);

        mAppSettingsIv.setOnClickListener(this);
        mSystemSettingsIv.setOnClickListener(this);
        mLauncherSettingsIv.setOnClickListener(this);
    }

    private void initLongClick() {
        mAirplaneModeIv.setOnLongClickListener(this);
        mDataIv.setOnLongClickListener(this);
        mWifiIv.setOnLongClickListener(this);

        mRingtoneIv.setOnLongClickListener(this);
        mHotSpotIv.setOnLongClickListener(this);
        mLocationIv.setOnLongClickListener(this);

        mAutoRotateIv.setOnLongClickListener(this);
        mBluetoothIv.setOnLongClickListener(this);
        mPowerSavingIv.setOnLongClickListener(this);

        mBrightIv.setOnLongClickListener(this);
        mAutoBrightnessIv.setOnLongClickListener(this);
    }

    private void setBrightTv() {
        int currentBrightness = 0;
        int lightnessPercentage = 0;
        currentBrightness = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, -1) - mMinBright;
        if (currentBrightness > -1) {
            lightnessPercentage = (int) Math.floor(currentBrightness / (float) (255 - mMinBright) * 100);
        } else {
            lightnessPercentage = 0;
        }
        mBrightTv.setText(mContext.getString(R.string.settings_brightness_text) + " " + lightnessPercentage + "%");
    }

    private void setAirplaneModeIv() {

        if (!mPhoneStateUtils.isAlplaneMode()) {
            mAirplaneModeIv.setImageResource(R.drawable.airplane_mode_close_selector);
        } else {
            mAirplaneModeIv.setImageResource(R.drawable.airplane_mode_open_selector);
        }
    }

    private void setWifiIv() {
        if (mPhoneStateUtils.isWifiOpen()) {
            mWifiIv.setImageResource(R.drawable.wifi_open_selector);
            mHotSpotIv.setImageResource(R.drawable.hot_spot_close_selector);
        } else {
            mWifiIv.setImageResource(R.drawable.wifi_close_selector);
        }
    }

    private void setRingtonIv() {
        int mode = audioManager.getRingerMode();
        if (AudioManager.RINGER_MODE_NORMAL == mode) {
            mRingtoneIv.setImageResource(R.drawable.ringtone_open_selector);
        } else if (AudioManager.RINGER_MODE_VIBRATE == mode) {
            mRingtoneIv.setImageResource(R.drawable.ringtone_vibrate_selector);
        } else {
            mRingtoneIv.setImageResource(R.drawable.ringtone_close_selector);
        }
    }

    private void setBrightIv() {
        if (mPhoneStateUtils.isAutoBrightness()) {
            mBrightIv.setImageResource(R.drawable.bright_close_selector);
            mAutoBrightnessIv.setImageResource(R.drawable.auto_brightness_open_selector);
        } else {
            mBrightIv.setImageResource(R.drawable.bright_open_selector);
            mAutoBrightnessIv.setImageResource(R.drawable.auto_brightness_close_selector);
        }
    }

    private void setBluetoothIv() {
        if (mPhoneStateUtils.isBlueToochOpen()) {
            mBluetoothIv.setImageResource(R.drawable.bluetooth_open_selector);
        } else {
            mBluetoothIv.setImageResource(R.drawable.bluetooth_close_selector);
        }
    }

    private void setLocationIv() {
        PermissionUtil.checkSelfPermissions(this, 0, new PermissionUtil.PermissionsRequestCallBackAdapter() {
            @Override
            public void onPermissionAllowed() {
                if (mPhoneStateUtils.isLocationModeOpen()) {
                    mLocationIv.setImageResource(R.drawable.location_open_selector);
                } else {
                    mLocationIv.setImageResource(R.drawable.location_close_selector);
                }
            }
        }, 0, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void setHotspotIv() {
        if (mPhoneStateUtils.isWifiApOpen()) {
            mHotSpotIv.setImageResource(R.drawable.hot_spot_open_selector);
        } else {
            mHotSpotIv.setImageResource(R.drawable.hot_spot_close_selector);
        }
    }

    private void setDataIv() {
        if (mPhoneStateUtils.checkPhoneNet() && !mPhoneStateUtils.isAlplaneMode()) {
            if (mPhoneStateUtils.isMobileDataOpen()) {
                mDataIv.setImageResource(R.drawable.date_open_selector);
            } else {
                mDataIv.setImageResource(R.drawable.date_close_selector);
            }
        } else {
            mDataIv.setImageResource(R.drawable.date_close_selector);
        }

    }

    private void setAutoRatateIv() {
        if (mPhoneStateUtils.isAutoRatation()) {
            mAutoRotateIv.setImageResource(R.drawable.auto_rotate_open_selector);
        } else {
            mAutoRotateIv.setImageResource(R.drawable.auto_rotate_close_selector);
        }
    }

    private void setTorchIv() {
        if (mIsLightOpen == true) {
            mTorchIv.setImageResource(R.drawable.torch_open_selector);
        } else {
            mTorchIv.setImageResource(R.drawable.torch_close_selector);
        }
    }

    private void setWifiTv() {
        if (mPhoneStateUtils.isWifiOpen()) {
            ifWifiLinkOk();
        } else {
            mWifiTv.setText(R.string.settings_wifi_text);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Sputil.saveTorchState(mContext, mIsLightOpen, lightKey, fileName);
    }


    @Override
    protected void onDestroy() {
        AnalyticsDelegate.onSettingsEvent(getApplicationContext(),UMEventConstants.SETTINGS_EXIT);
        unregisterReceiver(mSettingsReceiver);
        mObserver.stopObserver();
        mLightnessObserver.stopObserver();
        mHandler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mCameraManager != null) {
            mCameraManager.unregisterTorchCallback(mTorchCallback);
        }
        super.onDestroy();
    }

    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * 初始化界面
     */
    private void initImageView() {
        setAirplaneModeIv();
        setDataIv();
        setWifiIv();
        setWifiTv();

        setRingtonIv();
        setHotspotIv();
        setLocationIv();

        setAutoRatateIv();
        setBluetoothIv();

        setBrightTv();
        setBrightIv();
        setTorchIv();
    }

    private BroadcastReceiver mSettingsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                setDataIv();
                setWifiTv();
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                setWifiIv();
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                setBluetoothIv();
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                setAirplaneModeIv();
                setDataIv();
            } else if (ACTION_ANY_DATA_STATE.equals(action)) {
                setDataIv();
            } else if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
                setRingtonIv();
            } else if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                setLocationIv();
            } else if (ACTION_WIFI_AP_STATE_CHANGED.equals(action)) {
                setHotspotIv();
                setWifiIv();
                setWifiTv();
            } else if (SETTINGS_LIGHT_STATES.equals(action)) {
                mIsLightOpen = intent.getBooleanExtra(lightKey, true);
                if (mIsLightOpen == true) {
                    mTorchIv.setImageResource(R.drawable.torch_open_selector);
                } else {
                    mTorchIv.setImageResource(R.drawable.torch_close_selector);
                }
            }
        }
    };


    private int mNetWorkID = -1;
    private WifiInfo mWifiInfo;
    private String mWifiString;

    private void ifWifiLinkOk() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mWifiInfo = wifiManager.getConnectionInfo();
                mNetWorkID = mWifiInfo.getNetworkId();
                if (mNetWorkID != -1&&!TextUtils.equals(mWifiInfo.getSSID(),UNKNOWN_SSID)) {
                    mWifiString = mWifiInfo.getSSID();
                    mHandler.sendEmptyMessage(MESSAGE_WIFI_NAME);
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onClick(View v) {
        if (v == mAirplaneModeIv) {
            mFunctionUtils.operateFlightMode();
        } else if (v == mDataIv) {
            mFunctionUtils.operateNetwork();
        } else if (v == mWifiIv) {
            mFunctionUtils.operateWifi();
            //=============two==================
        } else if (v == mRingtoneIv) {
            mFunctionUtils.setRingtonMode();
        } else if (v == mHotSpotIv) {
            mFunctionUtils.operateWifiAp();
        } else if (v == mLocationIv) {
            mFunctionUtils.operateGps();
            //=============three==================
        } else if (v == mAutoRotateIv) {
            mFunctionUtils.operateRotation();
        } else if (v == mBluetoothIv) {
            mFunctionUtils.operateBluetooth();
        } else if (v == mPowerSavingIv) {
//            startActivity(new Intent(mContext, BS_MainActivity.class));
            //=============four==================
        } else if (v == mBrightIv) {
            if (!mFunctionUtils.requestWriteSetting()) {
                return;
            }
            if (mPhoneStateUtils.isAutoBrightness()) {
                Settings.System.putInt(getContentResolver(),
                        SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
            try {
                if (mBrightnessDialog == null) {
                    mBrightnessDialog = new SettingsBrightnessDialog(SettingsActivity.this);
                }
                mBrightnessDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v == mAutoBrightnessIv) {
            mFunctionUtils.operateDisplay();
        } else if (v == mTorchIv) {
            if (!mFunctionUtils.requestCamera(SettingsActivity.this)) {
                return;
            }
            if (checkCameraHardware(mContext)) {
                if (!mIsLightOpen) {
                    mTorchIv.setImageResource(R.drawable.torch_open_selector);
                } else {
                    mTorchIv.setImageResource(R.drawable.torch_close_selector);
                }
                Intent intent = new Intent();
                intent.putExtra(lightKey, !mIsLightOpen);
                intent.setAction(NOTIFICATION_LIGHT_STATES);
                sendBroadcast(intent);
                intent.setAction(TORCH_LIGHT_STATES);
                sendBroadcast(intent);
                new Thread(mLightThread).start();
            } else {
                Toast.makeText(mContext, R.string.settings_light_error_text, Toast.LENGTH_SHORT).show();
                mIsLightOpen = false;
                setTorchIv();
            }
            //=============five==================
        } else if (v == mAppSettingsIv) {
            AnalyticsDelegate.onSettingsEvent(getApplicationContext(),UMEventConstants.SETTINGS_APPMANAGE_CLICK);
            mFunctionUtils.openApplicationManager();
        } else if (v == mSystemSettingsIv) {
            AnalyticsDelegate.onSettingsEvent(getApplicationContext(),UMEventConstants.SETTINGS_SYSTEMSETTING_CLICK);
            mFunctionUtils.openSetting();
        } else if (v == mLauncherSettingsIv) {
            AnalyticsDelegate.onSettingsEvent(getApplicationContext(),UMEventConstants.SETTINGS_LAUNCHERSETTING_CLICK);
            LauncherRouter.launchSettingActivity(SettingsActivity.this);
        } else {
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mAirplaneModeIv) {
            try {
                Intent airplaneIntent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                startActivity(airplaneIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }

        } else if (v == mDataIv) {
            try {
                Intent dataIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                dataIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(dataIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }
        } else if (v == mWifiIv) {
            try {
                Intent wifiIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(wifiIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }
            //=============two==================
        } else if (v == mRingtoneIv) {
            try {
                Intent soundIntent = new Intent(Settings.ACTION_SOUND_SETTINGS);
                startActivity(soundIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }


        } else if (v == mHotSpotIv) {//							Intent sound1Intent=new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            try {
                Intent packageIntent = new Intent();
                ComponentName componetName = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                packageIntent.setComponent(componetName);
                packageIntent.setAction(ACTION_VIEW);
                startActivity(packageIntent);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Intent sound1Intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                    startActivity(sound1Intent);
                } catch (Exception e1) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
                }
            }

        } else if (v == mLocationIv) {
            try {
                Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(locationIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }
            //=============three==================
        } else if (v == mAutoRotateIv) {
            try {
                Intent displayIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(displayIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }
        } else if (v == mBluetoothIv) {
            try {
                Intent blueToothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(blueToothIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }

        } else if (v == mPowerSavingIv) {
            //=============four==================
        } else if (v == mBrightIv) {
            try {
                Intent brightIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(brightIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }
        } else if (v == mAutoBrightnessIv) {
            try {
                Intent autoBrightIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(autoBrightIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.settings_error_text, Toast.LENGTH_SHORT).show();
            }
        } else if (v == mTorchIv) {
            //=============five==================
        } else if (v == mAppSettingsIv) {
        } else if (v == mSystemSettingsIv) {
        } else if (v == mLauncherSettingsIv) {
        } else {
        }
        return true;
    }

    private LightThread mLightThread;

    private class LightThread implements Runnable {

        @Override
        public void run() {
            synchronized (this) {
                if (mIsLightOpen == false) {
                    mFunctionUtils.openTorch();
                    mIsLightOpen = true;
                } else {
                    mFunctionUtils.closeTorch();
                    mIsLightOpen = false;
                }
                mHandler.sendEmptyMessage(MESSAGE_TORCH);
            }
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TORCH:
                    setTorchIv();
                    break;
                case MESSAGE_WIFI_NAME:
                    if (mPhoneStateUtils.isWifiOpen()) {
                        if (!TextUtils.isEmpty(mWifiString)&&mWifiString.length() > 1) {
                            mWifiString = mWifiString.replace("\"", "");
                        }
                        mWifiTv.setText(mWifiString);
                    }
                    break;
                case MESSAGE_DATA:
                    setDataIv();
                    Intent intent = new Intent();
                    intent.setAction(NOTIFICATION_DATA_REFRESH);
                    sendBroadcast(intent);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

    };
    private boolean mIsLightOpen;

    /**
     * 设置模糊背景
     */
    private void applyBlur() {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = null;
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        final RenderScript rs = RenderScript.create(mContext);
                        bitmap = LauncherWallpaperManager.getCurrentWallpaper(mContext);
                        if (bitmap == null) {
                            return null;
                        }
                        float radius = 25;
                        final Allocation input = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
                                Allocation.USAGE_SCRIPT);
                        final Allocation output = Allocation.createTyped(rs, input.getType());
                        final ScriptIntrinsicBlur script;
                        script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                        script.setRadius(radius);
                        script.setInput(input);
                        script.forEach(output);
                        output.copyTo(bitmap);
                        script.destroy();
                        rs.finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (mFlBackground == null || mFlBackground2 == null) {
                    return;
                }
                if (bitmap != null) {
                    mFlBackground.setBackgroundDrawable(new BitmapDrawable(getResources(), bitmap));
                }
                mFlBackground2.setBackgroundResource(R.color.background_black_color);
                ObjectAnimator animator1 = ObjectAnimator.ofFloat(mFlBackground, "alpha", 0f, 1f);
                ObjectAnimator animator2 = ObjectAnimator.ofFloat(mFlBackground2, "alpha", 0f, 1f);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(animator1, animator2);
                animatorSet.setDuration(2000);
                animatorSet.start();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}