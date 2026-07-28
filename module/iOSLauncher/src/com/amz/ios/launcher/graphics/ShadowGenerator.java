/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.amz.ios.launcher.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.core.graphics.ColorUtils;

import com.amz.ios.launcher.LauncherAppState;

/**
 * Utility class to add shadows to bitmaps.
 */
public class ShadowGenerator {

    // Percent of actual icon size
    private static final float HALF_DISTANCE = 0.5f;
    public static final float BLUR_FACTOR = 0.5f/48;

    // Percent of actual icon size
    private static final float KEY_SHADOW_DISTANCE = 1f/48;
    public static final int KEY_SHADOW_ALPHA = 61;

    public static final int AMBIENT_SHADOW_ALPHA = 30;

    private static final Object LOCK = new Object();
    // Singleton object guarded by {@link #LOCK}
    private static ShadowGenerator sShadowGenerator;

    private final int mIconSize;

    private final Canvas mCanvas;
    private final Paint mBlurPaint;
    private final Paint mDrawPaint;
    private final BlurMaskFilter mDefaultBlurMaskFilter;

    private ShadowGenerator(Context context) {
        mIconSize = LauncherAppState.getInstance(context).getInvariantDeviceProfile().iconBitmapSize;
        mCanvas = new Canvas();
        mBlurPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mBlurPaint.setMaskFilter(new BlurMaskFilter(mIconSize * BLUR_FACTOR, Blur.NORMAL));
        mDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mDefaultBlurMaskFilter = new BlurMaskFilter(mIconSize * BLUR_FACTOR, Blur.NORMAL);
    }

    public synchronized Bitmap recreateIcon(Bitmap icon) {
        return recreateIcon(icon, true, mDefaultBlurMaskFilter, AMBIENT_SHADOW_ALPHA,
                KEY_SHADOW_ALPHA);
    }

    public synchronized Bitmap recreateIcon(Bitmap icon, boolean resize,
            BlurMaskFilter blurMaskFilter, int ambientAlpha, int keyAlpha) {
        int width = resize ? mIconSize : icon.getWidth();
        int height = resize ? mIconSize : icon.getHeight();
        int[] offset = new int[2];

        mBlurPaint.setMaskFilter(blurMaskFilter);
        Bitmap shadow = icon.extractAlpha(mBlurPaint, offset);
        Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        mCanvas.setBitmap(result);

        // Draw ambient shadow
        mDrawPaint.setAlpha(ambientAlpha);
        mCanvas.drawBitmap(shadow, offset[0], offset[1], mDrawPaint);

        // Draw key shadow
        mDrawPaint.setAlpha(keyAlpha);
        mCanvas.drawBitmap(shadow, offset[0], offset[1] + KEY_SHADOW_DISTANCE * mIconSize, mDrawPaint);

        // Draw the icon
        mDrawPaint.setAlpha(255);
        mCanvas.drawBitmap(icon, 0, 0, mDrawPaint);

        mCanvas.setBitmap(null);
        return result;
    }

    public static Bitmap createPillWithShadow(int rectColor, int width, int height) {

        float shadowRadius = height * 1f / 32;
        float shadowYOffset = height * 1f / 16;

        int radius = height / 2;

        Canvas canvas = new Canvas();
        Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        blurPaint.setMaskFilter(new BlurMaskFilter(shadowRadius, Blur.NORMAL));

        int centerX = Math.round(width / 2 + shadowRadius);
        int centerY = Math.round(radius + shadowRadius + shadowYOffset);
        int center = Math.max(centerX, centerY);
        int size = center * 2;
        Bitmap result = Bitmap.createBitmap(size, size, Config.ARGB_8888);
        canvas.setBitmap(result);

        int left = center - width / 2;
        int top = center - height / 2;
        int right = center + width / 2;
        int bottom = center + height / 2;

        // Draw ambient shadow, center aligned within size
        blurPaint.setAlpha(AMBIENT_SHADOW_ALPHA);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, blurPaint);

        // Draw key shadow, bottom aligned within size
        blurPaint.setAlpha(KEY_SHADOW_ALPHA);
        canvas.drawRoundRect(left, top + shadowYOffset, right, bottom + shadowYOffset,
                radius, radius, blurPaint);

        // Draw the circle
        Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        drawPaint.setColor(rectColor);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, drawPaint);

        return result;
    }

    public static ShadowGenerator getInstance(Context context) {
        // TODO: This currently fails as the system default icon also needs a shadow as it
        // uses adaptive icon.
        // Preconditions.assertNonUiThread();
        synchronized (LOCK) {
            if (sShadowGenerator == null) {
                sShadowGenerator = new ShadowGenerator(context);
            }
        }
        return sShadowGenerator;
    }

    /**
     * Returns the minimum amount by which an icon with {@param bounds} should be scaled
     * so that the shadows do not get clipped.
     */
    public static float getScaleForBounds(RectF bounds) {
        float scale = 1;

        // For top, left & right, we need same space.
        float minSide = Math.min(Math.min(bounds.left, bounds.right), bounds.top);
        if (minSide < BLUR_FACTOR) {
            scale = (HALF_DISTANCE - BLUR_FACTOR) / (HALF_DISTANCE - minSide);
        }

        float bottomSpace = BLUR_FACTOR + KEY_SHADOW_DISTANCE;
        if (bounds.bottom < bottomSpace) {
            scale = Math.min(scale, (HALF_DISTANCE - bottomSpace) / (HALF_DISTANCE - bounds.bottom));
        }
        return scale;
    }

    public static class Builder {

        public final RectF bounds = new RectF();
        public final int color;

        public int ambientShadowAlpha = AMBIENT_SHADOW_ALPHA;

        public float shadowBlur;

        public float keyShadowDistance;
        public int keyShadowAlpha = KEY_SHADOW_ALPHA;
        public float radius;

        public Builder(int color) {
            this.color = color;
        }

        public Builder setupBlurForSize(int height) {
            shadowBlur = height * 1f / 32;
            keyShadowDistance = height * 1f / 16;
            return this;
        }

        public Bitmap createPill(int width, int height) {
            radius = height / 2;

            int centerX = Math.round(width / 2 + shadowBlur);
            int centerY = Math.round(radius + shadowBlur + keyShadowDistance);
            int center = Math.max(centerX, centerY);
            bounds.set(0, 0, width, height);
            bounds.offsetTo(center - width / 2, center - height / 2);

            int size = center * 2;
            Bitmap result = Bitmap.createBitmap(size, size, Config.ARGB_8888);
            drawShadow(new Canvas(result));
            return result;
        }

        public void drawShadow(Canvas c) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            p.setColor(color);

            // Key shadow
            p.setShadowLayer(shadowBlur, 0, keyShadowDistance,
                    ColorUtils.setAlphaComponent(Color.BLACK, keyShadowAlpha));
            c.drawRoundRect(bounds, radius, radius, p);

            // Ambient shadow
            p.setShadowLayer(shadowBlur, 0, 0,
                    ColorUtils.setAlphaComponent(Color.BLACK, ambientShadowAlpha));
            c.drawRoundRect(bounds, radius, radius, p);
        }
    }
}
