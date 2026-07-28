/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.animation.DecelerateInterpolator;

import com.amz.ios.launcher.graphics.IconPalette;

public class FastBitmapDrawable extends Drawable {

    /**
     * The possible states that a FastBitmapDrawable can be in.
     */
    public enum State {

        NORMAL                      (0f, 0f, 1f, new DecelerateInterpolator()),
        PRESSED                     (0f, 100f / 255f, 1f, CLICK_FEEDBACK_INTERPOLATOR),
        FAST_SCROLL_HIGHLIGHTED     (0f, 0f, 1.15f, new DecelerateInterpolator()),
        FAST_SCROLL_UNHIGHLIGHTED   (0f, 0f, 1f, new DecelerateInterpolator()),
        DISABLED                    (1f, 0.5f, 1f, new DecelerateInterpolator());

        public final float desaturation;
        public final float brightness;
        /**
         * Used specifically by the view drawing this FastBitmapDrawable.
         */
        public final float viewScale;
        public final TimeInterpolator interpolator;

        State(float desaturation, float brightness, float viewScale, TimeInterpolator interpolator) {
            this.desaturation = desaturation;
            this.brightness = brightness;
            this.viewScale = viewScale;
            this.interpolator = interpolator;
        }
    }

    public static final TimeInterpolator CLICK_FEEDBACK_INTERPOLATOR = new TimeInterpolator() {

        @Override
        public float getInterpolation(float input) {
            if (input < 0.05f) {
                return input / 0.05f;
            } else if (input < 0.3f){
                return 1;
            } else {
                return (1 - input) / 0.7f;
            }
        }
    };
    public static final int CLICK_FEEDBACK_DURATION = 2000;
    public static final int FAST_SCROLL_HIGHLIGHT_DURATION = 225;
    public static final int FAST_SCROLL_UNHIGHLIGHT_DURATION = 150;
    public static final int FAST_SCROLL_UNHIGHLIGHT_FROM_NORMAL_DURATION = 225;
    public static final int FAST_SCROLL_INACTIVE_DURATION = 275;

    // Since we don't need 256^2 values for combinations of both the brightness and saturation, we
    // reduce the value space to a smaller value V, which reduces the number of cached
    // ColorMatrixColorFilters that we need to keep to V^2
    private static final int REDUCED_FILTER_VALUE_SPACE = 48;

    private static final int PRESSED_BRIGHTNESS = 100;
    private static ColorMatrix sGhostModeMatrix;
    private static final ColorMatrix sTempMatrix = new ColorMatrix();

    /**
     * Store the brightness colors filters to optimize animations during icon press. This
     * only works for non-ghost-mode icons.
     */
    private static final SparseArray<ColorFilter> sCachedBrightnessFilter =
            new SparseArray<ColorFilter>();

    private static final int GHOST_MODE_MIN_COLOR_RANGE = 130;
 // Temporary matrices used for calculation
    private static final ColorMatrix sTempBrightnessMatrix = new ColorMatrix();
    private static final ColorMatrix sTempFilterMatrix = new ColorMatrix();

    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Bitmap mBitmap;
    private int mAlpha;
    private State mState = State.NORMAL;

    private IconPalette mIconPalette;

    private PorterDuffColorFilter mDefultColorFilter = new PorterDuffColorFilter(0x64000000,
            PorterDuff.Mode.SRC_ATOP);

    // The saturation and brightness are values that are mapped to REDUCED_FILTER_VALUE_SPACE and
    // as a result, can be used to compose the key for the cached ColorMatrixColorFilters
    private int mDesaturation = 0;
    private int mBrightness = 0;
    private boolean mGhostModeEnabled = false;

    private boolean mPressed = false;
    private ObjectAnimator mPressedAnimator;

    // Animators for the fast bitmap drawable's properties
    private AnimatorSet mPropertyAnimator;

