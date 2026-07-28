package com.amz.ios.launcher.leftpage.drawables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;

import com.amz.ios.launcher.R;

import java.util.Calendar;

public final class ClockDrawable extends Drawable {

    public final int mHeight;
    public final int mWidth;
    public final int mClockPadding;
    public int mHandPadding;
    public final int mClockHandSize;
    public final Paint mDialPaint;
    public final Paint mBlackPaint;
    public final Paint mGeneralHandPaint;
    public final Paint mSecondHandPaint;
    public boolean isFirstDraw;
    public final int[] HOUR_FILED_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    public int mHandDiffLength = 0;
    public int mContentWidth = 0;
    public final Rect mRect = new Rect();

    public ClockDrawable(Context context, int width, int height, int roundCorner) {
        this.mWidth = width;

        this.mHeight = height;
        if (roundCorner == -1) {
            context.getResources().getDimensionPixelSize(R.dimen.icon_round_corner);
        }

        this.mClockHandSize = context.getResources().getDimensionPixelSize(R.dimen.clock_hand_size);
        this.mClockPadding = context.getResources().getDimensionPixelSize(R.dimen.clock_padding);

        this.mDialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mDialPaint.setAntiAlias(true);
        this.mDialPaint.setColor(Color.WHITE);
        this.mDialPaint.setStyle(Paint.Style.FILL);

        this.mBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mBlackPaint.setAntiAlias(true);
        this.mBlackPaint.setColor(Color.BLACK);
        this.mBlackPaint.setStyle(Paint.Style.FILL);

        this.mGeneralHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mGeneralHandPaint.setColor(Color.BLACK);
        this.mGeneralHandPaint.setStyle(Paint.Style.STROKE);
        this.mGeneralHandPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mGeneralHandPaint.setStrokeWidth(mClockHandSize);

        this.mSecondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mSecondHandPaint.setColor(-37632);
        this.mSecondHandPaint.setStyle(Paint.Style.STROKE);
        this.mSecondHandPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mSecondHandPaint.setStrokeWidth((mClockHandSize * 2) / 3.0f);
    }

    public final void drawClockHand(Canvas canvas, float cx, float cy, double angle, boolean isLongItem, boolean fillRect) {
        float startX;
        float startY;
        float endX;
        float endY;
        Paint paint;
        double radian = ((Math.PI * angle) / 30.0d) - Math.PI * 0.5;
        int length = this.mContentWidth - this.mHandPadding;
        if (isLongItem) {
            length -= this.mHandDiffLength;
        }
        if (fillRect) {
            this.mSecondHandPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, this.mClockHandSize, this.mSecondHandPaint);
            this.mSecondHandPaint.setStyle(Paint.Style.STROKE);
            double cos = Math.cos(radian);
            double sin = Math.sin(radian);
            startX = (float) ((double) cx - ((cos * (double) length) / 4.0d));
            startY = (float) ((double) cy - ((sin * (double) length) / 4.0d));
            endX = (float) ((cos * (double) length) + (double) cx);
            endY = (float) ((sin * (double) length) + (double) cy);
            paint = this.mSecondHandPaint;
        } else {
            double cos = Math.cos(radian);
            double sin = Math.sin(radian);
            startX = cx;
            startY = cy;
            endX = (float) ((cos * (double) length) + (double) cx);
            endY = (float) ((sin * (double) length) + (double) cy);
            paint = this.mGeneralHandPaint;
        }
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    @Override
    public final void draw(Canvas canvas) {
        Calendar calendar = Calendar.getInstance();
        try {
            int save = canvas.save();
            if (!this.isFirstDraw) {
                int min = Math.min(this.mHeight, this.mWidth);
                this.mContentWidth = (min / 2) - this.mClockPadding;
                this.mHandPadding = min / 16;
                this.mHandDiffLength = min / 9;
                canvas.clipRect(
                    new RectF(0.0f, 0.0f, this.mWidth, this.mHeight)
                );
                this.isFirstDraw = true;
            }

            float cx = this.mWidth / 2.f;
            float cy = this.mHeight / 2.f;

            drawBg(canvas, this.mWidth, this.mHeight, this.mBlackPaint);
            canvas.drawCircle(cx, cy, this.mContentWidth, this.mDialPaint);
            canvas.drawCircle(cx, cy, this.mClockHandSize * 1.5f, this.mBlackPaint);

            float textSize = this.mWidth / 8.f;
            this.mBlackPaint.setTextSize(textSize);

            for (int hour : HOUR_FILED_VALUES){
                String hourString = String.valueOf(hour);
                this.mBlackPaint.getTextBounds(hourString, 0, hourString.length(), this.mRect);
                double radianAngle = (hour - 3) * Math.PI / 6;
                double cos = Math.cos(radianAngle);
                double sin = Math.sin(radianAngle);
                float radius = this.mContentWidth - 0.8f * textSize;
                float midX = (float) ((cos * radius) + cx);
                float midY = (float) ((sin * radius) + cy);
                float startX = midX - this.mRect.width() / 2.0f;
                canvas.drawText(hourString, startX, midY + this.mRect.height() * .5f, this.mBlackPaint);
            }

            float secondAmount = calendar.get(Calendar.SECOND);
            float minuteAmount = calendar.get(Calendar.MINUTE);
            float hourAmount = (minuteAmount / 60.0f) + calendar.get(Calendar.HOUR_OF_DAY);

            if (hourAmount > 12.0f) {
                hourAmount -= 12.0f;
            }

            drawClockHand(canvas, cx, cy, hourAmount * 5.0f, true, false);
            drawClockHand(canvas, cx, cy, minuteAmount, false, false);
            drawClockHand(canvas, cx, cy, secondAmount, false, true);

            canvas.restoreToCount(save);
        } catch (Throwable th) {
            th.getMessage();
        }
    }

