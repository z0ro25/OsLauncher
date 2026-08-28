/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.TransactionTooLargeException;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;


/**
 * Specific {@link AppWidgetHost} that creates our {@link LauncherAppWidgetHostView}
 * which correctly captures all long-press events. This ensures that users can
 * always pick up and move widgets.
 */
public class LauncherAppWidgetHost extends AppWidgetHost {

    private final ArrayList<Runnable> mProviderChangeListeners = new ArrayList<Runnable>();

    private int mQsbWidgetId = -1;
    private Launcher mLauncher;

    public LauncherAppWidgetHost(Launcher launcher, int hostId) {
        super(launcher, hostId);
        mLauncher = launcher;
    }

    public void setQsbWidgetId(int widgetId) {
        mQsbWidgetId = widgetId;
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
                                             AppWidgetProviderInfo appWidget) {
        if (appWidgetId == mQsbWidgetId) {
            return new LauncherAppWidgetHostView(context) {

                @Override
                protected View getErrorView() {
                    // For the QSB, show an empty view instead of an error view.
                    return new View(getContext());
                }
            };
        }
        return new LauncherAppWidgetHostView(context);
    }

    @Override
    public void startListening() {
        try {
            super.startListening();
        } catch (Exception e) {
            if (e.getCause() instanceof TransactionTooLargeException) {
                // We're willing to let this slide. The exception is being caused by the list of
                // RemoteViews which is being passed back. The startListening relationship will
                // have been established by this point, and we will end up populating the
                // widgets upon bind anyway. See issue 14255011 for more context.
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stopListening() {
        super.stopListening();
        clearViews();
    }

    public void addProviderChangeListener(Runnable callback) {
        mProviderChangeListeners.add(callback);
    }

    public void removeProviderChangeListener(Runnable callback) {
        mProviderChangeListeners.remove(callback);
    }

    protected void onProvidersChanged() {
        if (!mProviderChangeListeners.isEmpty()) {
            for (Runnable callback : new ArrayList<>(mProviderChangeListeners)) {
                callback.run();
            }
        }

        if (Utilities.ATLEAST_MARSHMALLOW){
            mLauncher.notifyWidgetProvidersChanged();
        }
    }

    public AppWidgetHostView createView(Context context, int appWidgetId,
                                        LauncherAppWidgetProviderInfo appWidget) {

        // [FIX WIDGET TRỐNG] Quyết định nhánh render theo PACKAGE sở hữu, KHÔNG chỉ dựa cờ
        //   isIOSWidget trong map provider. Lý do (đo bằng logcat + truy vết getWidgetProviders):
        //   loop getAllProviders() đăng ký provider nội bộ với isIOSWidget=FALSE; guard "khung xám"
        //   là dead-code (loop IOSAppWidget rỗng vì ENABLE_CUSTOM_WIDGET_TEST=false), còn overwrite
        //   của loop queryBroadcastReceivers có thể TRƯỢT (getUser khác key, hoặc nuốt exception).
        //   Khi cờ = false, widget iOS kéo-thả rơi xuống super.createView (AppWidgetHost THẬT) ->
        //   RemoteViews chặn custom view (InflateException) -> host RỖNG = khung trống. Mọi provider
        //   CÙNG PACKAGE với launcher đều là widget nội bộ (custom view, initialLayout trỏ tới layout
        //   thật) nên luôn phải đi nhánh inflate nội bộ, bất kể cờ. Nhánh isOwnPackageWidget không
        //   ảnh hưởng widget hệ thống thật (khác package -> vẫn qua super.createView như cũ).
        boolean isOwnPackageWidget = appWidget.provider != null
                && context.getPackageName().equals(appWidget.provider.getPackageName());
        if (appWidget.isIOSWidget || isOwnPackageWidget) {
            String pkgName = appWidget.provider.getPackageName();
            LauncherAppWidgetHostView lahv = new LauncherAppWidgetHostView(context);
            if (pkgName.equals(context.getPackageName())) {
                LayoutInflater inflater = (LayoutInflater)
                        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                inflater.inflate(appWidget.initialLayout, lahv);
                lahv.setAppWidget(0, appWidget);
                lahv.updateLastInflationOrientation();
                if (appWidget.isIOSIconWidget) {
                    lahv.setPadding(0, 0, 0, 0);
                }
                // Widget nội bộ không đi qua onUpdate() nên phải gắn nội dung động
                // (ảnh gần đây) trực tiếp sau khi inflate. Dùng chung cho cả 3 size Photos
                // (2x2 / medium 4x2 / large 4x4) vì bindInflatedView tìm view theo id, độc lập layout.
                if (appWidget.provider != null) {
                    String cls = appWidget.provider.getClassName();
                    if (com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider.class.getName().equals(cls)
                            || com.amz.ios.launcher.widget.widgetprovider.PictureMediumWidgetProvider.class.getName().equals(cls)
                            || com.amz.ios.launcher.widget.widgetprovider.PictureLargeWidgetProvider.class.getName().equals(cls)) {
                        com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider
                                .bindInflatedView(context, lahv);
                        // Bấm widget (đặt màn) mở THƯ VIỆN ẢNH. Gắn ở đường đặt màn
                        // này (KHÔNG ở preview) để không phá tap mở carousel trong khay chọn.
                        com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider
                                .attachOpenGalleryClick(context, lahv, appWidgetId);
                    }
                }
                // Widget Battery cũng vẽ vòng pin động trực tiếp (không đi qua onUpdate).
                if (appWidget.provider != null
                        && com.amz.ios.launcher.widget.widgetprovider.BatteryWidgetProvider.class
                        .getName().equals(appWidget.provider.getClassName())) {
                    com.amz.ios.launcher.widget.widgetprovider.BatteryWidgetProvider
                            .bindInflatedView(context, lahv);
                }
                // Widget Weather: đổ dữ liệu thật từ Open-Meteo (hoặc lớp phủ xin quyền vị trí).
                // Dùng chung cho cả 3 size vì bindInflatedView tra view theo id, độc lập layout.
                if (appWidget.provider != null) {
                    String weatherCls = appWidget.provider.getClassName();
                    if (com.amz.ios.launcher.widget.widgetprovider.WeatherWidgetProvider.class.getName().equals(weatherCls)
                            || com.amz.ios.launcher.widget.widgetprovider.WeatherMediumWidgetProvider.class.getName().equals(weatherCls)
                            || com.amz.ios.launcher.widget.widgetprovider.WeatherLargeWidgetProvider.class.getName().equals(weatherCls)) {
                        com.amz.ios.launcher.widget.widgetprovider.WeatherWidgetProvider
                                .bindInflatedView(context, lahv);
                    }
                }

            } else {
                try {
                    Context newContext = mLauncher.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                    LayoutInflater inflater = (LayoutInflater) newContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    inflater.inflate(appWidget.initialLayout, lahv);
                    lahv.setAppWidget(0, appWidget);
                    lahv.updateLastInflationOrientation();
                } catch (Exception e) {
                }
            }

            return lahv;
        } else {
            return super.createView(context, appWidgetId, appWidget);
        }
    }

    /**
     * Called when the AppWidget provider for a AppWidget has been upgraded to a new apk.
     */
    @Override
    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
        LauncherAppWidgetProviderInfo info = LauncherAppWidgetProviderInfo.fromProviderInfo(
                mLauncher, appWidget);
        super.onProviderChanged(appWidgetId, info);
    }
}
