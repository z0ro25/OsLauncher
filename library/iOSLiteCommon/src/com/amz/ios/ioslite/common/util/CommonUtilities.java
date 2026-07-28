package com.amz.ios.ioslite.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.amz.ios.ioslite.common.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;


public class CommonUtilities {

    /**
     * 导航栏显示Menu键
     */
    public static void setNeedsMenuKey(Activity activity) {
        if (BuildUtil.ATLEAST_LOLLIPOP_MR1) {
            try {
                Method setNeedsMenuKey = Window.class.getDeclaredMethod("setNeedsMenuKey", int.class);
                setNeedsMenuKey.setAccessible(true);
                int value = WindowManager.LayoutParams.class.getField("NEEDS_MENU_SET_TRUE").getInt(null);
                setNeedsMenuKey.invoke(activity.getWindow(), value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                int flags = WindowManager.LayoutParams.class.getField("FLAG_NEEDS_MENU_KEY").getInt(null);
                activity.getWindow().addFlags(flags);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通用方法关闭释放Closeable对象
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 从assets中读取txt
     */
    public static String getStringFromAssert(Context context, String fileName) {
        BufferedInputStream bis = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bis = new BufferedInputStream(context.getAssets().open(fileName));
            byte[] buffer = new byte[4096];
            int readLen = -1;
            while ((readLen = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            Log.e("", "IOException :" + e.getMessage());
        } finally {
            close(bis);
        }

        return bos.toString();
    }

    /**
     * 启动Activity,捕获所有异常;
     */
    public static void startActivitySafely(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 判断布局方向
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(Resources res) {
        return BuildUtil.ATLEAST_JB_MR1 &&
                (res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }

    public static Intent parseUri(String uri) {
        Intent intent = null;
        if (!TextUtils.isEmpty(uri)) {
            try {
                intent = Intent.parseUri(uri, 0);
            } catch (Exception e) {

            }
        }
        return intent;
    }

    /**
     * 获取Manifest中meta元素值
     */
    public static String getMetaString(Context context, String key) {
        String content = "";
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            content = appInfo.metaData.getString(key);
        } catch (Exception e) {

        }
        return content;
    }


    /**
     * 居中裁剪壁纸
     */
    public static Bitmap cropWallpaperBitmap(Context context, Bitmap bitmap) {
        int sourceWidth = bitmap.getWidth();
        int sourceHeight = bitmap.getHeight();

        int screenHeight = DisplayUtil.getScreenHeightInPx(context);
        int screenWidth = DisplayUtil.getScreenWidthInPx(context);

        int cropX = Math.max(0, (sourceWidth - screenWidth) / 2);
        int cropY = Math.max(0, (sourceHeight - screenHeight) / 2);
        int cropWidth = Math.min(sourceWidth, screenWidth);
        int cropHeight = Math.min(sourceHeight, screenHeight);
        return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight, null, false);
    }

    public static boolean equalLocale(Locale localeA, Locale localeB) {
        return localeA.getLanguage().equals(localeB.getLanguage())
                && localeA.getCountry().equals(localeB.getCountry());
    }

    public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

//    public static void expandStatusBar(Context context) {
//        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
//        try {
//            Object service = context.getSystemService("statusbar");
//            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
//            Method expand = null;
//            if (service != null) {
//                if (currentApiVersion <= 16) {
//                    expand = statusbarManager.getMethod("expand");
//                } else {
//                    expand = statusbarManager
//                            .getMethod("expandNotificationsPanel");
//                }
//                expand.setAccessible(true);
//                expand.invoke(service);
//            }
//
//        } catch (Exception e) {
//            Toast.makeText(context, com.amz.ios.ioslite.common.R.string.settings_error_text, Toast.LENGTH_SHORT).show();
//        }
//    }

    public static void expandStatusBar(Context context) {
        try {
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel",
                    new Class[0]).invoke(context.getSystemService("statusbar"), new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