    @Override
    public final int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public final void setAlpha(int i) {
    }

    @Override
    public final void setColorFilter(ColorFilter colorFilter) {
    }

    public static void drawBg(Canvas canvas, float width, float height, Paint paint) {
        int save = canvas.save();
        float min = Math.min(width / 320.0f, height / 320.0f);
        float f3 = 320.0f * min;
        float f4 = 0;
        canvas.translate(((width - f3) / 2.0f) + f4, ((height - f3) / 2.0f) + f4);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(min, min);
        Path path = new Path();
        path.reset();
        path.moveTo(110.1f, 0.0f);
        path.lineTo(210.0f, 0.0f);
        path.cubicTo(241.7f, 0.0f, 257.5f, 0.0f, 274.6f, 5.4f);
        path.cubicTo(293.2f, 12.2f, 307.9f, 26.8f, 314.7f, 45.5f);
        path.cubicTo(320.1f, 62.6f, 320.1f, 78.4f, 320.1f, 110.1f);
        path.lineTo(320.1f, 210.0f);
        path.cubicTo(320.1f, 241.7f, 320.1f, 257.5f, 314.7f, 274.6f);
        path.cubicTo(307.9f, 293.2f, 293.3f, 307.9f, 274.6f, 314.7f);
        path.cubicTo(257.5f, 320.1f, 241.7f, 320.1f, 210.0f, 320.1f);
        path.lineTo(110.1f, 320.1f);
        path.cubicTo(78.4f, 320.1f, 62.6f, 320.1f, 45.5f, 314.7f);
        path.cubicTo(26.9f, 307.9f, 12.2f, 293.3f, 5.4f, 274.6f);
        path.cubicTo(0.0f, 257.5f, 0.0f, 241.6f, 0.0f, 209.9f);
        path.lineTo(0.0f, 110.0f);
        path.cubicTo(0.0f, 78.3f, 0.0f, 62.5f, 5.4f, 45.4f);
        path.cubicTo(12.2f, 26.8f, 26.8f, 12.1f, 45.5f, 5.3f);
        path.cubicTo(62.5f, 0.0f, 78.4f, 0.0f, 110.1f, 0.0f);
        path.lineTo(110.1f, 0.0f);
        path.transform(matrix);
        canvas.drawPath(path, paint);
        canvas.restoreToCount(save);
    }

    public static Bitmap getIconBitmap(Bitmap bitmap){

        Bitmap bg = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bg);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(paint);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float min = Math.min(width / 320.0f, height / 320.0f);
        float f3 = 320.0f * min;
        float f4 = 0;
        canvas.translate(((width - f3) / 2.0f) + f4, ((height - f3) / 2.0f) + f4);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(min, min);
        Path path = new Path();
        path.reset();
        path.moveTo(110.1f, 0.0f);
        path.lineTo(210.0f, 0.0f);
        path.cubicTo(241.7f, 0.0f, 257.5f, 0.0f, 274.6f, 5.4f);
        path.cubicTo(293.2f, 12.2f, 307.9f, 26.8f, 314.7f, 45.5f);
        path.cubicTo(320.1f, 62.6f, 320.1f, 78.4f, 320.1f, 110.1f);
        path.lineTo(320.1f, 210.0f);
        path.cubicTo(320.1f, 241.7f, 320.1f, 257.5f, 314.7f, 274.6f);
        path.cubicTo(307.9f, 293.2f, 293.3f, 307.9f, 274.6f, 314.7f);
        path.cubicTo(257.5f, 320.1f, 241.7f, 320.1f, 210.0f, 320.1f);
        path.lineTo(110.1f, 320.1f);
        path.cubicTo(78.4f, 320.1f, 62.6f, 320.1f, 45.5f, 314.7f);
        path.cubicTo(26.9f, 307.9f, 12.2f, 293.3f, 5.4f, 274.6f);
        path.cubicTo(0.0f, 257.5f, 0.0f, 241.6f, 0.0f, 209.9f);
        path.lineTo(0.0f, 110.0f);
        path.cubicTo(0.0f, 78.3f, 0.0f, 62.5f, 5.4f, 45.4f);
        path.cubicTo(12.2f, 26.8f, 26.8f, 12.1f, 45.5f, 5.3f);
        path.cubicTo(62.5f, 0.0f, 78.4f, 0.0f, 110.1f, 0.0f);
        path.lineTo(110.1f, 0.0f);
        path.transform(matrix);

