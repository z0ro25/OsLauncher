package com.amz.ios.ioslite.common;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;

/**
 *  CommonFragmentActivity with common fuctions;
 *
 *  1. data analytics include : umeng, google firbase;
 *
 */
public class CommonFragmentActivity extends FragmentActivity{

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
     * mAppliedLanguage của CHÍNH instance (không dùng static).
     */
    private void applyLocaleIfChanged() {
        String saved = IosLocale.getSavedLanguage(this);
        String savedSafe = (saved == null) ? "" : saved;
        if (!mAppliedLanguage.equals(savedSafe)) {
            mAppliedLanguage = savedSafe;
            final FragmentActivity activity = this;
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
