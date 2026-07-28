package com.amz.ios.blurkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.View;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BlurKit {

    private static final float FULL_SCALE = 1f;

    private static BlurKit instance;

    private static RenderScript rs;

    public static void init(Context context) {
        if (instance != null) {
            return;
        }

        instance = new BlurKit();
        rs = RenderScript.create(context.getApplicationContext());
    }

    public Bitmap blur(Context context, Bitmap src, int radius) {
        RenderScript renderScript = RenderScript.create(context);

        final Allocation input = Allocation.createFromBitmap(renderScript, src);
        final Allocation output = Allocation.createTyped(renderScript, input.getType());
        final ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        blur.setRadius(radius);
        blur.setInput(input);
        blur.forEach(output);
        output.copyTo(src);

        renderScript.destroy();
        return src;
    }

    public Bitmap blur(Context context, View src, int radius) {
        Bitmap bitmap = getBitmapForView(src);
        return blur(context, bitmap, radius);
    }

    public Bitmap fastBlur(Context context, View src, int radius, float downscaleFactor) {
        Bitmap bitmap = getBitmapForView(src, downscaleFactor);
        return blur(context, bitmap, radius);
    }

    private Bitmap getBitmapForView(View src, float downscaleFactor) {
        Bitmap bitmap = Bitmap.createBitmap(
                (int) (src.getWidth() * downscaleFactor),
                (int) (src.getHeight() * downscaleFactor),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        Matrix matrix = new Matrix();
        matrix.preScale(downscaleFactor, downscaleFactor);
        canvas.setMatrix(matrix);
        src.draw(canvas);

        return bitmap;
    }

    private Bitmap getBitmapForView(View src) {
        Bitmap bitmap = Bitmap.createBitmap(
                src.getWidth(),
                src.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        src.draw(canvas);

        return bitmap;
    }

    public static BlurKit getInstance() {
        if (instance == null) {
            throw new RuntimeException("BlurKit not initialized!");
        }

        return instance;
    }

}
