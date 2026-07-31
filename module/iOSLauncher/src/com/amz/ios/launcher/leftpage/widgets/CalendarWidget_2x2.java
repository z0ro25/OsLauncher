package com.amz.ios.launcher.leftpage.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
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
    public LinearLayoutCompat mCalendarContentView;
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
        mCalendarContentView = findViewById(R.id.calendar_widget_content);
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
        // Widget lịch không dùng quyền lịch nữa nên không còn hành vi xin quyền.
    }

    public void getEvents(){
        // Chỉ hiển thị ngày, không list sự kiện.
        if (mEventListRV != null) mEventListRV.setVisibility(View.GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        this.mLauncher.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        // Ngày lấy trực tiếp từ current millis, KHÔNG cần quyền lịch.
        Date time = new Date(System.currentTimeMillis());
        String format = new SimpleDateFormat("dd", Locale.getDefault()).format(time);
        String format2 = new SimpleDateFormat("EEEE", Locale.getDefault()).format(time);
        this.mCalendarDayView.setText(format);
        this.mCalendarDayInWeekView.setText(format2);
        // Không xin quyền READ_CALENDAR nữa: luôn hiện phần nội dung (ngày), ẩn màn permission.
        mCalendarPermissionView.setVisibility(View.GONE);
        if (mCalendarContentView != null) mCalendarContentView.setVisibility(View.VISIBLE);
        getEvents();
    }
}
