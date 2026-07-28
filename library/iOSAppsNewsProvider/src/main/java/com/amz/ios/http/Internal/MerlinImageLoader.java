package com.amz.ios.http.Internal;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

/**
 * Author       : yizhihao
 * Create time  : 2017-01-06 下午4:25
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MerlinImageLoader extends ImageLoader {

    private static final String TAG = MerlinImageLoader.class.getSimpleName();

    private long mBirthTime;
    private String mTagName;

    public MerlinImageLoader(RequestQueue queue, ImageCache imageCache) {
        super(queue, imageCache);
    }

    @Override
    protected Request<Bitmap> makeImageRequest(String requestUrl, int maxWidth, int maxHeight, ImageView.ScaleType scaleType, String cacheKey) {
        Log.d(TAG, ">>>>>>[ " + requestUrl + " ] [ ] ImageRequest : make a new request!");
        return super.makeImageRequest(requestUrl, maxWidth, maxHeight, scaleType, cacheKey);
    }

    @Override
    protected void onGetImageSuccess(String cacheKey, Bitmap response) {
        super.onGetImageSuccess(cacheKey, response);
        Log.d(TAG, ">>>>>>[ " + cacheKey + " ] [ ]# GetImageSuccess : ");
    }

    @Override
    protected void onGetImageError(String cacheKey, VolleyError error) {
        super.onGetImageError(cacheKey, error);
        Log.d(TAG, ">>>>>>[ " + cacheKey + " ] [ ]# GetImageError : ");
    }
}
