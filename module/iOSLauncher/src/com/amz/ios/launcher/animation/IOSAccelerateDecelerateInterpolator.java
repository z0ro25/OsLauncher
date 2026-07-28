package com.amz.ios.launcher.animation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Interpolator;

public class IOSAccelerateDecelerateInterpolator implements Interpolator {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "IOSAccelerateDecelerateInterpolator";

    public IOSAccelerateDecelerateInterpolator() {
    }

    public IOSAccelerateDecelerateInterpolator(Context context, AttributeSet attributeSet) {
    }

    public float getInterpolation(float f) {
        return f < 0.5f ? (float) ((Math.cos((Math.sin((((double) f) * 3.14d) / 2.0d) + 1.0d) * 3.14d) + 1.0d) / 2.0d) : (float) ((Math.cos((Math.sqrt((double) f) + 1.0d) * 3.14d) + 1.0d) / 2.0d);
    }
}
