package com.ios.sc.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;

import com.ios.sc.common.logs.SL_Log;

public class C_C_Util {

    public static final String C_DEVICE_UUID_FILE_NAME = "C_DEVICE_UUID";
    public static final String C_DEVICE_UUID_KEY_NAME = "KEY_DEVICE_UUID";

    public static final String CLEAN_TASK_SERVICE_PACKAGE = "com.sc.cleantask";
    /*public static final String SECURITY_CENTER_PACKAGE = "com.zhuoyi.security.lite";
    public static final String SECURITY_CENTER_PACKAGE    = "com.ios.ioslite";*/

    public static final String LOCK_SCREEN_SHOW_NOTIFICATIONS = "lock_screen_show_notifications";
    public static final String LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS ="lock_screen_allow_private_notifications";
    public static final int lock_screen_notifications_summary_disable = 0;
    public static final int lock_screen_notifications_summary_show = 1;
    public static final int lock_screen_notifications_summary_hide = 2;
    public static final int SECURITY__PERMISSIONS_REQUEST_CODE = 10001;

    private static long lastClickTime = -1 ;
    private static boolean isSystemPermission;
    private static boolean isJudgeInit = false;
    public static boolean isFastMultipleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (timeD > 0 && timeD <= 800) {
            SL_Log.logD("isFastMultipleClick() result = true 800ms");
            return true;
        }
        lastClickTime = time;
        SL_Log.logD("isFastMultipleClick() result = false 800ms");
        return false;
    }

    /**
     * Get system first init ok state
     * @param mCtx
     * @return
     */
    public static boolean systemFirstInitOK(Context mCtx){
        boolean result = Settings.System.getInt(mCtx.getContentResolver(),"ios_oobe_state_finish",1) == 1;
        SL_Log.logD("systemFirstInitOK() result = " + result);
        return result;
    }

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP && C_C_Util.isFastMultipleClick()) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }*/
    
    public static Class<?>[] getParameterTypes (Class<?> mClass,String mMethod){
        Class<?>[] mParamTypes = null;
        Method[] mMethods = mClass.getMethods();
        int length = mMethods.length;
        for(int i=0;i<length;i++){
            Method method = mMethods[i];
            //Logd(Modifier.toString(method.getModifiers()) + " "+method.getReturnType().getName()+" " + method.getName() + "(){}");
            if(mMethod.equals(method.getName())){
                //Logd(Modifier.toString(method.getModifiers()) + " "+method.getReturnType().getName()+" " + method.getName() + "(){}");
                mParamTypes = method.getParameterTypes();
                break;
            }
        }
        return mParamTypes;
    }

    /**
     * For JELLY_BEAN+ interface.
     *
     * @return Is this android 4.12 JELLY_BEAN version ?
     */
    @TargetApi(4) public static boolean isAndroidSdk_api_16_plus() {
        return Build.VERSION.SDK_INT >= 16;/*Build.VERSION_CODES.JELLY_BEAN*/
    }
    
    /**
     * For M+ interface.
     * @return Is this android 6.0 M version ?
     */
    @TargetApi(4) public static boolean isAndroidSdk_api_23_plus() {
        return Build.VERSION.SDK_INT >= 23;/*Build.VERSION_CODES.M*/
    }
   
    @TargetApi(4) public static boolean isAndroidSdk_api_22_plus() {
        return Build.VERSION.SDK_INT >= 22;
    }
    
    /**
     * For L+ interface.
     * @return  Is this android 5.0 L version ?
     */
    @TargetApi(4) public static boolean isAndroidSdk_api_21_plus() {
        return Build.VERSION.SDK_INT >= 21;/*Build.VERSION_CODES.LOLLIPOP*/
    }

    /**
     * For KK+ interface.
     * @return  Is this android 4.4 kk version ?
     */
    @TargetApi(4) public static boolean isAndroidSDK_api_19_plus(){
        return Build.VERSION.SDK_INT >= 19;/*Build.VERSION_CODES.KITKAT*/
    }

    public static void setComponentEnabledSetting(Context mCtx, String packageName, String className, int newState) {
        PackageManager pm = mCtx.getPackageManager();
        ComponentName mComponentName = new ComponentName(packageName, className);
        pm.setComponentEnabledSetting(mComponentName, newState, PackageManager.DONT_KILL_APP);
    }

    public static int getComponentEnabledSetting(Context mCtx, String packageName, String className) {
        PackageManager pm = mCtx.getPackageManager();
        ComponentName mComponentName = new ComponentName(packageName, className);
        return pm.getComponentEnabledSetting(mComponentName);
    }

    /**
     * It is get deivce uuid not random uuid
     * @return get device uuid
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String getDeviceUUID(Context mCtx){
        String data = "";

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(C_DEVICE_UUID_FILE_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        if(sharedPreferences.contains(C_DEVICE_UUID_KEY_NAME) && !TextUtils.isEmpty(sharedPreferences.getString(C_DEVICE_UUID_KEY_NAME,""))){
            data = sharedPreferences.getString(C_DEVICE_UUID_KEY_NAME,"");
            SL_Log.logD("getDeviceUUID from file");
        }else {
            Object result = null, result1 = null, result2 = null;
            try {
                Class<?> classType = Class.forName("android.os.ServiceManager");
                Object invokeOperation = classType.newInstance();
                Method getMethod = classType.getMethod("getService", String.class);
                result = getMethod.invoke(invokeOperation,"TydNativeMisc");

                Class<?> classType1 = Class.forName("com.ios.internal.server.INativeMiscService$Stub");
                Method getMethod1 = classType1.getMethod("asInterface", IBinder.class);
                result1 = getMethod1.invoke(classType1, result);
                Class<?> classType2 = result1.getClass();
                Method getMethod2 = classType2.getMethod("getDeviceUUID");
                result2 = getMethod2.invoke(result1);
            } catch (Exception e) {
                SL_Log.logE("getDeviceUUID:" + e.toString());
            }
            data = (result2 == null) ? "" : String.valueOf(result2);
            if(!TextUtils.isEmpty(data)) {
                sharedPreferences.edit().putString(C_DEVICE_UUID_KEY_NAME,data).apply();
            }
        }
        SL_Log.logD("getDeviceUUID data = " + data);
        return  data;
    }

    /**
     * get System property value for key.
     *
     * @param key
     * @return property key value
     */
    public static String getReflectSystemPropertyValue(String key) {
        String result = "";
        try {
            Class<?> classType = Class.forName("android.os.SystemProperties");
            Object invokeOperation = classType.newInstance();
            Method getMethod = classType.getMethod("get", String.class);
            result = String.valueOf(getMethod.invoke(invokeOperation, new Object[]{new String(key)}));
        } catch (Exception e) {
            SL_Log.logE(e.toString());
        }
        return result;
    }

    /**
     * get app package info for packagename
     *
     * @param pn packageName
     * @param pm packageManager
     * @return packageInfo
     */
    public static PackageInfo GetAppPackageInfo(String pn, PackageManager pm) {
        PackageInfo packInfo = null;
        try {
            packInfo = pm.getPackageInfo(pn, 0);
        } catch (NameNotFoundException e) {
            SL_Log.logE(e.toString());
        }
        return packInfo;
    }

    /**
     * get is show secure item
     *
     * @param mContext
     * @return isSecure
     */
    public static boolean isSecure(Context mContext) {
        boolean result = false;
        try {
            Class<?> classType = Class.forName("com.android.internal.widget.LockPatternUtils");
            Constructor constructor = classType.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            Object obj = constructor.newInstance(mContext);
            Method isSecure = obj.getClass().getMethod("isSecure");
            result = Boolean.valueOf(String.valueOf(isSecure.invoke(obj)));
        } catch (Exception e) {
            SL_Log.logE("isSecure error:" + e.toString());
        }
        SL_Log.logI("isSecure result = " + result);
        return result;
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public static void setNotificationState(Context mContext, boolean isShow, boolean isEnable) {
        Settings.Secure.putInt(mContext.getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, isShow ? 1 : 0);
        Settings.Secure.putInt(mContext.getContentResolver(), LOCK_SCREEN_SHOW_NOTIFICATIONS, isEnable ? 1 : 0);
    }

    /**
     * lock_screen_notifications_summary_disable
     * lock_screen_notifications_summary_show
     * lock_screen_notifications_summary_hide
     *
     * @param mContext
     * @param mSecure
     */
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public static int getNotificationState(Context mContext, boolean mSecure) {
        final boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(), LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
        final boolean ap = Settings.Secure.getInt(mContext.getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
        final boolean allowPrivate = !mSecure || ap;
        int result = !enabled ? lock_screen_notifications_summary_disable : allowPrivate ? lock_screen_notifications_summary_show : lock_screen_notifications_summary_hide;
        SL_Log.logI("getNotificationState() result = " + result);
        return result;
    }

    @TargetApi(Build.VERSION_CODES.DONUT)
    public static boolean isInstallSystemAppFile(Context context){
        if (isJudgeInit) {
            return isSystemPermission;
        } else {
            ApplicationInfo appInfo = context.getApplicationInfo();
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                isSystemPermission = false;
            }else{
                isSystemPermission = true;
            }
            isJudgeInit = true;

        }
        return isSystemPermission;


    }
    @TargetApi(4) public static boolean isJudgeChangeNetWorkPermission(Context context) {


        if(Build.VERSION.SDK_INT >= 21){
            boolean permission = (PackageManager.PERMISSION_GRANTED ==
                    context.getPackageManager().checkPermission("android.permission.MODIFY_PHONE_STATE", context.getPackageName()));
            if (permission) {
                return true;
            }else {
                return false;
            }
        }else{
            return true;
        }
    }

    public static String getPermissionGroupNameByPermissionName(List<String> permissioms, final Activity activity) {
        PackageManager pm = activity.getPackageManager();
        List<String> pgns = new ArrayList<String>();
        String str = "";
        try {
            int length = permissioms.size();
            for (int i = 0; i < length; i++) {
                PermissionInfo pi = pm.getPermissionInfo(permissioms.get(i),0);
                PermissionGroupInfo pgi = pm.getPermissionGroupInfo(pi.group,0);
                if (pgi != null && !pgns.contains(pi.group)) {
                    pgns.add(pi.group);
                    str += pgi.loadLabel(pm).toString()+",";
                }
            }
            if (str != null && str.length() > 0) {
                str = str.substring(0,str.length()-1);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            str = "required";
        }
        return str;
    }
}
