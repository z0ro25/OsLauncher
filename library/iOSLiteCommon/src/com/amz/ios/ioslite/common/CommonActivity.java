package com.amz.ios.ioslite.common;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;

/**
 *  CommonActivity with common fuctions;
 *
 *  1. data analytics include : umeng, google firbase;
 *
 */
public class CommonActivity extends Activity {

    /** Ngôn ngữ đã áp khi Activity này attach (rỗng = chưa ép / theo hệ thống). Riêng theo instance. */
    private String mAppliedLanguage = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = IosLocale.getSavedLanguage(newBase);
        mAppliedLanguage = (lang == null) ? "" : lang;
        super.attachBaseContext(IosLocale.wrapLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsDelegate.onCreate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsDelegate.onResume(this);
        applyLocaleIfChanged();
    }

    @Override
    protected void onPause() {
        AnalyticsDelegate.onPause(this);
        super.onPause();
    }

    /**
     * Localize: nếu người dùng đổi ngôn ngữ ở app (màn Language) trong lúc Activity đang mở, instance
     * này được tạo từ locale CŨ → recreate lại để áp ngôn ngữ mới (không cần thoát app). So theo
     * mAppliedLanguage của CHÍNH instance (không dùng static). Chỉ kích hoạt khi đã từng chọn ngôn ngữ.
     */
    private void applyLocaleIfChanged() {
        String saved = IosLocale.getSavedLanguage(this);
        String savedSafe = (saved == null) ? "" : saved;
        if (!mAppliedLanguage.equals(savedSafe)) {
            // Ghi đè trước để nếu recreate lỗi (đang trong chuỗi lifecycle) sẽ không lặp vô hạn;
            // instance mới sau recreate sẽ tự attach và cập nhật lại đúng giá trị này.
            mAppliedLanguage = savedSafe;
            final Activity activity = this;
            // Post ra sau khi onResume hoàn tất — recreate() ngay trong onResume có thể bị bỏ qua.
            getWindow().getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    if (activity != null && !activity.isFinishing()) {
                        activity.recreate();
                    }
                }
            });
        }
    }
}
