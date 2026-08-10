package com.amz.ios.launcher;

import android.animation.ObjectAnimator;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class PreloadIconDrawable extends Drawable {

    private static final float ANIMATION_PROGRESS_STOPPED = -1.0f;
    private static final float ANIMATION_PROGRESS_STARTED = 0f;
    private static final float ANIMATION_PROGRESS_COMPLETED = 1.0f;

    // ===== Giao diện kiểu iOS (B): logo mờ khi tải + vòng track xám mờ ôm logo + vòng tiến độ =====
    // Không dùng nền tròn Android + màu dominant + scale icon như bản gốc. Vòng tiến độ tông trắng iOS,
    // track (phần chưa chạy) là trắng mờ; logo giảm alpha khi đang tải để nổi vòng tiến độ.
    private static final int PROGRESS_COLOR = 0xFFFFFFFF;   // vòng tiến độ: trắng
    private static final int TRACK_ALPHA = 64;              // track trắng mờ (~25%)
    private static final int DIMMED_ICON_ALPHA = 110;       // logo mờ khi đang tải (~43%)
    // Khoảng cách thêm giữa vòng tiến độ và viền icon (tỉ lệ theo cạnh indicator) để vòng không sát viền.
    private static final float RING_INSET_RATIO = 0.08f;

    private static final Rect sTempRect = new Rect();

    private final RectF mIndicatorRect = new RectF();
    private boolean mIndicatorRectDirty;

    private final Paint mPaint;       // vòng tiến độ (đã chạy)
    private final Paint mTrackPaint;  // vòng track (chưa chạy)
    final Drawable mIcon;

    private Drawable mBgDrawable;
    private int mRingOutset;

    /** Alpha do bên ngoài đặt (vd page transition). Kết hợp với alpha mờ khi tải. */
    private int mExternalAlpha = 255;

    /**
     * Indicates the progress of the preloader [0-100]. If it goes above 100, only the icon
     * is shown with no progress bar.
     */
    private int mProgress = 0;

    private float mAnimationProgress = ANIMATION_PROGRESS_STOPPED;
    private ObjectAnimator mAnimator;

    public PreloadIconDrawable(Drawable icon, Theme theme) {
        mIcon = icon;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(PROGRESS_COLOR);

        mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrackPaint.setColor(PROGRESS_COLOR);
        mTrackPaint.setAlpha(TRACK_ALPHA);

        setBounds(icon.getBounds());
        applyPreloaderTheme(theme);
        onLevelChange(0);
    }

    public void applyPreloaderTheme(Theme t) {
        TypedArray ta = t.obtainStyledAttributes(R.styleable.PreloadIconDrawable);
        mBgDrawable = ta.getDrawable(R.styleable.PreloadIconDrawable_iconBackground);
        mBgDrawable.setFilterBitmap(true);
        float strokeW = ta.getDimension(R.styleable.PreloadIconDrawable_indicatorSize, 0);
        mPaint.setStrokeWidth(strokeW);
        mTrackPaint.setStrokeWidth(strokeW);
        mRingOutset = ta.getDimensionPixelSize(R.styleable.PreloadIconDrawable_ringOutset, 0);
        ta.recycle();
        onBoundsChange(getBounds());
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mIcon.setBounds(bounds);
        if (mBgDrawable != null) {
            sTempRect.set(bounds);
            sTempRect.inset(-mRingOutset, -mRingOutset);
            mBgDrawable.setBounds(sTempRect);
        }
        mIndicatorRectDirty = true;
    }

    public int getOutset() {
        return mRingOutset;
    }

    /**
     * The size of the indicator is same as the content region of the {@link #mBgDrawable} minus
     * half the stroke size to accommodate the indicator.
     */
    private void initIndicatorRect() {
        Drawable d = mBgDrawable;
        Rect bounds = d.getBounds();

        d.getPadding(sTempRect);
        // Amount by which padding has to be scaled
        float paddingScaleX = ((float) bounds.width()) / d.getIntrinsicWidth();
        float paddingScaleY = ((float) bounds.height()) / d.getIntrinsicHeight();
        mIndicatorRect.set(
                bounds.left + sTempRect.left * paddingScaleX,
                bounds.top + sTempRect.top * paddingScaleY,
                bounds.right - sTempRect.right * paddingScaleX,
                bounds.bottom - sTempRect.bottom * paddingScaleY);

        float inset = mPaint.getStrokeWidth() / 2;
        // Thêm khoảng cách để vòng tiến độ không sát viền icon.
        inset += Math.min(mIndicatorRect.width(), mIndicatorRect.height()) * RING_INSET_RATIO;
        mIndicatorRect.inset(inset, inset);
        mIndicatorRectDirty = false;
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect r = new Rect(getBounds());
        if (canvas.getClipBounds(sTempRect) && !Rect.intersects(sTempRect, r)) {
            // The draw region has been clipped.
            return;
        }
        if (mIndicatorRectDirty) {
            initIndicatorRect();
        }

        // Kiểu iOS (B): logo giữ nguyên kích thước, mờ khi đang tải; vòng track + vòng tiến độ vẽ ĐÈ
        // lên logo (ôm sát viền). Khi tải xong (finish animation) vòng mờ dần đi và logo sáng lại.
        if ((mAnimationProgress >= ANIMATION_PROGRESS_STARTED)
                && (mAnimationProgress < ANIMATION_PROGRESS_COMPLETED)) {
            // Đang chạy animation kết thúc: vòng mờ dần, logo sáng dần trở lại.
            float ringFade = 1 - mAnimationProgress;
            mTrackPaint.setAlpha((int) (TRACK_ALPHA * ringFade));
            mPaint.setAlpha((int) (255 * ringFade));
            mIcon.setAlpha(dimmedIconAlpha(1 - ringFade));

            mIcon.draw(canvas);
            canvas.drawOval(mIndicatorRect, mTrackPaint);
            canvas.drawOval(mIndicatorRect, mPaint);
        } else if (mAnimationProgress == ANIMATION_PROGRESS_STOPPED && mProgress > 0
                && mProgress < 100) {
            // Đang tải: logo mờ + vòng track đầy + vòng tiến độ chạy theo %.
            mIcon.setAlpha(dimmedIconAlpha(0f));
            mTrackPaint.setAlpha(TRACK_ALPHA);
            mPaint.setAlpha(255);

            mIcon.draw(canvas);
            canvas.drawOval(mIndicatorRect, mTrackPaint);
            canvas.drawArc(mIndicatorRect, -90, mProgress * 3.6f, false, mPaint);
        } else {
            // Không tải (hoặc đã xong): logo bình thường, không vòng.
            mIcon.setAlpha(mExternalAlpha);
            mIcon.draw(canvas);
        }
    }

    /** Alpha logo: từ mờ (khi đang tải, blend=0) về đầy đủ (khi xong, blend=1), theo mExternalAlpha. */
    private int dimmedIconAlpha(float blend) {
        int dimmed = (int) (DIMMED_ICON_ALPHA + (255 - DIMMED_ICON_ALPHA) * blend);
        return dimmed * mExternalAlpha / 255;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        // Lưu lại để kết hợp với alpha "mờ khi tải" trong draw(); không set thẳng vào mIcon.
        mExternalAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mIcon.setColorFilter(cf);
    }

    @Override
    protected boolean onLevelChange(int level) {
        mProgress = level;

        // Stop Animation
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        mAnimationProgress = ANIMATION_PROGRESS_STOPPED;
        // Kiểu iOS: logo được làm mờ bằng alpha trong draw(), không dùng ghost mode xám của Android.
        if (mIcon instanceof FastBitmapDrawable) {
            ((FastBitmapDrawable) mIcon).setGhostModeEnabled(false);
        }

        invalidateSelf();
        return true;
    }

    /**
     * Runs the finish animation if it is has not been run after last level change.
     */
    public void maybePerformFinishedAnimation() {
        if (mAnimationProgress > ANIMATION_PROGRESS_STOPPED) {
            return;
        }
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        setAnimationProgress(ANIMATION_PROGRESS_STARTED);
        mAnimator = ObjectAnimator.ofFloat(this, "animationProgress",
                ANIMATION_PROGRESS_STARTED, ANIMATION_PROGRESS_COMPLETED);
        mAnimator.start();
    }

    public void setAnimationProgress(float progress) {
        if (progress != mAnimationProgress) {
            mAnimationProgress = progress;
            invalidateSelf();
        }
    }

    public float getAnimationProgress() {
        return mAnimationProgress;
    }

    public boolean hasNotCompleted() {
        return mAnimationProgress < ANIMATION_PROGRESS_COMPLETED;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIcon.getIntrinsicHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        return mIcon.getIntrinsicWidth();
    }
}
