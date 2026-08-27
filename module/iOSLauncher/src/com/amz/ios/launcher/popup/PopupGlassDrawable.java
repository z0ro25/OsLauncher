package com.amz.ios.launcher.popup;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Nền kính của popup: ảnh đã blur (chụp từ vùng sau popup) + tint sáng + viền, bo góc.
 *
 * Dùng BitmapShader thay vì drawBitmap để ảnh tự co giãn theo bounds thật lúc vẽ — ảnh chụp đã bị
 * thu nhỏ {@code DOWNSCALE} lần cho nhanh, nên phải phóng lại đúng cỡ popup.
 *
 * Ba lớp vẽ chồng, đúng thứ tự:
 *   1. ảnh blur  — nội dung phía sau, đã nhoè
 *   2. tint sáng — tạo mặt kính để chữ tối đọc được
 *   3. viền      — nét sáng 1dp quanh mép, hoàn tất chất kính
 */
public class PopupGlassDrawable extends Drawable {

    private final Bitmap mBitmap;
    private final float mCornerRadius;
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mTintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final Matrix mShaderMatrix = new Matrix();
    private final float mStrokeWidth;

    public PopupGlassDrawable(Bitmap blurred, float cornerRadius, int tintColor, int strokeColor,
                              float strokeWidth) {
        mBitmap = blurred;
        mCornerRadius = cornerRadius;
        mStrokeWidth = strokeWidth;

        mBitmapPaint.setShader(
                new BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        mTintPaint.setColor(tintColor);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(strokeWidth);
        mStrokePaint.setColor(strokeColor);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRect.set(bounds);
        // Phóng ảnh (đã thu nhỏ lúc chụp) cho khớp bounds thật của popup.
        if (mBitmap.getWidth() > 0 && mBitmap.getHeight() > 0) {
            mShaderMatrix.setScale(
                    bounds.width() / (float) mBitmap.getWidth(),
                    bounds.height() / (float) mBitmap.getHeight());
            mShaderMatrix.postTranslate(bounds.left, bounds.top);
            mBitmapPaint.getShader().setLocalMatrix(mShaderMatrix);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (mRect.isEmpty()) return;
        canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mBitmapPaint);
        canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mTintPaint);
        if (mStrokeWidth > 0) {
            // Thu nửa nét vào trong để viền không bị cắt mất một nửa ở mép.
            float half = mStrokeWidth / 2f;
            RectF strokeRect = new RectF(
                    mRect.left + half, mRect.top + half,
                    mRect.right - half, mRect.bottom - half);
            canvas.drawRoundRect(strokeRect, mCornerRadius, mCornerRadius, mStrokePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mBitmapPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mBitmapPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
