package com.ezla.oslauncher.themes.test;

import android.content.Context;
import android.util.Log;

import com.ezla.oslauncher.themes.BuildConfig;
import com.mct.sag.SagClient;

import java.io.IOException;

public class TestApi {
    // Base url truyền vào thay vì fix cứng (lấy từ Remote Config domain_api).
    public static void test(Context context, String baseUrl) {
        FileLogger.init(context);
        FileLogger.d("AZZZ", "Step 1: start");
        SagClient client = SagClient.builder(context, baseUrl, BuildConfig.SAG_BOOTSTRAP_SECRET).build();
        new Thread(() -> {
            try {
                FileLogger.d("AZZZ", "Step 2: prepare");
                client.prepare();
                FileLogger.d("AZZZ", "Step 3: end");
            } catch (IOException e) {
                Log.e("AZZZ", "AZZZ: ", e);
                FileLogger.e("AZZZ", e);
            }
        }).start();
    }

}
