package com.amz.ios.launcher.leftpage.widgets;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.leftpage.model.CalendarEventInfo;
import com.amz.ios.launcher.leftpage.adapter.EventListAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

public class CalendarWidget_2x2 extends BlurConstraintLayoutWidget implements View.OnClickListener {

    final String EVENT_DT_END = "dtend";
    final String EVENT_DT_START = "dtstart";
    final String EVENT_TITLE = "title";
    final String EVENT_COLOR = "eventColor";
    final String EVENT_ID_FIELD = "event_id";
    final String EVENT_ALL_DAY_FIELD = "allDay";

    public Launcher mLauncher;
    public RecyclerView mEventListRV;
    public EventListAdapter mEventListAdapter;
    public Handler mHandler;
    public Runnable mReloadRunnable;
    public int mMargin;
    public TextViewCustomFont mCalendarDayView;
    public TextViewCustomFont mCalendarDayInWeekView;
    public TextViewCustomFont mRequestPermissionBtn;
    public LinearLayoutCompat mCalendarPermissionView;
    public Locale mLocale;
    public CalendarContentObserver mContentObserver;
    public boolean canLoadingEvents = true;

    public class CalendarContentObserver extends ContentObserver {
        public CalendarContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (canLoadingEvents) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(mReloadRunnable, 5000L);
            }
        }
    }

    public CalendarWidget_2x2(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CalendarWidget_2x2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        mHandler = new Handler();
        mReloadRunnable = new Runnable() {
            @Override
            public void run() {
                getEvents();
            }
        };
        LayoutInflater.from(context).inflate(
                R.layout.calendar_widget_2x2,
                this,
                true
        );
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setAdapter();
        setListeners();
    }

    void setUpView(){
        mEventListRV = findViewById(R.id.event_all);
        mCalendarDayView = findViewById(R.id.calendar_day);
        mCalendarDayInWeekView = findViewById(R.id.calendar_day_in_week);
        mCalendarPermissionView = findViewById(R.id.calendar_widget_permission);
        mRequestPermissionBtn = findViewById(R.id.button_request_calendar_permission);
        mLocale = Locale.getDefault();
        mHandler = new Handler();
        mContentObserver = new CalendarContentObserver(mHandler);

        Drawable drawable = ContextCompat.getDrawable(mLauncher,R.drawable.widget_left_page_background);
        drawable.setAlpha(255);
        setBackground(drawable);
        setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
    }

    void setAdapter(){
        mEventListAdapter = new EventListAdapter(mLauncher);
        mEventListRV.setAdapter(mEventListAdapter);
        mEventListRV.setLayoutManager(new LinearLayoutManager(mLauncher));
        mEventListRV.setItemAnimator(new DefaultItemAnimator());
    }

    void setListeners(){
        mRequestPermissionBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v != mRequestPermissionBtn) return;
        // TODO: 2023.11.20 Request Calendar Permission

    }

    public void getEvents(){
        this.canLoadingEvents = false;
        try {
            final ArrayList<CalendarEventInfo> list = new CalendarWidgetReloadCallable().call();
            postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (list.size() < 2) {
                                list.add(
                                        new CalendarEventInfo(0, -16711936, mLauncher.getString(R.string.no_more_events), "")
                                );
                            }
                            mEventListAdapter.setEvents(list);
                            canLoadingEvents = true;
                        }
                    }
            ,1000L);
        }
        catch (Throwable th){

        }
    }

    public class CalendarWidgetReloadCallable implements Callable<ArrayList<CalendarEventInfo>> {

        @SuppressLint("Range")
        @Override
        public ArrayList<CalendarEventInfo> call() throws Exception {
            String str;
            String str2;

            Thread.currentThread().getName();
            ArrayList<CalendarEventInfo> arrayList = new ArrayList<>();
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.getTimeInMillis();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long timeInMillis = calendar.getTimeInMillis();
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                long timeInMillis2 = calendar.getTimeInMillis();

                Calendar calendar2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar2.set(Calendar.HOUR_OF_DAY, 0);
                calendar2.set(Calendar.MINUTE, 0);
                calendar2.set(Calendar.SECOND, 0);
                calendar2.set(Calendar.MILLISECOND, 0);
                calendar2.getTimeInMillis();
                calendar2.add(Calendar.DAY_OF_MONTH, 1);
                calendar2.getTimeInMillis();

                Uri.Builder buildUpon = CalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(buildUpon, timeInMillis);
                ContentUris.appendId(buildUpon, timeInMillis2);
                Cursor query = mLauncher.getContentResolver().query(buildUpon.build(), new String[]{"event_id", "eventColor", "title", "dtstart", "dtend", "allDay"}, "visible=1", null, "startDay ASC, startMinute ASC, endDay ASC, endMinute ASC");
                if (query != null) {
                    int index = 0;
                    while (query.moveToNext()) {
                        int i3 = index + 1;
                        int eventId = query.getInt(query.getColumnIndex(EVENT_ID_FIELD));
                        int eventColor = query.getInt(query.getColumnIndex(EVENT_COLOR));
                        String eventTitle = query.getString(query.getColumnIndex(EVENT_TITLE));
                        int isAllDay = query.getInt(query.getColumnIndex(EVENT_ALL_DAY_FIELD));
                        long eventStartTime = query.getLong(query.getColumnIndex(EVENT_DT_START));
                        long eventEndTime = query.getLong(query.getColumnIndex(EVENT_DT_END));
                        if (isAllDay == 1) {
                            str2 = mLauncher.getString(R.string.all_day);
                        } else {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateFormat.is24HourFormat(mLauncher) ? "hh:mm" : "kk:mm", mLocale);
                            Date date = new Date(eventStartTime);
                            Date date2 = new Date(eventEndTime);
                            str2 = simpleDateFormat.format(date) + " - " + simpleDateFormat.format(date2);
                        }
                        if (eventColor == 0) {
                            eventColor = -6543440;
                        }
                        if (eventTitle == null || eventTitle.isEmpty()) {
                            eventTitle = mLauncher.getString(R.string.unknown);
                        }
                        CalendarEventInfo aVar = new CalendarEventInfo(eventId, eventColor, eventTitle, str2);
                        if (i3 <= 2) {
                            arrayList.add(aVar);
                        }
                        index = i3;
                    }
                    query.close();
                }
            } catch (Throwable th) {
                th.getMessage();
            }
            return arrayList;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        this.mLauncher.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        Date time = Calendar.getInstance().getTime();
        String format = new SimpleDateFormat("dd", Locale.getDefault()).format(time);
        String format2 = new SimpleDateFormat("EEEE", Locale.getDefault()).format(time);
        this.mCalendarDayView.setText(format);
        this.mCalendarDayInWeekView.setText(format2);
//        if (bb0.a(this.j)) {
            mCalendarPermissionView.setVisibility(View.GONE);
            mLauncher.getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mContentObserver);
            if (canLoadingEvents) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(this.mReloadRunnable, 5000L);
            }
            /*
        } else {
            mCalendarPermissionView.setVisibility(View.VISIBLE);
        }
             */
    }
}
