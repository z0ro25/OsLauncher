package com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/*
 * create by TRườngNT
 * Email support: truongntforwork@gmail.com
 * */


public class SharePrefUtils {
    //email nhận khi gửi mail qua rate hoặc ....
    public static String email = "trustedapp.help@gmail.com";
    public static String email1 = "";
    //chữ đầu ở body email ở đây nhé (subject + tên app)
    public static String subject = "Feedback ";
    private static SharedPreferences mSharePref;


    //gọi ở aplication hoặc màn splash
    public static void init(Context context) {
        if (mSharePref == null) {
            mSharePref = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    //lấy số lần đếm mở màn LFO
    public static int getCountOpenFirstLang(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataLang", Context.MODE_PRIVATE);
        return pre.getInt("first", 0);
    }

    //tăng số lần đếm mở màn LFO
    public static void increaseCountFirstLang(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataLang", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt("first", pre.getInt("first", 0) + 1);
        editor.apply();
    }

    //kiểm tra xem đã rate chưa
    public static boolean isRated(Context context) {
        SharedPreferences pre = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return pre.getBoolean("rated", false);
    }

    //lấy số lần sử dụng app
    public static int getCountOpenApp(Context context) {
        SharedPreferences pre = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return pre.getInt("counts", 0);
    }

    //tăng số lần sử dụng app
    public static void increaseCountOpenApp(Context context) {
        SharedPreferences pre = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt("counts", pre.getInt("counts", 0) + 1);
        editor.commit();
    }

    //lưu xuống sau khi rate
    public static void forceRated(Context context) {
        SharedPreferences pre = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putBoolean("rated", true);
        editor.commit();
    }


    public static void putString(Context context, String key, String defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putString(key, defaultValue);
        editor.commit();
    }

    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getString(key, defaultValue);
    }

    public static String getString(Context context, String key) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getString(key, "");
    }

    public static void putBoolean(Context context, String key, Boolean defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putBoolean(key, defaultValue);
        editor.commit();
    }

    public static Boolean getBoolean(Context context, String key, Boolean defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getBoolean(key, defaultValue);
    }

    public static Boolean getBoolean(Context context, String key) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getBoolean(key, false);
    }


    public static void putInteger(Context context, String key, int defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt(key, defaultValue);
        editor.commit();
    }

    public static int getInteger(Context context, String key, int defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getInt(key, defaultValue);
    }

    public static int getInteger(Context context, String key) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getInt(key, 0);
    }


    public static void putlong(Context context, String key, long defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putLong(key, defaultValue);
        editor.commit();
    }

    public static Long getLong(Context context, String key, long defaultValue) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getLong(key, defaultValue);
    }

    public static Long getLong(Context context, String key) {
        SharedPreferences pre = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return pre.getLong(key, 0L);
    }
}
