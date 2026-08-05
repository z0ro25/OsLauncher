package com.amz.ios.launcher.widget.view;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.util.BlurBuilder;

import java.io.File;
import java.io.FileInputStream;

/**
 * Nền kính kiểu iOS: FILL là ẢNH WALLPAPER ĐÃ LÀM MỜ crop đúng vùng view đứng, kèm bo góc
 * + viền glass. KHÔNG dùng {@link com.github.mmin18.widget.RealtimeBlurView} vì launcher bật
 * {@code windowShowWallpaper=true}: wallpaper do HỆ THỐNG vẽ SAU cửa sổ (ngoài decorView),
 * nên realtime-blur không có gì để làm mờ -> chỉ ra mảng tint phẳng ("không thấy blur").
 *
 * <p>Cách làm (giống màn trái / App Library trong repo): lấy wallpaper đã blur từ
 * {@link com.amz.ios.launcher.util.BlurWallpaperProvider#mBlurBmp} (fallback: cache "blur"
 * đã lưu, hoặc tự đọc {@link WallpaperManager#getDrawable()} rồi {@link BlurBuilder#fastBlur}),
 * scale về đúng kích thước màn, cache tĩnh dùng chung cho mọi GlassBlurView. Khi vẽ: lấy vị trí
 * view trong cửa sổ -> crop đúng ô wallpaper tương ứng -> clip bo góc -> vẽ + tint + viền.
 */
public class GlassBlurView extends View {

    private float mCornerRadius;
    private float mBorderWidth;
    private int mBorderColor;
    private int mOverlayColor;
    /** Ảnh nền theme (bg_glass) vẽ khi KHÔNG dựng được blur; null = không có fallback. */
    private Drawable mFallbackDrawable;
    /** Tỉ lệ dọc cố định để crop wallpaper (0..1); &lt;0 = crop theo vị trí thật của view. */
    private float mSampleAnchorFraction;

    private final RectF mRectF = new RectF();
    private final Rect mSrcRect = new Rect();
    private final Path mPath = new Path();
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final int[] mLoc = new int[2];

    /** Wallpaper đã blur, scale về đúng kích thước màn. Dùng CHUNG mọi GlassBlurView. */
    private static Bitmap sScreenBlur;
    private static int sScreenW, sScreenH;
    private boolean mScheduledReload;

