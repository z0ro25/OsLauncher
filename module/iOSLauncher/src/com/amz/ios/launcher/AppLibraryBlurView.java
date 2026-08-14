package com.amz.ios.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.amz.ios.launcher.config.Settings;

import java.lang.reflect.Method;

/**
 * Nền kính mờ frosted TOÀN MÀN cho App Library — hiệu ứng iOS: blur HÌNH NỀN THẬT phía sau
 * (thấy màu wallpaper sống động xuyên qua) + tint tối nhẹ cho chữ/icon dễ đọc.
 *
 * <p>Tái sử dụng NGUYÊN kỹ thuật của {@link DockBlurView} nhưng RIÊNG cho App Library (SUPREME
 * RULE 2 — không đụng {@code mSliderBlurBg}/BlurScreenLayout đang DÙNG CHUNG với negative page):
 * <ul>
 *   <li>Overlay là WINDOW RIÊNG {@code TYPE_APPLICATION_MEDIA} — compositor xếp DƯỚI window chính
 *       (App Library + icon vẫn nét) nhưng TRÊN wallpaper → chỉ wallpaper bị mờ.</li>
 *   <li>{@code ViewRootImpl.createBackgroundBlurDrawable} (@hide) mở khoá bằng HiddenApiBypass; đặt
 *       làm background → hệ thống blur nền TRONG bounds (đây là full-screen nên phủ cả màn).</li>
 * </ul>
 * Không bo góc, không viền (khác dock): App Library chiếm trọn màn.
 *
 * <p>View này chỉ là "anchor" vô hình trong cây launcher. {@link #setBlurFraction(float)} do
 * {@code Launcher.onAppsLibrarySlide()} gọi (0 = đóng → 1 = mở hẳn): fraction &gt; 0 thì thêm/hiện
 * overlay và chỉnh cường độ blur + tint theo tiến trình vuốt; = 0 thì gỡ overlay.
 */
public class AppLibraryBlurView extends View {

    private static final String TAG = "AppLibBlur";
    /** Bán kính blur tối đa (px) khi mở hẳn. Full màn nên cần mạnh hơn dock (60) để đủ mờ. */
    private static final int MAX_BLUR_RADIUS = 100;
    /** Alpha tint TRẮNG tối đa khi mở hẳn — khớp tone kính dock ({@code PANEL_TINT = 0x1AFFFFFF}). */
    private static final int MAX_TINT_ALPHA = 0x1A; // 26/255 ~10% trắng, giống hotseat

    private static boolean sHiddenApiUnlocked;

    private final int[] mLoc = new int[2];
    private final Rect mLastRect = new Rect();

    private WindowManager mWm;
    private BlurPanel mPanel;
    private WindowManager.LayoutParams mPanelLp;
    private boolean mAdded;
    private float mFraction;

