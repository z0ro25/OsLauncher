package com.amz.ios.launcher.effect;

import android.animation.TimeInterpolator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.amz.ios.launcher.PagedView;
import com.amz.ios.launcher.R;

/**
 * Created by server on 16-12-23.
 */
public abstract class ScrollEffect {

    public static final String SCROLL_EFFECT_NONE = "none";
    public static final String SCROLL_EFFECT_STACK = "stack";
    public static final String SCROLL_EFFECT_CUBE_IN = "cube-in";
    public static final String SCROLL_EFFECT_CUBE_OUT = "cube-out";
    public static final String SCROLL_EFFECT_ACCORDION = "accordion";
    public static final String SCROLL_EFFECT_FLIP = "flip";
    public static final String SCROLL_EFFECT_CROSS = "cross";
    public static final String SCROLL_EFFECT_OVERVIEW = "overview";
    public static final String SCROLL_EFFECT_WINDMILL = "rotate-up";
    public static final String SCROLL_EFFECT_WHEEL = "rotate-down";
    public static final String SCROLL_EFFECT_CAROUSEL_LEFT = "carousel-left";
    public static final String SCROLL_EFFECT_CAROUSEL_RIGHT = "carousel-right";

    protected static float CAMERA_DISTANCE = 6500;
    protected static final float TRANSITION_SCALE_FACTOR = 0.74f;
    protected static final float TRANSITION_SCREEN_ROTATION = 12.5f;
    protected int mCameraDistance;

    protected final PagedView mPagedView;
    private final String mName;
    protected boolean mFadeInAdjacentScreens;


    public ScrollEffect(PagedView pagedView, String name) {
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


    public static void setFromString(PagedView pagedView, String effect) {
        if (effect.equals(SCROLL_EFFECT_NONE)) {
            pagedView.setScrollEffect(null);
        } else if (effect.equals(SCROLL_EFFECT_STACK)) {
            pagedView.setScrollEffect(new Stack(pagedView));
        } else if (effect.equals(SCROLL_EFFECT_CUBE_IN)) {
            pagedView.setScrollEffect(new Cube(pagedView, true));
        } else if (effect.equals(SCROLL_EFFECT_CUBE_OUT)) {
            pagedView.setScrollEffect(new Cube(pagedView, false));
        } else if (effect.equals(SCROLL_EFFECT_OVERVIEW)) {
            pagedView.setScrollEffect(new Overview(pagedView));
        } else if (effect.equals(SCROLL_EFFECT_ACCORDION)) {
            pagedView.setScrollEffect(new Accordion(pagedView));
        } else if (effect.equals(SCROLL_EFFECT_CROSS)) {
            pagedView.setScrollEffect(new Flip(pagedView, false));
        } else if (effect.equals(SCROLL_EFFECT_FLIP)) {
            pagedView.setScrollEffect(new Flip(pagedView, true));
        } else if (effect.equals(SCROLL_EFFECT_WHEEL)) {
            pagedView.setScrollEffect(new Rotate(pagedView, false));
        } else if (effect.equals(SCROLL_EFFECT_WINDMILL)) {
            pagedView.setScrollEffect(new Rotate(pagedView, true));
        } else if (effect.equals(SCROLL_EFFECT_CAROUSEL_LEFT)) {
            pagedView.setScrollEffect(new Carousel(pagedView, true));
        } else if (effect.equals(SCROLL_EFFECT_CAROUSEL_RIGHT)) {
            pagedView.setScrollEffect(new Carousel(pagedView, false));
        } else {
            pagedView.setScrollEffect(null);
        }
    }


    public static class Stack extends ScrollEffect {
        private ZInterpolator mZInterpolator = new ZInterpolator(0.5f);
        private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);
        protected AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);

