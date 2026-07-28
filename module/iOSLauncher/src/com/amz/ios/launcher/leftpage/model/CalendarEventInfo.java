package com.amz.ios.launcher.leftpage.model;

public class CalendarEventInfo {

    public int mEventId;
    public int mEventColor;
    public String mEventName;
    public String mEventTime;

    public CalendarEventInfo(int id, int i2, String eventTitle, String eventTime) {
        this.mEventId = id;
        this.mEventColor = i2;
        this.mEventName = eventTitle;
        this.mEventTime = eventTime;
    }
}

