package com.amz.ios.launcher.widget.widgetprovider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import com.amz.ios.launcher.IOSAppWidget;

/**
 * Widget nội bộ "Calendar - Month" (2x2): lịch tháng mini kiểu iOS.
 *
 * Đây là widget IOS (IOS_WIDGET id -100) nên launcher inflate initialLayout thành View
 * thật; {@code onUpdate} KHÔNG bao giờ được gọi và không dùng RemoteViews. Toàn bộ nội
 * dung do {@link com.amz.ios.launcher.widget.view.CalendarMiniMonthView} tự vẽ Canvas.
 * Provider chỉ khai báo cỡ ô (span 2x2) + đăng ký qua Manifest để lọt vào picker widget.
 */
public class CalendarMonthWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public CalendarMonthWidgetProvider() {
        super();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        // Không làm gì: widget IOS tự vẽ trong View, không đi qua RemoteViews.
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
    public android.content.ComponentName getConfigure() {
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
