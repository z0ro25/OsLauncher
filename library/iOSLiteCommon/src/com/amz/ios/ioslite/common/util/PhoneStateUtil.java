package com.amz.ios.ioslite.common.util;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

import static android.content.Context.TELEPHONY_SERVICE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

/**
 * Created by YiYang on 16-10-18.
 */

public class PhoneStateUtil {
    private Context mContext;
    private static PhoneStateUtil mUtils;
    public WifiManager mWifiManager;
    public NotificationManager mNotificationManager;
    public AudioManager mAudioManager;
    public BluetoothAdapter mBluetoothAdapter;
    public ConnectivityManager mConnectivityManager;
    public TelephonyManager mTelephonyManager;
    public LocationManager mLocationManager;
    public Vibrator mVibrator;

    private PhoneStateUtil(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Activity.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mVibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static PhoneStateUtil getInstance(Context context) {
        if (mUtils == null) {
            synchronized (PhoneStateUtil.class) {
                if (mUtils == null) {
                    mUtils = new PhoneStateUtil(context);
                }
            }
        }
        return mUtils;
    }

    public boolean checkPhoneNet() {
        return mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }

    public boolean isAlplaneMode() {
        boolean isAlplaneMode = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isAlplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
        return isAlplaneMode;
    }

    public boolean isMobileDataOpen() {
        String methodName = "getMobileDataEnabled";
        Class cmClass = mConnectivityManager.getClass();
        Boolean isOpen = false;
        try {
            Method method = cmClass.getMethod(methodName);
            method.setAccessible(true);
            isOpen = (Boolean) method.invoke(mConnectivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOpen;
    }

    public boolean isBlueToochOpen() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }


    public boolean isWifiOpen() {
        return mWifiManager.isWifiEnabled();
    }

    public boolean isAutoRatation() {
        boolean mode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        return mode;
    }

    public boolean isAutoBrightness() {
        int mode = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        return SCREEN_BRIGHTNESS_MODE_MANUAL != mode;
    }

    public int getRingerMode() {
        return mAudioManager.getRingerMode();
    }

    public boolean isLocationModeOpen() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isWifiApOpen() {
        int i = 0;
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");
            i = (Integer) method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
            i = -1;
        }
        return i == 13;

    }
}
