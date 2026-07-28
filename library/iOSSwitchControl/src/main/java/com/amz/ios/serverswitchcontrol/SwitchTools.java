package com.amz.ios.serverswitchcontrol;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.serverswitchcontrol.bean.request.RegistRequestBean;
import com.amz.ios.serverswitchcontrol.bean.request.SwitchRequestBean;
import com.amz.ios.serverswitchcontrol.bean.request.CommonBean;
import com.amz.ios.serverswitchcontrol.bean.request.LocationBean;
import com.amz.ios.serverswitchcontrol.bean.request.TagBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SwitchTools {
    private static final String TAG = "SwitchTools";
    private static final String API_KEY = "MHZX_3b58249e479e44a2911600960f795dcd";
    private static final String DEFAULT_PROVINCE = "北京";
    private static final String DEFAULT_CITY_NAME = "北京";

    private static void logRequestAndResponse(String request, String response) {
        Log.d(TAG, "request:".concat(request));
        Log.d(TAG, "response:".concat(response));
    }

    private static final MediaType JSON
            = MediaType.parse("application/json;charset=utf-8");
    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static <T> void executeNetworkTask(String url, final String post, final Class cl, final ResultCallback<T> callback) {
        Request request;
//        if (TextUtils.isEmpty(post)) {
//            request = new Request.Builder().url(url).get().build();
//        } else {
//            RequestBody body = RequestBody.create(JSON, post);
//            request = new Request.Builder().url(url).post(body).build();
//        }
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                logRequestAndResponse(post, "failure");
//                callback.onFailure(call, e, "network error");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                String str = response.body().string();
//                logRequestAndResponse(post, str);
//                if (response.code() >= 300 || response.code() < 200) {
//                    callback.onFailure(call, null, "network response code is " + response.code());
//                    return;
//                }
//                try {
//                    T responseBean = (T) gson.fromJson(str, cl);
//                    callback.onResponse(call, response, responseBean);
//                } catch (JsonSyntaxException e) {
//                    callback.onFailure(call, e, "attention to Gson's use");
//                }
//            }
//        });
    }

    public static String getRequestContent(Context context, String ip) {
        CommonBean commonBean = CommonBean.newCommonBean(context);
        LocationBean locationBean = LocationBean.newCommonBean(PreferencesUtil.getString(context, Constants.SharedPreferencesConstants.PROVINCE_KEY, DEFAULT_PROVINCE)
                , PreferencesUtil.getString(context, Constants.SharedPreferencesConstants.CITY_NAME_KEY, DEFAULT_CITY_NAME));
        if(TextUtils.isEmpty(ip)){
            TagBean tagBean = TagBean.newTagBean(context);
            SwitchRequestBean switchRequestBean = SwitchRequestBean.newAppRecommendRequestBean(context, commonBean, tagBean, locationBean, API_KEY);
            return gson.toJson(switchRequestBean);
        }else {
            RegistRequestBean registRequestBean = RegistRequestBean.newAppRecommendRequestBean(context, commonBean, locationBean, ip, API_KEY);
            return gson.toJson(registRequestBean);
        }
    }

    public interface ResultCallback<T> {
        void onFailure(Call call, Exception e, String message);

        void onResponse(Call call, Response response, T t);
    }
}
