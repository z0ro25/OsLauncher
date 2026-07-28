package com.amz.ios.launcher;

import android.content.ComponentName;

public interface IOSAppWidget {
    String IOS_APP_WIDGET_ACTION = "android.appwidget.action.IOS_WIDGET";
    String IOS_SEARCH_WIDGET_ACTION = "android.appwidget.action.IOS_SEARCH_WIDGET";
    String IOS_NEWSPAGE_SEARCH_WIDGET_ACTION = "android.appwidget.action.IOS_NEWSPAGE_SEARCH_WIDGET";

    String getLabel();
    int getPreviewImage();
    int getIcon();
    int getWidgetLayout();
    ComponentName getConfigure();

    int getSpanX();
    int getSpanY();
    int getMinSpanX();
    int getMinSpanY();
    int getResizeMode();
}
