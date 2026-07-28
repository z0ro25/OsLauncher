package com.amz.ios.launcher.widget.widgetprovider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;

import java.util.Calendar;

public class BatteryWidgetProvider extends AppWidgetProvider implements IOSAppWidget {

    public final static String BATTERY_UPDATE_ACTION = "com.ios.ACTION_UPDATE_BATTERY_WIDGET";
    static PendingIntent mBatteryUpdatePI;
    static AlarmManager mAlarmManager;

    public Context mContext;
    public Paint mFillPaint;
    public Paint mUnFillPaint;
    public RectF mRectF;
    public Drawable mDrawable;
    public float mStrokeSize;
    public int mProgressSize;

    public BatteryWidgetProvider() {
        super();
    }

    @Override
    public IBinder peekService(Context myContext, Intent service) {
        return super.peekService(myContext, service);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mContext == null)
            mContext = context;

        if (intent == null || intent.getAction() == null || !BATTERY_UPDATE_ACTION.equals(intent.getAction())) {
            super.onReceive(context, intent);
        } else {
            updateWidgets(context, false);
        }
    }

    void updateAlarm(){
        initValues();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, 60000);
        mAlarmManager.setRepeating(AlarmManager.RTC,calendar.getTimeInMillis(),60000L, mBatteryUpdatePI);
    }

    void updateWidgets(Context context, boolean isForceUpdate){
        Intent registerReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int level = registerReceiver.getIntExtra("level", 0);
        int scale = registerReceiver.getIntExtra("scale", 1);
        if (scale <= 0) {
            scale = 100;
        }

        int percent = (level * 100) / scale;
        if (isForceUpdate || percent != -1){
            mContext = context;
            mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFillPaint.setStyle(Paint.Style.STROKE);
            mFillPaint.setStrokeCap(Paint.Cap.ROUND);
            mFillPaint.setColor(-16711936);

            mUnFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mUnFillPaint.setStrokeCap(Paint.Cap.ROUND);
            mUnFillPaint.setStyle(Paint.Style.STROKE);
            mUnFillPaint.setColor(-2130706433);

            mProgressSize = (int) mContext.getResources().getDimension(R.dimen.battery_widget_progress_size);
            mStrokeSize = mContext.getResources().getDimension(R.dimen.battery_widget_progress_stroke);
            mDrawable = ContextCompat.getDrawable(mContext,R.drawable.ic_phone);
            mRectF = new RectF();

            RemoteViews rv = new RemoteViews(mContext.getPackageName(),R.layout.battery_widget_provider);

            Bitmap bmp = Bitmap.createBitmap(mProgressSize,mProgressSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            mRectF.set(mStrokeSize, mStrokeSize, mProgressSize - mStrokeSize, mProgressSize - mStrokeSize);
            mUnFillPaint.setStrokeWidth(mStrokeSize);
            mFillPaint.setStrokeWidth(mStrokeSize);
            mFillPaint.setColor(percent < 10 ? -65536 : -16711936);

            canvas.drawArc(mRectF,0.0f,360.f,false,this.mUnFillPaint);
            canvas.drawArc(mRectF,270.0f,percent * 3.6f,false,this.mFillPaint);

            int size = (int) (mStrokeSize * 3.2);

            mDrawable.setBounds(size,size, mProgressSize - size, mProgressSize - size);
            mDrawable.draw(canvas);

            rv.setImageViewBitmap(R.id.widget_battery_progress,bmp);
            rv.setTextViewText(R.id.widget_battery_text,String.format("%s%%", percent));

            Intent intent = new Intent("android.intent.action.POWER_USAGE_SUMMARY");
            intent.setPackage(mContext.getPackageName());

            rv.setOnClickPendingIntent(R.id.widget_battery_layout, PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT));
            AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, BatteryWidgetProvider.class), rv);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        updateAlarm();
        updateWidgets(context,true);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context.getPackageName(), getClass().getName())).length == 0) {
            initValues();
            mAlarmManager.cancel(mBatteryUpdatePI);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    public void initValues(){
        if (mBatteryUpdatePI == null){
            Intent batteryUpdateIntent = new Intent(mContext, BatteryWidgetProvider.class);
            batteryUpdateIntent.setPackage(mContext.getPackageName());
            batteryUpdateIntent.setAction(BATTERY_UPDATE_ACTION);
            mBatteryUpdatePI = PendingIntent.getBroadcast(mContext, 868686868, batteryUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (mAlarmManager == null){
            mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
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

    @Override
    public ComponentName getConfigure() {
        return null;
    }
}
