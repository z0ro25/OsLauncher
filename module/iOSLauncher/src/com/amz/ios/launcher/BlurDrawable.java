package com.amz.ios.launcher;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;

public final class BlurDrawable extends Drawable {

    public final Paint paint1;

    public final Paint paint2;
    public final RectF rectF;
    public float top;
    public float paddingHr;
    public float paddingVr;
    public final float cornerRadius;
    public final Bitmap bmp;

    public BlurDrawable(float radius, Bitmap bitmap) {
        this.paint1 = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.paint2 = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.rectF = new RectF();
        this.cornerRadius = radius;

        if (radius > 0.0f) {
            paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        }

        Resources system = Resources.getSystem();
        int widthPixels = system.getDisplayMetrics().widthPixels;
        int width = bitmap.getWidth();
        int heightPixels = system.getDisplayMetrics().heightPixels;
        int height = bitmap.getHeight();
        if (width < widthPixels) {
            float widthRatio = widthPixels / (float) width;
            this.bmp = Bitmap.createScaledBitmap(bitmap, (int) ((float) width * widthRatio), (int) (height * widthRatio), true);
            bitmap.recycle();
        } else {
            this.bmp = bitmap;
        }
        this.paddingHr = width > widthPixels ? (width - widthPixels) * 0.5f : 0.0f;
        this.paddingVr = height > heightPixels ? (height - heightPixels) * 0.5f : 0.0f;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        try {
            this.rectF.set(0.0f, 0.0f, getBounds().width(), getBounds().height());
            Path path = new Path();
            path.addRoundRect(this.rectF, this.cornerRadius, this.cornerRadius, Path.Direction.CW);
            canvas.clipPath(path);
            canvas.drawBitmap(this.bmp, (-0.0f) - this.paddingHr, (-this.top) - this.paddingVr, this.paint1);
        } catch (Exception unused) {
            Log.d("BlurDrawable", "Exception draw");
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }
}
