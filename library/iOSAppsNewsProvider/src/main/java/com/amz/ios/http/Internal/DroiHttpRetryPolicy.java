package com.amz.ios.http.Internal;

import com.android.volley.DefaultRetryPolicy;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-17 下午5:28
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class DroiHttpRetryPolicy extends DefaultRetryPolicy {

    private long mTimeOut;

    private int mRetryTimes;

    public DroiHttpRetryPolicy() {
        mTimeOut = DroiRqConfigurations.DEFAULT_TIME_OUT;
        mRetryTimes = DroiRqConfigurations.DEFAULT_HTTP_FAILLURE_RETRY_TIMES;
    }

    @Override
    public int getCurrentTimeout() {
        return (int) mTimeOut;
    }

    @Override
    public int getCurrentRetryCount() {
        return mRetryTimes;
    }

}
