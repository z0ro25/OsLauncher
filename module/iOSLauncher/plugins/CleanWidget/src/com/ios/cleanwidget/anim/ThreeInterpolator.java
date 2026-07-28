package com.ios.cleanwidget.anim;

import android.view.animation.Interpolator;

/**
 * Created by server on 16-11-3.
 */
public class ThreeInterpolator implements Interpolator {
    @Override
    public float getInterpolation(float paramFloat) {
        return paramFloat * paramFloat * paramFloat;
    }
}
