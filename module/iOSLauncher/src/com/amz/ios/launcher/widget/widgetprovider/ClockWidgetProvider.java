package com.amz.ios.launcher.widget.widgetprovider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;

/**
 * Built-in iOS-style digital clock widget. Uses {@link android.widget.TextClock}
 * inside a RemoteViews, so the time ticks by itself without an AlarmManager.
 */
public class ClockWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public ClockWidgetProvider() {
        super();
    }

    void updateWidgets(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.clock_widget_provider);

        // Tap opens the system clock app if available, otherwise the alarms screen.
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage("com.android.deskclock");
        if (intent == null) {
            intent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
        }
        try {
            rv.setOnClickPendingIntent(R.id.widget_clock_layout,
                    PendingIntent.getActivity(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } catch (Throwable ignored) {
        }

        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, ClockWidgetProvider.class), rv);
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