    public AppLibraryBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removePanel();
    }

    /**
     * Cường độ nền frosted theo tiến trình vuốt App Library. 0 = đóng (gỡ overlay), 1 = mở hẳn
     * (blur mạnh + tint đầy). Gọi mỗi frame trong lúc kéo → nhẹ (chỉ set field của drawable).
     */
    public void setBlurFraction(float fraction) {
        mFraction = Math.max(0f, Math.min(1f, fraction));
        if (mFraction <= 0f) {
            removePanel();
            return;
        }
        syncPanel();
        applyIntensity();
    }

    /** Đặt overlay window phủ TOÀN MÀN. Anchor giữ 0x0 (để không chắn vuốt-đóng App Library) nên
     *  KHÔNG lấy size từ getWidth()/getHeight() mà lấy từ root decor view — full-screen thật. */
    private void syncPanel() {
        if (mWm == null || !isAttachedToWindow()) return;
        final View root = getRootView();
        final int w = root != null ? root.getWidth() : 0;
        final int h = root != null ? root.getHeight() : 0;
        if (w <= 0 || h <= 0) return;

        root.getLocationOnScreen(mLoc);
        final Rect rect = new Rect(mLoc[0], mLoc[1], mLoc[0] + w, mLoc[1] + h);

        if (mPanel == null) {
            mPanel = new BlurPanel(getContext());
            mPanelLp = buildLp();
        }
        if (mAdded && rect.equals(mLastRect)) return;
        mLastRect.set(rect);
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

    /** Chỉnh bán kính blur + tint của drawable theo fraction hiện tại (nếu compositor hỗ trợ). */
    private void applyIntensity() {
        if (mPanel == null) return;
        mPanel.setIntensity(mFraction);
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

    /**
     * Mở khoá hidden-API bằng HiddenApiBypass (LSPosed) — kỹ thuật native, cho phép gọi
     * ViewRootImpl.createBackgroundBlurDrawable (@hide) trên máy stock. Gọi 1 lần, im lặng nếu
     * thất bại (fallback: panel còn tint tối, không blur).
     */
    private static void unlockHiddenApi() {
        if (sHiddenApiUnlocked) return;
        try {
            org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("L");
            sHiddenApiUnlocked = true;
        } catch (Throwable t) {
            android.util.Log.e(TAG, "unlockHiddenApi fail: " + t);
        }
    }

    /**
     * Tạo BackgroundBlurDrawable của compositor cho window {@code panel}. Đặt làm background → hệ
     * thống blur nền phía sau window (chỉ wallpaper vì panel là MEDIA sub-window) trong bounds
     * full-screen. Trả null nếu không hỗ trợ.
     */
    private Drawable createBackgroundBlurDrawable(View panel) {
        if (!Settings.isDesktopBlurEnable(getContext())) return null; // tắt blur -> fallback scrim (không blur)
        if (Build.VERSION.SDK_INT < 31) return null;
        unlockHiddenApi();
        try {
            Object vri = View.class.getMethod("getViewRootImpl").invoke(panel);
            if (vri == null) return null;
            Method create = vri.getClass().getDeclaredMethod("createBackgroundBlurDrawable");
            create.setAccessible(true);
            return (Drawable) create.invoke(vri);
        } catch (Throwable t) {
            android.util.Log.e(TAG, "createBackgroundBlurDrawable fail: " + t);
            return null;
        }
    }

    /** Overlay: nền = BackgroundBlurDrawable (mờ wallpaper) + tint tối phủ theo cường độ. */
    private class BlurPanel extends View {
        private Drawable mBlur;
        private Method mSetBlurRadius;
        private Method mSetColor;
        private float mIntensity;

        BlurPanel(Context c) {
            super(c);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            // Có ViewRootImpl rồi mới tạo được blur drawable.
            mBlur = createBackgroundBlurDrawable(this);
            if (mBlur != null) {
                try {
                    mSetBlurRadius = mBlur.getClass().getMethod("setBlurRadius", int.class);
                    mSetColor = mBlur.getClass().getMethod("setColor", int.class);
                } catch (Throwable t) {
                    android.util.Log.e(TAG, "resolve blur setters fail: " + t);
                }
                setBackground(mBlur);
            }
            applyIntensity(mIntensity);
        }

        void setIntensity(float fraction) {
            mIntensity = fraction;
            applyIntensity(fraction);
        }

        private void applyIntensity(float fraction) {
            // ARGB TRẮNG (giống dock PANEL_TINT=0x1AFFFFFF), alpha theo fraction.
            int tint = (((int) (MAX_TINT_ALPHA * fraction)) << 24) | 0x00FFFFFF;
            if (mBlur != null && mSetBlurRadius != null && mSetColor != null) {
                try {
                    mSetBlurRadius.invoke(mBlur, (int) (MAX_BLUR_RADIUS * fraction));
                    mSetColor.invoke(mBlur, tint);
                    invalidate();
                    return;
                } catch (Throwable ignore) {
                    // rơi xuống fallback vẽ tint bằng canvas
                }
            }
            // Fallback (không có compositor blur): chỉ phủ scrim tối bằng onDraw.
            setBackgroundColor(tint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Nền do drawable/setBackgroundColor lo; không cần vẽ thêm.
        }
    }
}
