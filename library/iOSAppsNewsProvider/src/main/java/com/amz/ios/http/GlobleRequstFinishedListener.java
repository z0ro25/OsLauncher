package com.amz.ios.http;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-21 下午4:44
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class GlobleRequstFinishedListener implements RequestQueue.RequestFinishedListener {

    private static final String TAG = GlobleRequstFinishedListener.class.getSimpleName();

    @Override
    public void onRequestFinished(Request request) {
        if (!(request instanceof BaseDroiRequest)) return;
        Log.d(TAG, ">>>>>>GlobleRequstFinishedListener # onRequestFinished : " + request);
        ((BaseDroiRequest) request).finished(String.valueOf(request.getTag()));
    }
}
