package com.amz.ios.launcher.widget.widgetprovider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;

/**
 * Built-in iOS-style weather widget (2x2). Bản CƠ BẢN: hiển thị dữ liệu mẫu tĩnh
 * (thành phố / nhiệt độ / tình trạng / H-L) đúng bố cục thiết kế. Dự án chưa có
 * nguồn/API thời tiết nên nội dung là placeholder cố định trong layout, chỗ cắm
 * dữ liệu thật đã sẵn (các id text) để nối API sau nếu cần.
 */
public class WeatherWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public WeatherWidgetProvider() {
        super();
    }

    void updateWidgets(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.weather_widget_provider);
        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, WeatherWidgetProvider.class), rv);
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
