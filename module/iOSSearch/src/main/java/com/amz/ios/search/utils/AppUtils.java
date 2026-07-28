package com.amz.ios.search.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.amz.ios.search.entities.AppCardInfo;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-25 下午5:14
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class AppUtils {

    public static final String UNSAFE_HTTP_SCHAME = "http://";

    public static void filterApps(Context context, List<AppCardInfo> hotApps) {
        if (hotApps != null && hotApps.size() > 0) {
            List<String> pkgNames = getInstalledApksPackageName(context);

            ArrayList<AppCardInfo> willRemoved = new ArrayList<>();
            String pkgName = null;
            String downloadUrl = null;
            for (AppCardInfo hotApp : hotApps) {
                downloadUrl = hotApp.downloadUrl;
                pkgName = hotApp.packageName;
                try {
                    //filter unsafe http url app and installed app
                    if (pkgNames.contains(pkgName) || downloadUrl.startsWith(UNSAFE_HTTP_SCHAME)) {
                        willRemoved.add(hotApp);
                    }
                } catch (Exception e) {
                }
            }
            hotApps.removeAll(willRemoved);
        }
    }

    public static boolean isInstallMarketApp(Context context, String pageName) {
        List<String> pkgNames = getInstalledApksPackageName(context);
        return pkgNames.contains(pageName);
    }

    public static List<String> getInstalledApksPackageName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0x0);

        List<String> pkgNameList = new ArrayList<String>();
        for (ResolveInfo resolveInfo : apps) {
            pkgNameList.add(resolveInfo.activityInfo.applicationInfo.packageName);
        }

        return pkgNameList;
    }


    private static final void logRequestAndResponse(String request, String response) {
        Log.e("search request", request + "");
        Log.e("search response", response + "");
    }

    private static final MediaType JSON
            = MediaType.parse("application/json;charset=utf-8");
    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final Gson gson = new Gson();

    public static <T> void executeNetworkTask(String url, final String post, final Class cl, final DroiCallback<T> callback) {
//        RequestBody body = RequestBody.create(JSON, post);
//        Request request = new Request.Builder().url(url).post(body).build();
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                AppUtils.logRequestAndResponse(post, "failure");
//                callback.onFailure(call, e, "network error");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                String str = response.body().string();
//                AppUtils.logRequestAndResponse(post, str);
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
        public void onFailure(Call call, Exception e, String message);

        public void onResponse(Call call, Response response, T t);
    }


}
