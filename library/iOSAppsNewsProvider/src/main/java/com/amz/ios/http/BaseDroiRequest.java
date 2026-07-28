package com.amz.ios.http;

import android.os.SystemClock;
import android.util.Log;

import com.amz.ios.http.Internal.DroiHttpRetryPolicy;
import com.amz.newspage.newssource.config.Configeration;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.amz.ios.ioslite.common.BuildConfig;
import com.amz.ios.ioslite.common.util.ZipUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-17 上午10:53
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public abstract class BaseDroiRequest<T> extends Request<T> {

    public static final boolean DEBUG = BuildConfig.LOG_ENABLE;
    /**
     * whether is pressed
     */
    public static final String TAG_COMPRESSED = "isPress";

    /**
     * server cach control
     */
    public static final String TAG_CACHE_CONTROL = "Cache-control";

    protected static final String TAG = BaseDroiRequest.class.getSimpleName();


    private long mBirthTime;

    public void finished(String tag) {
        if (DEBUG) {
            final long takesTime = SystemClock.elapsedRealtime() - mBirthTime;
            if (takesTime > 3000) {
                Log.e(TAG, ">>>>>>" + this + " is too slow [" + takesTime + "ms]  " + tag != null ? "" : tag);
                return;
            }
            if (DEBUG)
                Log.d(TAG, ">>>>>>" + this + " finished! [" + takesTime + "ms]  " + tag != null ? "" : tag);
        }
    }

    @Override
    public void addMarker(String tag) {
        super.addMarker(tag);
        //start
        if ("network-queue-take".equals(tag) || "cache-queue-take".equals(tag)) {
            if (DEBUG) {
                try {
                    Log.d(TAG, ">>>>>>" + toString() + " start! " + tag + "\n request str = " + new String(getBody()));
                } catch (AuthFailureError authFailureError) {
                    authFailureError.printStackTrace();
                } catch (NullPointerException e) {
                }
                mBirthTime = SystemClock.elapsedRealtime();
            }
        }
        //finished
        if ("post-response".equals(tag) || "post-error".equals(tag)) {
            finished(tag);
        }
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return super.getBody();
    }

    public BaseDroiRequest(int method, String url, DroiResponse.Listener listener) {
        super(method, url, listener);
        setShouldCache(false);
    }

    public BaseDroiRequest cache() {
        setShouldCache(true);
        return this;
    }

    @Override
    public final Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
        super.setRetryPolicy(new DroiHttpRetryPolicy());
        return this;
    }

    @Override
    protected final Response<T> parseNetworkResponse(NetworkResponse response) {
        if (DEBUG) Log.d(TAG, ">>>>>>BaseDroiRequest # parseNetworkResponse : start!");
        final DroiResponse result = new DroiResponse(response);
        byte[] unCompressByte;
        String parsed = null;
        unCompressByte = responceWraper(response);
        //if (DEBUG) Log.d(TAG, ">>>>>>after result wraper");
        //from old version code
        if (response.headers.containsKey(TAG_COMPRESSED)) {
            final boolean isPress = Boolean.valueOf(response.headers.get("isPress"));
            if (isPress) {
                try {
                    unCompressByte = ZipUtil.uncompress(unCompressByte);
                    if (DEBUG) Log.d(TAG, ">>>>>>after unCompressByte" + parsed);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //Opps! server didn't consider such condition
        // so we do this help for sever.
        if (!response.headers.containsKey(TAG_CACHE_CONTROL) && shouldCache()) {
            StringBuilder sb = new StringBuilder();
            sb.append("max-age=").append(Configeration.MAX_CACH_AGE);
            response.headers.put(TAG_CACHE_CONTROL, sb.toString());
        }

        try {
            parsed = new String(unCompressByte, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        result.mRawStr = parsed;
        parseNetworkResponse(result);
        if (DEBUG) Log.d(TAG, ">>>>>>BaseDroiRequest#parseNetworkResponse : end!");
        return result.convert();
    }

    /**
     * handler result before return
     * for subclass custom handle
     *
     * @param response
     * @return
     */
    protected byte[] responceWraper(NetworkResponse response) {
        //Log.d(TAG, ">>>>>>BaseDroiRequest#responceWraper : no override result wraper");
        return response.data;
    }

    @Override
    protected final void deliverResponse(T response) {
        //if (DEBUG) Log.d(TAG, ">>>>>>BaseDroiRequest#deliverResponse : deliver");
        deliverResult(response);
    }

    /**
     * task will handle this in work thread
     *
     * @param response
     */
    protected abstract void parseNetworkResponse(DroiResponse<T> response);

    /**
     * after work out result , Request framework will send result in Uithread for handle final result;
     *
     * @param response
     */
    protected abstract void deliverResult(T response);

}
