package com.amz.ios.launcher.widget.widgetprovider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

import com.amz.ios.launcher.IOSAppWidget;

/**
 * Widget nội bộ "Calendar - Up Next vừa" (4x2): 2 cột hôm nay / ngày mai + "N more events".
 *
 * Widget IOS (id -100): tự vẽ trong View qua
 * {@link com.amz.ios.launcher.widget.view.CalendarUpNextView} (variant medium), không RemoteViews.
 */
public class CalendarUpNextMediumWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public CalendarUpNextMediumWidgetProvider() {
        super();
    }

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
