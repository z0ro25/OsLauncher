package com.amz.ios.themeclub.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.DESUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

/**
 * Created by server on 16-8-23.
 */

public class IOSJsonObjectRequest extends JsonObjectRequest {

    public IOSJsonObjectRequest(int method, String url, JSONObject jsonRequest,
                                   Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url,jsonRequest,listener,errorListener);
    }
    //封装的GET请求
    public IOSJsonObjectRequest(String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener,
                                   Response.ErrorListener errorListener) {
        this(jsonRequest == null ? Method.GET : Method.POST, url, jsonRequest,listener,errorListener);
    }

    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            byte[] decrypted;
            decrypted = DESUtil.decrypt(response.data, AppConfig.ENCODE_DECODE_KEY.getBytes());
            WeakReference<String> weakReference = new WeakReference(new String(decrypted, "UTF-8"));
            return Response.success(new JSONObject(weakReference.get()),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }
}
