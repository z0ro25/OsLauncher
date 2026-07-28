package com.amz.ios.search.http;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import com.amz.ios.http.BaseDroiRequest;
import com.amz.ios.http.DroiResponse;
import com.amz.ios.http.Internal.DroiRequestQueue;
import com.amz.ios.search.config.Urls;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liaozhongjun on 2017/1/4.
 */

public abstract class AppRecommendRequest<T> extends BaseDroiRequest<T> {

    private AppRecommendRequestBean recommendRequestBean;
    private Map<String, String> heads;

    public AppRecommendRequest(AppRecommendRequestBean recommendRequestBean, final DroiRequestQueue.CallBack<T> callBack) {
        super(Method.POST, Urls.URL_APP_RECOMMEND, new DroiResponse.Listener<T>() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (callBack == null) return;
                String message = error.getMessage();
                if (DEBUG) Log.d(TAG, ">>>>>>EncryptPostRequest#onErrorResponse :" + message);
                Log.e("search", new String(error.networkResponse.data));
                callBack.onFalure(error.getClass().getSimpleName(), 0);
            }

            @Override
            public void onSucessResponce(T o) {
            }
        });
        this.recommendRequestBean = recommendRequestBean;

    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (heads == null) {
            heads = new HashMap<>();
            heads.put("Content-Type", "application/json;charset=utf-8");
        }
        return heads;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        Gson gson = new Gson();
        String str = gson.toJson(recommendRequestBean);
        Log.e("search", "" + str);
        return str.getBytes();
    }

    @Override
    protected String getParamsEncoding() {
        return "utf-8";
    }

    @Override
    public String getBodyContentType() {
        return super.getBodyContentType();
    }

}