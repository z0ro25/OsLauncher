package com.amz.ios.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class CircleImageView extends AppCompatImageView {

    public CircleImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public final Bitmap setBitmap(Bitmap bitmap, int i) {
        Bitmap createScaledBitmap;
        try {
            if (bitmap.getWidth() == i && bitmap.getHeight() == i) {
                createScaledBitmap = bitmap;
                Bitmap createBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(createBitmap);
                Paint paint = new Paint();
                Rect rect = new Rect(0, 0, i, i);
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setDither(true);
                canvas.drawARGB(0, 0, 0, 0);
                paint.setColor(Color.parseColor("#BAB399"));
                float f = i / 2.0f;
                canvas.drawCircle(f, f, f, paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(createScaledBitmap, rect, rect, paint);
                return createBitmap;
            }
            float min = Math.min(bitmap.getWidth(), bitmap.getHeight()) * 1.f / i;
            createScaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() / min), (int) (bitmap.getHeight() / min), false);
            Bitmap createBitmap2 = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(createBitmap2);
            Paint paint2 = new Paint();
            Rect rect2 = new Rect(0, 0, i, i);
            paint2.setAntiAlias(true);
            paint2.setFilterBitmap(true);
            paint2.setDither(true);
            canvas2.drawARGB(0, 0, 0, 0);
            paint2.setColor(Color.parseColor("#BAB399"));
            float f2 = i / 2.0f;
            canvas2.drawCircle(f2, f2, f2, paint2);
            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas2.drawBitmap(createScaledBitmap, rect2, rect2, paint2);
            return createBitmap2;
        } catch (Throwable unused) {
            return bitmap;
        }
    }

    @Override
    public final void onDraw(Canvas canvas) {
        try {
            Drawable drawable = getDrawable();
            if (drawable == null) {
                super.onDraw(canvas);
                return;
            }
            if (getWidth() != 0 && getHeight() != 0) {
                Bitmap copy = ((BitmapDrawable) drawable).getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                int width = getWidth();
                getHeight();
                canvas.drawBitmap(setBitmap(copy, width), 0.0f, 0.0f, (Paint) null);
                return;
            }
            super.onDraw(canvas);
        } catch (Throwable unused) {
            super.onDraw(canvas);
        }
    }
}