    public GlassBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = context.getResources().getDisplayMetrics().density;
        float defCorner = 18 * density;
        float defBorder = 1 * density;
        int defBorderColor = 0x40FFFFFF;
        int defOverlay = 0x00000000; // không tint màu — chỉ blur wallpaper

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassBlurView);
        mCornerRadius = a.getDimension(R.styleable.GlassBlurView_gbvCornerRadius, defCorner);
        mBorderWidth = a.getDimension(R.styleable.GlassBlurView_gbvBorderWidth, defBorder);
        mBorderColor = a.getColor(R.styleable.GlassBlurView_gbvBorderColor, defBorderColor);
        mOverlayColor = a.getColor(R.styleable.GlassBlurView_gbvOverlayColor, defOverlay);
        mFallbackDrawable = a.getDrawable(R.styleable.GlassBlurView_gbvFallbackDrawable);
        mSampleAnchorFraction = a.getFloat(R.styleable.GlassBlurView_gbvSampleAnchorFraction, -1f);
        a.recycle();

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);
    }

    public void setCornerRadius(float radiusPx) {
        mCornerRadius = radiusPx;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int w = getWidth();
        final int h = getHeight();
        if (w == 0 || h == 0) return;

        mRectF.set(0, 0, w, h);
        mPath.reset();
        mPath.addRoundRect(mRectF, mCornerRadius, mCornerRadius, Path.Direction.CW);

        int save = canvas.save();
        canvas.clipPath(mPath);

        Bitmap blur = ensureScreenBlur();
        if (blur != null && !blur.isRecycled()) {
            if (mSampleAnchorFraction >= 0f) {
                // Ô search ở ĐỈNH màn: wallpaper vùng status bar tối + loang màu -> lệch tông.
                // Thay vì crop theo vị trí thật, lấy 1 Ô VUÔNG NHỎ (cạnh = chiều cao view) ở
                // GIỮA màn theo tỉ lệ dọc anchor (nơi pod đứng, xám phẳng) rồi kéo giãn phủ pill
                // -> cho ra 1 tông PHẲNG đúng bằng tông pod, đồng bộ với panel dưới.
                int side = Math.max(1, Math.min(h, Math.min(blur.getWidth(), blur.getHeight())));
                int cx = blur.getWidth() / 2;
                int cy = Math.round(mSampleAnchorFraction * blur.getHeight());
                int left = Math.max(0, Math.min(cx - side / 2, blur.getWidth() - side));
                int top = Math.max(0, Math.min(cy - side / 2, blur.getHeight() - side));
                mSrcRect.set(left, top, left + side, top + side);
            } else {
                // Vị trí view trong cửa sổ = vị trí trên màn (launcher fullscreen) -> crop wallpaper.
                getLocationInWindow(mLoc);
                int left = Math.max(0, Math.min(mLoc[0], blur.getWidth() - 1));
                int top = Math.max(0, Math.min(mLoc[1], blur.getHeight() - 1));
                int right = Math.min(blur.getWidth(), left + w);
                int bottom = Math.min(blur.getHeight(), top + h);
                mSrcRect.set(left, top, right, bottom);
            }
            // Vẽ lớp blur BÁN TRONG SUỐT để wallpaper THẬT (hệ thống vẽ sau cửa sổ trong
            // suốt, windowShowWallpaper=true) lộ qua & hòa màu -> kính "đè lên nền" kiểu
            // iOS 26 (thấy nền phía sau, mờ nhẹ) thay vì tấm panel màu rời.
            mBmpPaint.setAlpha(150);
            canvas.drawBitmap(blur, mSrcRect, mRectF, mBmpPaint);
            mBmpPaint.setAlpha(255);
        } else if (mFallbackDrawable != null) {
            // KHÔNG dựng được blur (máy chặn đọc wallpaper): vẽ ảnh nền theme (bg_glass)
            // — bản -night tự đổi cho dark mode — thay vì để trống. Clip bo góc sẵn ở trên.
            mFallbackDrawable.setBounds(0, 0, w, h);
            mFallbackDrawable.draw(canvas);
        }
        // Lớp tint kính (trên ảnh blur, hoặc đứng một mình nếu chưa có blur -> vẫn "kính mờ").
        canvas.drawColor(mOverlayColor);
        canvas.restoreToCount(save);

        // Viền glass chạy dọc bo góc (thụt nửa stroke để không bị cắt).
        if (mBorderWidth > 0) {
            float inset = mBorderWidth / 2f;
            mRectF.set(inset, inset, w - inset, h - inset);
            float r = Math.max(0, mCornerRadius - inset);
            canvas.drawRoundRect(mRectF, r, r, mBorderPaint);
        }
    }

    /** Lấy/ dựng bitmap wallpaper-đã-blur scale về kích thước màn hiện tại (cache tĩnh). */
    private Bitmap ensureScreenBlur() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        if (sScreenBlur != null && !sScreenBlur.isRecycled()
                && sScreenW == sw && sScreenH == sh) {
            return sScreenBlur;
        }
        Bitmap src = obtainBlurredWallpaper();
        if (src == null || src.isRecycled()) {
            return sScreenBlur; // có thể null lần đầu -> onDraw chỉ vẽ tint, đã lịch reload.
        }
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(src, sw, sh, true);
            sScreenBlur = scaled;
            sScreenW = sw;
            sScreenH = sh;
            return sScreenBlur;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Wallpaper đã blur từ nhiều nguồn dự phòng; kích hoạt reload nếu chưa sẵn. */
    private Bitmap obtainBlurredWallpaper() {
        // 1) Provider của launcher (wallpaper blur có màu sống động, chuẩn nhất).
        try {
            Launcher launcher = Launcher.getLauncher(getContext());
            if (launcher != null && launcher.getBlurWallpaperProvider() != null) {
                Bitmap b = launcher.getBlurWallpaperProvider().mBlurBmp;
                if (b != null && !b.isRecycled()) return b;
                scheduleReload(launcher);
            }
        } catch (Throwable ignored) {
        }
        // 2) Cache "blur" đã lưu ở getDir("image")/"blur".
        Bitmap stored = decodeStored("blur");
        if (stored != null && !stored.isRecycled()) return stored;
        // 3) Tự đọc wallpaper hệ thống (nếu máy còn cho) rồi blur nhanh.
        Bitmap raw = readSystemWallpaper();
        if (raw != null && !raw.isRecycled()) {
            Bitmap b = blurRaw(raw);
            if (b != null) return b;
        }
        // KHÔNG fallback về default_wallpaper: ảnh mặc định (bong bóng xanh) KHÁC wallpaper
        // thật người dùng đang đặt -> blur nó sẽ NHUỘM XANH sai màu lên dock/widget, và vì
        // luôn non-null nên chặn mất nhánh ảnh theme (mFallbackDrawable). Trả null để onDraw
        // dùng ảnh kính theme (bg_glass 9-patch, tự đổi -night) khi máy chặn đọc wallpaper.
        return null;
    }

    /** Đọc bitmap wallpaper hệ thống; trả null nếu Android chặn (getDrawable ném/không có quyền). */
    private Bitmap readSystemWallpaper() {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(getContext());
            if (wm == null || wm.getWallpaperInfo() != null) return null; // bỏ live wallpaper.
            Drawable d = wm.getDrawable();
            if (d instanceof BitmapDrawable) {
                Bitmap raw = ((BitmapDrawable) d).getBitmap();
                if (raw != null && !raw.isRecycled()) return raw;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Downsample mạnh + fastBlur -> kính mờ "đục" kiểu iOS. */
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

    private Bitmap decodeStored(String name) {
        try {
            File f = new File(new ContextWrapper(getContext()).getDir("image", Context.MODE_PRIVATE), name);
            if (!f.exists()) return null;
            return BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (Throwable t) {
            return null;
        }
    }

    private void scheduleReload(final Launcher launcher) {
        if (mScheduledReload) return;
        mScheduledReload = true;
        try {
            launcher.getBlurWallpaperProvider().reloadWallpaper();
        } catch (Throwable ignored) {
        }
        // Chờ provider dựng xong rồi vẽ lại (invalidate cache tĩnh để đọc nguồn 1).
        postDelayed(new Runnable() {
            @Override
            public void run() {
                sScreenBlur = null; // buộc dựng lại từ nguồn mới.
                mScheduledReload = false;
                invalidate();
            }
        }, 500);
    }
}
