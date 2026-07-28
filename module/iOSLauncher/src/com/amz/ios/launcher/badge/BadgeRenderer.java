/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.amz.ios.launcher.badge;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import androidx.annotation.Nullable;
import android.util.SparseArray;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.graphics.IconPalette;
import com.amz.ios.launcher.graphics.ShadowGenerator;
import android.util.Log;

/**
 * Contains parameters necessary to draw a badge for an icon (e.g. the size of the badge).
 * @see BadgeInfo for the data to draw
 */
public class BadgeRenderer {

    private static final boolean DOTS_ONLY = false;

    // The badge sizes are defined as percentages of the app icon size.
//    private static final float SIZE_PERCENTAGE = 0.33f;
    private static final float SIZE_PERCENTAGE = 0.4f;

    // Used to expand the width of the badge for each additional digit.
    private static final float CHAR_SIZE_PERCENTAGE = 0.12f;
//    private static final float TEXT_SIZE_PERCENTAGE = 0.26f;
    private static final float TEXT_SIZE_PERCENTAGE = 0.3f;
    private static final float OFFSET_PERCENTAGE = 0.02f;
    private static final float STACK_OFFSET_PERCENTAGE_X = 0.05f;
    private static final float STACK_OFFSET_PERCENTAGE_Y = 0.06f;
    private static final float DOT_SCALE = 0.9f;

    private final Context mContext;
    private final int mSize;
    private final int mCharSize;
    private final int mTextHeight;
    private final int mOffset;
    private final int mStackOffsetX;
    private final int mStackOffsetY;
    private final IconDrawer mLargeIconDrawer;
    private final IconDrawer mSmallIconDrawer;
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private final SparseArray<Bitmap> mBackgroundsWithShadow;
    private final float defaultTextSize;

