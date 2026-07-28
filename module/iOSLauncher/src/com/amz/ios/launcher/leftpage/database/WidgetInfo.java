package com.amz.ios.launcher.leftpage.database;

import org.litepal.LitePal;
import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

import java.util.List;

public class WidgetInfo extends LitePalSupport {
    public int layoutId;
    public int order;
    public int size;
    public int type;

    public static synchronized void swapItems(WidgetInfo widget1, WidgetInfo widget2){
        int order = widget1.order;
        widget1.order = widget2.order;
        widget2.order = order;
        widget1.save();
        widget2.save();
    }

    public static synchronized void reOrderItem(WidgetInfo info, int order){
        info.order = order;
        info.save();
    }

    public static synchronized List<WidgetInfo> getWidgets(){
        return LitePal.order("order").find(WidgetInfo.class);
    }

}
