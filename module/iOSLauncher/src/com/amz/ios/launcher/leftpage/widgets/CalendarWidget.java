package com.amz.ios.launcher.leftpage.widgets;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
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

public class CalendarWidget extends BlurConstraintLayoutWidget implements View.OnClickListener {

    final String EVENT_DT_END = "dtend";
    final String EVENT_DT_START = "dtstart";
    final String EVENT_TITLE = "title";
    final String EVENT_COLOR = "eventColor";
    final String EVENT_ID_FIELD = "event_id";
    final String EVENT_ALL_DAY_FIELD = "allDay";

    public Launcher mLauncher;
    public int mMargin;
    public LayoutInflater mLayoutInflater;
    public ArrayList<CalendarEventInfo> mCalenderEventOne;
    public ArrayList<CalendarEventInfo> mCalendarEventAll;
    public RecyclerView mEventOneRV;
    public RecyclerView mEventAllRV;
    public TextView mCalendarErrorTV;
    public TextView mPermissionRequestBtn;
    public AppCompatImageView mMoreIconIV;
    public TextViewCustomFont mCalendarDay;
    public TextViewCustomFont mCalendarDayInWeek;
    public EventListAdapter mEventAllAdapter;
    public EventListAdapter mEventOneAdapter;
    public Handler mHandler;
    public Runnable mEventReloadRunnable;
    public ContentObserver mCalendarChangeObserver;
    public Locale mLocale;
    public boolean showMore = false;
    public boolean canReload = true;

    public CalendarWidget(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CalendarWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mLayoutInflater = LayoutInflater.from(context);
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        mHandler = new Handler();
        mEventReloadRunnable = new EventReloadRunnable();
        mCalendarChangeObserver = new CalendarContentObserver(new Handler());
        mLayoutInflater.inflate(R.layout.calendar_widget,this,true);
    }

    public void getEvents(){
        canReload = false;
        try {
            Integer num = new EventLoadCallable().call();
            mEventOneAdapter.setEvents(mCalenderEventOne);
            if (num > 2) {
                mEventAllAdapter.setEvents(mCalendarEventAll);
                mEventAllRV.setVisibility(View.VISIBLE);
            } else {
                mEventAllRV.setVisibility(View.GONE);
            }
            canReload = true;
        }
        catch (Throwable throwable){

        }
    }

    @Override
    public void r() {
        super.r();
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        y();
        z();
    }

    void setUpView(){
        mEventOneRV = findViewById(R.id.event_one);
        mEventAllRV = findViewById(R.id.event_all);
        mMoreIconIV = findViewById(R.id.more_icon);
        mCalendarDay = findViewById(R.id.calendar_day);
        mCalendarDayInWeek = findViewById(R.id.calendar_day_in_week);
        mCalendarErrorTV = findViewById(R.id.calendar_error);
        mPermissionRequestBtn = findViewById(R.id.button_request_calendar_permission);
    }

    void setListeners(){
        mMoreIconIV.setOnClickListener(this);
        mPermissionRequestBtn.setOnClickListener(this);
    }

