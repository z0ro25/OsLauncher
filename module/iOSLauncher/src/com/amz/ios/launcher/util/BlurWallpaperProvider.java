package com.amz.ios.launcher.util;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;

import androidx.core.app.ActivityCompat;

import com.amz.ios.launcher.Launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public final class BlurWallpaperProvider {

    public Launcher mLauncher;
    public WallpaperManager mWallpaperManager;
    public Bitmap mBlurBmp;
    public Bitmap mWallpaperBmp;
    public int mScale;
    public int mBlurRadius;
    public DisplayMetrics mMetrics = new DisplayMetrics();
    Paint mPaint;
    Path mPath;
    Canvas mCanvas;
    boolean hasWallpaper = false;

    public class WallpaperSaveRunnable implements Runnable {

        @Override
        public void run() {
            Bitmap bitmap = null;
            hasWallpaper = mWallpaperManager != null && mWallpaperManager.getWallpaperInfo() != null;
            boolean isSetByThis = true;
            if (!hasWallpaper || mWallpaperManager.getWallpaperInfo().getPackageName().equalsIgnoreCase(mLauncher.getPackageName()))
                isSetByThis = false;
            if (isSetByThis) {
                try {
                    bitmap = BitmapFactory.decodeStream(
                            new FileInputStream(
                                    new File(
                                            new ContextWrapper(mLauncher).getDir("image", 0), "wallpaper")
                                )
                    );
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                if (ActivityCompat.checkSelfPermission(mLauncher, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    bitmap = ((BitmapDrawable) mWallpaperManager.getDrawable()).getBitmap();
                }
            }

            if (bitmap != null){
                mWallpaperBmp = getSizedBmp(bitmap.getWidth(),bitmap.getHeight());
                mBlurBmp = getBlurBg(bitmap);
                new WallPaperSaveThread(BlurWallpaperProvider.this,"wallpaper",bitmap);
                new WallPaperSaveThread(BlurWallpaperProvider.this,"blur",mBlurBmp);
            }

        }
    }

    public BlurWallpaperProvider(Context context) {
        this.mLauncher = Launcher.getLauncher(context);
        this.mPaint = new Paint(1);
        this.mPath = new Path();
        this.mScale = 36;
        this.mCanvas = new Canvas();
        mWallpaperManager = WallpaperManager.getInstance(context);
        mBlurRadius = 25;
    }

    public Bitmap getBlurBg(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int dstWidth = Math.round(width * 1.0f / this.mScale);
        int dstHeight = Math.round(height * 1.0f / this.mScale);

        try {
            Bitmap src = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false);
            Bitmap dst = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
            RenderScript renderScript = RenderScript.create(this.mLauncher);
            Allocation createFromBitmap = Allocation.createFromBitmap(renderScript, src);
            Allocation createTyped = Allocation.createTyped(renderScript, createFromBitmap.getType());
            ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            intrinsicBlur.setRadius(this.mBlurRadius);
            intrinsicBlur.setInput(createFromBitmap);
            intrinsicBlur.forEach(createTyped);
            createTyped.copyTo(dst);
            return Bitmap.createScaledBitmap(dst, width, height, true);
        } catch (Throwable unused) {
            return BlurBuilder.fastBlur(Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true), this.mBlurRadius);
        }

    }

    public final Bitmap getSizedBmp(int x, int y) {
        Bitmap bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        this.mCanvas.setBitmap(bitmap);
        this.mPath.moveTo(0.0f, 0.0f);
        this.mPath.lineTo(0.0f, (float) y);
        this.mPath.lineTo((float) x, (float) y);
        this.mPath.lineTo((float) x, 0.0f);
        this.mPaint.setXfermode(null);
        this.mPaint.setColor(1174405119);
        this.mCanvas.drawPath(this.mPath, this.mPaint);
        return bitmap;
    }

    public void reloadWallpaper(){
        mLauncher.getThreadExecutor().execute(
                new WallpaperSaveRunnable()
        );
    }

}



