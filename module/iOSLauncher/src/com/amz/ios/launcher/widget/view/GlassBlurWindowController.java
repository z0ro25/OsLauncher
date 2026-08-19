package com.amz.ios.launcher.widget.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.amz.ios.launcher.config.Settings;

import java.lang.reflect.Method;

/**
 * Controller gắn NGOÀI vào một {@link View} chủ ĐÃ tồn tại (page-indicator container, nút Edit/Done)
 * để phủ nền kính blur kiểu iOS 26 bằng CƠ CHẾ COMPOSITOR — y hệt
 * {@link com.amz.ios.launcher.DockBlurView} và {@link GlassCompositorBlurView}: overlay là WINDOW
 * RIÊNG {@code TYPE_APPLICATION_MEDIA} xếp DƯỚI window chính (nội dung view chủ vẫn nét) nhưng TRÊN
 * wallpaper, nền = {@code ViewRootImpl.createBackgroundBlurDrawable} (@hide, mở khoá bằng
 * HiddenApiBypass) → chỉ mờ wallpaper TRONG bounds + bo góc.
 *
 * <p>Vì sao là CONTROLLER (không phải View như {@link GlassCompositorBlurView}): 2 chỗ cần phủ là
 * View đã có sẵn (không thể thay bằng view blur). Controller bám vị trí THẬT của view chủ mỗi frame
 * qua {@link ViewTreeObserver.OnPreDrawListener}. KHÁC mẫu widget: tính kích thước hiển thị theo
 * {@code getWidth()*getScaleX()} / {@code getHeight()*getScaleY()} để bám khớp cả khi nút CHẠY
 * ANIMATION SCALE (CustomZoomButton zoom 0↔1); scale≈0 → gỡ window.
 *
 * <p>Vì sao KHÔNG dùng {@link GlassBlurDrawable}: nó đọc bitmap wallpaper rồi blur mềm — trên máy
 * chặn quyền đọc wallpaper (Android 13+) rớt về nền phẳng ("không thấy blur"). Compositor blur ở
 * tầng hệ thống nên luôn mờ được. Khi compositor KHÔNG dựng được (SDK&lt;31 / hidden-api bị chặn /
 * tắt blur) → rơi về {@code fallbackBackground} (chính là GlassBlurDrawable) làm background view chủ,
 * giữ đúng hành vi cũ (không regression).
 *
 * <p>Scope no side-effects: TÁCH RIÊNG khỏi DockBlurView/GlassCompositorBlurView/GlassBlurDrawable —
 * chỉ sao chép cơ chế, không sửa 3 lớp đó.
 */
public final class GlassBlurWindowController {

    private static final String TAG = "GlassBlurWinCtrl";

    private static boolean sHiddenApiUnlocked;

    private final View mTarget;
    /** &lt;0 = pill (r = rect.height()/2 động); &gt;=0 = bo góc cố định (px). */
    private final float mCornerRadiusPx;
    private final float mStrokeWidth;
    private final int mStrokeColor;
    private final int mTintColor;
    private final int mBlurRadiusPx;
    /** Background dùng cho view chủ khi compositor blur KHÔNG dựng được (GlassBlurDrawable). */
    private final Drawable mFallbackBackground;

    private final int[] mLoc = new int[2];
    private final Rect mLastRect = new Rect();
    /** Corner đang áp cho panel (px) — để phát hiện đổi khi pill co giãn theo height. */
    private float mCurrentCorner = -1f;

    private WindowManager mWm;
    private BlurPanel mPanel;
    private WindowManager.LayoutParams mPanelLp;
    private boolean mAdded;
    private boolean mAttached;
    /** true khi đã xác định compositor blur không dựng được -> chuyển hẳn sang fallback. */
    private boolean mUseFallback;

