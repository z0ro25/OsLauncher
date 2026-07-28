package com.amz.ios.search.entities;

import android.content.ComponentName;
import android.graphics.Bitmap;

/**
 * Created by server on 16-11-23.
 */
public class LauncherAppInfo {
    private CharSequence label;
    private Bitmap iconBitmap;
    private ComponentName componentName;

    public LauncherAppInfo(ComponentName cn, Bitmap iconBitmap, CharSequence label) {
        this.componentName = cn;
        this.label = label;
        this.iconBitmap = iconBitmap;
    }

    public CharSequence getLabel() {
        return label;
    }

    public Bitmap getIconBitmap() {
        return iconBitmap;
    }

    public ComponentName getComponentName() {
        return componentName;
    }

    @Override
    public String toString() {
        return "LauncherAppInfo{" +
                "label=" + label +
                '}';
    }
}
