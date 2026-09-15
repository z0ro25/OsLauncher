package com.amz.newspage.newssource.entities;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-11 下午2:24
 */

import android.graphics.Bitmap;
import androidx.collection.LruCache;
import android.util.Log;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapLruImageCache extends LruCache<String, Bitmap> implements ImageCache {

    private final String TAG = this.getClass().getSimpleName();

    public BitmapLruImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    @Override
    public Bitmap getBitmap(String url) {
        Log.e(TAG, "Retrieved item from Mem Cache");
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        Log.e(TAG, "Added item to Mem Cache");
        put(url, bitmap);
    }
}

