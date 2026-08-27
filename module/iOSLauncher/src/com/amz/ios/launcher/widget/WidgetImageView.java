/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.amz.ios.launcher.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.Utilities;

/**
 * View that draws a bitmap horizontally centered. If the image width is greater than the view
 * width, the image is scaled down appropriately.
 */
public class WidgetImageView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF mDstRectF = new RectF();
    private final int mBadgeMargin;

    private Bitmap mBitmap;
    private Drawable mBadge;

    /**
     * True = vẽ ảnh NẰM TRỌN trong khung (letterbox), giữ đúng tỉ lệ; False = hành vi GỐC
     * (chỉ thu theo bề rộng, cao hơn khung thì vẽ tràn rồi bị cắt).
     *
     * MẶC ĐỊNH false để KHÔNG đụng gì tới widget nội bộ (Calendar/Battery/Picture/Weather/Clock) —
     * chúng được thiết kế quanh khung vuông, đổi cách vẽ là vỡ bố cục. Chỉ luồng widget của APP
     * NGOÀI bật cờ này lên (xem WidgetAppStyleCell/GalleryWidgetCell.ensurePreview).
     */
    private boolean mFitInsideBox = false;

    /** Bật chế độ vẽ vừa-khung cho widget app ngoài. Xem {@link #mFitInsideBox}. */
    public void setFitInsideBox(boolean fit) {
        if (mFitInsideBox != fit) {
            mFitInsideBox = fit;
            invalidate();
        }
    }

    public WidgetImageView(Context context) {
        this(context, null);
    }

    public WidgetImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mBadgeMargin = context.getResources()
                .getDimensionPixelSize(R.dimen.profile_badge_margin);
    }

    public void setBitmap(Bitmap bitmap) {
        setBitmap(bitmap, null);
    }

    public void setBitmap(Bitmap bitmap, Drawable badge) {
        mBitmap = bitmap;
        mBadge = badge;
        invalidate();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            updateDstRectF();
            canvas.drawBitmap(mBitmap, null, mDstRectF, mPaint);

            // Only draw the badge if a preview was drawn.
            if (mBadge != null) {
                mBadge.draw(canvas);
            }
        }
    }

    /**
     * Prevents the inefficient alpha view rendering.
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void updateDstRectF() {
        float myWidth = getWidth();
        float myHeight = getHeight();
        float bitmapWidth = mBitmap.getWidth();

        float bitmapHeight = mBitmap.getHeight();

        // [FIX] Widget của APP NGOÀI hiện thiếu/bị cắt trong khay preview.
        //   Hành vi GỐC (nhánh else) chỉ thu theo BỀ RỘNG, rồi khi ảnh vẫn cao hơn khung thì đặt
        //   top=0/bottom=scaledHeight — tức CỐ Ý vẽ tràn xuống dưới cho khung cắt bớt (chú thích
        //   AOSP: "let the widget preview be clipped in the vertical dimension"). Với widget nhiều
        //   dòng như "Dấu trang Chrome" thì phần dưới mất hẳn.
        //   Chỉ đổi cho luồng app ngoài (mFitInsideBox = true): thu theo min(rộng, cao) để ảnh nằm
        //   TRỌN trong khung và căn giữa 2 chiều. Widget nội bộ giữ NGUYÊN nhánh gốc.
        if (mFitInsideBox) {
            float scale = 1f;
            if (bitmapWidth > myWidth || bitmapHeight > myHeight) {
                scale = Math.min(myWidth / bitmapWidth, myHeight / bitmapHeight);
            }
            float scaledWidth = bitmapWidth * scale;
            float scaledHeight = bitmapHeight * scale;

            mDstRectF.left = (myWidth - scaledWidth) / 2;
            mDstRectF.right = (myWidth + scaledWidth) / 2;
            mDstRectF.top = (myHeight - scaledHeight) / 2;
            mDstRectF.bottom = (myHeight + scaledHeight) / 2;
        } else {
            final float scale = bitmapWidth > myWidth ? myWidth / bitmapWidth : 1;
            float scaledWidth = bitmapWidth * scale;
            float scaledHeight = bitmapHeight * scale;

            mDstRectF.left = (myWidth - scaledWidth) / 2;
            mDstRectF.right = (myWidth + scaledWidth) / 2;

            if (scaledHeight > myHeight) {
                mDstRectF.top = 0;
                mDstRectF.bottom = scaledHeight;
            } else {
                mDstRectF.top = (myHeight - scaledHeight) / 2;
                mDstRectF.bottom = (myHeight + scaledHeight) / 2;
            }
        }

        if (mBadge != null) {
            Rect bounds = mBadge.getBounds();
            int left = Utilities.boundToRange(
                    (int) (mDstRectF.right + mBadgeMargin - bounds.width()),
                    mBadgeMargin, getWidth() - bounds.width());
            int top = Utilities.boundToRange(
                    (int) (mDstRectF.bottom + mBadgeMargin - bounds.height()),
                    mBadgeMargin, getHeight() - bounds.height());
            mBadge.setBounds(left, top, bounds.width() + left, bounds.height() + top);
        }
    }

    /**
     * @return the bounds where the image was drawn.
     */
    public Rect getBitmapBounds() {
        updateDstRectF();
        Rect rect = new Rect();
        mDstRectF.round(rect);
        return rect;
    }
}
