package com.amz.ios.themeclub.bean;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.util.IOSResources;

import java.io.Serializable;

/**
 * Created by ubuntu on 20/06/17.
 */

public class LockscreenInfo implements Serializable {

    // private static final String LOCKSCREEN_PREVIEW = "lockscreen_preview";
    private static final String LOCKSCREEN_TITLE = "lockscreen_title";
    private static final String LOCKSCREEN_WALLPAPER = "default_wallpaper_lockscreen";

    private static final String FRAMEWORK_PACKAGE_NAME = "com.oslauncher.applauncher.themelauncher";

    private Context mContext;
    private String mPackagePath;
    private String mPackageName;
    private String mTitle;

    private Context mPackageContext;
    private int mLockscreenWallpaperId = 0;

    private boolean mLockscreenPackageFlag = true;


    public LockscreenInfo() {
    }

    public LockscreenInfo(Context context, String name, String packageName, String packagePath) {
        mContext = context;
        mPackageName = packageName;
        mPackagePath = packagePath;
        mTitle= name;
        getPackageInfo();
    }

    public BitmapDrawable getPreview() {
        IOSResources res = new IOSResources();
        return res.getThemePreview(mContext, mPackageName, mPackagePath, IOSResources.THEME_PREVIEW_LOCKSCREEN);
    }

    public BitmapDrawable getPreviewThumb() {
        IOSResources res = new IOSResources();
        return res.getThemePreview(mContext, mPackageName, mPackagePath, IOSResources.THEME_PREVIEW_LOCKSCREEN_THUMB);
    }

    public String getTitle() {
        return mTitle;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public boolean isLockscreenPackage() {
        return this.mLockscreenPackageFlag;
    }

    public void setTitle(String paramString) {
        this.mTitle = paramString;
    }

    public void setPackageName(String paramString) {
        this.mPackageName = paramString;
    }

    public String getPackagePath() {
        return mPackagePath;
    }

    public void setPackagePath(String packagePath) {
        this.mPackagePath = packagePath;
    }

    private void getPackageInfo() {
        try {
            mTitle = android.os.Build.MODEL;
            if (FRAMEWORK_PACKAGE_NAME.equals(mPackageName))
                return;

            mPackageContext = mContext.createPackageContext(mPackageName, Context.CONTEXT_IGNORE_SECURITY);
            int titleId = mPackageContext.getResources().getIdentifier(LOCKSCREEN_TITLE, "string", mPackageName);

            mLockscreenWallpaperId = mPackageContext.getResources().getIdentifier(LOCKSCREEN_WALLPAPER, "drawable",
                    mPackageName);

            mTitle = mPackageContext.getResources().getString(titleId);

        } catch (Exception e) {
            mLockscreenPackageFlag = false;
        }
    }

    public Bitmap getLockscreenWallpaper(String packageName) {
        Bitmap lockscreenWallpaper = null;
        try {
            mPackageContext = ThemeClubApplication.getContext().createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            mLockscreenWallpaperId = mPackageContext.getResources().getIdentifier(LOCKSCREEN_WALLPAPER, "drawable", packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (FRAMEWORK_PACKAGE_NAME.equals(packageName)) {
             lockscreenWallpaper = BitmapFactory
             .decodeResource(
             mContext.getResources(),
             R.drawable.themeclub_default_theme);
        } else {
            if (mLockscreenWallpaperId != 0 && mPackageContext != null)
                lockscreenWallpaper = BitmapFactory.decodeResource(mPackageContext.getResources(),
                        mLockscreenWallpaperId);
        }
        return lockscreenWallpaper;
    }
}
