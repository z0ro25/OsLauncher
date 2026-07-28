package com.amz.ios.http.Internal;

import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.BuildConfig;

/**
 * Wrap request call back add some state to control some logic and show action.
 * <p>
 * Author       : yizhihao
 * Create time  : 2016-11-18 下午12:14
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public abstract class CancelableCallBack<T> implements DroiRequestQueue.CallBack<T> {

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private String name;

    private static final String TAG = CancelableCallBack.class.getSimpleName();

    private boolean canceled = false;
    private boolean showTips = false;

    private Action mHandler;

    public CancelableCallBack setHandler(Action handler) {
        mHandler = handler;
        return this;
    }

    public boolean shouldShowTips() {
        return showTips;
    }

    public CancelableCallBack setShowTips(boolean showTips) {
        this.showTips = showTips;
        return this;
    }

    public CancelableCallBack setCanceled(boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public CancelableCallBack setName(String name) {
        this.name = name;
        return this;
    }


    public String getName() {
        return name;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void onRealSucess(T t) {
        if (!isCanceled() && mHandler.isValid()) {
            onSucess(t);
        } else {
            Log.d(TAG, ">>>>>>CancelableCallBack#onRealSucess : \n" +
                    "CallBack " + (!TextUtils.isEmpty(name) ? name : "") + " is canceled!" +
                    "or data is not valid" + mHandler.isValid());
            onFalure("", 0);
        }
    }

}
