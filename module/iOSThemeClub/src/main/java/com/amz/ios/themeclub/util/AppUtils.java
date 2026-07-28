package com.amz.ios.themeclub.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.LockScreenBean;
import com.amz.ios.themeclub.bean.LockScreenNewestBean;
import com.amz.ios.themeclub.bean.ThemeNewestBean;
import com.amz.ios.themeclub.bean.ThemeSelectionPieceBean;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.bean.WallPaperJsonBean;
import com.amz.ios.themeclub.bean.WallPaperSelectionPieceBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by ZhangMingZhe on 11/17/16.
 */

public class AppUtils {
    private static final String TAG = "AppUtils";
    private static final String DROI_APP_MARKET_TAG = "FromTydKeDouMarket";
    private static final String FILE_PATH_AUTHOITY = "com.amz.ios.themeclub.file.provider";
    private static final String INSTALL_TYPE = "application/vnd.android.package-archive";

    //获取屏幕分辨率
    public static String getLCD(Context context,boolean isTheme) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeigh = dm.heightPixels;
        if(isTheme){
            return String.valueOf(screenHeigh).concat("x").concat(String.valueOf(screenWidth));
        }else{
            return String.valueOf(screenHeigh).concat("x").concat(String.valueOf(screenWidth*2));
        }
    }

    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        int heightPixels;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        heightPixels = metrics.heightPixels;
        // includes window decorations (statusbar bar/navigation bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                heightPixels = (Integer) Display.class
                        .getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
            // includes window decorations (statusbar bar/navigation bar)
        else if (Build.VERSION.SDK_INT >= 17)
            try {
                android.graphics.Point realSize = new android.graphics.Point();
                Display.class.getMethod("getRealSize",
                        android.graphics.Point.class).invoke(d, realSize);
                heightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        return heightPixels;
    }


    public static int getStatusHeight(Context context) {
        int statusHeight = -1;
        try
        {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return statusHeight;
    }

    //获取版本号
    public static String getVersion(Context context)
    {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return context.getString(R.string.themeclub_version_unknown);
        }
    }
    //获取语言
    public static String getLanguage(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        return locale.toString();
    }
    //获取sd卡路径
    public static String getSDPath() {
        String sdPath = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return sdPath;
    }

    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if(grantResults.length < 1){
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static WallPaperJsonBean spliteWallPaperNewest(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response.toString());
        String body = obj.getString("body");
        Type type = new TypeToken<WallPaperJsonBean>(){}.getType();
        return GsonUtils.getGsonInstance().fromJson(body,type);
    }


    public static WallPaperSelectionPieceBean spliteWallPaperSelection(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response);
        String body = obj.getString("body");
        Type type = new TypeToken<WallPaperSelectionPieceBean>(){}.getType();
        return GsonUtils.getGsonInstance().fromJson(body,type);
    }

    public static ThemeNewestBean spliteThemeNewest(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response);
        String body = obj.getString("body");
        Type type = new TypeToken<ThemeNewestBean>(){}.getType();
        return GsonUtils.getGsonInstance().fromJson(body,type);
    }

    public static LockScreenNewestBean spliteLockScreenNewest(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response);
        String body = obj.getString("body");
        Type arrayType = new TypeToken<LockScreenNewestBean>() {
        }.getType();
        return GsonUtils.getGsonInstance().fromJson(body,arrayType);
    }

    public static ArrayList<LockScreenBean> splitLockScreenBeanData(String result) throws JSONException {
        JSONObject obj = new JSONObject(result);
        String body = obj.getString("body");
        String lock = new JSONObject(body).getString("screens");
        Type arrayType = new TypeToken<ArrayList<LockScreenBean>>(){}.getType();
        ArrayList<LockScreenBean> list = new ArrayList<>();
        list = GsonUtils.getGsonInstance().fromJson(lock,arrayType);
        return list;
    }

    public static ThemesBean spliteOneTheme(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response);
        String body = obj.getString("body");
        JSONObject obj1 = new JSONObject(body);
        String theme = obj1.getString("theme");
