package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;

import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;

/**
 * Nội dung HEADER (nhóm + tiêu đề + mô tả + icon) cho màn chọn size widget (level-2).
 * <p>
 * Widget "nhà làm" khớp theo tên class provider -> dùng chuỗi curated cho đúng thiết kế
 * iOS (vd: Clock Digital / "Display the current time."). Widget lạ (bên thứ 3) -> fallback
 * nhãn hệ thống (loadLabel/appLabel/loadDescription). CHỈ dùng cho sheet này; 1 nguồn text.
 */
public final class WidgetSheetMeta {

    /** Tên nhóm hiện ở header (vd "Clock"). */
    public final CharSequence group;
    /** Tiêu đề lớn (vd "Clock Digital"). */
    public final CharSequence title;
    /** Mô tả xám 1 dòng (vd "Display the current time."). Có thể rỗng. */
    public final CharSequence desc;
    /** Icon nhỏ ở header (preview widget, fallback icon provider). Có thể null. */
    public final Drawable icon;

    private WidgetSheetMeta(CharSequence group, CharSequence title, CharSequence desc, Drawable icon) {
        this.group = group;
        this.title = title;
        this.desc = desc;
        this.icon = icon;
    }

    public static WidgetSheetMeta forWidget(Context ctx, LauncherAppWidgetProviderInfo info) {
        CharSequence group = null;
        CharSequence title = null;
        CharSequence desc = null;
        String cls = (info.provider != null) ? info.provider.getClassName() : "";

        if (endsWith(cls, "ClockWidgetProvider")) {
            group = ctx.getString(R.string.clock);
            title = ctx.getString(R.string.widget_meta_title_clock_digital);
            desc = ctx.getString(R.string.widget_meta_desc_clock_digital);
        } else if (endsWith(cls, "AnalogClockDarkWidgetProvider")
                || endsWith(cls, "AnalogClockWidgetProvider")) {
            group = ctx.getString(R.string.clock);
            title = ctx.getString(R.string.widget_meta_title_clock_analog);
            desc = ctx.getString(R.string.widget_meta_desc_clock_analog);
        } else if (endsWith(cls, "WorldClockWidgetProvider")) {
            group = ctx.getString(R.string.clock);
            title = ctx.getString(R.string.widget_meta_title_world_clock);
            desc = ctx.getString(R.string.widget_meta_desc_world_clock);
        } else if (endsWith(cls, "MiniAnalogClockWidgetProvider")) {
            group = ctx.getString(R.string.clock);
            title = ctx.getString(R.string.widget_meta_title_clock_grid);
            desc = ctx.getString(R.string.widget_meta_desc_clock_grid);
        } else if (endsWith(cls, "CalendarWidgetProvider")) {
            group = ctx.getString(R.string.calendar);
            title = ctx.getString(R.string.widget_meta_title_calendar);
            desc = ctx.getString(R.string.widget_meta_desc_calendar);
        } else if (endsWith(cls, "PictureAppWidgetProvider")) {
            group = ctx.getString(R.string.picture);
            title = ctx.getString(R.string.widget_meta_title_photos);
            desc = ctx.getString(R.string.widget_meta_desc_photos);
        } else if (endsWith(cls, "BatteryWidgetProvider")) {
            group = ctx.getString(R.string.battery);
            title = ctx.getString(R.string.widget_meta_title_batteries);
            desc = ctx.getString(R.string.widget_meta_desc_batteries);
        } else if (endsWith(cls, "WeatherWidgetProvider")) {
            group = ctx.getString(R.string.weather);
            title = ctx.getString(R.string.widget_meta_title_weather);
            desc = ctx.getString(R.string.widget_meta_desc_weather);
        }

        // Fallback cho widget bên thứ 3 (hoặc chưa curated): nhãn hệ thống.
        if (TextUtils.isEmpty(title)) {
            title = safeLoadLabel(ctx, info);
        }
        if (TextUtils.isEmpty(group)) {
            group = appLabel(ctx, info);
            if (TextUtils.isEmpty(group)) {
                group = title;
            }
        }
        if (desc == null) {
            desc = loadDescription(ctx, info);
        }

        return new WidgetSheetMeta(group, title, desc, loadHeaderIcon(ctx, info));
    }

    private static boolean endsWith(String cls, String simpleName) {
        return cls != null && (cls.endsWith("." + simpleName) || cls.equals(simpleName));
    }

    private static CharSequence safeLoadLabel(Context ctx, LauncherAppWidgetProviderInfo info) {
        try {
            CharSequence l = info.loadLabel(ctx.getPackageManager());
            return l != null ? l : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private static CharSequence appLabel(Context ctx, LauncherAppWidgetProviderInfo info) {
        try {
            if (info.provider == null) return "";
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(info.provider.getPackageName(), 0);
            return pm.getApplicationLabel(ai);
        } catch (Throwable t) {
            return "";
        }
    }

    private static CharSequence loadDescription(Context ctx, LauncherAppWidgetProviderInfo info) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                CharSequence d = info.loadDescription(ctx);
                if (!TextUtils.isEmpty(d)) return d;
            } catch (Throwable ignored) {
            }
        }
        return "";
    }

    private static Drawable loadHeaderIcon(Context ctx, LauncherAppWidgetProviderInfo info) {
        int density = ctx.getResources().getDisplayMetrics().densityDpi;
        try {
            Drawable d = info.loadPreviewImage(ctx, density);
            if (d != null) return d;
        } catch (Throwable ignored) {
        }
        try {
            Drawable d = info.loadIcon(ctx, density);
            if (d != null) return d;
        } catch (Throwable ignored) {
        }
        return null;
    }
}
