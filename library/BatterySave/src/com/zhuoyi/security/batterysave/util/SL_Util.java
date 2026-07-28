package com.zhuoyi.security.batterysave.util;

import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.ios.sc.common.db.softlock.SL_LocksoftDatabaseHelp;
import com.ios.sc.common.logs.SL_Log;
import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.soft.lock.SL_LockSoftInfo;

public class SL_Util {
    private static Collator sCollator = Collator.getInstance();

    public static class LockSoftInfoComparator implements Comparator<Object> {
        private HashMap<Object, String> mLabelCache;

        public LockSoftInfoComparator() {
            mLabelCache = new HashMap<Object, String>();
        }

        public final int compare(Object a, Object b) {
            String labelA, labelB;
            if (mLabelCache.containsKey(a)) {
                labelA = mLabelCache.get(a);
            } else {
                labelA = ((SL_LockSoftInfo) a).getName();
                mLabelCache.put(a, labelA);
            }
            if (mLabelCache.containsKey(b)) {
                labelB = mLabelCache.get(b);
            } else {
                labelB = ((SL_LockSoftInfo) b).getName();
                mLabelCache.put(b, labelB);
            }
            return sCollator.compare(labelA, labelB);
        }
    }

    public static class PackageInfoComparator implements Comparator<Object> {
        private HashMap<Object, String> mLabelCache;
        ContextWrapper mContextwrapper;
        public PackageInfoComparator(ContextWrapper contextwrapper) {
            mLabelCache = new HashMap<Object, String>();
            this.mContextwrapper = contextwrapper;
        }
        public final int compare(Object a, Object b) {
            String labelA, labelB;
            if (mLabelCache.containsKey(a)) {
                labelA = mLabelCache.get(a);
            } else {
                labelA = ((PackageInfo)a).applicationInfo.loadLabel(mContextwrapper.getPackageManager()).toString();
                mLabelCache.put(a, labelA);
            }
            if (mLabelCache.containsKey(b)) {
                labelB = mLabelCache.get(b);
            } else {
                labelB = ((PackageInfo)b).applicationInfo.loadLabel(mContextwrapper.getPackageManager()).toString();
                mLabelCache.put(b, labelB);
            }
            return sCollator.compare(labelA, labelB);
        }
    }
    
    public static void setEditText(EditText editText,String text) {
        String temp = editText.getText().toString();
        if(temp.length() < 6) {
            editText.setText(temp+text);
        }
    }

    public static void reInputEditText(EditText editText){

        editText.setText("");

    }
    //
    public static void backEditText(EditText editText, TextView tv){

        String temp = editText.getText().toString();
        if (temp.length() > 0 && temp.length()<17) {
            temp=temp.substring(0,temp.length()-1);
            editText.setText(temp);
            editText.setSelection(temp.length());
            if(temp.length() == 0){
                tv.setText(R.string.sl_enter_psw_16);
            }
        }
    }

    public static ArrayList<SL_LockSoftInfo> getLockList(Context context) {
        ArrayList<SL_LockSoftInfo> list = new ArrayList<SL_LockSoftInfo>();
        SL_LocksoftDatabaseHelp helper = new SL_LocksoftDatabaseHelp(context);
        SQLiteDatabase database = helper.getWritableDatabase();
        Cursor c = null;
        try{
            c = database.query(SL_LocksoftDatabaseHelp.Locksoft_table, SL_LocksoftDatabaseHelp.all, null,null, null, null, null);
            for(c.moveToFirst();!c.isAfterLast();c.moveToNext()){
                //Log.d("SL_Log","SL_LocksoftDatabaseHelp.PACKAGE_NAME="+SL_LocksoftDatabaseHelp.PACKAGE_NAME);
                list.add(new SL_LockSoftInfo(c.getString(c.getColumnIndex(SL_LocksoftDatabaseHelp.PACKAGE_NAME)),"true","",null));
            }
        }finally{
            if (c != null && !c.isClosed()) {
                c.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
        return list;
    }
    
    @TargetApi(21) public static String getTopPackageNameFor21(Context mContext) {
        String topPackageName = "";
        android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) mContext.getSystemService("usagestats");
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        long startTime = calendar.getTimeInMillis() - 60*1000;

        //CT_Log.logD(" Range start:" + dateFormat.format(startTime) );
        //CT_Log.logD(" Range   end:" + dateFormat.format(endTime));
        android.app.usage.UsageEvents uEvents = usm.queryEvents(startTime,endTime);
        
        while (uEvents.hasNextEvent()){
            android.app.usage.UsageEvents.Event e = new android.app.usage.UsageEvents.Event();
            uEvents.getNextEvent(e);
            if (e != null){
                 //CT_Log.logE(" Event: " + dateFormat.format(e.getTimeStamp()) + "::" +  e.getPackageName());
                 topPackageName = e.getPackageName();
            }
        }
        return topPackageName;
    }

    public static boolean getTimeInit(Context con) {
        SharedPreferences sp = con.getSharedPreferences("SL_SOFTLOCK", Context.MODE_PRIVATE);
        if (!sp.contains("sl_last_time")) {
            return true;
        } else {
            String data1 = getLastTime(con);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());
            Date lastTime = null;
            try {
                lastTime = formatter.parse(data1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long diff = curDate.getTime() - lastTime.getTime();
            long min = diff / (1000 * 60);
            SL_Log.logE("min = "+min);
            if (min >= 2) {
                return true;
            }
            return false;
        }
    }

    public static void setLastTime(Context con, String date) {
        SL_Log.logE("date = "+date);
        SharedPreferences sp = con.getSharedPreferences("SL_SOFTLOCK", Context.MODE_PRIVATE);
        SharedPreferences.Editor et = sp.edit();
        et.putString("sl_last_time", date);
        et.commit();
    }

    private static String getLastTime(Context con) {
        SharedPreferences sp = con.getSharedPreferences("SL_SOFTLOCK", Context.MODE_PRIVATE);
        return sp.getString("sl_last_time", "");
    }

    public static void setFloatState(Context con, boolean state) {
        SharedPreferences sp = con.getSharedPreferences("SL_SOFTLOCK", Context.MODE_PRIVATE);
        SharedPreferences.Editor et = sp.edit();
        et.putBoolean("sl_float_first", state);
        et.commit();
    }

    public static boolean getFloatState(Context con) {
        SharedPreferences sp = con.getSharedPreferences("SL_SOFTLOCK", Context.MODE_PRIVATE);
        return sp.getBoolean("sl_float_first", true);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager.getActiveNetworkInfo() != null) {
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (null != networkInfo) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }
}