//        Type type = new TypeToken<ThemesBean>(){}.getType();
        return GsonUtils.getGsonInstance().fromJson(theme,ThemesBean.class);
    }

    public static ThemeSelectionPieceBean spliteThemeSelection(String string) throws JSONException {
        if(TextUtils.isEmpty(string)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(string);
        String body = obj.getString("body");
        Type type = new TypeToken<ThemeSelectionPieceBean>(){}.getType();
        return GsonUtils.getGsonInstance().fromJson(body,type);
    }

    public static ArrayList<ThemesBean> getCommonTheme(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response.toString());
        String body = obj.getString("body");
        JSONObject themePark = new JSONObject(body);
        String themes = themePark.getString("themePack");
        JSONObject themeBean = new JSONObject(themes);
        String theme = themeBean.getString("themes");
        Type type = new TypeToken<ArrayList<ThemesBean>>(){}.getType();
        return GsonUtils.getGsonInstance().fromJson(theme,type);
    }

    public static ArrayList<WallPapersBean> getCommonWallpapers(String response) throws JSONException {
        if(TextUtils.isEmpty(response)){
            throw new RuntimeException("response can't be null");
        }
        JSONObject obj = new JSONObject(response.toString());
        String body = obj.getString("body");
        JSONObject wallpaperPark = new JSONObject(body);
        String wallpapers = wallpaperPark.getString("wallPaperPack");
        Log.e("ldq", "wallpaper =  + " + wallpapers);
        JSONObject themeBean = new JSONObject(wallpapers);
        String wallpaper = themeBean.getString("wallPapers");
        Log.e("ldq", "wallpaper =  + " + wallpaper);
        Type type = new TypeToken<ArrayList<WallPapersBean>>(){}.getType();
        return new Gson().fromJson(wallpaper, type);
    }

    public static void AppInstall(String filePath, Context act) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }

        File f = null;
        try {
            f = new File(filePath);
            if (null == f || !f.exists()) {
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "===========AppInstall error:" + e);
            e.printStackTrace();
        }
        Intent i = new Intent();
        Uri data;
        if (BuildUtil.ATLEAST_NOUGAT){
            data = FileProvider.getUriForFile(act, FILE_PATH_AUTHOITY, f);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }else {
            data = Uri.fromFile(f);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        i.setAction(android.content.Intent.ACTION_VIEW);
        i.putExtra(DROI_APP_MARKET_TAG, true);
        i.setDataAndType(data, INSTALL_TYPE);
        act.startActivity(i);
    }

    public static void deleteApkFile(String filePath){
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File f = null;
        try {
            f = new File(filePath);
            if (null == f || !f.exists()) {
                return;
            }
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否横竖屏
     * @param context
     * @return true:平板,false:手机
     */
    public static boolean isTabletDevice(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean checkInstalled(Context context, String pName) {
        DebugLog.w(TAG, "================checkInstalled:" + pName);
        PackageInfo packageInfo;
        if (pName == null) {
            return false;
        } else {
            try {
                packageInfo = context.getPackageManager().getPackageInfo(pName,0);
            } catch (Exception e) {
                Log.e("OnlineTheme","01" + e.getMessage());
                packageInfo = null;
            }
            if (packageInfo == null) {
                return false;
            }else{
                return true;
            }
        }
    }

    public static boolean fileIsExists(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean fileIsExists(String path,long size) {
        try {
            File f = new File(path);
            if (f.exists()&& f.length() == size) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static void immersive(Activity activity){
        if(Build.VERSION.SDK_INT >Build.VERSION_CODES.LOLLIPOP_MR1) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    //获取状态栏高度
    public static int getStatusBarHeight(Context context) {
        int statusBarHeight2 = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusBarHeight2 = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight2;
    }

    /*
    * change statusbar style ,must be called before setContentView()
    * */
    public static void changeStatusBarStyle(Activity activity) {
        Window window = activity.getWindow();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
            }
        }
    }

    public static boolean checkIfDownSuccessByMD5(String MD5, File file) {
        try {
            String fileMD5 = FileMD5Util.getFileMD5String(file);
            Log.w(TAG, "================checkIfDownSuccessByMD5:" + fileMD5);
            if (fileMD5.equals(MD5)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "checkIfDownSuccessByMD5 error============ " + e);
        }
        return false;
    }
}