        public Stack(PagedView pagedView) {
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

    public static class Cube extends ScrollEffect {
        private boolean mIn;

        public Cube(PagedView pagedView, boolean in) {
            super(pagedView, in ? SCROLL_EFFECT_CUBE_IN : SCROLL_EFFECT_CUBE_OUT);
            mIn = in;
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            float rotation = (mIn ? 90.0f : -90.0f) * scrollProgress;

            if (mIn) {
                v.setCameraDistance(mPagedView.mDensity * CAMERA_DISTANCE* 2.5f);
            } else {
                v.setCameraDistance(mPagedView.mDensity * CAMERA_DISTANCE);
            }

            v.setPivotX(scrollProgress < 0 ? 0 : v.getMeasuredWidth());
            v.setPivotY(v.getMeasuredHeight() * 0.5f);
            v.setRotationY(rotation);
        }
    }

    public static class Rotate extends ScrollEffect {
        private boolean mUp;

        public Rotate(PagedView pagedView, boolean up) {
            super(pagedView, up ? SCROLL_EFFECT_WINDMILL : SCROLL_EFFECT_WHEEL);
            mUp = up;
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            float rotation =
                    (mUp ? TRANSITION_SCREEN_ROTATION : -TRANSITION_SCREEN_ROTATION) * scrollProgress;

            float translationX = v.getMeasuredWidth() * scrollProgress;

            float rotatePoint =
                    (v.getMeasuredWidth() * 0.5f) /
                            (float) Math.tan(Math.toRadians((double) (TRANSITION_SCREEN_ROTATION * 0.5f)));

            v.setPivotX(v.getMeasuredWidth() * 0.5f);
            if (mUp) {
                v.setPivotY(-rotatePoint);
            } else {
                v.setPivotY(v.getMeasuredHeight() + rotatePoint);
            }
            v.setRotation(rotation);
            v.setTranslationX(translationX);
        }
    }


    public static class Overview extends ScrollEffect {
        private AccelerateDecelerateInterpolator mScaleInterpolator = new AccelerateDecelerateInterpolator();

        public Overview(PagedView pagedView) {
            super(pagedView, SCROLL_EFFECT_OVERVIEW);
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            float scale = 1.0f - 0.1f *
                    mScaleInterpolator.getInterpolation(Math.min(0.3f, Math.abs(scrollProgress)) / 0.3f);
            v.setPivotX(scrollProgress < 0 ? 0 : v.getMeasuredWidth());
            v.setPivotY(v.getMeasuredHeight() * 0.5f);
            v.setScaleX(scale);
            v.setScaleY(scale);

            v.setAlpha(1 - Math.abs(scrollProgress));
        }
    }

    public static class Accordion extends ScrollEffect {
        public Accordion(PagedView pagedView) {
            super(pagedView, SCROLL_EFFECT_ACCORDION);
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            float scale = 1.0f - Math.abs(scrollProgress);

            v.setScaleX(scale);
            v.setPivotX(scrollProgress < 0 ? 0 : v.getMeasuredWidth());
            v.setPivotY(v.getMeasuredHeight() / 2f);
        }
    }


    public static class Flip extends ScrollEffect {
        private boolean mVertical;

        public Flip(PagedView pagedView, boolean vertical) {
            super(pagedView, vertical ? SCROLL_EFFECT_FLIP : SCROLL_EFFECT_CROSS);
            mVertical = vertical;
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            float rotation = -180.0f * Math.max(-1f, Math.min(1f, scrollProgress));

            v.setCameraDistance(mPagedView.mDensity * CAMERA_DISTANCE);
            v.setPivotX(v.getMeasuredWidth() * 0.5f);
            v.setPivotY(v.getMeasuredHeight() * 0.5f);
            if (mVertical) {
                v.setRotationX(rotation);
            } else {
                v.setRotationY(rotation);
            }

            v.setAlpha(1 - Math.abs(scrollProgress));


            if (scrollProgress >= -0.5f && scrollProgress <= 0.5f) {
                v.setTranslationX(v.getMeasuredWidth() * scrollProgress);
                if (v.getVisibility() != View.VISIBLE) {
                    v.setVisibility(View.VISIBLE);
                }
            } else {
                v.setTranslationX(v.getMeasuredWidth() * -10f);
            }
        }
    }

    public static class Carousel extends ScrollEffect {
        private boolean mLeft;

        public Carousel(PagedView pagedView, boolean left) {
            super(pagedView, left ? SCROLL_EFFECT_CAROUSEL_LEFT : SCROLL_EFFECT_CAROUSEL_RIGHT);
            mLeft = left;
        }

        @Override
        public void onScreenScrolled(View v, int i, float scrollProgress) {
            float rotation = 90.0f * scrollProgress;

            v.setCameraDistance(mPagedView.mDensity * CAMERA_DISTANCE);
            v.setTranslationX(v.getMeasuredWidth() * scrollProgress);
            if (mLeft) {
                v.setPivotX(0f);
            } else {
                v.setPivotX(v.getMeasuredWidth());
            }

            v.setPivotY(v.getMeasuredHeight() / 2);
            v.setRotationY(-rotation);
            v.setAlpha(1 - Math.abs(scrollProgress));
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
