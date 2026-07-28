package com.zhuoyi.security.batterysave.util;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by tangxiaohui on 2016/8/17.
 */

public class BS_SettingsUtil {
    private static String TAG = "BS_SettingsUtil";
    public static boolean gpsCheck(Context context) {
        String str = Settings.Secure.getString(context.getContentResolver(), "location_providers_allowed");
        return (str != null) && (str.contains("gps"));
    }
    public static boolean gpsClick(Context context) {
        Intent localIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(localIntent);
            return true;
        } catch (Exception localException) {
            localException.printStackTrace();
        }
        return false;
    }
    
    public static boolean bluetoothCheck() {
        BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();
        if(blueadapter != null){
            return blueadapter.isEnabled();
        }
        return false;
    }
    public static boolean bluetoothClick(Context context) {
        BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();
        if(blueadapter != null){
            boolean stat = blueadapter.isEnabled();
            if (stat) {
                blueadapter.disable();
                return false;
            } else {
                blueadapter.enable();
                return true;
            }
        }
        return false;
    }
    
    public static boolean getMobileDataState(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class cmClass = cm.getClass();
        Class[] argClasses = null;
        Object[] argObject = null;
        Boolean isOpen = false;
        try {
//            Method method = cmClass.getMethod("getMobileDataEnabled", argClasses);
            Method method = cm.getClass().getMethod("getMobileDataEnabled");
            method.setAccessible(true);
            isOpen = (Boolean)method.invoke(cm, argObject);
        } catch(Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "isOpen="+isOpen);
        return isOpen;
    }
    public static boolean dataClick(Context context) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
            context.startActivity(localIntent);
        } catch (Exception localException) {}
        return getMobileDataState(context);
    }

    public static boolean rotationCheck(Context context) {
        int i1 = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        boolean bool = false;
        if (i1 != 0) {
            bool = true;
        }
        return bool;
    }
    public static boolean rotationClick(Context context) {
        int stat = rotationCheck(context) == true ? 0 : 1;
        try {
            ContentResolver localContentResolver = context.getContentResolver();
            setSettingsValue(context, localContentResolver, Settings.System.ACCELEROMETER_ROTATION, stat);
        } catch (Exception e) {
            Log.e(TAG,"11i1="+e.toString());
        }
        return stat == 1 ? true : false;
    }

    public static boolean brightnessCheck(Context context) {
        int briStat = getSwitcherSet(context, "brightness_state");
        return briStat == 0;
    }
    @TargetApi(23)
    public static boolean brightnessClick(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(context)) {
                    return setBrightnessMode(context);
                } else {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } else {
                return setBrightnessMode(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private static boolean setBrightnessMode(Context context) {
        try {
            boolean state = brightnessCheck(context);
            int screenMode, screenBrightness;
            ContentResolver contentResolver = context.getContentResolver();
            if (state) {
                screenMode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
                screenBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS);
                setSwitcherSet(context, "brightness_mode", screenMode);
                setSwitcherSet(context, "screen_brightness", screenBrightness);
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 36);
                setSwitcherSet(context, "brightness_state", 1);
                return false;
            } else {
                screenMode = getSwitcherSet(context, "brightness_mode");
                screenBrightness = getSwitcherSet(context, "screen_brightness");
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, screenMode);
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
                setSwitcherSet(context, "brightness_state", 0);
                return true;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean timeCheck(Context context) {
        int m = Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, -1) / 1000;
        return m != 15;
    }

    public static boolean timeClick(Context context) {
        int m = Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, -1);
        ContentResolver contentResolver = context.getContentResolver();
        if (m == 15000) {
            int time = getSwitcherSet(context, "time_off");
            if (time == 0) {
                time = 30000;
                setSwitcherSet(context, "time_off", m);
            }
            setSettingsValue(context, contentResolver, android.provider.Settings.System.SCREEN_OFF_TIMEOUT, time);
            return time != 15000;
        } else {
            setSwitcherSet(context, "time_off", m);
            setSettingsValue(context, contentResolver, android.provider.Settings.System.SCREEN_OFF_TIMEOUT, 15000);
            return false;
        }
    }
    
    public static int getRingerMode(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio.getRingerMode();
    }
    public static void ringerClick(Context context, int state) {
        AudioManager localAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = localAudioManager.getRingerMode();
        boolean paramBoolean1, paramBoolean2;
        if(state == 1) {
            setSwitcherSet(context, "audio_mode_vibrate", mode);
            paramBoolean1 = getSwitcherSet(context, "switcher_ringer") == 1;
            paramBoolean2 = !(getSwitcherSet(context, "switcher_vibrate") == 1);
            setSwitcherSet(context, "switcher_vibrate", paramBoolean2 == true ? 1 : 0);
        } else {
            setSwitcherSet(context, "audio_mode_ringer", mode);
            paramBoolean1 = !(getSwitcherSet(context, "switcher_ringer") == 1);
            paramBoolean2 = getSwitcherSet(context, "switcher_vibrate") == 1;
            setSwitcherSet(context, "switcher_ringer", paramBoolean1 == true ? 1 : 0);
        }
        Log.e(TAG, "paramBoolean1="+paramBoolean1+" paramBoolean2="+paramBoolean2);
        if ((paramBoolean1) && (paramBoolean2)) {
            localAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            return;
        }
        if ((paramBoolean1) && (!paramBoolean2)) {
            localAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            return;
        }
        if ((!paramBoolean1) && (paramBoolean2)) {
            localAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            return;
        }
        if ((!paramBoolean1) && (!paramBoolean2)) {
            localAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            return;
        }
        localAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    public static boolean vibrateCheck(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED) == 1;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean vibrateClick(Context context) {
        boolean state = !vibrateCheck(context);
        ContentResolver localContentResolver = context.getContentResolver();
        try {
            setSettingsValue(context, localContentResolver, android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, state == true ? 1 : 0);
        } catch (Exception e) {
            Log.e(TAG, "vibrateClick err="+e.getMessage());
        }
        return state;
    }

    public static boolean isSyncSwitchOn(Context context) {
        return ContentResolver.getMasterSyncAutomatically();
    }
    public static boolean syncSwitchUtils(Context context) {
        ContentResolver.setMasterSyncAutomatically(!isSyncSwitchOn(context));
        return isSyncSwitchOn(context);
    }
    
    public static boolean wifiCheck(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return wifiManager.isWifiEnabled();
        }
        return false;
    }
    public static boolean wifiClick(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
                return false;
            } else {
                wifiManager.setWifiEnabled(true);
                return true;
            }
        }
        return false;
    }

    public static boolean isWifiApEnabled(Context context) {
        return getWifiApState(context) == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
    }
    private static WIFI_AP_STATE getWifiApState(Context context){
        int tmp;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            method.setAccessible(true);
            tmp = ((Integer) method.invoke(wifiManager));
            // Fix for Android 4
            if (tmp > 10) {
                tmp = tmp - 10;
            }
            return WIFI_AP_STATE.class.getEnumConstants()[tmp];
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }
    }
    enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING, WIFI_AP_STATE_DISABLED, WIFI_AP_STATE_ENABLING,  WIFI_AP_STATE_ENABLED, WIFI_AP_STATE_FAILED
    }
    public static boolean setWifiApEnabled(Context context) {
        boolean enabled = !isWifiApEnabled(context);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (enabled) { 
             if (wifiManager != null) {
                 setSwitcherSet(context, "wifi_state", wifiManager.isWifiEnabled() == true ? 1 : 0);
                 wifiManager.setWifiEnabled(false);
             }
        }
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(wifiManager, null, enabled);
        } catch (Exception e) {
            Log.e(TAG,"setWifiApEnabled err="+e.getMessage());
        }
        if (!enabled) {
            boolean state = (getSwitcherSet(context, "wifi_state") == 1);
            Log.e(TAG,"wifi_state="+state);
            wifiManager.setWifiEnabled(state);
        }
        return enabled;
    }


    @TargetApi(23) private static void setSettingsValue(Context context, ContentResolver cr, String key, int value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(cr, key, value);
            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } else {
            Settings.System.putInt(cr, key, value);
        }
    }
    public static int getSwitcherSet(Context context, String keyName) {
        SharedPreferences sp = context.getSharedPreferences("SWITCHER_SETTINGS", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        return sp.getInt(keyName, 0);
    }
    public static void setSwitcherSet(Context context, String keyName, int packName){
        SharedPreferences sp = context.getSharedPreferences("SWITCHER_SETTINGS", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor et = sp.edit();
        et.putInt(keyName, packName);
        et.commit();
    }
}
