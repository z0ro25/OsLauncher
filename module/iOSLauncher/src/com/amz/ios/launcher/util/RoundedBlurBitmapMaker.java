package com.amz.ios.launcher.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.health.PackageHealthStats;

import com.amz.ios.launcher.R;

public class RoundedBlurBitmapMaker {
    public static Bitmap getRoundedBmp(int cornerR, int blurR, int width, int height, int color, int alpha){
        Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(2);
        paint.setColor(
            color
        );
        paint.setAlpha(alpha);

        RectF rectF = new RectF(0,0,width,height);

        Path path = new Path();
        path.addRoundRect(rectF,cornerR,cornerR, Path.Direction.CCW);

        canvas.clipPath(path);
        canvas.drawPaint(paint);
        return bitmap;
    }

    public static Drawable getRoundedBlurDrawable(int cornerR, int blurR, int width, int height, int color, int alpha){
        return new BitmapDrawable(
                getRoundedBmp(cornerR, blurR, width, height, color, alpha)
        );
    }
}
