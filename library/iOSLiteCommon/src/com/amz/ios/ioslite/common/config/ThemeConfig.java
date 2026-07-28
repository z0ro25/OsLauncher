package com.amz.ios.ioslite.common.config;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.PackageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ThemeConfig {
    private static final String TAG = "ThemeConfig";
    private static final String PROP_WALLPAPER = "ro.config.wallpaper";

    //  获取默认主题包名；
    public static final String getDefaultThemePkg() {
        String pkg;
        Context context = CommonSdk.getApplicationContext();
        if (BuildUtil.isCustomerBuild()) {
            pkg = Partner.getString(context, Partner.DEF_APPLY_THEME_PACKAGE);
            if (!TextUtils.isEmpty(pkg)) {
                if (PackageUtil.isAppInstalled(context, pkg)) {
                    return pkg;
                }
            }
        }

        return context.getPackageName();
    }

    public static boolean isLiveWallpaperEnable() {
        return Partner.getBoolean(CommonSdk.getApplicationContext(), Partner.FEATURE_LIVE_WALLPAPER_ENABLE);
    }

    public static Bitmap readSystemDefaultWallpaper(Context context) {
        final String path = DeviceInfoUtil.getSystemProperty(PROP_WALLPAPER, "");
        Bitmap bitmap = null;

        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    InputStream is = new FileInputStream(file);
                    bitmap = BitmapFactory.decodeStream(is);
                } catch (Exception e) {
                    Log.e(TAG, "readSystemDefaultWallpaper fail", e);
                }
            } else {
                Log.i(TAG, "wallpaper file not exit");
            }
        }
        return bitmap;
    }

    public static boolean isLockScreenEnable() {
        //return true;
        return Partner.getBoolean(CommonSdk.getApplicationContext(), Partner.FEATURE_LOCK_SCREEN_ENABLE);
    }
}
