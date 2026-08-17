package com.amz.ios.launcher.widget.widgetprovider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;

/**
 * Built-in iOS-style calendar widget (2x2). Uses {@link android.widget.TextClock}
 * inside a RemoteViews so weekday/day/month tự cập nhật theo ngày hệ thống,
 * không cần AlarmManager hay nguồn dữ liệu ngoài.
 */
public class CalendarWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public CalendarWidgetProvider() {
        super();
    }

    void updateWidgets(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.calendar_widget_provider);

        // Chạm mở app Lịch hệ thống nếu có.
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build());
        try {
            rv.setOnClickPendingIntent(R.id.widget_calendar_layout,
                    PendingIntent.getActivity(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } catch (Throwable ignored) {
        }

        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, CalendarWidgetProvider.class), rv);
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