    @SuppressLint("SuspiciousIndentation")
    public FastBitmapDrawable(Bitmap b) {
        mAlpha = 255;
        mBitmap = b;
        if (b != null)
            setBounds(0, 0, b.getWidth(), b.getHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect r = getBounds();
        // Draw the bitmap into the bounding rect
        canvas.drawBitmap(mBitmap, null, r, mPaint);
    }

    public IconPalette getIconPalette() {
        if (mIconPalette == null) {
            mIconPalette = IconPalette.fromDominantColor(Utilities
                    .findDominantColorByHue(mBitmap, 20), true /* desaturateBackground */);
        }
        return mIconPalette;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // No op
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setFilterBitmap(boolean filterBitmap) {
        mPaint.setFilterBitmap(filterBitmap);
        mPaint.setAntiAlias(filterBitmap);
    }

    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public int getMinimumWidth() {
        return getBounds().width();
    }

    @Override
    public int getMinimumHeight() {
        return getBounds().height();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }


	/**
     * Animates this drawable to a new state.
     *
     * @return whether the state has changed.
     */
    @SuppressLint("ObjectAnimatorBinding")
    public boolean animateState(State newState) {
        State prevState = mState;
        if (mState != newState) {
            mState = newState;

            if (mState == State.PRESSED) {
                setAlpha(200);
                mPaint.setColorFilter(mDefultColorFilter);
                invalidateSelf();
                return true;
            } else {
                setAlpha(255);
                mPaint.setColorFilter(null);
                invalidateSelf();
            }

            mPropertyAnimator = cancelAnimator(mPropertyAnimator);
            mPropertyAnimator = new AnimatorSet();
            mPropertyAnimator.playTogether(
                    ObjectAnimator
                            .ofFloat(this, "desaturation", newState.desaturation),
                    ObjectAnimator
                            .ofFloat(this, "brightness", newState.brightness));
            mPropertyAnimator.setInterpolator(newState.interpolator);
            mPropertyAnimator.setDuration(getDurationForStateChange(prevState, newState));
            mPropertyAnimator.setStartDelay(getStartDelayForStateChange(prevState, newState));
            mPropertyAnimator.start();
            return true;
        }
        return false;
    }
	
	/**
     * Immediately sets this drawable to a new state.
     *
     * @return whether the state has changed.
     */
    public boolean setState(State newState) {
        if (mState != newState) {
            mState = newState;

            mPropertyAnimator = cancelAnimator(mPropertyAnimator);

            setDesaturation(newState.desaturation);
            setBrightness((int) newState.brightness);
            return true;
        }
        return false;
    }

    /**
     * Returns the current state.
     */
    public State getCurrentState() {
        return mState;
    }

    /**
     * Returns the duration for the state change animation.
     */
    public static int getDurationForStateChange(State fromState, State toState) {
        switch (toState) {
            case NORMAL:
                switch (fromState) {
                    case PRESSED:
                        return 0;
                    case FAST_SCROLL_HIGHLIGHTED:
                    case FAST_SCROLL_UNHIGHLIGHTED:
                        return FAST_SCROLL_INACTIVE_DURATION;
                }
            case PRESSED:
                return CLICK_FEEDBACK_DURATION;
            case FAST_SCROLL_HIGHLIGHTED:
                return FAST_SCROLL_HIGHLIGHT_DURATION;
            case FAST_SCROLL_UNHIGHLIGHTED:
                switch (fromState) {
                    case NORMAL:
                        // When animating from normal state, take a little longer
                        return FAST_SCROLL_UNHIGHLIGHT_FROM_NORMAL_DURATION;
                    default:
                        return FAST_SCROLL_UNHIGHLIGHT_DURATION;
                }
        }
        return 0;
    }

    /**
     * Returns the start delay when animating between certain fast scroll states.
     */
    public static int getStartDelayForStateChange(State fromState, State toState) {
        switch (toState) {
            case FAST_SCROLL_UNHIGHLIGHTED:
                switch (fromState) {
                    case NORMAL:
                        return FAST_SCROLL_UNHIGHLIGHT_DURATION / 4;
                }
        }
        return 0;
    }


    /**
     * Sets the saturation of this icon, 0 [full color] -> 1 [desaturated]
     */
    public void setDesaturation(float desaturation) {
        int newDesaturation = (int) Math.floor(desaturation * REDUCED_FILTER_VALUE_SPACE);
        if (mDesaturation != newDesaturation) {
            mDesaturation = newDesaturation;
            updateFilter();
        }
    }

    public float getDesaturation() {
        return (float) mDesaturation / REDUCED_FILTER_VALUE_SPACE;
    }

    /**
     * When enabled, the icon is grayed out and the contrast is increased to give it a 'ghost'
     * appearance.
     */
    public void setGhostModeEnabled(boolean enabled) {
        if (mGhostModeEnabled != enabled) {
            mGhostModeEnabled = enabled;
            updateFilter();
        }
    }

    public void setPressed(boolean pressed) {
        if (mPressed != pressed) {
            mPressed = pressed;
            if (mPressed) {
                mPressedAnimator = ObjectAnimator
                        .ofInt(this, "brightness", PRESSED_BRIGHTNESS)
                        .setDuration(CLICK_FEEDBACK_DURATION);
                mPressedAnimator.setInterpolator(CLICK_FEEDBACK_INTERPOLATOR);
                mPressedAnimator.start();
            } else if (mPressedAnimator != null) {
                mPressedAnimator.cancel();
                setBrightness(0);
            }
        }
        invalidateSelf();
    }

    public boolean isGhostModeEnabled() {
        return mGhostModeEnabled;
    }

    public int getBrightness() {
        return mBrightness;
    }

    public void setBrightness(int brightness) {
        if (mBrightness != brightness) {
            mBrightness = brightness;
            updateFilter();
            invalidateSelf();
        }
    }

    private void updateFilter() {
        if (mGhostModeEnabled) {
            if (sGhostModeMatrix == null) {
                sGhostModeMatrix = new ColorMatrix();
                sGhostModeMatrix.setSaturation(0);

                // For ghost mode, set the color range to [GHOST_MODE_MIN_COLOR_RANGE, 255]
                float range = (255 - GHOST_MODE_MIN_COLOR_RANGE) / 255.0f;
                sTempMatrix.set(new float[] {
                        range, 0, 0, 0, GHOST_MODE_MIN_COLOR_RANGE,
                        0, range, 0, 0, GHOST_MODE_MIN_COLOR_RANGE,
                        0, 0, range, 0, GHOST_MODE_MIN_COLOR_RANGE,
                        0, 0, 0, 1, 0 });
                sGhostModeMatrix.preConcat(sTempMatrix);
            }

            if (mBrightness == 0) {
                mPaint.setColorFilter(new ColorMatrixColorFilter(sGhostModeMatrix));
            } else {
                setBrightnessMatrix(sTempMatrix, mBrightness);
                sTempMatrix.postConcat(sGhostModeMatrix);
                mPaint.setColorFilter(new ColorMatrixColorFilter(sTempMatrix));
            }
        } else if (mBrightness != 0) {
            ColorFilter filter = sCachedBrightnessFilter.get(mBrightness);
            if (filter == null) {
                filter = new PorterDuffColorFilter(Color.argb(mBrightness, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                sCachedBrightnessFilter.put(mBrightness, filter);
            }
            mPaint.setColorFilter(filter);
        } else {
            mPaint.setColorFilter(null);
        }
    }

    private static void setBrightnessMatrix(ColorMatrix matrix, int brightness) {
        // Brightness: C-new = C-old*(1-amount) + amount
        float scale = 1 - brightness / 255.0f;
        matrix.setScale(scale, scale, scale, 1);
        float[] array = matrix.getArray();

        // Add the amount to RGB components of the matrix, as per the above formula.
        // Fifth elements in the array correspond to the constant being added to
        // red, blue, green, and alpha channel respectively.
        array[4] = brightness;
        array[9] = brightness;
        array[14] = brightness;
    }

    private AnimatorSet cancelAnimator(AnimatorSet animator) {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
        return null;
    }
}
