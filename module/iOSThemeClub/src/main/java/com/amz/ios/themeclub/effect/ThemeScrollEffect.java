package com.amz.ios.themeclub.effect;

import android.animation.TimeInterpolator;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.view.ThemePagedView;

/**
 * Created by server on 16-12-23.
 */
public abstract class ThemeScrollEffect {

    public static final String SCROLL_EFFECT_NONE = "none";
    public static final String SCROLL_EFFECT_STACK = "stack";


    protected static float CAMERA_DISTANCE = 6500;
    protected static final float TRANSITION_SCALE_FACTOR = 0.74f;
    protected static final float TRANSITION_SCREEN_ROTATION = 12.5f;
    protected int mCameraDistance;

    protected final ThemePagedView mPagedView;
    private final String mName;
    protected boolean mFadeInAdjacentScreens;


    public ThemeScrollEffect(ThemePagedView pagedView, String name) {
        mPagedView = pagedView;
        mName = name;
    }

    public void screenScrolled(View v, int i, float scrollProgress) {
        // Get and set the default camera distance.
        // Several of the TransitionEffects set a custom distance, reset it here.
        Float defaultCameraDistance = (Float) v.getTag(R.id.tag_key_default_camera_distance);
        if (defaultCameraDistance == null) {
            defaultCameraDistance = v.getCameraDistance();
            v.setTag(R.id.tag_key_default_camera_distance, defaultCameraDistance);
        }

        v.setCameraDistance(defaultCameraDistance);
        onScreenScrolled(v, i, scrollProgress);
    }

    public abstract void onScreenScrolled(View v, int i, float scrollProgress);


    public final String getName() {
        return mName;
    }


    public static void setFromString(ThemePagedView pagedView, String effect) {
        if (effect.equals(SCROLL_EFFECT_NONE)) {
            pagedView.setScrollEffect(null);
        } else if (effect.equals(SCROLL_EFFECT_STACK)) {
            pagedView.setScrollEffect(new Stack(pagedView));
        }
//        else if (effect.equals(SCROLL_EFFECT_CUBE_IN)) {
//            pagedView.setScrollEffect(new Cube(pagedView, true));
//        } else if (effect.equals(SCROLL_EFFECT_CUBE_OUT)) {
//            pagedView.setScrollEffect(new Cube(pagedView, false));
//        } else if (effect.equals(SCROLL_EFFECT_OVERVIEW)) {
//            pagedView.setScrollEffect(new Overview(pagedView));
//        } else if (effect.equals(SCROLL_EFFECT_ACCORDION)) {
//            pagedView.setScrollEffect(new Accordion(pagedView));
//        } else if (effect.equals(SCROLL_EFFECT_CROSS)) {
//            pagedView.setScrollEffect(new Flip(pagedView, false));
//        } else if (effect.equals(SCROLL_EFFECT_FLIP)) {
//            pagedView.setScrollEffect(new Flip(pagedView, true));
//        } else if (effect.equals(SCROLL_EFFECT_WHEEL)) {
//            pagedView.setScrollEffect(new Rotate(pagedView, false));
//        } else if (effect.equals(SCROLL_EFFECT_WINDMILL)) {
//            pagedView.setScrollEffect(new Rotate(pagedView, true));
//        } else if (effect.equals(SCROLL_EFFECT_CAROUSEL_LEFT)) {
//            pagedView.setScrollEffect(new Carousel(pagedView, true));
//        } else if (effect.equals(SCROLL_EFFECT_CAROUSEL_RIGHT)) {
//            pagedView.setScrollEffect(new Carousel(pagedView, false));
//        } else {
//            pagedView.setScrollEffect(null);
//        }
    }


    public static class Stack extends ThemeScrollEffect {
        private ZInterpolator mZInterpolator = new ZInterpolator(0.5f);
        private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);
        protected AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);

        public Stack(ThemePagedView pagedView) {
            super(pagedView, SCROLL_EFFECT_STACK);
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            final boolean isRtl = mPagedView.isLayoutRtl();
            float interpolatedProgress;
            float translationX;
            float maxScrollProgress = Math.max(0, scrollProgress);
            float minScrollProgress = Math.min(0, scrollProgress);

            if (mPagedView.isLayoutRtl()) {
                translationX = maxScrollProgress * v.getMeasuredWidth();
                interpolatedProgress = mZInterpolator.getInterpolation(Math.abs(maxScrollProgress));
            } else {
                translationX = minScrollProgress * v.getMeasuredWidth();
                interpolatedProgress = mZInterpolator.getInterpolation(Math.abs(minScrollProgress));
            }
            float scale = (1 - interpolatedProgress) +
                    interpolatedProgress * TRANSITION_SCALE_FACTOR;

            float alpha;
            if (isRtl && (scrollProgress > 0)) {
                alpha = mAlphaInterpolator.getInterpolation(1 - Math.abs(maxScrollProgress));
            } else if (!isRtl && (scrollProgress < 0)) {
                alpha = mAlphaInterpolator.getInterpolation(1 - Math.abs(scrollProgress));
            } else {
                //  On large screens we need to fade the page as it nears its leftmost position
                alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
            }

            v.setTranslationX(translationX);
            v.setScaleX(scale);
            v.setScaleY(scale);
            v.setAlpha(alpha);
        }
    }




    /*
* This interpolator emulates the rate at which the perceived scale of an object changes
* as its distance from a camera increases. When this interpolator is applied to a scale
* animation on a view, it evokes the sense that the object is shrinking due to moving away
* from the camera.
*/
    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            focalLength = foc;
        }

        public float getInterpolation(float input) {
            return (1.0f - focalLength / (focalLength + input)) /
                    (1.0f - focalLength / (focalLength + 1.0f));
        }
    }

}
