package com.amz.ios.business;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by liaozhongjun on 2017/2/25.
 */

public class BusinessUtils {
    private static final String TAG = "BusinessUtils ";

    public static final String IOS_SEARCH_ACTION = "ios.intent.action.SearchActivity";
    public static final String IOS_SEARCH_EXTRA_KEYWORD = "keyword";
    public static final String IOS_SEARCH_URI_PRE = "iosSearch://droi.com:200/";

    private static final void logRequestAndResponse(String request, String response) {
        Log.d("search request", request + "");
        Log.d("search response", response + "");
    }

    private static final MediaType JSON
            = MediaType.parse("application/json;charset=utf-8");
    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .create();

    public static <T> void executeNetworkTask(String url, final String post, final Class cl, final DroiCallback<T> callback) {
//        Request request;
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


    public interface DroiCallback<T> {
        void onFailure(Call call, Exception e, String message);

        void onResponse(Call call, Response response, T t);
    }

    public static void openUrl(Context context, String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//Min SDK 15
        context.startActivity(intent);
    }

    public static boolean toApp(Context context, ComponentName componentName) {
        try {
            Intent intent = new Intent();
            intent.setComponent(componentName);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, ">>>>>>IntentUtils#toApp : " + e.getMessage());
        }
        return false;
    }

    public static boolean toApp(Context context, String packageName, String className) {
        try {
            if (TextUtils.isEmpty(packageName)) {
                return false;
            }
            Intent intent;
            if (TextUtils.isEmpty(className)) {
                PackageManager manager = context.getPackageManager();
                intent = manager.getLaunchIntentForPackage(packageName);
            } else {
                intent = Intent.makeMainActivity(new ComponentName(packageName, className));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            if (intent == null) {
                return false;
            }
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, ">>>>>>IntentUtils#toApp : " + e.getMessage());
        }
        return false;
    }

    public static final void toIOSSearch(Context context, String key) {
        Intent intent = new Intent();
        intent.setAction(IOS_SEARCH_ACTION);
        intent.putExtra(IOS_SEARCH_EXTRA_KEYWORD, key);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(IOS_SEARCH_URI_PRE + key));
        context.startActivity(intent);
    }
}
