package com.amz.ios.launcher.util;

import static android.graphics.drawable.GradientDrawable.RECTANGLE;

import android.graphics.drawable.GradientDrawable;

public class DrawableUtils {

    public static GradientDrawable makeGradientDrawable(int color, float cornerRadius) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(RECTANGLE);
        gradientDrawable.setCornerRadius((cornerRadius * 42.0f) / 180.0f);
        gradientDrawable.setColor(color);
        return gradientDrawable;
    }
}
