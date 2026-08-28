package com.amz.ios.launcher.widget.widgetprovider;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.widget.weather.WeatherRepository;

/**
 * Widget thời tiết kiểu iOS (2x2), lấy dữ liệu THẬT từ Open-Meteo qua {@link WeatherRepository}.
 *
 * Hai đường vẽ, cùng một nguồn dữ liệu:
 *   - {@link #updateWidgets} — đường RemoteViews (widget hệ thống).
 *   - {@link #bindInflatedView} — đường widget NỘI BỘ, được LauncherAppWidgetHost gọi sau khi
 *     inflate (widget iOS không đi qua onUpdate). Đây là đường thực tế của launcher này.
 *
 * Chưa cấp quyền vị trí -> phủ lớp mờ "Nhấn để cấp quyền" lên widget; chỉ xin quyền khi người dùng
 * BẤM vào widget, không hỏi lúc vào desktop.
 */
public class WeatherWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    /** Mã request khi widget thời tiết tự xin quyền vị trí. */
    public static final int REQUEST_LOCATION_PERMISSION = 4211;

    private static final String PREF_ASKED_LOCATION = "weather_widget_asked_permission";

    public WeatherWidgetProvider() {
        super();
    }

    void updateWidgets(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.weather_widget_provider);
        applyToRemoteViews(context, rv);
        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, WeatherWidgetProvider.class), rv);
    }

    /**
     * Đổ dữ liệu thời tiết vào RemoteViews (đường widget hệ thống).
     *
     * LƯU Ý: lớp phủ xin quyền và id gốc {@code widget_weather_layout} chỉ có ở layout 2x2. Với
     * medium/large, RemoteViews bỏ qua lệnh trỏ tới id không tồn tại trong layout của nó nên các
     * lời gọi dưới là vô hại — không cần tách nhánh riêng cho từng cỡ.
     */
    private static void applyToRemoteViews(Context context, RemoteViews rv) {
        boolean granted = WeatherRepository.hasLocationPermission(context);
        // Lớp phủ xin quyền: hiện khi thiếu quyền, che nội dung mẫu phía dưới.
        rv.setViewVisibility(R.id.widget_weather_permission,
                granted ? View.GONE : View.VISIBLE);

        if (!granted) {
            // Đường RemoteViews chỉ chạy được PendingIntent, KHÔNG gọi được requestPermissions
            // (thứ đòi Activity) -> mở thẳng màn Cài đặt app để người dùng tự bật.
            try {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.getPackageName(), null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                rv.setOnClickPendingIntent(R.id.widget_weather_layout,
                        PendingIntent.getActivity(context, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            } catch (Throwable ignored) {
            }
            return;
        }

        WeatherRepository.refreshIfStale(context, false);
        WeatherRepository.WeatherData data = WeatherRepository.cached(context);
        if (!data.valid) {
            return;   // chưa lấy được lần nào -> giữ nội dung mẫu trong layout
        }
        rv.setTextViewText(R.id.widget_weather_temp, WeatherRepository.formatTemp(data.temp));
        rv.setTextViewText(R.id.widget_weather_condition,
                WeatherRepository.conditionText(context, data.code));
        rv.setTextViewText(R.id.widget_weather_highlow,
                WeatherRepository.formatHighLow(data.high, data.low));
        rv.setImageViewResource(R.id.widget_weather_icon,
                WeatherRepository.conditionIcon(data.code));
        if (!TextUtils.isEmpty(data.city)) {
            rv.setTextViewText(R.id.widget_weather_city, data.city);
        }
    }

    /**
     * Đổ dữ liệu vào view ĐÃ INFLATE của widget nội bộ.
     *
     * Cùng khuôn với {@code BatteryWidgetProvider.bindInflatedView} và
     * {@code PictureAppWidgetProvider.bindInflatedView}: widget iOS được inflate thành View thật
     * (LauncherAppWidgetHost.createView) và KHÔNG đi qua onUpdate/RemoteViews.
     * Tra view theo id nên dùng chung được cho cả 3 cỡ widget (2x2 / 4x2 / 4x4) — cỡ nào thiếu id
     * nào thì bỏ qua id đó.
     */
    public static void bindInflatedView(final Context context, View root) {
        if (context == null || root == null) return;

        View permissionOverlay = root.findViewById(R.id.widget_weather_permission);
        boolean granted = WeatherRepository.hasLocationPermission(context);
        if (permissionOverlay != null) {
            permissionOverlay.setVisibility(granted ? View.GONE : View.VISIBLE);
        }

        // Bấm widget: chưa có quyền thì xin quyền (đúng thứ lớp phủ đang mời gọi).
        View clickTarget = root.findViewById(R.id.widget_weather_layout);
        if (clickTarget == null) clickTarget = root;
        clickTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!WeatherRepository.hasLocationPermission(context)) {
                    requestLocationPermission(context);
                }
            }
        });

        if (!granted) return;

        WeatherRepository.refreshIfStale(context, false);
        WeatherRepository.WeatherData data = WeatherRepository.cached(context);
        if (!data.valid) return;   // chưa có dữ liệu -> giữ nội dung mẫu

        TextView temp = root.findViewById(R.id.widget_weather_temp);
        TextView condition = root.findViewById(R.id.widget_weather_condition);
        TextView highLow = root.findViewById(R.id.widget_weather_highlow);
        TextView city = root.findViewById(R.id.widget_weather_city);
        ImageView icon = root.findViewById(R.id.widget_weather_icon);

        if (temp != null) temp.setText(WeatherRepository.formatTemp(data.temp));
        if (condition != null) {
            condition.setText(WeatherRepository.conditionText(context, data.code));
        }
        if (highLow != null) {
            highLow.setText(WeatherRepository.formatHighLow(data.high, data.low));
        }
        if (city != null && !TextUtils.isEmpty(data.city)) city.setText(data.city);
        if (icon != null) icon.setImageResource(WeatherRepository.conditionIcon(data.code));
    }

    /**
     * Xin quyền vị trí từ widget.
     *
     * Người dùng đã chọn "Không hỏi lại" thì hệ thống bỏ qua hộp thoại — lúc đó mở màn Cài đặt.
     * Cần cờ đã-hỏi trong pref vì shouldShowRequestPermissionRationale() trả false ở CẢ hai trường
     * hợp "chưa hỏi lần nào" và "đã từ chối vĩnh viễn".
     */
    private static void requestLocationPermission(Context context) {
        String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            boolean canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, permission)
                    || !PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean(PREF_ASKED_LOCATION, false);
            if (canAsk) {
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putBoolean(PREF_ASKED_LOCATION, true).apply();
                ActivityCompat.requestPermissions(
                        activity, new String[]{permission}, REQUEST_LOCATION_PERMISSION);
                return;
            }
        }
        try {
            Intent intent = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Vẽ lại mọi widget thời tiết đang có.
     *
     * Gọi từ Launcher.onResume và sau khi người dùng cấp quyền. Widget nội bộ không đi qua onUpdate
     * mà chỉ được bind một lần lúc inflate, nên phải duyệt cây view bind lại — giống
     * PictureAppWidgetProvider.refreshAll.
     */
    public static void refreshAll(Context context) {
        // Vừa cấp quyền hoặc vừa quay lại launcher -> lấy dữ liệu mới nếu cache đã cũ.
        //
        // KHÔNG lo vòng lặp dù listener của Launcher gọi ngược lại refreshAll: sau khi lấy xong,
        // fetchAndStore đã ghi PREF_AT nên lượt refreshIfStale này thấy cache còn tươi và return
        // ngay, không gọi mạng lần nữa.
        WeatherRepository.refreshIfStale(context, false);

        // 1) Widget qua AppWidgetHost thật (nếu có).
        //    Mỗi cỡ có LAYOUT RIÊNG — phải dựng RemoteViews đúng layout của nó, dùng chung layout
        //    2x2 cho cả ba sẽ vẽ sai bố cục widget 4x2 và 4x4.
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            Class<?>[] providers = {
                    WeatherWidgetProvider.class,
                    WeatherMediumWidgetProvider.class,
                    WeatherLargeWidgetProvider.class,
            };
            int[] layouts = {
                    R.layout.weather_widget_provider,
                    R.layout.weather_medium_widget_provider,
                    R.layout.weather_large_widget_provider,
            };
            for (int i = 0; i < providers.length; i++) {
                int[] ids = manager.getAppWidgetIds(new ComponentName(context, providers[i]));
                if (ids == null || ids.length == 0) continue;
                RemoteViews rv = new RemoteViews(context.getPackageName(), layouts[i]);
                applyToRemoteViews(context, rv);
                manager.updateAppWidget(ids, rv);
            }
        } catch (Throwable ignored) {
        }

        // 2) Widget NỘI BỘ: bind lại trực tiếp trên cây view.
        try {
            View root = ((Activity) context).getWindow().getDecorView();
            rebindInflatedWeatherWidgets(context, root);
        } catch (Throwable ignored) {
        }
    }

    /** Duyệt cây view, gặp widget thời tiết nội bộ nào thì bind lại. */
    private static void rebindInflatedWeatherWidgets(Context context, View view) {
        if (view == null) return;

        // [BUG FIX] "Widget thời tiết không tự cập nhật sau khi cấp quyền."
        //   Bản trước nhận diện bằng view.findViewById(widget_weather_temp) != null. Sai vì
        //   findViewById tìm TRONG CẢ CÂY CON: decorView cũng "chứa" id đó nên khớp ngay ở lần gọi
        //   đầu -> bind nhầm vào decorView rồi return, KHÔNG BAO GIỜ duyệt xuống widget thật.
        //   Nay nhận diện bằng KIỂU host view, thứ chỉ đúng ở đúng một cấp trong cây.
        if (view instanceof com.amz.ios.launcher.LauncherAppWidgetHostView) {
            com.amz.ios.launcher.LauncherAppWidgetProviderInfo info =
                    ((com.amz.ios.launcher.LauncherAppWidgetHostView) view)
                            .getLauncherAppWidgetProviderInfo();
            if (info != null && info.provider != null && isWeatherProvider(info.provider.getClassName())) {
                bindInflatedView(context, view);
            }
            return;   // host view là một widget hoàn chỉnh, không đi sâu hơn
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rebindInflatedWeatherWidgets(context, group.getChildAt(i));
            }
        }
    }

    /** Tên class có phải một trong 3 provider thời tiết không. */
    private static boolean isWeatherProvider(String className) {
        return WeatherWidgetProvider.class.getName().equals(className)
                || WeatherMediumWidgetProvider.class.getName().equals(className)
                || WeatherLargeWidgetProvider.class.getName().equals(className);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        updateWidgets(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public int getPreviewImage() {
        return 0;
    }

    @Override
    public int getIcon() {
        return 0;
    }

    @Override
    public int getWidgetLayout() {
        return 0;
    }

    @Override
    public ComponentName getConfigure() {
        return null;
    }

    @Override
    public int getSpanX() {
        return 2;
    }

    @Override
    public int getSpanY() {
        return 2;
    }

    @Override
    public int getMinSpanX() {
        return 2;
    }

    @Override
    public int getMinSpanY() {
        return 2;
    }

    @Override
    public int getResizeMode() {
        return 0;
    }
}
