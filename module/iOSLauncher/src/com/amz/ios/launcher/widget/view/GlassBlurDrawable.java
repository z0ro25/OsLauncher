package com.amz.ios.launcher.widget.view;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.util.BlurBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;

/**
 * Bản {@link Drawable} của {@link GlassBlurView}: nền kính iOS (wallpaper ĐÃ làm mờ, crop đúng
 * vùng view đứng + bo góc + viền glass). Dùng khi cần đặt kính làm {@code background} cho view
 * KHÔNG thể chèn view con phía sau (nút Edit/Done tự scale animation) hoặc view {@code wrap_content}
 * động (thanh page-indicator) — vì background scale/khớp bounds THEO view chủ, còn một
 * {@link GlassBlurView} anh-em không bám theo được.
 *
 * <p>Cơ chế mờ y hệt {@link GlassBlurView} (cùng nguồn wallpaper-blur, cùng cache tĩnh riêng của
 * lớp này — KHÔNG dùng chung {@code sScreenBlur} của GlassBlurView để tránh side-effect chéo). Vì
 * là Drawable không tự biết vị trí trên màn, ta giữ {@link WeakReference} tới view chủ và gọi
 * {@link View#getLocationInWindow(int[])} lúc vẽ để crop đúng ô wallpaper.
 */
public class GlassBlurDrawable extends Drawable {

    private final WeakReference<View> mHost;
    /** Bo góc px; &lt;0 = pill (r = height/2). */
    private final float mCornerRadius;
    private final float mBorderWidth;
    private final Drawable mFallbackDrawable;

    private final RectF mRectF = new RectF();
    private final Rect mSrcRect = new Rect();
    private final Path mPath = new Path();
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint mTintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mOverlayColor;
    private final int[] mLoc = new int[2];

    /** Wallpaper đã blur scale về đúng kích thước màn — RIÊNG cho GlassBlurDrawable. */
    private static Bitmap sScreenBlur;
    private static int sScreenW, sScreenH;
    private boolean mScheduledReload;

    public GlassBlurDrawable(View host, float cornerRadiusPx, float borderWidthPx,
                             int borderColor, int overlayColor, Drawable fallback) {
        mHost = new WeakReference<>(host);
        mCornerRadius = cornerRadiusPx;
        mBorderWidth = borderWidthPx;
        mOverlayColor = overlayColor;
        mFallbackDrawable = fallback;

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(borderColor);
        mBorderPaint.setStrokeWidth(borderWidthPx);
        mTintPaint.setStyle(Paint.Style.FILL);
        mTintPaint.setColor(overlayColor);
    }

    private float corner(int h) {
        return mCornerRadius >= 0f ? mCornerRadius : h / 2f;
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect b = getBounds();
        final int w = b.width();
        final int h = b.height();
        if (w <= 0 || h <= 0) return;

        final float r = corner(h);
        mRectF.set(b.left, b.top, b.right, b.bottom);
        mPath.reset();
        mPath.addRoundRect(mRectF, r, r, Path.Direction.CW);

        int save = canvas.save();
        canvas.clipPath(mPath);

        Bitmap blur = ensureScreenBlur();
        View host = mHost.get();
        if (blur != null && !blur.isRecycled() && host != null) {
            // Vị trí view trong cửa sổ = vị trí trên màn (launcher fullscreen) -> crop wallpaper.
            host.getLocationInWindow(mLoc);
            int left = Math.max(0, Math.min(mLoc[0], blur.getWidth() - 1));
            int top = Math.max(0, Math.min(mLoc[1], blur.getHeight() - 1));
            int right = Math.min(blur.getWidth(), left + w);
            int bottom = Math.min(blur.getHeight(), top + h);
            mSrcRect.set(left, top, right, bottom);
            // Bán trong suốt để wallpaper THẬT (hệ thống vẽ sau cửa sổ trong suốt) lộ qua & hòa màu.
            // Alpha cao hơn -> lớp wallpaper-đã-blur đậm hơn -> kính "đục"/mờ nhiều hơn.
            mBmpPaint.setAlpha(220);
            canvas.drawBitmap(blur, mSrcRect, mRectF, mBmpPaint);
            mBmpPaint.setAlpha(255);
        } else if (mFallbackDrawable != null) {
            mFallbackDrawable.setBounds(b);
            mFallbackDrawable.draw(canvas);
        }
        if (mOverlayColor != 0) {
            canvas.drawRect(mRectF, mTintPaint);
        }
        canvas.restoreToCount(save);

        if (mBorderWidth > 0) {
            float inset = mBorderWidth / 2f;
            mRectF.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            float rb = Math.max(0, r - inset);
            canvas.drawRoundRect(mRectF, rb, rb, mBorderPaint);
        }
    }

