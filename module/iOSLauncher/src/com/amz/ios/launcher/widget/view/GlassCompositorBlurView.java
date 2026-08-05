package com.amz.ios.launcher.widget.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.amz.ios.launcher.R;

import java.lang.reflect.Method;

/**
 * Nền kính blur kiểu iOS 26 cho WIDGET trên workspace — dùng CƠ CHẾ COMPOSITOR y hệt
 * {@link com.amz.ios.launcher.DockBlurView} (hotseat): overlay là WINDOW RIÊNG
 * {@code TYPE_APPLICATION_MEDIA} xếp DƯỚI window chính (nội dung widget vẫn nét) nhưng TRÊN
 * wallpaper, nền = {@code ViewRootImpl.createBackgroundBlurDrawable} (@hide, mở khoá bằng
 * HiddenApiBypass) → chỉ mờ wallpaper TRONG bounds + bo góc.
 *
 * <p>Vì sao KHÔNG dùng {@link GlassBlurView} cho widget này: GlassBlurView đọc bitmap wallpaper
 * rồi blur — trên máy CHẶN quyền đọc wallpaper (Android 13+) nó rớt về nền phẳng, "không thấy
 * blur". Compositor blur ở tầng hệ thống nên KHÔNG cần đọc wallpaper → luôn mờ được (giống dock).
 *
 * <p>KHÁC DockBlurView ở chỗ: dock cố định (chỉ sync vị trí lúc onLayout); widget CUỘN NGANG khi
 * vuốt đổi page nên phải bám vị trí THẬT mỗi frame qua {@link ViewTreeObserver.OnPreDrawListener},
 * và tự ẩn overlay khi widget bị che/ mờ (mở App Library, folder... workspace fade alpha→0).
 *
 * <p>Scope no side-effects: TÁCH RIÊNG khỏi DockBlurView (hotseat) và GlassBlurView (4 widget đồng
 * hồ khác) — chỉ view này phục vụ nền blur widget compositor, không đụng các cơ chế đang chạy.
 */
public class GlassCompositorBlurView extends View {

    private static final String TAG = "WidgetGlassBlur";
    /** Bán kính background blur (px) — chỉ mờ phần nền TRONG khung widget. */
    private static final int BACKGROUND_BLUR_RADIUS = 60;
    /** Tint trắng rất nhẹ phủ lên nền glass (vẫn thấy wallpaper mờ xuyên qua). */
    private static final int PANEL_TINT = 0x1AFFFFFF;

    private static boolean sHiddenApiUnlocked;

    private final float mCornerRadius;
    private final float mStrokeWidth;
    private final int mStrokeColor;
    /** Ảnh nền theme (bg_glass) vẽ khi thiết bị KHÔNG dựng được compositor blur; null = không có. */
    private final Drawable mFallbackDrawable;

    private final int[] mLoc = new int[2];
    private final Rect mLastRect = new Rect();
    private final RectF mRectF = new RectF();
    private final Path mPath = new Path();
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private WindowManager mWm;
    private BlurPanel mPanel;
    private WindowManager.LayoutParams mPanelLp;
    private boolean mAdded;
    /** true khi đã xác định compositor blur KHÔNG dựng được -> chuyển hẳn sang vẽ fallback. */
    private boolean mUseFallback;

