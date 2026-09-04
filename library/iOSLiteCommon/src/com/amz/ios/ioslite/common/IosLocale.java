package com.amz.ios.ioslite.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Ép ngôn ngữ đã chọn ở màn hình Language (module app) cho mọi Activity của các module iOS.
 *
 * Bối cảnh: màn Language của app (themelauncher) lưu lựa chọn vào SharedPreferences file "data",
 * key "KEY_LANGUAGE". Trước đây chỉ module app tự áp (LanguageUtil/BaseActivity) và desktop core
 * (iOSLauncher/LauncherBaseActivity); các màn phụ (iOSSettings, iOSSearch, iOSThemeClub...) đi qua
 * CommonActivity/CommonAppCompatActivity/CommonFragmentActivity nên chạy theo locale hệ thống.
 * Các base đó gọi {@link #wrapLocale(Context)} trong attachBaseContext để ép đúng ngôn ngữ.
 *
 * Bất biến: chưa từng chọn ngôn ngữ (KEY_LANGUAGE rỗng) → KHÔNG can thiệp, giữ theo hệ thống.
 */
public final class IosLocale {

    private IosLocale() {
    }

    @SuppressLint("ApplySharedPref")
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return prefs.getString("KEY_LANGUAGE", "");
    }

    /**
     * Trả về base context đã ép đúng ngôn ngữ nếu người dùng có chọn; ngược lại trả về base nguyên.
     * Gọi ngay đầu attachBaseContext (TRƯỚC khi framework dựng view) để Activity tạo bằng đúng locale.
     */
    public static Context wrapLocale(Context base) {
        String lang = getSavedLanguage(base);
        if (lang == null || lang.isEmpty()) {
            return base;
        }
        try {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocale(locale);
            return base.createConfigurationContext(config);
        } catch (Throwable t) {
            // Không để lỗi locale phá hoạt động — fallback về base gốc.
            return base;
        }
    }
}
