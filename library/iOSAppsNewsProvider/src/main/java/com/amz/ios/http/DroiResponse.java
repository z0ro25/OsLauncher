package com.amz.ios.http;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.amz.ios.ioslite.common.BuildConfig;

import java.util.Map;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-17 上午10:55
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class DroiResponse<T> {

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String TAG = DroiResponse.class.getSimpleName();

    //wrap requestlistener
    public static abstract class Listener<T> implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
        }

        public abstract void onSucessResponce(T t);

        public void onResponceNow(T t) {
        }
    }

    public static final int STATE_ERROR = 0x1;
    public static final int STATE_SUCESS = 0x2;

    public void sucess(T t) {
        //if(DEBUG) Log.d(TAG, ">>>>>>DroiResponse#sucess :" + t);
        mContent = t;
        mState = STATE_SUCESS;
    }

    public void error(String message) {
        mState = STATE_ERROR;
        this.mMessage = message;
    }

    public int mState;

    public String mMessage = "";

    DroiResponse(NetworkResponse response) {
        this.networkResponce = response;
        this.statusCode = response.statusCode;
        this.data = response.data;
        this.headers = response.headers;
        this.notModified = response.notModified;
        this.networkTimeMs = response.networkTimeMs;
        mState = STATE_SUCESS;
    }

    NetworkResponse networkResponce;

    Response<T> convert() {
        if (mState == STATE_SUCESS)
            return Response.success(mContent, HttpHeaderParser.parseCacheHeaders(networkResponce));
        else
            return Response.error(new VolleyError(mMessage));
    }

    public String mRawStr;

    public T mContent;

    /**
     * The HTTP status code.
     */
    public final int statusCode;

    /**
     * Raw data from this response.
     */
    public final byte[] data;

    /**
     * Response headers.
     */
    public final Map<String, String> headers;

    /**
     * True if the server returned a 304 (Not Modified).
     */
    public final boolean notModified;

    /**
     * Network roundtrip time in milliseconds.
     */
    public final long networkTimeMs;

}