        canvas.clipPath(path);
        canvas.drawBitmap(bitmap,0,0,null);
        canvas.setBitmap(null);
        return bg;
    }

    public static int drawBgAndReturnSave(Canvas canvas, Bitmap bitmap, float width, float height, Paint paint) {
        int save = canvas.save();
        float min = Math.min(width / 320.0f, height / 320.0f);
        float f3 = 320.0f * min;
        float f4 = 0;
        canvas.translate(((width - f3) / 2.0f) + f4, ((height - f3) / 2.0f) + f4);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(min, min);
        Path path = new Path();
        path.reset();
        path.moveTo(110.1f, 0.0f);
        path.lineTo(210.0f, 0.0f);
        path.cubicTo(241.7f, 0.0f, 257.5f, 0.0f, 274.6f, 5.4f);
        path.cubicTo(293.2f, 12.2f, 307.9f, 26.8f, 314.7f, 45.5f);
        path.cubicTo(320.1f, 62.6f, 320.1f, 78.4f, 320.1f, 110.1f);
        path.lineTo(320.1f, 210.0f);
        path.cubicTo(320.1f, 241.7f, 320.1f, 257.5f, 314.7f, 274.6f);
        path.cubicTo(307.9f, 293.2f, 293.3f, 307.9f, 274.6f, 314.7f);
        path.cubicTo(257.5f, 320.1f, 241.7f, 320.1f, 210.0f, 320.1f);
        path.lineTo(110.1f, 320.1f);
        path.cubicTo(78.4f, 320.1f, 62.6f, 320.1f, 45.5f, 314.7f);
        path.cubicTo(26.9f, 307.9f, 12.2f, 293.3f, 5.4f, 274.6f);
        path.cubicTo(0.0f, 257.5f, 0.0f, 241.6f, 0.0f, 209.9f);
        path.lineTo(0.0f, 110.0f);
        path.cubicTo(0.0f, 78.3f, 0.0f, 62.5f, 5.4f, 45.4f);
        path.cubicTo(12.2f, 26.8f, 26.8f, 12.1f, 45.5f, 5.3f);
        path.cubicTo(62.5f, 0.0f, 78.4f, 0.0f, 110.1f, 0.0f);
        path.lineTo(110.1f, 0.0f);
        path.transform(matrix);
        canvas.clipPath(path);

        if (paint != null)
            canvas.drawPath(path, paint);

        canvas.drawBitmap(bitmap,0,0,paint);
        canvas.restoreToCount(save);
        return save;
    }

    public static int drawBgAndReturnSave(Canvas canvas, Drawable drawable, float width, float height, Paint paint) {
        int save = canvas.save();
        float min = Math.min(width / 320.0f, height / 320.0f);
        float f3 = 320.0f * min;
        float f4 = 0;
        canvas.translate(((width - f3) / 2.0f) + f4, ((height - f3) / 2.0f) + f4);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(min, min);
        Path path = new Path();
        path.reset();
        path.moveTo(110.1f, 0.0f);
        path.lineTo(210.0f, 0.0f);
        path.cubicTo(241.7f, 0.0f, 257.5f, 0.0f, 274.6f, 5.4f);
        path.cubicTo(293.2f, 12.2f, 307.9f, 26.8f, 314.7f, 45.5f);
        path.cubicTo(320.1f, 62.6f, 320.1f, 78.4f, 320.1f, 110.1f);
        path.lineTo(320.1f, 210.0f);
        path.cubicTo(320.1f, 241.7f, 320.1f, 257.5f, 314.7f, 274.6f);
        path.cubicTo(307.9f, 293.2f, 293.3f, 307.9f, 274.6f, 314.7f);
        path.cubicTo(257.5f, 320.1f, 241.7f, 320.1f, 210.0f, 320.1f);
        path.lineTo(110.1f, 320.1f);
        path.cubicTo(78.4f, 320.1f, 62.6f, 320.1f, 45.5f, 314.7f);
        path.cubicTo(26.9f, 307.9f, 12.2f, 293.3f, 5.4f, 274.6f);
        path.cubicTo(0.0f, 257.5f, 0.0f, 241.6f, 0.0f, 209.9f);
        path.lineTo(0.0f, 110.0f);
        path.cubicTo(0.0f, 78.3f, 0.0f, 62.5f, 5.4f, 45.4f);
        path.cubicTo(12.2f, 26.8f, 26.8f, 12.1f, 45.5f, 5.3f);
        path.cubicTo(62.5f, 0.0f, 78.4f, 0.0f, 110.1f, 0.0f);
        path.lineTo(110.1f, 0.0f);
        path.transform(matrix);
        canvas.clipPath(path);
        canvas.drawPath(path, paint);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        canvas.restoreToCount(save);
        return save;
    }
}

