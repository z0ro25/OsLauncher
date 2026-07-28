package com.amz.ios.launcher.assembly;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.config.Settings;

import java.util.List;

public class SearchWidgetUtil {
    private static final String TAG = "SearchWidgetUtil";

    public static boolean hasSearchWidget(Context context) {
        return Partner.getBoolean(context, Partner.FEATURE_SEARCH_BOX_ENABLE,true);
    }

    public static boolean shouldShow(Context context) {
        return Settings.isSearchBarEnabled(context) && hasSearchWidget(context);
    }

    public static LauncherAppWidgetProviderInfo getSearchWidgetProvider(Context context) {
        LauncherAppWidgetProviderInfo searchWidgetProvider = getIOSSearchWidgetProvider(context, IOSAppWidget.IOS_NEWSPAGE_SEARCH_WIDGET_ACTION);
        if (searchWidgetProvider == null) {
            searchWidgetProvider = getIOSSearchWidgetProvider(context, IOSAppWidget.IOS_SEARCH_WIDGET_ACTION);
        }

        if (searchWidgetProvider != null) {
            return searchWidgetProvider;
        }

        return getDefaultSearchWidgetProvider(context);
    }

    public static LauncherAppWidgetProviderInfo getIOSSearchWidgetProvider(Context context, String action) {

        LauncherAppWidgetProviderInfo iosSearchProviderInfo = null;

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> queryInfos = packageManager.queryBroadcastReceivers(new Intent(action), PackageManager.GET_META_DATA);

        if (queryInfos.size() > 0) {
            ActivityInfo activityInfo = queryInfos.get(0).activityInfo;
            try {
                iosSearchProviderInfo = LauncherAppWidgetProviderInfo.fromIOSWidgetComponent(context, new ComponentName(activityInfo.packageName, activityInfo.name));
            } catch (Exception e) {
                Log.e(TAG, "getIOSSearchWidgetProvider fail ", e);
                return null;
            }
        }
        return iosSearchProviderInfo;
    }


    /**
     * Returns a widget with category {@link AppWidgetProviderInfo#WIDGET_CATEGORY_SEARCHBOX}
     * provided by the same package which is set to be global search activity.
     * If widgetCategory is not supported, or no such widget is found, returns the first widget
     * provided by the package.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static LauncherAppWidgetProviderInfo getDefaultSearchWidgetProvider(Context context) {
        SearchManager searchManager =
                (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        ComponentName searchComponent = searchManager.getGlobalSearchActivity();
        if (searchComponent == null) return null;
        String providerPkg = searchComponent.getPackageName();

        AppWidgetProviderInfo defaultWidgetForSearchPackage = null;

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (AppWidgetProviderInfo info : appWidgetManager.getInstalledProviders()) {
            if (info.provider.getPackageName().equals(providerPkg)) {
                if (Utilities.ATLEAST_JB_MR1) {
                    if ((info.widgetCategory & AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX) != 0) {
                        defaultWidgetForSearchPackage = info;
                        break;
                    } else if (defaultWidgetForSearchPackage == null) {
                        defaultWidgetForSearchPackage = info;
                    }
                } else {
                    defaultWidgetForSearchPackage = info;
                    break;
                }
            }
        }
        return LauncherAppWidgetProviderInfo.fromProviderInfo(defaultWidgetForSearchPackage);
    }
}
