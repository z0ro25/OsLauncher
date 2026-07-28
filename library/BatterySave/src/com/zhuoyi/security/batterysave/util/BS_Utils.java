package com.zhuoyi.security.batterysave.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


import com.zhuoyi.security.batterysave.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by zengrui on 2016/8/18.
 */
public class BS_Utils {

    public static final String BatteryKeyStatus = "status";
    public static final String BatteryKeyLevel = "level";
    public static final String BatteryKeyTime = "time";

    public static SharedPreferences getBsSharedPreferences(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        return sp;
    }
    public static long getUnchargeLeftTime(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        long unchargerLeftTime = sp.getLong("unchargerLeftTime", 0);
        return unchargerLeftTime;
    }

    public static void setUnchargeLeftTime(Context context, long unchargerLeftTime) {// leftTime
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("unchargerLeftTime", unchargerLeftTime);
        editor.commit();
    }

    public static long getSpUnchargeCurTime(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        long spUnchargeCurTime = sp.getLong("spUnchargeCurTime", 0);
        return spUnchargeCurTime;
    }

    public static void setSpUnchargeCurTime(Context context, long spUnchargeCurTime) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("spUnchargeCurTime", spUnchargeCurTime);
        editor.commit();
    }





    public static double getOnClickAppOffsetTime(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        double appClickOffsetTime = Double.valueOf(sp.getString("onclickAppOffsetTime", "0"));
        return appClickOffsetTime;
    }

    public static void setOnClickAppOffsetTime(Context context, String onclickAppOffsetTime) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("onclickAppOffsetTime", onclickAppOffsetTime);
        editor.commit();
    }

    public static double getOnClickAppOffsetLevel(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        double appClickOffsetLevel = Double.valueOf(sp.getString("onclickAppOffsetLevel", "0"));
        return appClickOffsetLevel;
    }

    public static void setOnClickAppOffsetLevel(Context context, String onclickAppOffsetLevel) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("onclickAppOffsetLevel", onclickAppOffsetLevel);
        editor.commit();
    }


    public static long getOnClickAppTime(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        long appClickTime = sp.getLong("onclickAppTime", 0);
        return appClickTime;
    }

    public static void setOnClickAppTime(Context context, long onclickAppTime) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("onclickAppTime", onclickAppTime);
        editor.commit();
    }

    public static String getBatteryInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        String baterryValue = sp.getString("baterry", "");
        return baterryValue;
    }

    public static void setBatteryInfo(Context context, String batteryInfo) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("baterry", batteryInfo);
        editor.commit();
    }

    public static String getValueforKey(Context context, String batteryKey) {
        String batteryInfo = getBatteryInfo(context);
        String[] values = batteryInfo.split(":");
        if (values != null && values.length > 0) {
            if ("status".equalsIgnoreCase(batteryKey)) {
                return values[0];
            } else if ("level".equals(batteryKey)) {
                return values[1];
            } else if ("time".equals(batteryKey)) {
                return values[2];
            }
        }
        return "error";
    }

    public static void setValueByKey(Context context, String batteryValue, String batteryKey) {
        String batteryInfo = getBatteryInfo(context);
        String[] values = batteryInfo.split(":");
        if ("status".equalsIgnoreCase(batteryKey)) {
            values[0] = batteryValue;
        } else if ("level".equals(batteryKey)) {
            values[1] = batteryValue;
        } else if ("time".equals(batteryKey)) {
            values[2] = batteryValue;
        }
        batteryInfo = values[0] + ":" + values[1] + ":" + values[2];
        setBatteryInfo(context, batteryInfo);
    }

    public static String getBatteryChargeRateInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        String chargeRateInfo = sp.getString("baterry_charge_rate_info", "");
        return chargeRateInfo;
    }

    public static void setBatteryChargeRateInfo(Context context, String chargeRateInfo) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("baterry_charge_rate_info", chargeRateInfo);
        editor.commit();
    }

    public static float getBatteryChargeRate(Context context) {
        String chargeRateInfo = getBatteryChargeRateInfo(context);
        float chargeRate = 0;
        if (chargeRateInfo.contains(":")) {
            String[] values = chargeRateInfo.split(":");
            if (values != null && values.length > 0) {
                chargeRate = Float.valueOf(values[0]);
            }
        }

        return chargeRate;
    }

    public static long getBatteryChargeRateTime(Context context) {
        String chargeRateInfo = getBatteryChargeRateInfo(context);
        long chargeRateTime = 0;
        if (chargeRateInfo.contains(":")) {
            String[] values = chargeRateInfo.split(":");
            if (values != null && values.length > 0) {
                chargeRateTime = Long.valueOf(values[1]);
            }
        }
        return chargeRateTime;
    }

    public static void setBatteryChargeRate(Context context, float chargeRate) {

        String batteryChargeRateInfo = getBatteryChargeRateInfo(context);
        String[] values = batteryChargeRateInfo.split(":");
        if (values != null && values.length > 0) {
            values[0] = chargeRate + "";
        }
        batteryChargeRateInfo = values[0] + ":" + values[1];
        setBatteryChargeRateInfo(context, batteryChargeRateInfo);

    }

    public static void setBatteryChargeRateTime(Context context, long chargeRateTime) {

        String batteryChargeRateInfo = getBatteryChargeRateInfo(context);
        String[] values = batteryChargeRateInfo.split(":");
        if (values != null && values.length > 0) {
            values[1] = chargeRateTime + "";
        }
        batteryChargeRateInfo = values[0] + ":" + values[1];
        setBatteryChargeRateInfo(context, batteryChargeRateInfo);

    }


    public static String getBatteryUseRateInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        String chargeRateInfo = sp.getString("baterry_use_rate_info", "");
        return chargeRateInfo;
    }

    public static void setBatteryUseRateInfo(Context context, String chargeRateInfo) {
        SharedPreferences sp = context.getSharedPreferences("bs_baterry", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("baterry_use_rate_info", chargeRateInfo);
        editor.commit();
    }

    public static float getBatteryUseRate(Context context) {
        String useRateInfo = getBatteryUseRateInfo(context);
        float useRate = 0;
        if (useRateInfo.contains(":")) {
            String[] values = useRateInfo.split(":");

            if (values != null && values.length > 0) {
                useRate = Float.valueOf(values[0]);
            }
        }

        return useRate;
    }

    public static long getBatteryUseRateTime(Context context) {
        String useRateInfo = getBatteryUseRateInfo(context);
        long useRateTime = 0;
        if (useRateInfo.contains(":")) {
            String[] values = useRateInfo.split(":");

            if (values != null && values.length > 0) {
                useRateTime = Long.valueOf(values[1]);
            }
        }
        return useRateTime;
    }

    public static void setBatteryUseRate(Context context, float useRate) {

        String batteryUseRateInfo = getBatteryUseRateInfo(context);
        String[] values = batteryUseRateInfo.split(":");
        if (values != null && values.length > 0) {
            values[0] = useRate + "";
        }
        batteryUseRateInfo = values[0] + ":" + values[1];
        setBatteryChargeRateInfo(context, batteryUseRateInfo);

    }

    public static void setBatteryUseRateTime(Context context, long useRateTime) {

        String batteryUseRateInfo = getBatteryUseRateInfo(context);
        String[] values = batteryUseRateInfo.split(":");
        if (values != null && values.length > 0) {
            values[1] = useRateTime + "";
        }
        batteryUseRateInfo = values[0] + ":" + values[1];
        setBatteryChargeRateInfo(context, batteryUseRateInfo);

    }


    public static String getHourAndMinute(long millis) {
        millis = millis + System.currentTimeMillis();

        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Date date = new Date(millis);
        String result = format.format(date);
        return result;

    }

    public static String getDay(Context context, long millis) {
        long curMillis = System.currentTimeMillis();
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(millis + curMillis);
        int year = mCalendar.get(Calendar.YEAR);
        int mounth = mCalendar.get(Calendar.MONTH) + 1;
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);

        mCalendar.setTimeInMillis(curMillis);
        int curYear = mCalendar.get(Calendar.YEAR);
        int curMonth = mCalendar.get(Calendar.MONTH) + 1;
        int curDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        //Toast.makeText(context,"11111===="+year+": "+mounth+":"+day+"\n"+"22222===="+curYear+": "+curMonth+":"+curDay,Toast.LENGTH_LONG).show();
        String result = "";
        int days = 0;
        if (year == curYear) {
            if (mounth == curMonth) {
                days = day - curDay + 1;

            } else {
                if ((mounth - curMonth) == 1) {
                    days = (mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) - curDay + 1 ) + day;
                    /*Toast.makeText(context,"mounth===="+mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)+"\n days="+days
                            +"\n curDay="+curDay+", day="+day,Toast.LENGTH_LONG).show();*/
                } else {
                    BS_LOG.logE("error----month===="+curMonth+":"+mounth);
                }
            }

        }else {
            if ((year - curYear) == 1) {
                if (curMonth == 12 && mounth == 1) {
                    days = (mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) - curDay + 1 ) + day;
                    //Toast.makeText(context,"year===="+mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)+"\n days="+days,Toast.LENGTH_SHORT).show();
                } else {
                    BS_LOG.logE("error----year==="+curMonth+":"+mounth);
                }
            }
        }

        //Toast.makeText(context,days+"",Toast.LENGTH_SHORT).show();
        if (days == 1) {
            result = context.getResources().getString(R.string.bs_baterry_text3);
        } else if (days == 2) {
            result = context.getResources().getString(R.string.bs_tomorrow);
        } else if (days > 2){
            days  = Math.min(days,7);//最长待机时间7天
            result = days + " " + context.getString(R.string.bs_days);
        } else {
            BS_LOG.logE("error----days==="+days);
        }
        return result;
    }


    /**
     * Use format Battery Stats computeBatteryTimeRemaining and computeChargeTimeRemaining time.
     *
     * @param mCtx
     * @param timeValue 毫秒
     * @return String
     */
    public static String getStringByTimeMinitueN(Context mCtx, long timeValue) {
        DecimalFormat fnum = new DecimalFormat("##");
        long value = timeValue / 1000;
        String str = "";
        if ((value / 3600) > 0) {
            str += fnum.format(value / 3600) + mCtx.getString(R.string.bs_unit_hour);
        }
        str += fnum.format((value % 3600) / 60) + mCtx.getString(R.string.bs_mode_second);
        return str;
    }

   /* public static void setImmersiveBars(Activity activity, View topView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        int statusBarHeight = BS_Utils.getStatusBarHeight(activity);
        topView.setPadding(0, statusBarHeight, 0, 0);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        Log.e("zengrui","systemBar  resourceId   "+resourceId);
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        Log.e("zengrui","systemBar  result   "+result);
        return result;
    }*/
}
