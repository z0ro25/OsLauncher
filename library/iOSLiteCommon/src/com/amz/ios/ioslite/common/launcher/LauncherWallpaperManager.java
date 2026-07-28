package com.amz.ios.ioslite.common.launcher;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.DisplayUtil;

/**
 * Created by server on 17-9-28.
 */

public class LauncherWallpaperManager {

    private static int sScreenCount = 1;
    private static int sScreenCurrentPosition;

    public static void setScreenCount(int screenCount) {
        LauncherWallpaperManager.sScreenCount = screenCount;
    }

    public static void setScreenCurrentPosition(int screenCurrentPosition) {
        LauncherWallpaperManager.sScreenCurrentPosition = screenCurrentPosition;
    }

    /**
     * 获取当前壁纸
     *
     * @param context
     * @return
     */
    public static Bitmap getCurrentWallpaper(Context context) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        Bitmap bkg = ((BitmapDrawable) wallpaperDrawable).getBitmap();
        if (bkg == null || bkg.isRecycled()) {
            return null;
        }
        int width = DisplayUtil.getScreenWidthInPx(context);
        int height = DisplayUtil.getRealScreenHeightPx(context);
        int x = 0, y = 0;
        float widthScale = bkg.getWidth() / (width * 1f);
        float heightScale = bkg.getHeight() / (height * 1f);
        boolean isScroll = IOSSettings.getBoolean(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, Partner.getBoolean(context, Partner.DEF_WALLPAPER_SCROLL_ENABLED));
        if(widthScale == heightScale){
            return bkg;
        }else if(widthScale > heightScale){
            height = bkg.getHeight();
            int realWidth = (int) (width * heightScale);
            x= isScroll ? 0 : (bkg.getWidth() - realWidth) / 2;
            width = realWidth;
        }else {
            width = bkg.getWidth();
            int realHeight = (int) (height * widthScale);
            y = (bkg.getHeight() - realHeight) / 2;
            height = realHeight;
        }
        int pagerCount = sScreenCount;
        int currentPagerPosition = isScroll ? sScreenCurrentPosition : 0;
        float step = pagerCount <= 1 ? 0 : (bkg.getWidth() - width / (pagerCount - 1));
        bkg = Bitmap.createBitmap(bkg, (int) (currentPagerPosition * step) + x, y, width, height);
        return bkg;
    }
}
