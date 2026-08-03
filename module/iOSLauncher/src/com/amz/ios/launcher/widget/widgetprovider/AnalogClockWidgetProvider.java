package com.amz.ios.launcher.widget.widgetprovider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

import com.amz.ios.launcher.IOSAppWidget;

/**
 * Widget đồng hồ kim lớn (2x2) dùng cho page 0. Nội dung do custom
 * {@link com.amz.ios.launcher.widget.view.AnalogClockView} trong initialLayout
 * (@layout/widget_analog_clock) tự vẽ & tự tick — widget nội bộ (IOS_WIDGET id
 * -100) được launcher inflate thành View thật nên không cần RemoteViews/onUpdate.
 */
public class AnalogClockWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
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
