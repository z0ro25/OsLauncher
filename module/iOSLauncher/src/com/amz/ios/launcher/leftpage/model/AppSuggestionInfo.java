package com.amz.ios.launcher.leftpage.model;


import android.graphics.Bitmap;

public final class AppSuggestionInfo {
    public int mIndex;
    public String mLabel;
    public String mComponentName;
    public Bitmap mBitmap;

    public AppSuggestionInfo(int index, String label, String componentName, Bitmap bitmap) {
        this.mIndex = index;
        this.mLabel = label;
        this.mComponentName = componentName;
        this.mBitmap = bitmap;
    }
}

