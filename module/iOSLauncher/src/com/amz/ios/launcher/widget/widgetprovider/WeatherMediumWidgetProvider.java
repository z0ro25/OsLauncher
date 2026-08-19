package com.amz.ios.launcher.widget.widgetprovider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;

/**
 * Weather MEDIUM (4x2). Nhân bản {@link WeatherWidgetProvider} cho size 4x2: header +
 * dải giờ. Dữ liệu là mẫu tĩnh trong layout (dự án chưa có API thời tiết). Span thực lấy
 * từ minWidth/minHeight trong res/xml/weather_medium_widget_provider_info.xml, không từ
 * getSpanX/Y ở đây (xem LauncherAppWidgetProviderInfo.initSpans()).
 */
public class WeatherMediumWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public WeatherMediumWidgetProvider() {
        super();
    }

    void updateWidgets(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.weather_medium_widget_provider);
        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, WeatherMediumWidgetProvider.class), rv);
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
        return 4;
    }

    @Override
    public int getSpanY() {
        return 2;
    }

    @Override
    public int getMinSpanX() {
        return 4;
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
