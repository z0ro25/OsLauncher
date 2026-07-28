package com.amz.ios.themeclub.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.DESUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by ubuntu on 15/06/17.
 */

public class IOSLockScreenJsonObjectRequest extends JsonObjectRequest{
    public IOSLockScreenJsonObjectRequest(int method, String url, JSONObject jsonRequest,
                                   Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url,jsonRequest,listener,errorListener);
    }
    //封装的GET请求
    public IOSLockScreenJsonObjectRequest(String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener,
                                   Response.ErrorListener errorListener) {
        this(jsonRequest == null ? Request.Method.GET : Request.Method.POST, url, jsonRequest,listener,errorListener);
    }

    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            byte[] decrypted;
            decrypted = DESUtil.decrypt(response.data, AppConfig.LOCK_ENCODE_DECODE_KEY.getBytes());
            String jsonString = new String(decrypted,
                    "UTF-8");
            return Response.success(new JSONObject(jsonString),
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