    public BadgeRenderer(Context context, int iconSizePx) {
        mContext = context;
        Resources res = context.getResources();
        mSize = (int) (SIZE_PERCENTAGE * iconSizePx);
        mCharSize = (int) (CHAR_SIZE_PERCENTAGE * iconSizePx);
        mOffset = (int) (OFFSET_PERCENTAGE * iconSizePx);
        mStackOffsetX = (int) (STACK_OFFSET_PERCENTAGE_X * iconSizePx);
        mStackOffsetY = (int) (STACK_OFFSET_PERCENTAGE_Y * iconSizePx);
        defaultTextSize = iconSizePx * TEXT_SIZE_PERCENTAGE;
        mTextPaint.setTextSize(defaultTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mLargeIconDrawer = new IconDrawer(res.getDimensionPixelSize(R.dimen.badge_small_padding));
        mSmallIconDrawer = new IconDrawer(res.getDimensionPixelSize(R.dimen.badge_large_padding));
        // Measure the text height.
        Rect tempTextHeight = new Rect();
        mTextPaint.getTextBounds("0", 0, 1, tempTextHeight);
        mTextHeight = tempTextHeight.height();

        mBackgroundsWithShadow = new SparseArray<>(3);
    }

    /**
     * Draw a circle in the top right corner of the given bounds, and draw
     * {@link BadgeInfo#getNotificationCount()} on top of the circle.
     * @param palette The colors (based on the icon) to use for the badge.
     * @param badgeInfo Contains data to draw on the badge. Could be null if we are animating out.
     * @param iconBounds The bounds of the icon being badged.
     * @param badgeScale The progress of the animation, from 0 to 1.
     * @param spaceForOffset How much space is available to offset the badge up and to the right.
     */
    public void drawFolderBadge(Canvas canvas, IconPalette palette, @Nullable BadgeInfo badgeInfo,
            Rect iconBounds, float badgeScale, Point spaceForOffset) {
        mTextPaint.setColor(Color.WHITE);
        IconDrawer iconDrawer = badgeInfo != null && badgeInfo.isIconLarge()
                ? mLargeIconDrawer : mSmallIconDrawer;
        Shader icon = badgeInfo == null ? null : badgeInfo.getNotificationIconForBadge(
                mContext, palette.backgroundColor, mSize, iconDrawer.mPadding);
        int notificationCount = badgeInfo == null ? 0 : badgeInfo.getNotificationCount();
        String strNotificationCount = String.valueOf(notificationCount);
        if (notificationCount >= 9) {
            strNotificationCount = "9+";
        }

        int numChars = strNotificationCount.length();
        if (numChars < 0) {
            numChars = 1;
        }

        int width = DOTS_ONLY ? mSize : mSize;// + mCharSize * (numChars - 1);
        float fontSizeScales[] = {1.0f, 0.8f, 0.6f, 0.4f, 0.3f, 0.1f};
        mTextPaint.setTextSize(defaultTextSize*fontSizeScales[numChars-1]);

        // Lazily load the background with shadow.
        Bitmap backgroundWithShadow = mBackgroundsWithShadow.get(numChars);
        if (backgroundWithShadow == null) {
            backgroundWithShadow = ShadowGenerator.createPillWithShadow(Color.RED, width, mSize);
            mBackgroundsWithShadow.put(numChars, backgroundWithShadow);
        }
        canvas.save();
        // We draw the badge relative to its center.
        int badgeCenterX = iconBounds.right - width / 2;
        int badgeCenterY = iconBounds.top + mSize / 2;
        boolean isText = !DOTS_ONLY && badgeInfo != null && notificationCount != 0;
        boolean isIcon = !DOTS_ONLY && icon != null;
        boolean isDot = !(isText || isIcon);
        if (isDot) {
            badgeScale *= DOT_SCALE;
        }
        int offsetX = Math.min(mOffset, spaceForOffset.x);
        int offsetY = Math.min(mOffset, spaceForOffset.y);
        canvas.translate(badgeCenterX + offsetX, badgeCenterY - offsetY);
        canvas.scale(badgeScale, badgeScale);
        // Prepare the background and shadow and possible stacking effect.
        mBackgroundPaint.setColorFilter(palette.backgroundColorMatrixFilter);
        int backgroundWithShadowSize = backgroundWithShadow.getHeight(); // Same as width.
        boolean shouldStack = !isDot && badgeInfo != null && badgeInfo.getNotificationKeys().size() > 1;

        if (shouldStack) {
            int offsetDiffX = mStackOffsetX - mOffset;
            int offsetDiffY = mStackOffsetY - mOffset;
            canvas.translate(offsetDiffX, offsetDiffY);
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15,-backgroundWithShadowSize / 2 - 10, mBackgroundPaint);
            canvas.translate(-offsetDiffX, -offsetDiffY);
        }

        if (isText) {
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15,-backgroundWithShadowSize / 2 - 10, mBackgroundPaint);

            if(strNotificationCount.length() >= 3){
                canvas.drawText(strNotificationCount, 16, mTextHeight / 2 - 16, mTextPaint);
            }else{
                canvas.drawText(strNotificationCount, 15, mTextHeight / 2 - 12, mTextPaint);
            }
        } else if (isIcon) {
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15,
                    -backgroundWithShadowSize / 2 - 10, mBackgroundPaint);
            iconDrawer.drawIcon(icon, canvas);
        } else if (isDot) {
            mBackgroundPaint.setColorFilter(palette.saturatedBackgroundColorMatrixFilter);
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15,
                    -backgroundWithShadowSize / 2 - 10, mBackgroundPaint);
        }
        canvas.restore();
    }
    
    public void drawIconBadge(Canvas canvas,
                     IconPalette palette,
                     @Nullable BadgeInfo badgeInfo,
                     Rect iconBounds,
                     float badgeScale,
                     Point spaceForOffset,
                     int number) {
        mTextPaint.setColor(palette.textColor);
        IconDrawer iconDrawer = badgeInfo != null && badgeInfo.isIconLarge()? mLargeIconDrawer : mSmallIconDrawer;
        Shader icon = badgeInfo == null ? null : badgeInfo.getNotificationIconForBadge(mContext, palette.backgroundColor, mSize, iconDrawer.mPadding);
        String strNumber = String.valueOf(number);
        if (number >= 9) {
            strNumber = "9+";
        }

        int numChars = strNumber.length();
        int width = mSize;// + mCharSize * (numChars - 1);

        float fontSizeScales[] = {1.0f, 0.8f, 0.6f, 0.4f, 0.3f, 0.1f};
        mTextPaint.setTextSize(defaultTextSize*fontSizeScales[numChars-1]);

        Log.d("hct  ==  xiaopeng 169","numChars" + numChars);
        // Lazily load the background with shadow.
        Bitmap backgroundWithShadow = mBackgroundsWithShadow.get(numChars);
        if (backgroundWithShadow == null) {
            backgroundWithShadow = ShadowGenerator.createPillWithShadow(Color.RED, width, mSize);
            mBackgroundsWithShadow.put(numChars, backgroundWithShadow);
        }
        canvas.save();
        // We draw the badge relative to its center.
        int badgeCenterX = iconBounds.right - width / 2;
        int badgeCenterY = iconBounds.top + mSize / 2;
        boolean isText = !DOTS_ONLY && badgeInfo != null && badgeInfo.getNotificationCount() != 0;
        boolean isIcon = !DOTS_ONLY && icon != null;
        boolean isDot = !(isText || isIcon);
        if (isDot) {
            badgeScale *= DOT_SCALE;
        }
        int offsetX = Math.min(mOffset, spaceForOffset.x);
        int offsetY = Math.min(mOffset, spaceForOffset.y);
        canvas.translate(badgeCenterX + offsetX, badgeCenterY - offsetY);
        canvas.scale(badgeScale, badgeScale);
        // Prepare the background and shadow and possible stacking effect.
        //mBackgroundPaint.setColorFilter(palette.backgroundColorMatrixFilter);
        //mBackgroundPaint.setColorFilter(palette.backgroundColorMatrixFilter);
        int backgroundWithShadowSize = backgroundWithShadow.getHeight(); // Same as width.
        boolean shouldStack = !isDot && badgeInfo != null && badgeInfo.getNotificationKeys().size() > 1;
        if (shouldStack && 1==0) {
            int offsetDiffX = mStackOffsetX - mOffset;
            int offsetDiffY = mStackOffsetY - mOffset;
            canvas.translate(offsetDiffX, offsetDiffY);
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2, -backgroundWithShadowSize / 2, mBackgroundPaint);
            canvas.translate(-offsetDiffX, -offsetDiffY);
        }

        if (true) {
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15, -backgroundWithShadowSize / 2 - 10, mBackgroundPaint);
            mTextPaint.setColor(Color.WHITE);
                                
            if(strNumber.length() >= 3){
                canvas.drawText(strNumber, 16, mTextHeight / 2 - 16, mTextPaint);
            }else{
                canvas.drawText(strNumber, 15, mTextHeight / 2 - 12, mTextPaint);
            }                    
            Log.d("hct--->xiaopeng  213", "isIcon......." + number);
        } else if (isIcon) {
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15,
                    -backgroundWithShadowSize / 2 - 10, mBackgroundPaint);
            Log.d("hct--->xiaopeng  217", "isIcon.......");
            iconDrawer.drawIcon(icon, canvas);
        } else if (isDot) {
            //mBackgroundPaint.setColorFilter(palette.saturatedBackgroundColorMatrixFilter);
            canvas.drawBitmap(backgroundWithShadow, -backgroundWithShadowSize / 2 + 15,
                    -backgroundWithShadowSize / 2 - 10, mBackgroundPaint);
            Log.d("hct--->xiaopeng  223", "isDot.......");
        }
        canvas.restore();
    }

    /** Draws the notification icon with padding of a given size. */
    private class IconDrawer {

        private final int mPadding;
        private final Bitmap mCircleClipBitmap;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG |
                Paint.FILTER_BITMAP_FLAG);

        public IconDrawer(int padding) {
            mPadding = padding;
            mCircleClipBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ALPHA_8);
            Canvas canvas = new Canvas();
            canvas.setBitmap(mCircleClipBitmap);
            canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2 - padding, mPaint);
        }

        public void drawIcon(Shader icon, Canvas canvas) {
            mPaint.setShader(icon);
            canvas.drawBitmap(mCircleClipBitmap, -mSize / 2, -mSize / 2, mPaint);
            mPaint.setShader(null);
        }
    }
    
    //*/wangqingsong add show miss number
    /*
    public void draw(
            Canvas canvas, int color, Rect iconBounds, float badgeScale, Point spaceForOffset,int number) {
        if (iconBounds == null || spaceForOffset == null) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        canvas.save();
        // We draw the badge relative to its center.
        float badgeCenterX = iconBounds.right - mDotCenterOffset / 2;
        float badgeCenterY = iconBounds.top + mDotCenterOffset / 2;

        int offsetX = Math.min(mOffset, spaceForOffset.x);
        int offsetY = Math.min(mOffset, spaceForOffset.y);
        canvas.translate(badgeCenterX + offsetX, badgeCenterY - offsetY);
        canvas.scale(badgeScale, badgeScale);

        mCirclePaint.setColor(Color.RED);
        //canvas.drawBitmap(mBackgroundWithShadow, mBitmapOffset, mBitmapOffset, mCirclePaint);
        //mCirclePaint.setColor(color);
        if(number >= 10){
                canvas.drawCircle(17, -3, mCircleRadius+7, mCirclePaint);
        }else{
                canvas.drawCircle(8, -3, mCircleRadius+7, mCirclePaint);
        }
        
        //mCirclePaint.setFakeBoldText(true); //字体加粗
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setTextSize(25); 
       // canvas.drawArc(mRectf, 100, 100, false, mRingPaint);
		mCirclePaint.setStrokeWidth(15);
		canvas.drawText(number+"",1,5,mCirclePaint);
        canvas.restore();
    }
    */
//*/
}
