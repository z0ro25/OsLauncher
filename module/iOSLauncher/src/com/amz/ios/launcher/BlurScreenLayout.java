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

    public Bitmap getAppsLibraryBlurBackground() {
        try {
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
                    mBlurScreenLayout.setAlpha(1.0f);
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
