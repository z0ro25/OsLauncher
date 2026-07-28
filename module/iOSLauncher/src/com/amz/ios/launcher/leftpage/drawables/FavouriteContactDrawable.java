package com.amz.ios.launcher.leftpage.drawables;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

public final class FavouriteContactDrawable extends ShapeDrawable {

    public static final int f = 0;
    public final Paint mTextPaint;
    public final String b;
    public final int c;
    public final int d;
    public final int e;

    public static class DrawableData {
        public String mText = "";
        public int mTextColor = -7829368;
        public RectShape mShape = new RectShape();
        public Typeface c = Typeface.create("sans-serif-light",Typeface.NORMAL);
        public int e = -1;
    }

    public FavouriteContactDrawable(DrawableData aVar) {
        super(aVar.mShape);

        this.c = -1;
        this.d = -1;
        this.b = aVar.mText;
        int i = aVar.mTextColor;
        this.e = aVar.e;

        mTextPaint = new Paint();
        mTextPaint.setColor(-1);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setFakeBoldText(false);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTypeface(aVar.c);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStrokeWidth(0);
        Paint paint2 = new Paint();
        paint2.setColor(Color.rgb((int) (Color.red(i) * 0.9f), (int) (Color.green(i) * 0.9f), (int) (Color.blue(i) * 0.9f)));
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(0);
        getPaint().setColor(i);
    }

    @Override
    public final void draw(Canvas canvas) {
        super.draw(canvas);
        Rect bounds = getBounds();
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        int i = this.d;
        if (i < 0) {
            i = bounds.width();
        }
        int i2 = this.c;
        if (i2 < 0) {
            i2 = bounds.height();
        }
        int i3 = this.e;
        if (i3 < 0) {
            i3 = Math.min(i, i2) / 2;
        }
        this.mTextPaint.setTextSize(i3);
        canvas.drawText(this.b, i * .5f, (i2 * .5f) - ((this.mTextPaint.ascent() + this.mTextPaint.descent()) / 2.0f), this.mTextPaint);
        canvas.restoreToCount(save);
    }

    @Override
    public final int getIntrinsicHeight() {
        return this.c;
    }

    @Override
    public final int getIntrinsicWidth() {
        return this.d;
    }

    @Override
    public final int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public final void setAlpha(int i) {
        this.mTextPaint.setAlpha(i);
    }

    @Override
    public final void setColorFilter(ColorFilter colorFilter) {
        this.mTextPaint.setColorFilter(colorFilter);
    }

}
