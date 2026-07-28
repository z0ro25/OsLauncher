package com.amz.ios.themeclub.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.amz.ios.ioslite.common.http.VolleyUtil;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.network.IOSJsonObjectRequest;
import com.amz.ios.themeclub.network.IOSLockScreenJsonObjectRequest;
import com.amz.ios.themeclub.network.Header;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ZhangMingZhe on 11/17/16.
 */

public class NetWorkUtils {

    private NetWorkUtils(){};

    private static NetWorkUtils INSTANCE = null;

    public static NetWorkUtils getInstance(){
        if(INSTANCE == null){
            synchronized (NetWorkUtils.class){
                if(INSTANCE == null){
                    INSTANCE = new NetWorkUtils();
                }
            }
        }
        return INSTANCE;
    }
    public static String buildHeadData(int msgCode) {
        String result = "";
        Header header = new Header();
        header.setBasicVer((byte) 1);
        header.setLength(84);
        header.setType((byte) 1);
        header.setReserved((short) 0);
        header.setMessageCode(msgCode);
        result = header.toString();
        return result;
    }

    public static JSONObject getCommonData(Context mContext) throws JSONException {
        return getCommonData(mContext,false);
    }

    public static JSONObject getCommonData(Context mContext,boolean isWidth) throws JSONException {
        if(mContext == null){
            mContext = ThemeClubApplication.getContext();
        }
        JSONObject object = new JSONObject();
        object.put("language", mContext.getResources().getString(R.string.themeclub_language));
        object.put("resolution", AppUtils.getLCD(mContext,isWidth));
        object.put("appVersion",AppUtils.getVersion(mContext));
        return object;
    }


    public void getDataFromServer(JSONObject request, Response.Listener listener, Response.ErrorListener errorListener){
        if(request!=null){
            try {
                final byte [] encrypted = DESUtil.encrypt(request.toString().getBytes("utf-8"), AppConfig.ENCODE_DECODE_KEY.getBytes("utf-8"));
                IOSJsonObjectRequest objectRequest = new IOSJsonObjectRequest(Request.Method.POST,
                       getServerUrl(),request,listener,errorListener){
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> map = new HashMap<>();
                        map.put("contentType", "utf-8");
                        map.put("Content-Type", "application/x-www-form-urlencoded");
                        map.put("Content-Length", String.valueOf(encrypted.length));
                        return map;
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/x-www-form-urlencoded; charset=" + "utf-8";
                    }

                    @Override
                    public byte[] getBody() {
                        return encrypted;
                    }
                };
                VolleyUtil.getRequestQue().add(objectRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            throw new RuntimeException("request can't be null");
        }
    }

    public void getLockDataFromServer(JSONObject request, Response.Listener listener, Response.ErrorListener errorListener){
        if(request!=null){
            try {
                final byte [] encrypted = DESUtil.encrypt(request.toString().getBytes("utf-8"), AppConfig.LOCK_ENCODE_DECODE_KEY.getBytes("utf-8"));
                IOSLockScreenJsonObjectRequest objectRequest = new IOSLockScreenJsonObjectRequest(Request.Method.POST,
                        getLockServerUrl(),request,listener,errorListener){
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> map = new HashMap<>();
                        map.put("contentType", "utf-8");
                        map.put("Content-Type", "application/x-www-form-urlencoded");
                        map.put("Content-Length", String.valueOf(encrypted.length));
                        return map;
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/x-www-form-urlencoded; charset=" + "utf-8";
                    }

                    @Override
                    public byte[] getBody() {
                        return encrypted;
                    }
                };
                VolleyUtil.getRequestQue().add(objectRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            throw new RuntimeException("request can't be null");
        }
    }
    //是否联网
    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }
    public static JSONObject CommonResourceFactory(long id,int type, String source,Context context) {
        JSONObject obj = null;
        JSONObject paramInfo = null;
        obj = new JSONObject();
        paramInfo = new JSONObject();
        try {
            paramInfo.put("id",id);
            paramInfo.put("type",type);
            paramInfo.put("source",source);
            paramInfo.put("common", NetWorkUtils.getCommonData(context,true));
            obj.put("head",NetWorkUtils.buildHeadData(AppConfig.MessageCode.MESSAGECODE_COMMON_RESOURCE));
            obj.put("body",paramInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static JSONObject postDownloadCountsFactory(long id,int type,Context context) {
        JSONObject obj = null;
        JSONObject paramInfo = null;
        obj = new JSONObject();
        paramInfo = new JSONObject();
        try {
            paramInfo.put("id",id);
            paramInfo.put("type",type);
            paramInfo.put("common", NetWorkUtils.getCommonData(context,true));
            obj.put("head",NetWorkUtils.buildHeadData(AppConfig.MessageCode.MESSAGECODE_DOWNLOAD_COUNTS));
            obj.put("body",paramInfo.toString());
            Log.e("ldq","count = "  + paramInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }


    public String getServerUrl() {
        if(BuildUtil.isCNBuild()) {
            return AppConfig.CN_SERVER;
        }
        return AppConfig.HW_SERVER_URL;
    }

    public String getLockServerUrl() {
        return AppConfig.SERVER_URL_FORMAL;
    }
}
