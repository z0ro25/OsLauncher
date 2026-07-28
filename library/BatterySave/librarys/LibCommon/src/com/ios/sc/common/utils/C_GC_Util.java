package com.ios.sc.common.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

public class C_GC_Util {

    public static Bitmap drawable2Bitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 :Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    /*public static Bitmap drawable9pn2Bitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int w = C_ScreenUtils.getInstance().getScreenW();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }*/

    public static void recycleBitmap(Bitmap bitmap, boolean isNowGC) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        if (isNowGC) {
            System.gc();
        }
    }

    public static void releaseImageViewMemory(ImageView iv) {
        releaseImageViewMemory(iv, false);
    }

    public static void releaseImageViewMemory(ImageView iv,boolean isNowGC) {
        if (null != iv) {
            iv.clearAnimation();
            iv.setBackgroundDrawable(null);
            Drawable drawable = iv.getDrawable();
            if(null != drawable ){
                if(drawable instanceof BitmapDrawable){
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        //bitmap.recycle();
                        bitmap = null;
                    }
                }
            }
            iv.setImageResource(android.R.color.transparent);
            if(isNowGC){
                System.gc();
            }
        }
    }

    public static void releaseViewMemory(View v) {
        releaseViewMemory(v,false);
    }

    public static void releaseViewMemory(View v,boolean isNowGC) {
        if(null != v){
            v.clearAnimation();
            v.setBackgroundDrawable(null);
            v.setBackgroundResource(0);
            v.setBackgroundResource(android.R.color.transparent);
        }
    }
}