    private final ViewTreeObserver.OnPreDrawListener mPreDraw =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    syncPanel();
                    return true;
                }
            };

    private final View.OnAttachStateChangeListener mAttachListener =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    onTargetAttached();
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    onTargetDetached();
                }
            };

    public GlassBlurWindowController(View target, float cornerRadiusPx, float strokeWidthPx,
                                     int strokeColor, int tintColor, int blurRadiusPx,
                                     Drawable fallbackBackground) {
        mTarget = target;
        mCornerRadiusPx = cornerRadiusPx;
        mStrokeWidth = strokeWidthPx;
        mStrokeColor = strokeColor;
        mTintColor = tintColor;
        mBlurRadiusPx = blurRadiusPx;
        mFallbackBackground = fallbackBackground;
    }

    /** Bắt đầu quản lý: đăng ký attach-listener + preDraw. Idempotent. */
    public void attach() {
        if (mAttached || mTarget == null) return;
        mAttached = true;
        mTarget.addOnAttachStateChangeListener(mAttachListener);
        if (mTarget.isAttachedToWindow()) {
            onTargetAttached();
        }
    }

    /** Ngừng quản lý và gỡ window (thường không cần vì tự cleanup theo attach-state view chủ). */
    public void detach() {
        if (!mAttached) return;
        mAttached = false;
        try {
            mTarget.removeOnAttachStateChangeListener(mAttachListener);
        } catch (Throwable ignore) {
        }
        removePreDraw();
        removePanel();
    }

    private void onTargetAttached() {
        mWm = (WindowManager) mTarget.getContext().getSystemService(Context.WINDOW_SERVICE);
        // SDK<31 chắc chắn không có compositor background-blur; tắt blur -> dùng background cũ.
        if (Build.VERSION.SDK_INT < 31 || !Settings.isDesktopBlurEnable(mTarget.getContext())) {
            switchToFallback();
            return;
        }
        // Compositor lo blur -> view chủ trong suốt (đặt 1 lần, KHÔNG trong preDraw để tránh loop).
        mTarget.setBackground(null);
        addPreDraw();
    }

    private void onTargetDetached() {
        removePreDraw();
        removePanel();
    }

    private void addPreDraw() {
        try {
            mTarget.getViewTreeObserver().addOnPreDrawListener(mPreDraw);
        } catch (Throwable ignore) {
        }
    }

    private void removePreDraw() {
        try {
            mTarget.getViewTreeObserver().removeOnPreDrawListener(mPreDraw);
        } catch (Throwable ignore) {
        }
    }

    /** Chuyển hẳn sang dùng background cũ (GlassBlurDrawable) khi máy không dựng được compositor blur. */
    private void switchToFallback() {
        if (mUseFallback) return;
        mUseFallback = true;
        removePreDraw();
        removePanel();
        mTarget.setBackground(mFallbackBackground);
    }

    /** Đặt/di chuyển overlay window trùng khít vị trí THẬT (kèm scale) của view chủ mỗi frame. */
    private void syncPanel() {
        if (mWm == null || mUseFallback) return;
        // Ẩn overlay khi view chủ bị che/mờ (mở App Library/folder → workspace fade alpha→0) hoặc GONE.
        if (!mTarget.isShown() || mTarget.getAlpha() < 0.05f
                || mTarget.getWindowVisibility() != View.VISIBLE) {
            removePanel();
            return;
        }
        // Kích thước HIỂN THỊ = kích thước layout * scale (bám animation zoom của nút Edit/Done).
        final int w = Math.round(mTarget.getWidth() * mTarget.getScaleX());
        final int h = Math.round(mTarget.getHeight() * mTarget.getScaleY());
        if (w <= 0 || h <= 0) { removePanel(); return; } // nút scale≈0 -> gỡ panel.
        if (!mTarget.isAttachedToWindow()) return;
        if (mTarget.getWindowToken() == null) return; // token chưa sẵn -> chờ frame sau.

        // getLocationOnScreen đã áp ma trận biến đổi (scale quanh pivot) -> góc trái-trên đã đúng.
        mTarget.getLocationOnScreen(mLoc);
        final Rect rect = new Rect(mLoc[0], mLoc[1], mLoc[0] + w, mLoc[1] + h);

        final float corner = (mCornerRadiusPx < 0f) ? h / 2f : mCornerRadiusPx; // pill động.
        final boolean cornerChanged = Math.abs(corner - mCurrentCorner) > 0.5f;

        if (mAdded && rect.equals(mLastRect) && !cornerChanged) return; // không đổi -> bỏ qua.
        mLastRect.set(rect);
        mCurrentCorner = corner;

        if (mPanel == null) {
            mPanel = new BlurPanel(mTarget.getContext());
            mPanelLp = buildLp();
        }
        mPanelLp.token = mTarget.getWindowToken(); // gán lại mỗi lần (đổi khi recreate).
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
                if (cornerChanged) mPanel.applyCorner();
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
        lp.token = mTarget.getWindowToken();
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
        if (!Settings.isDesktopBlurEnable(mTarget.getContext())) return null; // tắt blur -> fallback.
        if (Build.VERSION.SDK_INT < 31) return null;
        unlockHiddenApi();
        try {
            Object vri = View.class.getMethod("getViewRootImpl").invoke(panel);
            if (vri == null) return null;
            Method create = vri.getClass().getDeclaredMethod("createBackgroundBlurDrawable");
            create.setAccessible(true);
            Drawable dr = (Drawable) create.invoke(vri);
            if (dr == null) return null;
            dr.getClass().getMethod("setBlurRadius", int.class).invoke(dr, mBlurRadiusPx);
            dr.getClass().getMethod("setCornerRadius", float.class).invoke(dr, mCurrentCorner);
            dr.getClass().getMethod("setColor", int.class).invoke(dr, mTintColor);
            return dr;
        } catch (Throwable t) {
            android.util.Log.e(TAG, "createBackgroundBlurDrawable fail: " + t);
            return null;
        }
    }

    /** Overlay: nền = BackgroundBlurDrawable (mờ trong khung, bo góc động) + viền glass vẽ đè. */
    private class BlurPanel extends View {
        private final Paint mStroke;
        private final RectF mRect = new RectF();
        /** Giữ ref blur drawable để cập nhật corner khi pill co giãn. */
        private Drawable mBlurDrawable;

        BlurPanel(Context c) {
            super(c);
            mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStroke.setStyle(Paint.Style.STROKE);
            mStroke.setStrokeWidth(mStrokeWidth);
            mStroke.setColor(mStrokeColor);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), mCurrentCorner);
                }
            });
            setClipToOutline(true);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            Drawable blur = createBackgroundBlurDrawable(this);
            if (blur != null) {
                mBlurDrawable = blur;
                setBackground(blur);
            } else {
                // Máy chặn hidden-API/không hỗ trợ -> gỡ overlay, dùng background cũ.
                post(GlassBlurWindowController.this::switchToFallback);
            }
        }

        /** Cập nhật bo góc (pill động theo height) cho blur drawable + outline + viền. */
        void applyCorner() {
            if (mBlurDrawable != null) {
                try {
                    mBlurDrawable.getClass().getMethod("setCornerRadius", float.class)
                            .invoke(mBlurDrawable, mCurrentCorner);
                } catch (Throwable ignore) {
                }
            }
            invalidateOutline();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float half = mStrokeWidth / 2f;
            mRect.set(half, half, getWidth() - half, getHeight() - half);
            float r = Math.max(0, mCurrentCorner - half);
            canvas.drawRoundRect(mRect, r, r, mStroke);
        }
    }
}
