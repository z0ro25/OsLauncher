package com.amz.ios.launcher.widget.widgetprovider;

import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.widget.configure.PictureAppWidgetProviderConfigureActivity;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

public class PictureAppWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public static final String APP_WIDGET_PREFIX = "appwidget_";

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.picture_app_widget_provider);
        String string = sharedPreferences.getString("appwidget_" + appWidgetId, null);

        if (string != null) {
            Log.e("Update Id is" + appWidgetId, "Bitmap is Not Null");
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(new FileInputStream(new File(URI.create(string))));
            } catch (Throwable th) {
                th.printStackTrace();
            }

            if (bitmap != null) {
                rv.setImageViewBitmap(R.id.widget_picture_layout, bitmap);
            }
        }
        else {
            Log.e("Update Id is" + appWidgetId, "Bitmap is Null");
        }

        rv.setImageViewBitmap(R.id.widget_picture_layout, BitmapFactory.decodeResource(context.getResources(),R.drawable.above_shadow)
        );

        Intent intent = new Intent();
        intent.setClass(context, PictureAppWidgetProviderConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setAction("android.appwidget.action.APPWIDGET_CONFIGURE" + appWidgetId);

        ComponentName componentName = new ComponentName(context,PictureAppWidgetProvider.class);

        int[] ids = appWidgetManager.getAppWidgetIds(componentName);
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int appWidgetId : appWidgetIds){
            String key = APP_WIDGET_PREFIX + appWidgetId;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.remove(key);
            if (!editor.commit()) editor.apply();
            new File(new ContextWrapper(context).getDir("image", 0), key.replace("/", "_") + ".jpg").delete();
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds){
            Log.e("Update Widget", String.valueOf(appWidgetId));
            updateWidget(context,appWidgetManager,appWidgetId);
        }
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
