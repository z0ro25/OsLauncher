package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.amz.ios.launcher.R;

import java.util.Calendar;

/**
 * Giải mã Dark/Light cho khay Add Widget (level-1) + màn chọn size widget (level-2).
 * <p>
 * Engine (module iOSLauncher) là Activity THƯỜNG nên KHÔNG nhận night-mode mà
 * AppThemeManager (module app, AppCompat) set qua AppCompatDelegate. Vì vậy đọc THẲNG
 * prefs mà AppThemeManager đã ghi ({@code app_theme_settings}) để suy ra dark/light —
 * giữ 1 nguồn sự thật duy nhất. Các hằng số dưới PHẢI khớp AppThemeManager.
 */
public final class WidgetSheetTheme {

    private static final String PREFS = "app_theme_settings";
    private static final String KEY_MODE = "app_theme_mode";
    private static final String KEY_POLICY = "app_theme_policy";

    private static final int MODE_DARK = 1;
    private static final int POLICY_MANUAL = 0;
    private static final int POLICY_AUTO = 1;
    private static final int POLICY_SYSTEM = 2;

    private static final int DARK_START_HOUR = 19; // 19:00 -> Dark
    private static final int DARK_END_HOUR = 6;    // 06:00 -> Light

    private static final int TEXT_PRIMARY_LIGHT = 0xFF1C1C1E;
    private static final int TEXT_PRIMARY_DARK = 0xFFFFFFFF;
    /** Màu phụ (icon search, hint, kích thước "w × h") hợp cả 2 nền. */
    public static final int TEXT_SECONDARY = 0xFF8E8E93;

    private WidgetSheetTheme() {}

    public static boolean isDark(Context ctx) {
        SharedPreferences p = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        switch (p.getInt(KEY_POLICY, POLICY_MANUAL)) {
            case POLICY_AUTO: {
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                return hour >= DARK_START_HOUR || hour < DARK_END_HOUR;
            }
            case POLICY_SYSTEM: {
                int night = ctx.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
                return night == Configuration.UI_MODE_NIGHT_YES;
            }
            default:
                return p.getInt(KEY_MODE, 0) == MODE_DARK;
        }
    }

    /** Màu chữ chính theo nền: trắng khi dark, tối khi light. */
    public static int textPrimary(Context ctx) {
        return isDark(ctx) ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT;
    }

    /** Drawable nền sheet (bo 2 góc trên) theo theme. */
    public static int sheetBackgroundRes(Context ctx) {
        return isDark(ctx) ? R.drawable.widget_sheet_bg_dark
                : R.drawable.widget_sheet_bg_white;
    }
}
