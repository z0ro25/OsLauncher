package com.amz.ios.launcher.leftpage.model;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

public class CustomWidgetSummaryInfo {

    public Drawable mDrawable;
    public String mName;
    public boolean isFullItem;
    public ArrayList<CustomWidgetDetailInfo> mDetails;

    public CustomWidgetSummaryInfo(Drawable drawable, String name, boolean isFullItem, ArrayList<CustomWidgetDetailInfo> details) {
        this.mDrawable = drawable;
        this.mName = name;
        this.isFullItem = isFullItem;
        this.mDetails = details;
    }
}
