package com.amz.ios.launcher.leftpage.widgets;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PhotoWidget_2x2 extends BlurConstraintLayoutWidget {

    Launcher mLauncher;
    AppCompatImageView mPhotoIV;
    LinearLayoutCompat mPermissionView;
    int mMargin;

    static Lock lock;

    static {
        HashSet hashSet = new HashSet(Arrays.asList("XT1085", "XT1092", "XT1093", "XT1094", "XT1095", "XT1096", "XT1097", "XT1098", "XT1031", "XT1028", "XT937C", "XT1032", "XT1008", "XT1033", "XT1035", "XT1034", "XT939G", "XT1039", "XT1040", "XT1042", "XT1045", "XT1063", "XT1064", "XT1068", "XT1069", "XT1072", "XT1077", "XT1078", "XT1079"));
        lock = hashSet.contains(Build.MODEL) ? new ReentrantLock() : new TransformLock();
    }

    public static class TransformLock implements Lock {
        @Override
        public void lock() {

        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {

        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Should not be called");
        }
    }

    public PhotoWidget_2x2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoWidget_2x2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        LayoutInflater.from(mLauncher).inflate(
                R.layout.picture_app_widget_2x2,
                this,
                true
        );
    }

    public static String loadPhotoWidget(PhotoWidget_2x2 photoWidget_2x2, Context context) {
        Cursor query;
        photoWidget_2x2.getClass();
        String[] strArr = {"_data"};
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= 26) {
            Bundle bundle = new Bundle();
            bundle.putInt("android:query-arg-limit", 1);
            bundle.putString("android:query-arg-sql-sort-order", "RANDOM()");
            query = context.getContentResolver().query(uri, strArr, bundle, null);
        } else {
            query = context.getContentResolver().query(uri, strArr, null, null, "RANDOM() LIMIT 1");
        }
        int columnIndex = query.getColumnIndex("_data");
        String string = query.moveToFirst() ? query.getString(columnIndex) : null;
        query.close();
        return string;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhotoIV = (AppCompatImageView) findViewById(R.id.picture_widget_content);
        this.mPermissionView = (LinearLayoutCompat) findViewById(R.id.picture_widget_permission);
        findViewById(R.id.button_request_storage_permission).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: 2023.11.20 PERMISSION
                    }
                }
        );

        this.mPhotoIV.setLayerType(LAYER_TYPE_SOFTWARE, null);

        ((ConstraintLayout.LayoutParams) this.mPermissionView.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        reload(mLauncher);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void r() {
        super.r();
    }

    void reload(final Context context) {
        try {
            final String path = new PhotoReloadCallable().call();
            if (path != null && !path.equals("")) {

                postOnAnimationDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                // Runnable trễ 268ms: lúc chạy, Launcher (context) có thể đã bị
                                // destroy (vuốt page attach/detach liên tục, hoặc launcher reload).
                                // Glide.with(activity đã destroy) sẽ ném IllegalArgumentException
                                // "You cannot start a load for a destroyed activity" -> crash liên
                                // tục. Chặn khi activity destroy/finishing hoặc view đã detach.
                                if (context instanceof Activity) {
                                    Activity act = (Activity) context;
                                    if (act.isFinishing() || act.isDestroyed()) return;
                                }
                                if (!isAttachedToWindow()) return;
                                Glide.with(context)
                                        .load(path)
                                        .centerCrop()
                                        .transform(new BitmapRoundTransformer(context, getResources().getDimensionPixelSize(R.dimen.widget_round_corner)))
                                        .into(new CustomTarget<Drawable>() {
                                            @Override
                                            public void onResourceReady(@NonNull Drawable drawable, @Nullable Transition<? super Drawable> transition) {
                                                mPhotoIV.setImageDrawable(drawable);
                                                mPhotoIV.setVisibility(View.VISIBLE);
                                                mPhotoIV.setAlpha(1.0f);
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable drawable) {

                                            }
                                        });
                            }
                        },
                        268L
                );
            }
        } catch (Throwable th) {

        }
    }

    public class PhotoReloadCallable implements Callable<String> {

        @Override
        public String call() throws Exception {
            return loadPhotoWidget(PhotoWidget_2x2.this, mLauncher);
        }
    }

    public static class BitmapRoundTransformer extends BitmapTransformation {

        int mRadius;

        public BitmapRoundTransformer(Context context, int radius) {
            super();
            mRadius = radius;
        }

        @Override
        protected Bitmap transform(BitmapPool bitmapPool, Bitmap bitmap, int i, int i1) {
            Bitmap bmp;

            bmp = bitmap;

            Bitmap bg = Bitmap.createBitmap(bitmap);
            bg.setHasAlpha(true);
            Shader.TileMode tileMode = Shader.TileMode.CLAMP;
            BitmapShader bitmapShader = new BitmapShader(bmp, tileMode, tileMode);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(bitmapShader);
            RectF rectF = new RectF(0, 0, bg.getWidth(), bg.getHeight());

            lock.lock();

            try {
                Canvas canvas = new Canvas(bg);
                //canvas.drawColor(Color.RED, PorterDuff.Mode.CLEAR);
                canvas.drawRoundRect(rectF, mRadius, mRadius, paint);
                canvas.setBitmap(null);
                lock.unlock();
                // NOTE: do NOT put `bg` into the BitmapPool here — `bg` is the
                // bitmap we return to be displayed. Pooling it lets Glide reuse
                // and recycle it while the widget-preview ImageView is still
                // drawing it, throwing "trying to use a recycled bitmap" when the
                // widgets panel builds its drawing cache during the slide-up.
                return bg;
            } catch (Throwable th) {
                lock.unlock();
                throw th;
            }
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BitmapRoundTransformer that = (BitmapRoundTransformer) o;
            return mRadius == that.mRadius;
        }

        @Override
        public int hashCode() {
            return ((mRadius + 527) * 31) - 569625254;
        }


    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        reload(mLauncher);
    }

}
