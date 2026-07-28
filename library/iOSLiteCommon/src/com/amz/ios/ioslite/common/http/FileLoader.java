package com.amz.ios.ioslite.common.http;

import android.util.Log;

import com.amz.ios.ioslite.common.util.CommonUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.Request;
//import okhttp3.Response;

public class FileLoader {
    private static final String TAG = "FileLoader";

    /**
     * 下载文件
     */
    public static void downloadFile(String fileUrl, final String destFile, final LoadCallback loadCallback) {
        final File file = new File(destFile);
        if (file.exists()) {
            loadCallback.onSuccess();
            return;
        }

//        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
//        final Request request = new Request.Builder().url(fileUrl).build();
//        final Call call = client.newCall(request);
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.d(TAG, "downloadFile fail");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                InputStream is = null;
//                byte[] buf = new byte[2048];
//                int len = 0;
//                FileOutputStream fos = null;
//                try {
//                    long total = response.body().contentLength();
//                    long current = 0;
//                    is = response.body().byteStream();
//                    fos = new FileOutputStream(file);
//                    while ((len = is.read(buf)) != -1) {
//                        current += len;
//                        fos.write(buf, 0, len);
//                    }
//                    fos.flush();
//                    Log.d(TAG, "download file success");
//                    loadCallback.onSuccess();
//                } catch (IOException e) {
//                    loadCallback.onFailure();
//                    Log.e(TAG, "downloadFile fail", e);
//                } finally {
//                    CommonUtilities.close(is);
//                    CommonUtilities.close(fos);
//                }
//            }
//        });
    }


    public interface LoadCallback {
        void onFailure();

        void onSuccess();
    }
}
