package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * 1. Get general information about a display, such as its
 * size, density, and font scaling.
 * <p/>
 * 2. Unis Trasform : px2dip, dip2px, px2sp;
 */

public class DisplayUtil {
    private static int sScreenHeightPx;
    private static int sScreenWidthPx;

    /**
     * Get the absolute height of the available display size in pixels.
     */
    public static int getScreenHeightInPx(Context context) {
        if (sScreenHeightPx > 0) {
            return sScreenHeightPx;
        }
        sScreenHeightPx = context.getResources().getDisplayMetrics().heightPixels;
        return sScreenHeightPx;
    }

    /**
     * Get the  absolute width of the available display size in pixels.
     */
    public static int getScreenWidthInPx(Context context) {
        if (sScreenWidthPx > 0) {
            return sScreenWidthPx;
        }
        sScreenWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        return sScreenWidthPx;
    }

    /**
     * Get the absolute height of the available display size in dps.
     */
    public static int getScreenHeightInDp(Context context) {
        return px2dip(context, getScreenHeightInPx(context));
    }

    /**
     * Get the  absolute width of the available display size in dps.
     */
    public static int getScreenWidthInDp(Context context) {
        return px2dip(context, getScreenWidthInPx(context));
    }

    /**
     * Transform px to  dpi;
     * <p/>
     * Note :  1px, density=1.5,  (int) (pxValue / scale) is 0 ;
     * So, should add 0.5 offset;
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * Transform dpi to px;
     * <p/>
     * Note :  1dp, density=0.75,  (int) (dipValue * scale ) is 0 ;
     * So, should add 0.5 offset;
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * Transform px to sp;
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    public static int getStatusHeight(Context context) {

        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    /**
     * 获取屏幕高度（包括状态栏和导航栏）
     *
     * @param context
     * @return
     */
    public static int getRealScreenHeightPx(Context context) {
        int height = 0;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(dm);
            height = dm.heightPixels;
        } else {
            @SuppressWarnings("rawtypes")
            Class c;
            try {
                c = Class.forName("android.view.Display");
                @SuppressWarnings("unchecked")
                Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
                method.invoke(display, dm);
                height = dm.heightPixels;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return height;
    }

    //获取导航栏高度
    public static int getNavigationbarHeight(Context context){
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }
}
