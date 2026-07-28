package com.amz.ios.themeclub.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

import com.amz.ios.ioslite.common.debug.DebugLog;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ubuntu on 20/06/17.
 */

public class IOSResources {

    static final String TAG = IOSResources.class.getSimpleName();
    private static final String THEME_DESCRIPTION_PATH = "description.xml";
    private static final String DEFAULT_THEME_PATH = "/system/framework/framework-res.apk";
    public static String sThemePath = DEFAULT_THEME_PATH;
    private static final String THEME_PREVIEW_FOLDER_PREFIX = "preview";
    public static final String THEME_PREVIEW_SUFFIX = ".jpg";
    public static final String THEME_PREVIEW_LOCKSCREEN_THUMB = "theme_preview_lockscreen_thumb";
    public static final String THEME_PREVIEW_LOCKSCREEN = "theme_preview_lockscreen";


    String densityString = "mdpi";
    public IOSResources() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int density = displayMetrics.densityDpi;
        densityString = "mdpi";
        if (density >= 120 && density < 160) {
            densityString = "ldpi";
        } else if (density >= 160 && density < 240) {
            densityString = "mdpi";
        } else if (density >= 240 && density < 320) {
            densityString = "hdpi";
        } else if (density >= 320 && density < 480) {
            densityString = "xhdpi";
        } else if (density >= 480) {
            densityString = "xxhdpi";
        }
    }

    public BitmapDrawable getThemePreview(Context context, String packageName, String themePath, String previewType) {
        String fileName = previewType.contains(".") ? previewType : previewType + THEME_PREVIEW_SUFFIX;
        DebugLog.w(TAG, "================getThemePreview:" + fileName);
        BitmapDrawable dr = getThemeImage(context, packageName, themePath, fileName);
        return dr;
    }

    public BitmapDrawable getThemeImage(Context context, String packageName, String themePath, String fileName) {
        BitmapDrawable dr = null;

        if (android.text.TextUtils.isEmpty(themePath))
            themePath = sThemePath;

        Context mRemoteContext;
        try {
            mRemoteContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            AssetManager am = mRemoteContext.getAssets();
            InputStream is = null;
            Bitmap bitmap = null;
            try {
                if (packageName.equals("android")) {
                    is = am.open(THEME_PREVIEW_FOLDER_PREFIX + "-" + densityString + "/" + fileName);
                } else {
                    is = am.open("assets/" + THEME_PREVIEW_FOLDER_PREFIX + "-" + densityString + "/" + fileName);
                }
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    if (bitmap == null) {
                        // is =
                        // am.open(THEME_PREVIEW_FOLDER_PREFIX+"-hdpi-960x540"+"/"
                        // +fileName);
                        is = am.open(THEME_PREVIEW_FOLDER_PREFIX + "-hdpi-960x540" + "/" + fileName);
                        bitmap = BitmapFactory.decodeStream(is);
                    }

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
            if (is != null) {
                is.close();
            }
            if (bitmap != null)
                dr = new BitmapDrawable(Resources.getSystem(), bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dr;
    }

}
