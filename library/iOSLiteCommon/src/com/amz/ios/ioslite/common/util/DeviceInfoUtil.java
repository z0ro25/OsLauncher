package com.amz.ios.ioslite.common.util;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.R;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Properties;

public class DeviceInfoUtil {
    private static final String TAG = "DeviceInfoUtil";

    private static final String IOSOS_CHANNEL_NO_SYSTEM_PROPERTY_NAME = "ro.build.iosos_channel_no";
    private static final String IOSOS_CUSTOMER_NO_SYSTEM_PROPERTY_NAME = "ro.build.iosos_customer_no";
    private static final String IOSOS_PHONE_MODEL_SYSTEM_PROPERTY_NAME = "ro.build.iosos_customer_br";

    private static final String UNKNOWN = "unknown";
    private static final String DEFAULT_IMEI = "012345678912345";
    private static final String DEFAULT_IMSI = "012345678912345";
    private static final String DEFAULT_MCC = "000";

    private static final String KEY_CHANNEL = "cp";
    private static final String KEY_CUSTOMER = "td";
    private static final String CHANNEL_ASSET_FILE = "channel";

    private static String channelId;
    private static String imei = DEFAULT_IMEI;
    private static String imsi = DEFAULT_IMSI;
    private static String mcc = DEFAULT_MCC;
    private static String customerId;
    private static String phoneModel;

    /**
     * 手机品牌
     */
    public static String getBrand() {
        return Build.BRAND;
    }

    /**
     * 读取渠道号按以下顺序：
     * 1) 客户版本，读取配置apk内的渠道配置；
     * 2) 读取应用默认渠道号；
     */
    public static String getChannel(Context context) {
        if (!TextUtils.isEmpty(channelId)) {
            return channelId;
        }
        channelId = getChannelValue(context, KEY_CHANNEL);
        return channelId;
    }

    /**
     * 读取客户号
     */
    public static String getCustomer(Context context) {
        if (!TextUtils.isEmpty(customerId)) {
            return customerId;
        }
        customerId = getChannelValue(context, KEY_CUSTOMER);
        return customerId;
    }


    /**
     * 读取assert下 channel配置文件
     */
    private static String getChannelValue(Context context, String key) {
        String value = "";
        AssetManager assetManager = null;

        //IOSOS,优先读取系统属性
        if (isIOSOs()) {
            if (KEY_CHANNEL.equals(key)) {
                value = getSystemProperty(IOSOS_CHANNEL_NO_SYSTEM_PROPERTY_NAME, null);
            } else if (KEY_CUSTOMER.equals(key)) {
                value = getSystemProperty(IOSOS_CUSTOMER_NO_SYSTEM_PROPERTY_NAME, null);
            }
        }

        //读取配置Apk中的值
        if (TextUtils.isEmpty(value)) {
            Context customContext = Partner.getPartnerContext(context);
            if (customContext != null) {
                assetManager = customContext.getAssets();
                value = getPropertyFormAssert(assetManager, CHANNEL_ASSET_FILE, key);
            }
        }

        // 读取默认值
        if (TextUtils.isEmpty(value)) {
            assetManager = context.getAssets();
            value = getPropertyFormAssert(assetManager, CHANNEL_ASSET_FILE, key);
        }

        return value;
    }

    /**
     * 读取Phone Model
     */
    public static String getPhoneModel() {
        if(phoneModel == null){

            //IOSOS,优先读取系统属性
            if (isIOSOs()) {
                phoneModel = getSystemProperty(IOSOS_PHONE_MODEL_SYSTEM_PROPERTY_NAME, null);
            }

            // 读取默认值
            if (TextUtils.isEmpty(phoneModel)) {
                phoneModel = Build.MODEL;
            }
        }

        return phoneModel;
    }


    private static String getPropertyFormAssert(AssetManager assetManager, String fileName, String key) {
        String value = "";
        Properties prop = new Properties();
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(assetManager.open(fileName));
            prop.load(dis);
            value = prop.getProperty(key, "");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            CommonUtilities.close(dis);
        }
        return value;
    }

    /**
     * 读取 IMEI
     */
    public static String getImei(Context context) {
        if (!imei.equals(DEFAULT_IMEI)) {
            return imei;
        }
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            imei = tm.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(imei)) {
            imei = DEFAULT_IMEI;
        }
        return imei;
    }

    /**
     * 读取 IMSI
     */
    public static String getImsi(Context context) {
        if (!imsi.equals(DEFAULT_IMSI)) {
            return imsi;
        }
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            imsi = tm.getSubscriberId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(imsi)) {
            imsi = DEFAULT_IMSI;
        }
        return imsi;
    }

    /**
     * 读取 MCC
     */
    public static String getMcc(Context context) {
        if (!mcc.equals(DEFAULT_MCC)) {
            return mcc;
        }

        if (!getImsi(context).equals(DEFAULT_IMSI)) {
            mcc = getImsi(context).substring(0, 3);
        }

        mcc = DEFAULT_MCC;
        return mcc;
    }


    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getLocale(Context context) {
        if (BuildUtil.ATLEAST_NOUGAT) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }

    /**
     * 判断是否为mtk平台手机
     */
    public static boolean isMtkPlatform() {
        return getBoardPlatform().toLowerCase().startsWith("mt");
    }

    /**
     * 判断是否为 IOSOS 系统
     */
    public static boolean isIOSOs() {
        return !TextUtils.isEmpty(getSystemProperty("ro.build.iosos_label", ""));
    }


    /**
     * 判断是否为平板
     */
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.is_tablet);
    }

    public static boolean isLargeTablet(Context context) {
        return context.getResources().getBoolean(R.bool.is_large_tablet);
    }
	
	/**
     * 读取系统项目号
     */
	
    public static String getProject() {
        String projectID = "";
        if (!TextUtils.isEmpty(getSystemProperty("ro.build.tyd.custom.hw_version", ""))) {
            projectID = getSystemProperty("ro.build.tyd.custom.hw_version", "");
        } else {
            projectID = "ios";
        }
        return projectID;
    }

    /**
     * 读取系统属性
     */
    public static String getSystemProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, null));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }


    /**
     * 读取系统属性
     */
    public static int getSystemProperty(String key, int def) {
        Integer value = def;
        try {
            Class<?> clss = Class.forName("android.os.SystemProperties");
            Method get = clss.getMethod("getInt", String.class, int.class);
            value = (Integer) get.invoke(clss, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }


    /**
     * 读取广告平台
     */
    private static String getBoardPlatform() {
        return getSystemProperty("ro.board.platform", UNKNOWN);
    }

	/**
     * 读取系统版本号
     */
    public static String getOsVersion() {
        String iosVersion = "";
        try {
            Field iososField = Build.VERSION.class.getDeclaredField("IOSOS");
            iososField.setAccessible(true);
            Build.VERSION v = new Build.VERSION();
            Object o = iososField.get(v);
            iosVersion = o.toString();
            Log.i(TAG, "iosVersion = " + iosVersion);
        } catch (Exception e) {
            return iosVersion;
        }
        return iosVersion;
    }

}
