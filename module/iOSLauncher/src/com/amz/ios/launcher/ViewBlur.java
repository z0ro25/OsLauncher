package com.amz.ios.launcher;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class ViewBlur extends View {

    public Paint mPaint;

    public Rect rect;

    public Bitmap blurBmp;

    public ViewBlur(Context context) {
        super(context);
        setAlpha(0.7f);
        this.mPaint = new Paint(2);
    }

    @Override // android.view.View
    public final void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.rect == null) {
            this.rect = new Rect(0, 0, getWidth(), getHeight());
        }
        Bitmap bitmap = this.blurBmp;
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        canvas.drawBitmap(this.blurBmp, (Rect) null, this.rect, this.mPaint);
    }

    public void setBmBlur(Bitmap bitmap) {
        if (this.blurBmp != bitmap) {
            this.blurBmp = bitmap;
            invalidate();
        }
    }

    public void setBmBlurOneTime(Bitmap bitmap) {
        Bitmap bitmap2 = this.blurBmp;
        if (bitmap2 != null && !bitmap2.isRecycled()) {
            this.blurBmp.recycle();
        }
        this.blurBmp = bitmap;
        invalidate();
    }

    public ViewBlur(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setAlpha(0.7f);
        this.mPaint = new Paint(2);
    }
}

