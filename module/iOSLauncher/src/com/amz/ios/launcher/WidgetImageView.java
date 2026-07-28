package com.amz.ios.launcher;

import android.view.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

public class WidgetImageView extends View {
    public final Paint mPaint;
    public final RectF mRectF;
    public Bitmap mBitmap;

    public WidgetImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint(3);
        this.mRectF = new RectF();
    }

    public final void setRect() {
        if (this.mBitmap.getWidth() > getWidth()) {
            this.mRectF.set(
                    0.0f,
                    0.0f,
                    getWidth(),
                    (int)((getWidth() * 1.0f / mBitmap.getWidth()) * this.mBitmap.getHeight())
            );
        }
        else if (this.mBitmap.getHeight() > getHeight()) {
            this.mRectF.set(
                    0.0f,
                    0.0f,
                    (int)((getHeight() * 1.0f / this.mBitmap.getHeight()) * this.mBitmap.getWidth()),
                    getHeight()
            );
        } else {
            float width = getWidth() * 1.0f / this.mBitmap.getWidth();
            float height = getHeight() * 1.0f / this.mBitmap.getHeight();
            this.mRectF.set(0.0f, 0.0f, Math.min(width, height) * this.mBitmap.getWidth(), Math.min(width, height) * this.mBitmap.getHeight());
        }
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public Rect getBitmapBounds() {
        setRect();
        Rect rect = new Rect(
                0,
                0,
                this.mBitmap.getWidth(),
                this.mBitmap.getHeight()
        );
        this.mRectF.round(rect);
        return rect;
    }

    @Override
    public final boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public final void onDraw(Canvas canvas) {
        if (this.mBitmap != null) {
            setRect();
            canvas.drawBitmap(this.mBitmap, (Rect) null, this.mRectF, this.mPaint);
        }
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        invalidate();
    }
}