    private final ViewTreeObserver.OnPreDrawListener mPreDraw =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    syncPanel();
                    return true;
                }
            };

    public GlassCompositorBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Bo góc/viền theo họ glass của widget (18dp, viền #40FFFFFF 1dp).
        mCornerRadius = getResources().getDimension(R.dimen.widget_round_corner);
        mStrokeWidth = getResources().getDisplayMetrics().density; // 1dp
        mStrokeColor = 0x40FFFFFF;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassCompositorBlurView);
        mFallbackDrawable = a.getDrawable(R.styleable.GlassCompositorBlurView_gcbFallbackDrawable);
        a.recycle();

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mStrokeColor);
        mBorderPaint.setStrokeWidth(mStrokeWidth);

        // SDK < 31 chắc chắn không có compositor background-blur -> dùng fallback ngay từ đầu.
        if (Build.VERSION.SDK_INT < 31) {
            mUseFallback = true;
        }
        // Nếu dùng fallback thì anchor phải TỰ VẼ (ảnh nền + viền); ngược lại trong suốt hoàn toàn.
        setWillNotDraw(!mUseFallback);
    }

    /** Chuyển sang vẽ fallback (ảnh nền cũ) khi compositor blur không dựng được trên máy này. */
    private void switchToFallback() {
        if (mUseFallback) return;
        mUseFallback = true;
        removePanel();
        setWillNotDraw(false);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Chỉ chạy khi mUseFallback = true (setWillNotDraw(false)). Vẽ ảnh nền theme cũ + viền,
        // bo góc — y như GlassBlurView khi máy chặn wallpaper, để "dùng background như cũ".
        if (!mUseFallback) return;
        final int w = getWidth();
        final int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (mFallbackDrawable != null) {
            mRectF.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRectF, mCornerRadius, mCornerRadius, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(mPath);
            mFallbackDrawable.setBounds(0, 0, w, h);
            mFallbackDrawable.draw(canvas);
            canvas.restoreToCount(save);
        }
        if (mStrokeWidth > 0) {
            float inset = mStrokeWidth / 2f;
            mRectF.set(inset, inset, w - inset, h - inset);
            float r = Math.max(0, mCornerRadius - inset);
            canvas.drawRoundRect(mRectF, r, r, mBorderPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        getViewTreeObserver().addOnPreDrawListener(mPreDraw);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            getViewTreeObserver().removeOnPreDrawListener(mPreDraw);
        } catch (Throwable ignore) {
        }
        removePanel();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) removePanel();
    }

    /** Đặt/di chuyển overlay window trùng khít vị trí THẬT của anchor mỗi frame (cuộn page). */
    private void syncPanel() {
        if (mWm == null || mUseFallback) return;
        // Ẩn overlay khi widget bị che/mờ (mở App Library/folder → workspace fade alpha→0).
        if (!isShown() || getAlpha() < 0.05f || getWindowVisibility() != VISIBLE) {
            removePanel();
            return;
        }
        final int w = getWidth();
        final int h = getHeight();
        if (w <= 0 || h <= 0) { removePanel(); return; }
        if (!isAttachedToWindow()) return;

        getLocationOnScreen(mLoc);
        final Rect rect = new Rect(mLoc[0], mLoc[1], mLoc[0] + w, mLoc[1] + h);
        if (mAdded && rect.equals(mLastRect)) return;
        mLastRect.set(rect);

        if (mPanel == null) {
            mPanel = new BlurPanel(getContext());
            mPanelLp = buildLp();
        }
        mPanelLp.x = rect.left;
        mPanelLp.y = rect.top;
        mPanelLp.width = w;
        mPanelLp.height = h;

        try {
            if (!mAdded) {
                mWm.addView(mPanel, mPanelLp);
                mAdded = true;
            } else {
                mWm.updateViewLayout(mPanel, mPanelLp);
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "syncPanel add/update fail", t);
        }
    }

    private void removePanel() {
        if (mAdded && mPanel != null && mWm != null) {
            try { mWm.removeViewImmediate(mPanel); } catch (Throwable ignore) {}
        }
        mAdded = false;
        mLastRect.setEmpty();
    }

    private WindowManager.LayoutParams buildLp() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        // Sub-window bám token window launcher; MEDIA -> xếp DƯỚI window chính, TRÊN wallpaper.
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA;
        lp.token = getWindowToken();
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        return lp;
    }

    /** Mở khoá hidden-API bằng HiddenApiBypass (LSPosed). Gọi 1 lần, im lặng nếu thất bại. */
    private static void unlockHiddenApi() {
        if (sHiddenApiUnlocked) return;
        try {
            org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("L");
            sHiddenApiUnlocked = true;
        } catch (Throwable t) {
            android.util.Log.e(TAG, "unlockHiddenApi fail: " + t);
        }
    }

    /** BackgroundBlurDrawable của compositor cho window {@code panel} + bán kính/bo góc/tint. */
    private Drawable createBackgroundBlurDrawable(View panel) {
        if (Build.VERSION.SDK_INT < 31) return null;
        unlockHiddenApi();
        try {
            Object vri = View.class.getMethod("getViewRootImpl").invoke(panel);
            if (vri == null) return null;
            Method create = vri.getClass().getDeclaredMethod("createBackgroundBlurDrawable");
            create.setAccessible(true);
            Drawable dr = (Drawable) create.invoke(vri);
            if (dr == null) return null;
            dr.getClass().getMethod("setBlurRadius", int.class)
                    .invoke(dr, BACKGROUND_BLUR_RADIUS);
            dr.getClass().getMethod("setCornerRadius", float.class)
                    .invoke(dr, mCornerRadius);
            dr.getClass().getMethod("setColor", int.class)
                    .invoke(dr, PANEL_TINT);
            return dr;
        } catch (Throwable t) {
            android.util.Log.e(TAG, "createBackgroundBlurDrawable fail: " + t);
            return null;
        }
    }

    /** Overlay: nền = BackgroundBlurDrawable (mờ trong khung, bo góc) + viền glass vẽ đè. */
    private class BlurPanel extends View {
        private final Paint mStroke;
        private final RectF mRect = new RectF();

        BlurPanel(Context c) {
            super(c);
            mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStroke.setStyle(Paint.Style.STROKE);
            mStroke.setStrokeWidth(mStrokeWidth);
            mStroke.setColor(mStrokeColor);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), mCornerRadius);
                }
            });
            setClipToOutline(true);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            Drawable blur = createBackgroundBlurDrawable(this);
            if (blur != null) {
                setBackground(blur);
            } else {
                // Máy chặn hidden-API/không hỗ trợ compositor blur -> gỡ overlay, dùng nền cũ.
                post(GlassCompositorBlurView.this::switchToFallback);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float half = mStrokeWidth / 2f;
            mRect.set(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(mRect, mCornerRadius - half, mCornerRadius - half, mStroke);
        }
    }
}
