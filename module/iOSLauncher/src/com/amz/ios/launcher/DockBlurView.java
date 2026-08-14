package com.amz.ios.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;

import com.amz.ios.launcher.config.Settings;
import com.github.mmin18.widget.RealtimeBlurView;

import java.lang.reflect.Method;

/**
 * Khung kính blur cho thanh dock (hotseat) — hiệu ứng glass kiểu iOS 26.
 *
 * <p>Kế thừa {@link RealtimeBlurView} để giữ nguyên id/attr trong XML NHƯNG KHÔNG dùng cơ chế
 * blur decor-view gốc (setBlurRadius(0)). Lý do đã khảo sát kỹ trên máy thật (Android 16):
 * <ul>
 *   <li>RealtimeBlurView chỉ làm mờ DECOR VIEW (icon/widget launcher) — wallpaper là window RIÊNG
 *       phía sau, không nằm trong decor → dock blur ra trống.</li>
 *   <li>WallpaperManager.getDrawable() ném SecurityException(READ_EXTERNAL_STORAGE) trên Android 13+.</li>
 *   <li>PixelCopy trên window launcher chỉ chụp surface TRONG SUỐT của chính launcher.</li>
 * </ul>
 *
 * <p><b>Giải pháp: window BACKGROUND blur (chỉ mờ TRONG khung, không phải cả màn hình).</b>
 * Phân biệt 2 API compositor (Android 12+):
 * <ul>
 *   <li>{@code setBlurBehindRadius}: mờ TOÀN màn hình phía sau window → SAI (cả wallpaper mờ).</li>
 *   <li>background-blur ({@code ViewRootImpl.createBackgroundBlurDrawable}): mờ nền chỉ TRONG bounds
 *       + bo góc của drawable → ĐÚNG (chỉ mờ vùng trong khối dock).</li>
 * </ul>
 * Cách dựng:
 * <ol>
 *   <li>Overlay là WINDOW RIÊNG {@code TYPE_APPLICATION_MEDIA} — compositor xếp DƯỚI window chính
 *       chứa icon (icon vẫn nét) nhưng TRÊN wallpaper (chỉ wallpaper trong khung bị mờ).</li>
 *   <li>{@code createBackgroundBlurDrawable} là API @hide → máy stock chặn reflection. Mở khoá bằng
 *       {@code VMRuntime.setHiddenApiExemptions("L")} (kỹ thuật FreeReflection) 1 lần lúc init.</li>
 *   <li>Drawable trả về set {@code setBlurRadius}/{@code setCornerRadius}/{@code setColor} rồi làm
 *       background cho panel → hệ thống blur nền trong bounds bo góc. Viền vẽ đè lên trên.</li>
 * </ol>
 *
 * <p>View này chỉ là "anchor" trong hotseat: Hotseat.positionDockGlass() gán size/vị trí; ta bám vị
 * trí THẬT trên màn hình để đặt overlay window trùng khít. onDraw của anchor để trống.
 */
public class DockBlurView extends RealtimeBlurView {

    private static final String TAG = "DockBlur";
    /** Bán kính background blur (px) — chỉ mờ phần nền TRONG khung dock. */
    private static final int BACKGROUND_BLUR_RADIUS = 60;
    /** Tint trắng rất nhẹ phủ lên nền glass (vẫn thấy wallpaper mờ xuyên qua). */
    private static final int PANEL_TINT = 0x1AFFFFFF;

    private static boolean sHiddenApiUnlocked;

    private final float mCornerRadius;
    private final float mStrokeWidth;
    private final int mStrokeColor;

    private final int[] mLoc = new int[2];
    private final Rect mLastRect = new Rect();

    private WindowManager mWm;
    private BlurPanel mPanel;
    private WindowManager.LayoutParams mPanelLp;
    private boolean mAdded;

    public DockBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCornerRadius = getResources().getDimension(R.dimen.dock_glass_corner_radius);
        mStrokeWidth = getResources().getDimension(R.dimen.dock_glass_stroke_width);
        mStrokeColor = getResources().getColor(R.color.dock_glass_stroke_color);
        setBlurRadius(0);
        setWillNotDraw(false);
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

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) removePanel();
        else post(this::syncPanel);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        post(this::syncPanel);
    }

    /** Đặt/di chuyển overlay window trùng khít vị trí THẬT của anchor trên màn hình. */
    private void syncPanel() {
        if (mWm == null || getVisibility() != VISIBLE) return;
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

    /**
     * Mở khoá hidden-API bằng HiddenApiBypass (LSPosed) — kỹ thuật native, mạnh hơn
     * VMRuntime.setHiddenApiExemptions (đã bị Android 12+ vá). Cho phép sau đó gọi
     * ViewRootImpl.createBackgroundBlurDrawable (@hide) trên máy stock. Gọi 1 lần, im lặng nếu thất
     * bại (fallback: panel còn tint + viền).
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
     * Tạo BackgroundBlurDrawable của compositor cho window của {@code panel} rồi cấu hình bán kính,
     * bo góc, tint. Drawable này khi làm background sẽ khiến hệ thống blur nền phía sau window (chỉ
     * wallpaper vì panel là MEDIA sub-window) trong bounds + bo góc. Trả null nếu không hỗ trợ.
     */
    private Drawable createBackgroundBlurDrawable(View panel) {
        if (!Settings.isDesktopBlurEnable(getContext())) return null; // tắt blur -> panel fallback (không blur)
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

    /** Nội dung overlay: nền = BackgroundBlurDrawable (mờ trong khung, bo góc) + viền vẽ đè. */
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
            // Có ViewRootImpl rồi mới tạo được blur drawable.
            Drawable blur = createBackgroundBlurDrawable(this);
            if (blur != null) setBackground(blur);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float half = mStrokeWidth / 2f;
            mRect.set(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(mRect, mCornerRadius - half, mCornerRadius - half, mStroke);
        }
    }

    /** Anchor trong suốt hoàn toàn (blur + viền nằm ở overlay window). */
    @Override
    public void onDraw(Canvas canvas) {
        // no-op.
    }

    /** Gọi lại khi wallpaper đổi — background-blur tự cập nhật, chỉ cần đồng bộ vị trí. */
    public void refreshBlur() {
        post(this::syncPanel);
    }
}
