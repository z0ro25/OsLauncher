package com.amz.ios.launcher.parser;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.customview.widget.ViewDragHelper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.http.FileLoader;
import com.amz.ios.ioslite.common.http.VolleyUtil;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.ioslite.common.util.encrypt.MD5Util;
import com.amz.ios.launcher.LauncherAppState;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

public class AppCategoryModel {
    private static final String TAG = "AppCategoryModel";
    private static final String KEY_PRE_INSTALL_DB_VERION = "app_category_pre_db_version";
    private static final int PRE_INSTALL_DB_VERSION = 2;
    private static AppCategoryModel sCatModel;
    private Context mContext;
    private Callback mCallback;

    interface Category extends BaseColumns {
        String DATABASE_NAME = "launcher_app_type.db";
        String TABLE_NAME = "google_app";
        String CATEGORY_VALUE = "type";
        String PACKAGE_NAME = "package_name";
    }

    public static AppCategoryModel getInstance() {
        if (sCatModel == null) {
            synchronized (AppCategoryProvider.class) {
                final Context context = CommonSdk.getApplicationContext();
                sCatModel = new AppCategoryModel(context);
            }
        }
        return sCatModel;
    }

    private AppCategoryModel(Context context) {
        mContext = context;
    }


    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * 判断分类数据库是否存在，不存在执行复制操作
     */
    void initalize(Context context) {
        if (!hasCompleteInitalize()) {
            executeCopyPreInstallDbTask(context);
        }
    }

    /**
     * 判断分类数据库是否存在
     */
    boolean hasCompleteInitalize() {
        File file = mContext.getDatabasePath(Category.DATABASE_NAME);
        return file.exists() && getPreInstallDbVersion() == PRE_INSTALL_DB_VERSION;
    }

    /**
     * 执行数据库复制任务，通知数据库更新
     */
    private void executeCopyPreInstallDbTask(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                if (mCallback != null) {
                    mCallback.startUpdateDatabase();
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                copyPreInstallDbFile(context);
                savePreInstallDbVersion();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (mCallback != null) {
                    mCallback.onDatabaseUpdate();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    /**
     * 复制apk预置数据库到用户数据库目录下
     */
    private void copyPreInstallDbFile(Context context) {
        File dest = context.getDatabasePath(Category.DATABASE_NAME);
        try {
            if (dest.exists()) {
                dest.delete();
            }
            dest.createNewFile();
            InputStream in = context.getAssets().open(Category.DATABASE_NAME);
            int size = in.available();
            byte buf[] = new byte[size];
            in.read(buf);
            in.close();
            FileOutputStream out = new FileOutputStream(dest);
            out.write(buf);
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "copyDatabaseFile fail", e);
        }

    }

    // 请求网址
    private static final String SERVER_URL = "http://192.168.0.52:9901/dbInfo/info";
    // API Key
    private static final String SERVER_API_KEY = "MHZX_3b58249e479e44a2911600960f7slx001";

    /**
     * 服务器请求数据库更新
     */
    public void requestDbFromServer() {
        RequestQueue requestQueue = VolleyUtil.getRequestQue();
        JSONObject requestJosn = createRequestJson();
        Response.Listener listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                DebugLog.w(TAG, "onResponse : " + response.toString());
                parseServerResponse(response);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                DebugLog.w(TAG, "onErrorResponse :" + error);
            }
        };
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, SERVER_URL, requestJosn,
                listener, errorListener);
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * 服务器请求参数
     */
    private JSONObject createRequestJson() {
        JSONObject requestJosn = new JSONObject();
        try {
            String appVersion = BuildUtil.getIOSVersionName(mContext);
            long requestTime = Calendar.getInstance().getTimeInMillis();
            String sign = MD5Util.encypt(appVersion + requestTime + SERVER_API_KEY);
            JSONObject commonObject = new JSONObject();
            commonObject.put("appVersion", appVersion);
            commonObject.put("requestTime", requestTime);
            commonObject.put("sign", sign);

            String mmc = DeviceInfoUtil.getMcc(mContext);
            Locale currentLocale = DeviceInfoUtil.getLocale(mContext);
            String language = currentLocale.getLanguage() + "_" + currentLocale.getCountry();
            JSONObject localObject = new JSONObject();
            localObject.put("mcc", mmc);
            localObject.put("language", language);

            int dbVersion = 1;
            requestJosn.put("dataVersion", dbVersion);
            requestJosn.put("common", commonObject);
            requestJosn.put("local", localObject);
        } catch (Exception e) {

        }

        DebugLog.w(TAG, "createRequestJson: " + requestJosn);
        return requestJosn;
    }

    /**
     * 解析服务器响应
     */
    private void parseServerResponse(JSONObject response) {
        DebugLog.w(TAG, "parseServerResponse: " + response);
        try {
            int code = response.getInt("code");
            if (code == 0) {
                JSONObject data = response.getJSONObject("data");
                int version = data.getInt("dataVersion");
                String url = data.getString("fileUrl");
                DbState serverState = new DbState(version, url);
                downloadDbIfNecessary(serverState);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseServerResponse fail", e);
        }
    }

    /**
     * 如果数据库版本低于服务器，根据返回地址下载数据库
     */
    private void downloadDbIfNecessary(DbState serverState) {
        DebugLog.w(TAG, "downloadDbIfNecessary: " + serverState);
        String destFile = new File(FileUtil.getRootFilesDir(), "db").getAbsolutePath();
        FileLoader.downloadFile(serverState.downloadUrl, destFile, new FileLoader.LoadCallback() {
            @Override
            public void onFailure() {
                DebugLog.w(TAG, "downloadDbIfNecessary, download failure ");
            }

            @Override
            public void onSuccess() {
                DebugLog.w(TAG, "downloadDbIfNecessary: download success ");
            }
        });
    }

    private int getPreInstallDbVersion() {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        return mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE).getInt(KEY_PRE_INSTALL_DB_VERION, 0);
    }

    private void savePreInstallDbVersion() {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_PRE_INSTALL_DB_VERION, PRE_INSTALL_DB_VERSION).commit();
    }

    static final class DbState {
        int dbVersion;
        String downloadUrl;

        public DbState(int version, String url) {
            dbVersion = version;
            downloadUrl = url;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("dbVersion: " + dbVersion + ", ");
            builder.append("downloadUrl： " + downloadUrl);
            return builder.toString();
        }
    }

    public interface Callback {
        void onDatabaseUpdate();

        void startUpdateDatabase();
    }
}
