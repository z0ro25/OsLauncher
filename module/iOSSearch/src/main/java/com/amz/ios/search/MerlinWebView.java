package com.amz.ios.search;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-28 下午6:36
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MerlinWebView extends WebView {

    private static final String TAG = MerlinWebView.class.getSimpleName();

    public MerlinWebView(Context context) {
        super(context);
    }

    public MerlinWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MerlinWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_MOVE)
            Log.d(TAG, ">>>>>>MerlinCardView#onTouchEvent : " + event.getAction());
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_MOVE)
            Log.d(TAG, ">>>>>>MerlinWebView#dispatchTouchEvent : " + ev.getAction());
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_MOVE)
            Log.d(TAG, ">>>>>>MerlinWebView#onInterceptTouchEvent : " + ev.getAction());
        return super.onInterceptTouchEvent(ev);
    }
}