    /** Lấy/ dựng bitmap wallpaper-đã-blur scale về kích thước màn hiện tại (cache tĩnh). */
    private Bitmap ensureScreenBlur() {
        View host = mHost.get();
        if (host == null) return sScreenBlur;
        int sw = host.getResources().getDisplayMetrics().widthPixels;
        int sh = host.getResources().getDisplayMetrics().heightPixels;
        if (sScreenBlur != null && !sScreenBlur.isRecycled()
                && sScreenW == sw && sScreenH == sh) {
            return sScreenBlur;
        }
        Bitmap src = obtainBlurredWallpaper(host);
        if (src == null || src.isRecycled()) {
            return sScreenBlur; // null lần đầu -> vẽ fallback, đã lịch reload.
        }
        try {
            sScreenBlur = Bitmap.createScaledBitmap(src, sw, sh, true);
            sScreenW = sw;
            sScreenH = sh;
            return sScreenBlur;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Wallpaper đã blur từ nhiều nguồn dự phòng; kích hoạt reload nếu chưa sẵn. (như GlassBlurView) */
    private Bitmap obtainBlurredWallpaper(View host) {
        try {
            Launcher launcher = Launcher.getLauncher(host.getContext());
            if (launcher != null && launcher.getBlurWallpaperProvider() != null) {
                Bitmap b = launcher.getBlurWallpaperProvider().mBlurBmp;
                if (b != null && !b.isRecycled()) return b;
                scheduleReload(host, launcher);
            }
        } catch (Throwable ignored) {
        }
        Bitmap stored = decodeStored(host.getContext(), "blur");
        if (stored != null && !stored.isRecycled()) return stored;
        Bitmap raw = readSystemWallpaper(host.getContext());
        if (raw != null && !raw.isRecycled()) {
            Bitmap b = blurRaw(raw);
            if (b != null) return b;
        }
        return null;
    }

    private Bitmap readSystemWallpaper(Context ctx) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(ctx);
            if (wm == null || wm.getWallpaperInfo() != null) return null;
            Drawable d = wm.getDrawable();
            if (d instanceof BitmapDrawable) {
                Bitmap raw = ((BitmapDrawable) d).getBitmap();
                if (raw != null && !raw.isRecycled()) return raw;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Bitmap blurRaw(Bitmap raw) {
        try {
            int dw = Math.max(1, Math.round(raw.getWidth() * 0.22f));
            int dh = Math.max(1, Math.round(raw.getHeight() * 0.22f));
            Bitmap small = Bitmap.createScaledBitmap(raw, dw, dh, true);
            Bitmap blurred = BlurBuilder.fastBlur(small, 10);
            return blurred != null ? blurred : small;
        } catch (Throwable t) {
            return null;
        }
    }

    private Bitmap decodeStored(Context ctx, String name) {
        try {
            File f = new File(new ContextWrapper(ctx).getDir("image", Context.MODE_PRIVATE), name);
            if (!f.exists()) return null;
            return BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (Throwable t) {
            return null;
        }
    }

    private void scheduleReload(final View host, final Launcher launcher) {
        if (mScheduledReload) return;
        mScheduledReload = true;
        try {
            launcher.getBlurWallpaperProvider().reloadWallpaper();
        } catch (Throwable ignored) {
        }
        host.postDelayed(new Runnable() {
            @Override
            public void run() {
                sScreenBlur = null; // buộc dựng lại từ nguồn mới.
                mScheduledReload = false;
                invalidateSelf();
            }
        }, 500);
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(ColorFilter colorFilter) { }

    @Override
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
