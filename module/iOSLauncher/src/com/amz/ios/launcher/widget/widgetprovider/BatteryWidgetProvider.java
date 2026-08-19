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
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.widget.WidgetSheetTheme;

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

    /**
     * Gắn nội dung động (vòng pin + %) TRỰC TIẾP vào view đã inflate của widget nội bộ.
     * Widget iOS được inflate thành View thật (xem LauncherAppWidgetHost.createView) và
     * KHÔNG đi qua onUpdate()/RemoteViews, nên phải vẽ vòng pin ở đây — giống cách
     * PictureAppWidgetProvider.bindInflatedView() làm. Dùng cho cả widget đặt màn lẫn
     * preview sống trong khay. Vòng pin: track mờ + arc XANH iOS (đỏ khi &lt;10%) + icon phone.
     */
    public static void bindInflatedView(Context context, View root) {
        if (context == null || root == null) return;
        ImageView ring = root.findViewById(R.id.widget_battery_progress);
        TextView text = root.findViewById(R.id.widget_battery_text);
        View card = root.findViewById(R.id.widget_battery_layout);

        int percent = readBatteryPercent(context);

        // Theme dark/light theo nguồn sự thật của khay (engine bỏ qua -night).
        boolean dark = WidgetSheetTheme.isDark(context);
        int fg = WidgetSheetTheme.textPrimary(context); // trắng khi dark, #1C1C1E khi light
        if (card != null) {
            int pad = (int) context.getResources().getDimension(R.dimen.widget_padding);
            card.setBackgroundResource(dark
                    ? R.drawable.widget_battery_card_dark
                    : R.drawable.widget_battery_card_light);
            // setBackgroundResource có thể reset padding -> đặt lại cho chắc.
            card.setPadding(pad, pad, pad, pad);
        }

        if (ring != null) {
            int progressSize = (int) context.getResources()
                    .getDimension(R.dimen.battery_widget_progress_size);
            float stroke = context.getResources()
                    .getDimension(R.dimen.battery_widget_progress_stroke);
            if (progressSize <= 0) progressSize = 1;

            Bitmap bmp = Bitmap.createBitmap(progressSize, progressSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeCap(Paint.Cap.ROUND);
            track.setStrokeWidth(stroke);
            track.setColor(0x33FFFFFF);              // track mờ (100% thì bị arc phủ hết)

            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setStyle(Paint.Style.STROKE);
            fill.setStrokeCap(Paint.Cap.ROUND);
            fill.setStrokeWidth(stroke);
            fill.setColor(percent < 10 ? 0xFFFF3B30 : 0xFF34C759); // đỏ / xanh iOS

            RectF rectF = new RectF(stroke, stroke, progressSize - stroke, progressSize - stroke);
            canvas.drawArc(rectF, 0f, 360f, false, track);
            canvas.drawArc(rectF, 270f, percent * 3.6f, false, fill);

            Drawable phone = ContextCompat.getDrawable(context, R.drawable.ic_phone);
            if (phone != null) {
                phone = phone.mutate();
                phone.setTint(fg);                   // phone tối trên nền sáng, trắng trên nền tối
                int inset = (int) (stroke * 3.2f);
                phone.setBounds(inset, inset, progressSize - inset, progressSize - inset);
                phone.draw(canvas);
            }

            ring.setScaleType(ImageView.ScaleType.FIT_START);
            ring.setImageBitmap(bmp);
        }

        if (text != null) {
            text.setTextColor(fg);
            text.setText(String.format("%d%%", percent));
        }
    }

    /** Đọc % pin hiện tại từ sticky BATTERY_CHANGED (không cần chờ broadcast). */
    private static int readBatteryPercent(Context context) {
        try {
            Intent b = context.registerReceiver(null,
                    new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (b == null) return 100;
            int level = b.getIntExtra("level", 0);
            int scale = b.getIntExtra("scale", 1);
            if (scale <= 0) scale = 100;
            return (level * 100) / scale;
        } catch (Throwable t) {
            return 100;
        }
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
