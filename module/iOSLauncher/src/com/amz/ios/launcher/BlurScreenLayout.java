package com.amz.ios.launcher;

import static com.amz.ios.ioslite.common.AsyncHandler.runOnUiThread;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Keep;

import com.amz.ios.ioslite.common.launcher.Insettable;
import com.amz.ios.launcher.util.BlurBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class BlurScreenLayout extends InsettableFrameLayout implements ViewGroup.OnHierarchyChangeListener, Insettable {

    Paint mPaint;
    public boolean mIsNeedUpdateWindow;
    public int mLayoutRightPosition;
    public View mRootView;
    Handler mHandler1;
    Handler mHandler2;
    Handler.Callback mCallBack1;
    Handler.Callback mCallBack2;
    Launcher mLauncher;
    BitmapDrawable mBlurDrawable;

    public BlurScreenLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context instanceof Launcher) {
            mLauncher = (Launcher) context;
        }
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);

        mCallBack1 = new GetBlurCallBack(this);
        mCallBack2 = new SetBlurCallback(this);

        mHandler1 = new Handler(LauncherModel.sWorkerThread.getLooper(),mCallBack1);
        mHandler2 = new Handler(Looper.getMainLooper(), mCallBack2);
    }

    public static Bitmap getBlurBitmap(BlurScreenLayout blurScreenLayout, View view) {
        Bitmap curBlurBmp = null;
        Bitmap result = null;

        if (blurScreenLayout == null) return null;

        Launcher launcher = blurScreenLayout.mLauncher;
        DeviceProfile deviceProfile = launcher.getDeviceProfile();

        if (launcher.isOpeningFolder()) {
            if (launcher.showingFloatingMenu) {
                return BlurBuilder.getBlurBmp(
                        launcher,
                        blurScreenLayout.getBmpFromView(launcher.getDragLayer())
                );
            }
        }

        Bitmap blurBmp = blurScreenLayout.mLauncher.getBlurWallpaperProvider().mBlurBmp;

        if (blurBmp != null) {
            try {
                blurBmp = Bitmap.createBitmap(blurBmp, 0, 0, deviceProfile.getCurrentWidth(), deviceProfile.getCurrentHeight());
            } catch (Throwable th2) {
                return blurBmp;
            }
        }

        if ((blurBmp == null || blurBmp.isRecycled()) && (blurBmp = blurScreenLayout.getBlurImageFromStorage()) == null) {
            blurBmp = BitmapFactory.decodeResource(blurScreenLayout.getResources(), R.drawable.blur_default);
        }

        // Màn trái (negative screen) kiểu iOS: dùng ĐÚNG bitmap frosted MÀU sẽ hiển thị
        // lúc mở hẳn (mBlurBmp + base đen + scrim). Nhờ vậy mSliderBlurBg fade-in mượt
        // khi kéo và KHÔNG "pop" đổi hình khi mở hẳn -> animation vuốt giống App Library.
        if (launcher.isOpeningLeftPage() && !launcher.isOpeningAppsLibrary()) {
            Bitmap leftPageBmp = blurScreenLayout.getLeftPageFrostedBitmap();
            if (leftPageBmp != null) return leftPageBmp;
            if (blurBmp != null) return blurBmp;
        }

        curBlurBmp = BlurBuilder.getBlurBmp(
                launcher,
                view != null ? blurScreenLayout.makeRoundBitmap(
                        blurScreenLayout.getBmpFromView(launcher.getDragLayer()),
                        view
                ) : blurScreenLayout.getBmpFromView(launcher.getDragLayer()));

        if (blurBmp == null || curBlurBmp == null) {
            if (blurBmp != null) {
                result = blurBmp;
            }
            else
                result = curBlurBmp;
        } else {
            try {
                result = blurScreenLayout.getMixedBmp(blurBmp, curBlurBmp);
            } catch (Throwable th3) {
                return result;
            }
        }
        return result;
    }

    @Override
    public final void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mIsNeedUpdateWindow) {
            int width = getWidth();
            canvas.drawRect(width - this.mLayoutRightPosition, 0.0f, width, getHeight(), this.mPaint);
        }
    }

    @Override
    public final boolean fitSystemWindows(Rect rect) {
        this.mIsNeedUpdateWindow = rect.right > 0 && (!(Build.VERSION.SDK_INT >= 23) || ((ActivityManager) getContext().getSystemService(ActivityManager.class)).isLowRamDevice());
        this.mLayoutRightPosition = rect.right;
        setInsets(this.mIsNeedUpdateWindow ? new Rect(0, rect.top, 0, rect.bottom) : rect);
        if (this.mRootView != null && this.mIsNeedUpdateWindow) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mRootView.getLayoutParams();
            int leftMargin = marginLayoutParams.leftMargin;
            int left = rect.left;
            if (leftMargin != left || marginLayoutParams.rightMargin != rect.right) {
                marginLayoutParams.leftMargin = left;
                marginLayoutParams.rightMargin = rect.right;
                this.mRootView.setLayoutParams(marginLayoutParams);
            }
        }
        return true;
    }

    @Override
    public final void onFinishInflate() {
        if (getChildCount() > 0) {
            this.mRootView = getChildAt(0);
        }
        super.onFinishInflate();
    }

    @Override
    @Keep
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
    }
    public final Bitmap getMixedBmp(Bitmap src1, Bitmap src2) {
        try {
            int width = src2.getWidth();
            int height = src2.getHeight();
            Bitmap dst = Bitmap.createBitmap(width, height, src2.getConfig());
            Canvas canvas = new Canvas(dst);
            canvas.drawBitmap(src1, (Rect) null, new RectF(0.0f, 0.0f, width, height), (Paint) null);
            canvas.drawBitmap(src2, 0.0f, 0.0f, (Paint) null);
            return dst;
        } catch (Throwable unused) {
            return src2;
        }
    }

    public synchronized static Bitmap getAppsLibraryBlurBg(BlurScreenLayout layout){
        if (layout == null) return null;
        return layout.getAppsLibraryBlurBackground();
    }

    // Nền kính mờ "đục" GIỐNG App Library để màn trái (negative page) dùng chung: wallpaper
    // làm mờ mạnh + scrim tối. App Library đặc là nhờ root layout có nền base @color/black
    // bên dưới bitmap; màn trái không có base nên bitmap (có thể chỉ là frost bán trong suốt
    // khi API 36 chặn đọc wallpaper) để lộ wallpaper sắc nét. -> Trả LayerDrawable: nền tối
    // ĐẶC ở dưới + bitmap frosted ở trên, đảm bảo đục đồng nhất với App Library.
    // Bitmap frosted MÀU cho màn trái: dùng ĐÚNG nguồn App Library dùng — mBlurBmp
    // (wallpaper đã blur bởi BlurWallpaperProvider, có màu sống động) + scrim tối baked-in,
    // crop về đúng kích thước màn. Fallback: ảnh blur lưu sẵn -> blur_default. Dùng CHUNG
    // cho mSliderBlurBg (fade khi kéo) và nền CustomContentView (lúc mở hẳn) để 2 pha
    // GIỐNG HỆT nhau -> không bị "pop", animation vuốt mượt như App Library.
    public Bitmap getLeftPageFrostedBitmap() {
        try {
            Bitmap src = mLauncher.getBlurWallpaperProvider().mBlurBmp;
            if (src == null || src.isRecycled()) src = getBlurImageFromStorage();
            if (src == null || src.isRecycled())
                src = BitmapFactory.decodeResource(getResources(), R.drawable.blur_default);
            if (src == null || src.isRecycled()) return null;
            int w = mLauncher.getDeviceProfile().getCurrentWidth();
            int h = mLauncher.getDeviceProfile().getCurrentHeight();
            Bitmap sized;
            try {
                sized = Bitmap.createBitmap(src, 0, 0,
                        Math.min(w, src.getWidth()), Math.min(h, src.getHeight()));
            } catch (Throwable t) {
                sized = Bitmap.createScaledBitmap(src, w, h, true);
            }
            // Vẽ base đen + bitmap + scrim vào 1 bitmap ĐẶC (opaque) để mSliderBlurBg
            // hiển thị đầy đủ, không lộ layer sau.
            Bitmap out = Bitmap.createBitmap(sized.getWidth(), sized.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            c.drawColor(0xFF000000);
            c.drawBitmap(sized, 0, 0, null);
            c.drawColor(LEFT_PAGE_SCRIM);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    public android.graphics.drawable.Drawable getFrostedWallpaperDrawable() {
        try {
            Bitmap bmp = getLeftPageFrostedBitmap();
            if (bmp == null || bmp.isRecycled()) return null;
            return new BitmapDrawable(getResources(), bmp);
        } catch (Throwable t) {
            return null;
        }
    }

    public Bitmap getAppsLibraryBlurBackground() {
        try {
            // App Library kiểu iOS: nền là HÌNH NỀN THẬT làm mờ nhẹ (thấy màu wallpaper
            // sống động xuyên qua như frosted glass), KHÔNG blur ảnh drag-layout nền đen
            // (cho ra mảng xám mất màu). Fallback về cách cũ nếu chưa có wallpaper lưu.
            Bitmap wp = getLeftPageWallpaper(mLauncher.getDeviceProfile());
            if (wp != null && !wp.isRecycled()) return wp;
            return BlurBuilder.getBlurBmp(mLauncher, getBmpFromView(mLauncher.mDragAppsLibraryLayout));
        } catch (Throwable th) {
            th.getMessage();
            return null;
        }
    }

    private Bitmap getBlurImageFromStorage() {
        try {
            return BitmapFactory.decodeStream(
                    new FileInputStream(
                            new File(
                                    new ContextWrapper(this.mLauncher).getDir("image", Context.MODE_PRIVATE),
                                    "blur"
                            )
                    )
            );
        } catch (FileNotFoundException e) {
            e.getMessage();
            this.mLauncher.getBlurWallpaperProvider().reloadWallpaper();
            return null;
        } catch (Throwable th) {
            th.getMessage();
            return null;
        }
    }

    // Nạp ảnh hình nền GỐC (chưa làm mờ mạnh) đã lưu ở getDir("image")/"wallpaper".
    private Bitmap getOriginalWallpaperFromStorage() {
        try {
            return BitmapFactory.decodeStream(
                    new FileInputStream(
                            new File(
                                    new ContextWrapper(this.mLauncher).getDir("image", Context.MODE_PRIVATE),
                                    "wallpaper"
                            )
                    )
            );
        } catch (FileNotFoundException e) {
            this.mLauncher.getBlurWallpaperProvider().reloadWallpaper();
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    // Đọc wallpaper ĐANG DÙNG (live) từ hệ thống, không phải file cache cũ.
    // File cache getDir("image")/"wallpaper" chỉ cập nhật khi reloadWallpaper() chạy,
    // nên có thể là ảnh nền CŨ (xám) trong khi màn home vẽ wallpaper mới nhiều màu.
    // -> Ưu tiên WallpaperManager.getDrawable() để nền màn trái ăn đúng màu wallpaper hiện tại.
    private Bitmap getLiveWallpaperBitmap() {
        try {
            android.app.WallpaperManager wm = android.app.WallpaperManager.getInstance(mLauncher);
            if (wm == null) return null;
            // Bỏ qua live wallpaper (dạng service) — getDrawable trả null/không phải bitmap.
            if (wm.getWallpaperInfo() != null) return null;
            android.graphics.drawable.Drawable d = wm.getDrawable();
            if (d instanceof BitmapDrawable) {
                Bitmap b = ((BitmapDrawable) d).getBitmap();
                if (b != null && !b.isRecycled()) return b;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // Lớp "frost" bán trong suốt để hình nền HỆ THỐNG (đang được vẽ xuyên qua cửa sổ
    // launcher trong suốt, windowShowWallpaper=true) HIỆN LÊN sống động thay vì bị phủ
    // mảng xám. Dùng khi không đọc được bitmap wallpaper (Android 14+/API 36 chặn
    // WallpaperManager.getDrawable() -> trả ảnh xám mặc định). 1x1 ARGB, view sẽ kéo dãn.
    private Bitmap makeFrostBitmap(int argb) {
        Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(argb);
        return bmp;
    }

    // Lớp phủ tối (scrim) đặt lên trên wallpaper đã blur để ra "kính mờ đục kiểu iOS":
    // wallpaper mờ mạnh + tối vừa phải, các pod/widget bên trên nổi rõ. ~28% đen.
    private static final int LEFT_PAGE_SCRIM = 0x48000000;

    // Vẽ lớp scrim tối lên trên bitmap (in-place). Trả lại chính bitmap đó.
    private Bitmap applyScrim(Bitmap bmp, int scrimColor) {
        try {
            if (bmp == null) return null;
            Bitmap target = bmp.isMutable() ? bmp : bmp.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(target);
            canvas.drawColor(scrimColor);
            return target;
        } catch (Throwable t) {
            return bmp;
        }
    }

    // Nền cho màn trái (negative screen) & App Library kiểu iOS: wallpaper mờ MẠNH
    // (frosted glass) + lớp phủ tối vừa phải để các pod/widget nổi rõ, đồng nhất 2 màn.
    // Cache bitmap kính mờ ĐẸP gần nhất dựng được. App Library hay bắt được wallpaper
    // (nên nền mờ sáng, có màu), còn màn trái đôi khi rơi vào fallback tối (API 36 đọc
    // wallpaper chập chờn) -> 2 màn lệch tông. Dùng chung cache này để màn trái tái dùng
    // ĐÚNG bitmap App Library đã dựng, đảm bảo đồng nhất.
    private static Bitmap sLastGoodLeftPageBmp;

    private Bitmap getLeftPageWallpaper(DeviceProfile deviceProfile) {
        Bitmap original = getLiveWallpaperBitmap();
        if (original == null || original.isRecycled()) original = getOriginalWallpaperFromStorage();
        // Không đọc được wallpaper (API 36 chặn): ưu tiên tái dùng bitmap ĐẸP gần nhất
        // (để 2 màn giống hệt); nếu chưa có thì mới trả frost tối mờ ~40% đen.
        if (original == null || original.isRecycled()) {
            if (sLastGoodLeftPageBmp != null && !sLastGoodLeftPageBmp.isRecycled())
                return sLastGoodLeftPageBmp;
            return makeFrostBitmap(0x66000000);
        }
        try {
            int w = deviceProfile.getCurrentWidth();
            int h = deviceProfile.getCurrentHeight();
            Bitmap scaled = Bitmap.createScaledBitmap(original, w, h, true);
            // Downsample mạnh hơn (12%) + blur radius lớn hơn (25) -> kính mờ "đục" iOS,
            // không còn nhìn rõ chi tiết wallpaper gây rối mắt.
            int dw = Math.max(1, Math.round(w * 0.12f));
            int dh = Math.max(1, Math.round(h * 0.12f));
            Bitmap small = Bitmap.createScaledBitmap(scaled, dw, dh, true);
            Bitmap blurred = BlurBuilder.fastBlur(small, 25);
            if (blurred == null) blurred = small;
            Bitmap upscaled = Bitmap.createScaledBitmap(blurred, w, h, true);
            Bitmap result = applyScrim(upscaled, LEFT_PAGE_SCRIM);
            if (result != null && !result.isRecycled()) sLastGoodLeftPageBmp = result;
            return result;
        } catch (Throwable t) {
            if (sLastGoodLeftPageBmp != null && !sLastGoodLeftPageBmp.isRecycled())
                return sLastGoodLeftPageBmp;
            return original;
        }
    }

    public final Bitmap getBmpFromView(View view) {
        Bitmap blurBmp = BlurBuilder.getBlurBmp(view);
        if (blurBmp == null && Build.VERSION.SDK_INT >= 26) {
            blurBmp = BlurBuilder.getCopiedBmpFromView(this.mLauncher.getWindow(), view);
        }
        if (blurBmp == null && this.mLauncher.getDragController() != null) {
            view.clearFocus();
            view.setPressed(false);
            boolean willNotCacheDrawing = view.willNotCacheDrawing();
            view.setWillNotCacheDrawing(false);
            int drawingCacheBackgroundColor = view.getDrawingCacheBackgroundColor();
            view.setDrawingCacheBackgroundColor(0);
            float alpha = view.getAlpha();
            view.setAlpha(1.0f);
//            if (drawingCacheBackgroundColor != 0) {
//                view.destroyDrawingCache();
//            }
//            view.buildDrawingCache();
            Bitmap drawingCache = view.getDrawingCache();
            if (drawingCache == null) {
                blurBmp = null;
            } else {
                Bitmap createBitmap = Bitmap.createBitmap(drawingCache);
                view.destroyDrawingCache();
                view.setAlpha(alpha);
                view.setWillNotCacheDrawing(willNotCacheDrawing);
                view.setDrawingCacheBackgroundColor(drawingCacheBackgroundColor);
                blurBmp = createBitmap;
            }
        }
        if (blurBmp == null) {
            View decorView = this.mLauncher.getWindow().getDecorView();
            decorView.setDrawingCacheEnabled(true);
            decorView.buildDrawingCache();
            Bitmap createBitmap2 = Bitmap.createBitmap(decorView.getDrawingCache());
            decorView.setDrawingCacheEnabled(false);
            return createBitmap2;
        }
        return blurBmp;
    }

    public final void clear(boolean willRemove) {
        if (this.mLauncher.isOpeningSearchView() && getBackground() == null) {
            return;
        }
        Handler handler = this.mHandler1;
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", 0.0f).setDuration((long) ((getAlpha() > 0.0f ? getAlpha() : 1.0f) * 255.0f));
        animator.addListener(new SearchAnimatorListenerAdapter(this, willRemove));
        if (animator.isRunning()) {
            return;
        }
        animator.start();
    }

    public final void clearBlur() {
        this.mBlurDrawable = null;
        setBackground(null);
    }

    class SearchAnimatorListenerAdapter extends AnimatorListenerAdapter {

        public final boolean willRemove;
        public final BlurScreenLayout mBlurScreen;

        public SearchAnimatorListenerAdapter(BlurScreenLayout blurScreenLayout, boolean flag) {
            this.mBlurScreen = blurScreenLayout;
            this.willRemove = flag;
        }

        @Override
        public final void onAnimationEnd(Animator animator) {
            this.mBlurScreen.getAlpha();
            this.mBlurScreen.clearBlur();
            if (this.willRemove)
                this.mBlurScreen.mLauncher.getDragLayer().removeView(this.mBlurScreen);
        }
    }

    public final Bitmap makeRoundBitmap(Bitmap bitmap, View view) {
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        float roundCornerRadius = 0;

        if (view != null) {
            int[] position = new int[2];
            boolean isAppIcon = view instanceof BubbleTextView;
            if (isAppIcon) {
                ((BubbleTextView)view).getLocalIconCenter(position);
            } else if (view instanceof LauncherAppWidgetHostView) {
                position = ((LauncherAppWidgetHostView) view).getLocationWidget();
            }

            DeviceProfile deviceProfile = mLauncher.getDeviceProfile();

            Bitmap createScaledBitmap = Bitmap.createScaledBitmap(bitmap, deviceProfile.getCurrentWidth(), deviceProfile.getCurrentHeight(), true);
            Bitmap doubleBufferBitmap = Bitmap.createBitmap(createScaledBitmap.getWidth(), createScaledBitmap.getHeight(), createScaledBitmap.getConfig());
            Canvas canvas = new Canvas(doubleBufferBitmap);
            Paint paint = new Paint();
            canvas.drawBitmap(createScaledBitmap, 0.0f, 0.0f, paint);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            if (isAppIcon) {
                int[] locations = new int[2];
                view.getLocationOnScreen(locations);
                position[0] += locations[0];
                position[1] += locations[1];
                double cornerRadius = getResources().getDimensionPixelSize(R.dimen.icon_round_corner);
                roundCornerRadius = (int) (cornerRadius * 1.1d);
                double size = mLauncher.getDeviceProfile().iconSizePx;
                int half = (int) (size / 2);
                left = position[0] - (int)((size * 0.1d) / 2.0d) - half;
                top = position[1] - (int)((size * 0.1d) / 2.0d) - half;
                right = (int) ((size * 1.0d) + left);
                bottom = (int) ((size * 1.0d) + (double) top);

            } else if (view instanceof LauncherAppWidgetHostView) {
                LauncherAppWidgetHostView rVar = (LauncherAppWidgetHostView) view;
                int width = (rVar.getWidth() - rVar.mLeftMargin) - rVar.mRightMargin;
                int height = (rVar.getHeight() - rVar.mTopMargin) - rVar.mBottomMargin - rVar.mLabelHeight;
                double dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.icon_round_corner);
                double d6 = (width > this.mLauncher.getDeviceProfile().cellWidthPx || height > this.mLauncher.getDeviceProfile().cellHeightPx) ? 1.6d : 1.0d;
                roundCornerRadius = (int) (dimensionPixelSize2 * d6);
                left = (rVar.mLeftMargin + position[0]) - ((int) (((double) width * 0.05d) / 2.0d));
                top = (rVar.mTopMargin + position[1]) - ((int) ((0.05d * (double) height) / 2.0d));
                right = (int) (((double) width * 1.05d) + (double) left);
                bottom = (int) (((double) height * 1.05d) + top);
            } else {
                double width2 = view.getWidth();
                double height2 = view.getHeight();
                left = position[0] - (int) ((width2 * 0.05d) / 2.0d);
                top = position[1] - ((int) ((height2 * 0.05d) / 2.0d));
                right = (int) ((view.getWidth() * 1.05d) + left);
                bottom = (int) ((view.getHeight() * 1.05d) + top);
                double dimensionPixelSize3 = getResources().getDimensionPixelSize(R.dimen.icon_round_corner);
                roundCornerRadius = (int) (dimensionPixelSize3 * 1.6d);
            }

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, roundCornerRadius, roundCornerRadius, paint);
            return doubleBufferBitmap;
        }
        return null;
    }

    public class GetBlurCallBack implements Handler.Callback {
        public final BlurScreenLayout mBlurScreenLayout;

        public GetBlurCallBack(BlurScreenLayout blurScreenLayout) {
            this.mBlurScreenLayout = blurScreenLayout;
        }

        @Override
        public final boolean handleMessage(Message message) {
            if (message != null && message.what == 2) {
                try {
                    // handleMessage chạy trên worker thread (sWorkerThread) => KHÔNG được
                    // chạm view hierarchy trực tiếp. setAlpha phải chạy trên main thread,
                    // nếu không sẽ ném CalledFromWrongThreadException. Dùng mHandler2 (đã bind
                    // Looper.getMainLooper()) thay vì AsyncHandler.runOnUiThread — cái sau
                    // gán sUiThread lúc static-init nên có thể trỏ nhầm sang worker thread.
                    mBlurScreenLayout.mHandler2.post(new Runnable() {
                        @Override
                        public void run() {
                            mBlurScreenLayout.setAlpha(1.0f);
                        }
                    });
                    Object obj = message.obj;
                    if (obj instanceof View){
                      Bitmap bitmap = BlurScreenLayout.getBlurBitmap(mBlurScreenLayout, (View) obj);
                      if (bitmap != null) {
                          mBlurScreenLayout.mBlurDrawable = new BitmapDrawable(
                                  mBlurScreenLayout.getResources(),
                                  mBlurScreenLayout.makeRoundBitmap(bitmap, (View) message.obj)
                          );
                      }
                      mBlurScreenLayout.mHandler2.obtainMessage(message.what).sendToTarget();
                    }
                    else if (obj instanceof Boolean){
                      Bitmap blurBg = BlurScreenLayout.getAppsLibraryBlurBg(mBlurScreenLayout);
                      if (blurBg != null)
                          mBlurScreenLayout.mBlurDrawable = new BitmapDrawable(mBlurScreenLayout.getResources(), blurBg);;
                      mBlurScreenLayout.mHandler2.obtainMessage(message.what).sendToTarget();
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap bitmap = BlurScreenLayout.getBlurBitmap(mBlurScreenLayout, null);
                                if (mBlurScreenLayout.mBlurDrawable == null && bitmap != null)
                                    mBlurScreenLayout.mBlurDrawable = new BitmapDrawable(mBlurScreenLayout.getResources(), bitmap);
                                mBlurScreenLayout.mHandler2.obtainMessage(message.what).sendToTarget();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public class SetBlurCallback implements Handler.Callback {
        public final BlurScreenLayout mBlurScreenLayout;

        public SetBlurCallback(BlurScreenLayout mBlurScreenLayout) {
            this.mBlurScreenLayout = mBlurScreenLayout;
        }

        @Override
        public final boolean handleMessage(Message message) {
            ViewPropertyAnimator interpolator;
            if (message != null) {
                try {
                    if (message.what == 2) {
                        boolean isOpeningAppsLibrary = mLauncher.isOpeningAppsLibrary();
                        boolean isOpeningLeftPage = mLauncher.isOpeningLeftPage();
                        boolean isOpeningFloatingMenu = mLauncher.isOpeningFloatingMenu();
                        boolean isOpeningFolder = mLauncher.isOpeningFolder();
                        boolean isOpeningSearchView = mLauncher.isOpeningSearchView();
                        BitmapDrawable drawable = mBlurScreenLayout.mBlurDrawable;
                        if (drawable != null && isOpeningFloatingMenu) {
                            this.mBlurScreenLayout.setAlpha(0.0f);
                            this.mBlurScreenLayout.setBackground(drawable);
                            interpolator = this.mBlurScreenLayout.animate().alpha(1.0f).setDuration(268L).setInterpolator(new DecelerateInterpolator());
                        } else {
                            if (drawable != null && isOpeningFolder) {
                                this.mBlurScreenLayout.setAlpha(0.0f);
                                this.mBlurScreenLayout.setBackground(drawable);
                                // Mở folder: cả màn frost đều (alpha 1.0) như glass iOS thật.
                                // Khung KHÔNG có nền đục riêng — chỉ là viền sáng mảnh bao quanh
                                // lớp wallpaper-blur chung, nên trong/ngoài khung cùng độ mờ,
                                // app nổi nhờ nằm trên nền blur tối + viền phân tách.
                                interpolator = this.mBlurScreenLayout.animate().alpha(1.0f).setDuration(268L).setInterpolator(new DecelerateInterpolator());
                            } else {
                                if (drawable == null) {
                                    return true;
                                }
                                if (!isOpeningLeftPage && !isOpeningAppsLibrary && !isOpeningSearchView) {
                                    return true;
                                }
                                float alpha = this.mBlurScreenLayout.getAlpha();
                                this.mBlurScreenLayout.setBackground(drawable);
                                interpolator = this.mBlurScreenLayout.animate().alpha(alpha).setDuration((long) (255.0f * alpha)).setInterpolator(new DecelerateInterpolator());
                            }
                        }
                        interpolator.start();
                        return true;
                    }
                    return true;
                } catch (Throwable th) {
                    th.getMessage();
                    return true;
                }
            }
            return true;
        }
    }

    public final void changeBlur(float amount) {
        try {

            Log.e("Blur Amount is ", "" + amount);

            if (amount == 0.0f) {
                clearBlur();
            } else if (getBackground() != null) {
                setAlpha(amount);
            } else {
                setAlpha(amount);
                if (!this.mHandler1.hasMessages(2)) {
                    this.mHandler1.obtainMessage(2).sendToTarget();
                }
            }
        } catch (Throwable th) {
            th.getMessage();
        }
    }
}