    void setAdapter(){
        mEventOneAdapter = new EventListAdapter(mLauncher);
        mEventOneRV.setItemAnimator(new DefaultItemAnimator());
        mEventOneRV.setLayoutManager(new LinearLayoutManager(mLauncher));
        mEventOneRV.setAdapter(mEventOneAdapter);

        mEventAllAdapter = new EventListAdapter(mLauncher);
        mEventAllRV.setItemAnimator(new DefaultItemAnimator());
        mEventAllRV.setLayoutManager(new LinearLayoutManager(mLauncher));
        mEventAllRV.setAdapter(mEventAllAdapter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setListeners();
        setAdapter();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LinearLayoutCompat linearLayoutCompat = findViewById(R.id.calendar_widget_content);
        ((ConstraintLayout.LayoutParams) linearLayoutCompat.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
        setTextAndBackgroundColor(linearLayoutCompat);
        LinearLayoutCompat linearLayoutCompat2 = findViewById(R.id.calendar_widget_permission);
        ((ConstraintLayout.LayoutParams) linearLayoutCompat2.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
        setTextAndBackgroundColor(linearLayoutCompat2);
        y();
        z();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLauncher.getContentResolver().unregisterContentObserver(mCalendarChangeObserver);
    }

    @Override
    public void onClick(View v) {
        if (v == mMoreIconIV){
            boolean z = true;
            this.showMore = !this.showMore;
            RecyclerView recyclerView;
            if (showMore) {
                this.mMoreIconIV.animate().rotation(90.0f).setDuration(268L).start();
                recyclerView = mEventAllRV;
            } else {
                this.mMoreIconIV.animate().rotation(0.0f).setDuration(268L).start();
                recyclerView = mEventOneRV;
                z = false;
            }
            w(recyclerView, z);
        }
        else if (v == mPermissionRequestBtn){
            // TODO: 2023.11.22 Request Permission
        }
    }

    public class EventReloadRunnable implements Runnable {

        @Override
        public void run() {
            getEvents();
        }
    }

    public class EventLoadCallable implements Callable<Integer>{

        @SuppressLint("Range")
        public final Integer call() {
            String time;
            ArrayList<CalendarEventInfo> arrayList;
            Thread.currentThread().getName();
            int i = 0;
            try {
                if (mCalenderEventOne == null) {
                    mCalenderEventOne = new ArrayList<>();
                } else {
                    mCalenderEventOne.clear();
                }

                if (mCalendarEventAll == null) {
                    mCalendarEventAll = new ArrayList<>();
                } else {
                    mCalendarEventAll.clear();
                }

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
                    while (query.moveToNext()) {
                        i++;
                        int eventId = query.getInt(query.getColumnIndex(EVENT_ID_FIELD));
                        int color = query.getInt(query.getColumnIndex(EVENT_COLOR));
                        String eventTitle = query.getString(query.getColumnIndex(EVENT_TITLE));
                        int eventAll = query.getInt(query.getColumnIndex(EVENT_ALL_DAY_FIELD));
                        long eventDTStart = query.getLong(query.getColumnIndex(EVENT_DT_START));
                        long eventDTEnd = query.getLong(query.getColumnIndex(EVENT_DT_END));

                        if (eventAll == 1) {
                            time = mLauncher.getString(R.string.all_day);
                        } else {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateFormat.is24HourFormat(mLauncher) ? "hh:mm" : "kk:mm", mLocale);
                            Date date = new Date(eventDTStart);
                            Date date2 = new Date(eventDTEnd);
                            time = simpleDateFormat.format(date) + " - " + simpleDateFormat.format(date2);
                        }

                        int eventColor = color == 0 ? -6543440 : color;
                        if (eventTitle == null || eventTitle.isEmpty()) {
                            eventTitle = mLauncher.getString(R.string.unknown);
                        }

                        CalendarEventInfo aVar = new CalendarEventInfo(eventId, eventColor, eventTitle, time);
                        if (i <= 2) {
                            arrayList = mCalenderEventOne;
                        } else {
                            arrayList = mCalendarEventAll;
                        }
                        arrayList.add(aVar);
                    }
                    query.close();
                }
            } catch (Throwable th) {
                th.getMessage();
            }
            return i;
        }
    }

    public class CalendarContentObserver extends ContentObserver {

        public CalendarContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(mEventReloadRunnable,5000L);
        }
    }

    public final void y() {

        // TODO: 2023.11.22 Check CalendarPermission
        boolean isPermissionGranted = false;

        if (!isPermissionGranted){
            mPermissionRequestBtn.setVisibility(View.VISIBLE);
            mCalendarErrorTV.setVisibility(View.VISIBLE);
        }
        else {
            mPermissionRequestBtn.setVisibility(View.GONE);
            mCalendarErrorTV.setVisibility(View.GONE);
            mLauncher.getContentResolver().registerContentObserver(
                CalendarContract.Events.CONTENT_URI, true, mCalendarChangeObserver
            );
            if (!canReload || (mCalenderEventOne != null && mCalenderEventOne.size() <= 0)){
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(mEventReloadRunnable,5000L);
            }
        }

    }

    public final void z() {
        Date time = Calendar.getInstance().getTime();
        String date = new SimpleDateFormat("dd", Locale.getDefault()).format(time);
        String dayInWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(time);
        mCalendarDay.setText(date);
        mCalendarDayInWeek.setText(dayInWeek);
    }
}
