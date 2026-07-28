package com.amz.ios.launcher.util;

import android.graphics.Bitmap;

public class WallPaperSaveThread extends Thread {

    public BlurWallpaperProvider mBlurWallpaperProvider;
    public String mType;
    public Bitmap mBitmap;

    public WallPaperSaveThread(BlurWallpaperProvider provider, String str, Bitmap bitmap) {
        this.mBlurWallpaperProvider = provider;
        this.mType = str;
        this.mBitmap = bitmap;
    }

    @Override
    public void run() {
        super.run();
    }
}
