package com.amz.ios.ioslite.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    public static final long MINUTE_OF_MILLISECONDS = 60 * 1000;
    public static final long HOUR_OF_MILLISECONDS = 60 * MINUTE_OF_MILLISECONDS;
    public static final long DAY_OF_MILLISECONDS = 24 * HOUR_OF_MILLISECONDS;


    /**
     * 格式化时间
     */
    public static String formatTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(time));
    }

    /**
     * Localization date
     */
    public static String localizatedTime(long time, String preYear, String preMonth, String preDay, String preHour, String preMinute, String preSecond) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy" + preYear +
                "M" + preMonth +
                "d" + preDay +
                "H" + preHour +
                "m" + preMinute +
                "s" + preSecond);
        return sdf.format(new Date(time));
    }
}
