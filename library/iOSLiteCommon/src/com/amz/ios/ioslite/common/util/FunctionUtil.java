package com.amz.ios.ioslite.common.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.amz.ios.ioslite.common.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

/**
 * Created by YiYang on 16-10-24.
 */

public class FunctionUtil {
    private static final int CAMERA_REQUEST_CODE = 0x1111;
    private static FunctionUtil mFunctionUtils;
    private final int NO_SIM = 0x111111;
    private final int NO_PERMISSION = 0x111112;
    private PhoneStateUtil mPhoneStateUtils;
    private Context mContext;
    private Camera mCamera;
    private final static String TAG = "FunctionUtil";
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NO_SIM:
                    break;
                case NO_PERMISSION:
                    break;
            }
        }
    };
    private String mCameraId;
    private SensorManager mSensorManager;
    private WifiManager.LocalOnlyHotspotReservation mReservation;

    private FunctionUtil(Context context) {
        mContext = context.getApplicationContext();
        mPhoneStateUtils = PhoneStateUtil.getInstance(mContext);
    }

    public static FunctionUtil getInstance(Context context) {
        if (mFunctionUtils == null) {
            synchronized (FunctionUtil.class) {
                if (mFunctionUtils == null) {
                    mFunctionUtils = new FunctionUtil(context);
                }
            }
        }
        return mFunctionUtils;

    }

    /**
     * 返回到主桌面
     */
    public void backToHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        intent.setPackage(mContext.getPackageName());
        mContext.startActivity(intent);
    }

    /**
     * 开启最近应用列表
     */
    public void showRecentlyApp() {
        Class serviceManagerClass;
        try {
            serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClass.getMethod("getService",
                    String.class);
            IBinder retbinder = (IBinder) getService.invoke(
                    serviceManagerClass, "statusbar");
            Class statusBarClass = Class.forName(retbinder
                    .getInterfaceDescriptor());
            Object statusBarObject = statusBarClass.getClasses()[0].getMethod(
                    "asInterface", IBinder.class).invoke(null,
                    new Object[]{retbinder});
            Method clearAll = statusBarClass.getMethod("toggleRecentApps");
            clearAll.setAccessible(true);
            clearAll.invoke(statusBarObject);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 操作蓝牙
     */
    public void operateBluetooth() {
        if (mPhoneStateUtils.isBlueToochOpen()) {
            mPhoneStateUtils.mBluetoothAdapter.disable();
        } else {
            mPhoneStateUtils.mBluetoothAdapter.enable();
        }
    }

    /**
     * 操作移动数据
     */
    public void operateNetwork() {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                if (mPhoneStateUtils.checkPhoneNet()) {
                    if (mPhoneStateUtils.isMobileDataOpen()) {
                        setMobileDataStatus(false);
                    } else {
                        setMobileDataStatus(true);
                    }
                } else {
                    /*Message message = mHandler.obtainMessage();
                    message.what = NO_SIM;
                    mHandler.sendMessage(message);*/
                    try {
                        Intent data1Intent = new Intent();
                        ComponentName data1ComponentName = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
                        data1Intent.setComponent(data1ComponentName);
                        data1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        data1Intent.putExtra(EXTRA_SHOW_FRAGMENT, "com.android.settings.DataUsageSummary");
                        mContext.startActivity(data1Intent);
                    } catch (Exception e) {
                        Intent dataIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                        dataIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(dataIntent);
                    }
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setMobileDataStatus(boolean enabled) {
        Method setMobileDataEnabl;
        try {
            setMobileDataEnabl = mPhoneStateUtils.mConnectivityManager.getClass().getDeclaredMethod("setMobileDataEnabled", boolean.class);
            setMobileDataEnabl.invoke(mPhoneStateUtils.mConnectivityManager, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                //if ("samsung".equals(android.os.Build.BRAND)) {
                Intent data1Intent = new Intent();
                ComponentName data1ComponentName = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
                data1Intent.setComponent(data1ComponentName);
                data1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                data1Intent.putExtra(EXTRA_SHOW_FRAGMENT, "com.android.settings.DataUsageSummary");
                mContext.startActivity(data1Intent);
                //} else {
                //    Intent dataIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                //    dataIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //    mContext.startActivity(dataIntent);
                //}
            } catch (Exception e2) {
                e2.printStackTrace();
                Message message = mHandler.obtainMessage();
                message.what = NO_PERMISSION;
                mHandler.sendMessage(message);
            }
        }
    }

    /**
     * 操作wifi
     */
    public void operateWifi() {
        if (mPhoneStateUtils.isWifiOpen()) {
            mPhoneStateUtils.mWifiManager.setWifiEnabled(false);
        } else {
            setWifiApEnabled(false);
            mPhoneStateUtils.mWifiManager.setWifiEnabled(true);
        }
    }

    private boolean setWifiApEnabled(boolean enabled) {
        if (requestWriteSetting()) {
            if (enabled) {
                mPhoneStateUtils.mWifiManager.setWifiEnabled(false);
            }
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.N_MR1){
                if(enabled){
                    mPhoneStateUtils.mWifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback(){

                        @Override
                        public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                            super.onStarted(reservation);
                            mReservation = reservation;
                            WifiConfiguration con = reservation.getWifiConfiguration();
                            Log.d(TAG, "Wifi Hotspot is on now:"+con.SSID+"/"+con.preSharedKey);
                        }

                        @Override
                        public void onStopped() {
                            super.onStopped();
                            mReservation = null;
                            Log.d(TAG, "onStopped: ");
                        }

                        @Override
                        public void onFailed(int reason) {
                            super.onFailed(reason);
                            Log.d(TAG, "onFailed: ");
                        }
                    },null);
                    return  true;
                }else {
                    if(mReservation!=null){
                        mReservation.close();
                    }else{
                        try {
                            Method method = mPhoneStateUtils.mWifiManager.getClass().getDeclaredMethod("cancelLocalOnlyHotspotRequest");
                            method.invoke(mPhoneStateUtils.mWifiManager);
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                }
                /*ConnectivityManager connManager = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                try {
                    Field iConnMgrField = connManager.getClass().getDeclaredField("mService");
                    iConnMgrField.setAccessible(true);
                    Object iConnMgr = iConnMgrField.get(connManager);
                    Class<?> iConnMgrClass = Class.forName(iConnMgr.getClass().getName());

                    if(enabled){
                        Method startTethering = iConnMgrClass.getMethod("startTethering", int.class, ResultReceiver.class, boolean.class);
                        startTethering.invoke(iConnMgr, 0, null, true);
                    }else{
                        Method startTethering = iConnMgrClass.getMethod("stopTethering", int.class);
                        startTethering.invoke(iConnMgr, 0);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }*/
            }else {
                try {
                /*WifiConfiguration apConfig = new WifiConfiguration();
                apConfig.SSID = "ioswifi";
                apConfig.preSharedKey = "123456789";*/
                    Method method = mPhoneStateUtils.mWifiManager.getClass().getMethod(
                            "setWifiApEnabled", WifiConfiguration.class, boolean.class);
                    return (Boolean) method.invoke(mPhoneStateUtils.mWifiManager, null, enabled);
                } catch (Exception e) {
                    return false;
                }
            }

        }
        return false;
    }

    /**
     * 操作旋转
     */
    public void operateRotation() {
        if (requestWriteSetting()) {
            if (mPhoneStateUtils.isAutoRatation()) {
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            } else {
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
            }
        }
    }

    /**
     * 操作gps
     */
    public void operateGps() {
        try {
            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            locationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            locationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(locationIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 操作飞行模式
     */
    public void operateFlightMode() {
        try {
            Intent airplaneIntent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            airplaneIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            airplaneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(airplaneIntent);
        } catch (Exception e) {
            try {
                Intent airplaneIntent2 = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                airplaneIntent2.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                airplaneIntent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(airplaneIntent2);
            } catch (Exception e1) {
                e1.printStackTrace();

            }

        }
    }

    /**
     * 打开设置
     */
    public void openSetting() {
        try {
            Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(settingsIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置情景模式
     */
    public void setRingtonMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !mPhoneStateUtils.mNotificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            mContext.startActivity(intent);
        } else {
            int mode = mPhoneStateUtils.getRingerMode();
            if (AudioManager.RINGER_MODE_NORMAL == mode) {
                mPhoneStateUtils.mVibrator.vibrate(350);
                mPhoneStateUtils.mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                mPhoneStateUtils.mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
                mPhoneStateUtils.mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
            } else if (AudioManager.RINGER_MODE_VIBRATE == mode) {
                mPhoneStateUtils.mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                mPhoneStateUtils.mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
                mPhoneStateUtils.mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
            } else {
                mPhoneStateUtils.mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                mPhoneStateUtils.mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
                mPhoneStateUtils.mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
            }
        }
    }

    /**
     * 打开通知栏
     */
    public void OpenNotify() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        try {
            Object service = mContext.getSystemService("statusbar");
            Class<?> statusbarManager = Class
                    .forName("android.app.StatusBarManager");
            Method expand = null;
            if (service != null) {
                if (currentApiVersion <= 16) {
                    expand = statusbarManager.getMethod("expand");
                } else {
                    expand = statusbarManager
                            .getMethod("expandNotificationsPanel");
                }
                expand.setAccessible(true);
                expand.invoke(service);
            }

        } catch (Exception e) {
        }
    }

    /**
     * 锁屏
     */
    public void lockScreen() {
        String packageName = "com.ios.locknow";
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(packageName, "com.ios.locknow.LockNowActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Utilities.copyApkFromAssets(mContext, packageName, "apps/LockNowActivity.apk");
            Utilities.installNewApp(mContext, packageName);
        }
    }

    /**
     * 设置显示
     */
    public void operateDisplay() {
        if (requestWriteSetting()) {
            if (!mPhoneStateUtils.isAutoBrightness()) {
                Settings.System.putInt(mContext.getContentResolver(),
                        SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            } else {
                Settings.System.putInt(mContext.getContentResolver(),
                        SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        }
    }

    /**
     * 打开闹钟
     */
    public void openAlarmas() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName("com.android.deskclock",
                "com.android.deskclock.DeskClock"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            mContext.startActivity(intent);
        } catch (SecurityException e) {
        } catch (ActivityNotFoundException e) {
            List<String> packages = Arrays.asList(mContext.getResources().getStringArray(R.array.clock_packages));
            PackageManager manager = mContext.getPackageManager();
            for (String packageName : packages) {
                try {
                    if ((intent = manager.getLaunchIntentForPackage(packageName)) != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        mContext.startActivity(intent);
                        break;
                    }
                } catch (Exception ex) {
                    continue;
                }
            }

        }
    }

    /**
     * 打开相机
     */
    public void openCamera() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camera.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(camera);
    }

    /**
     * 打开拨号
     */
    public void openCall() {
        Intent call = new Intent(Intent.ACTION_DIAL);
        call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(call);
    }

    /**
     * 打开短信
     */
    public void openMessage() {
        Uri smsToUri = Uri.parse("smsto:");
        Intent chat = new Intent(Intent.ACTION_SENDTO, smsToUri);
        chat.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(chat);
    }

    /**
     * 打开联系人
     */
    public void openContact() {
        Intent contact = new Intent();
        contact.setAction(Intent.ACTION_PICK);
        contact.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        contact.setData(ContactsContract.Contacts.CONTENT_URI);
        mContext.startActivity(contact);
    }

    /**
     * 操作wifi热点
     */
    public void operateWifiAp() {
        if (mPhoneStateUtils.isWifiApOpen()) {
            setWifiApEnabled(false);
        } else {
            setWifiApEnabled(true);
        }
    }

    /**
     * 打开应用管理
     */
    public void openApplicationManager() {
        try {
            Intent maIntent = new Intent("com.ios.ioslite.APP_MANAGER");
            maIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(maIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PhoneStateUtil getPhoneStateUtils() {
        return mPhoneStateUtils;
    }

    /**
     * open torch
     */
    public void openTorch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            operateTorchM(true);
        } else {
            try {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
                mCamera = Camera.open();
                Camera.Parameters param = mCamera.getParameters();
                param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(param);
                mCamera.setPreviewTexture(new SurfaceTexture(0));
                mCamera.startPreview();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * close torch
     */
    public void closeTorch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            operateTorchM(false);
        } else {
            try {
                if (mCamera == null) {
                    mCamera = Camera.open();
                }
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void operateTorchM(boolean open){
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(mCameraId)) {
            Toast.makeText(mContext, "获取不到相机", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            cameraManager.setTorchMode(mCameraId, open);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取手机屏幕最低亮度值
     *
     * @return
     */
    public int getMinBright() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        Class c = pm.getClass();
        try {
            Method method = c.getMethod("getMinimumScreenBrightnessSetting");
            method.setAccessible(true);
            int min = (int) method.invoke(pm);
            return min;
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * 请求write_settings权限
     *
     * @return
     */
    public boolean requestWriteSetting() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(mContext)) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    return false;
                } else {
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean requestCamera(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            return false;
        }
        return true;
    }

    public boolean havaLightSensor() {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mContext.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        }
        boolean havaLightSensor = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);
        Sensor lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        return havaLightSensor && lightSensor != null;
    }
}