package com.ios.sc.clean.task.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.ios.sc.common.db.cleantask.CT_SaveUtils;
import com.ios.sc.common.logs.CT_Log;

public class CT_Utils {

    public static long getTotalMemory() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");

            /*for (String num : arrayOfString) {
                C_I_Log.logI(str2, num);
            }*/

            initial_memory = Integer.valueOf(arrayOfString[1]).intValue();
            localBufferedReader.close();

        } catch (IOException e) {
        }
        return initial_memory / 1024;
    }

    @TargetApi(16)
    public static float getAvailMemory2(Context context) {

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        long totalMem = 0;
        long availMem = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            totalMem = mi.totalMem;
            availMem = mi.availMem;
        }else{
            totalMem = getTotalMemory();//MB
            availMem = mi.availMem/(1024*1024);//B to KB to MB
        }

        long num1= totalMem - availMem;
        float num2 = (float)num1 / totalMem;
        return num2;
    }


    public static long getAvailMemory3(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        long result = mi.availMem / 1024 /1024;
        return result;
    }
    
    public static String usedMemory(Context context) {
        String str = "65";
        int percent = 0;
        float availMemory = getAvailMemory2(context);
        try {
            percent = ((int) (availMemory * 100));
            if (percent > 0 && percent < 100) {
                str = String.valueOf(percent);
            }
        } catch (Exception e) {
            CT_Log.logE("formatAvailMemory err:" + e.toString() + ",availMemory = " + availMemory + ",percent = " + percent);
        }
        CT_Log.logE("str=" + str);
        return str;
    }

    public static void killBgProcess(Context context,String pkgnames) {
        List<String> list = new ArrayList<String>();
        if (!TextUtils.isEmpty(pkgnames)) {
            if(pkgnames.contains(",")){
                String[] temp = pkgnames.split(",");
                list =  Arrays.asList(temp);
            }else{
                list.add(pkgnames);
            }
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (int i = 0; i < list.size(); i++) {
                am.killBackgroundProcesses(list.get(i));
            }
        }
    }
    
    public static String killBgCleanTask (Context context, List<String> whitePkgName) {
        boolean lastTime = getTimeInit(context);
        CT_Log.logE("lastTime = "+lastTime);
        String cleanSize = "";
        if (lastTime) {
            long sizeBefore = getAvailMemory3(context);
            String propkgName = getKillList(context, whitePkgName); 
            
            if (!TextUtils.isEmpty(propkgName)) {
                killBgProcess(context, propkgName);
            }
            long sizeAfter = getAvailMemory3(context) - sizeBefore;
            if (sizeAfter > 0) {
                cleanSize = sizeAfter + "";    
            } else {
                cleanSize = "20";
            }
            CT_Log.logE("propkgName="+propkgName+" , cleanSize="+cleanSize);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());
            String str = formatter.format(curDate);
            setLastTime(context, str);
        } else {
            cleanSize = "0";
        }
        return cleanSize;
    }

    public static String getKillList(Context con, List<String> whitePkgName) {
        List<String> pkg = new ArrayList<String>();
        String pkgName = "";
        List<PackageInfo> mPkgList = null;
        String rPackName = getRunningApk();
        if (mPkgList == null) {
            CT_GetInstalledPackageList ct_insAppList = CT_GetInstalledPackageList.getInstance(con);
            mPkgList = ct_insAppList.getInstalledPackages();
        }
        
        String topN = CT_SaveUtils.getTopPackageName(con);
        pkg.add(topN);
        
        if (whitePkgName != null && whitePkgName.size() >0) {
            pkg.addAll(whitePkgName);
        }

        int length = mPkgList.size();
        for (int y = 0; y < length; y++) {
            String pac = mPkgList.get(y).packageName;
            if (rPackName.contains(pac)) {
                if (!pkg.contains(pac)) {
                    pkgName += pac +",";
                }
            }
            
        }
        if(pkgName.contains(",") && pkgName.length() > 0 ){
            pkgName = pkgName.substring(0, pkgName.length()-1);
        }
        return pkgName;
    }
    
    private static  String getRunningApk() {
        long startTime = System.currentTimeMillis();
        String pgkProcessAppMap = "";
        Set<String> rProcess = new HashSet<String>();
        String cmd = "ps";
        try {
            java.lang.Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            int index = 0;
            while ((line = in.readLine()) != null) {
                if (index > 0) {
                    StringTokenizer st = new StringTokenizer(line);
                    int lenght = st.countTokens();
                    if (lenght > 0) {
                        String uid = st.nextToken();//0 index USER
                        if (uid.startsWith("u0_") ) {
                            String processInfo = "";
                            for (int i = 0; i < (lenght - 1); i++) {
                                if (!(i == (lenght - 2))) {
                                    st.nextToken();
                                } else {
                                    processInfo = st.nextToken();
                                }
                            }
                            
                            if (!TextUtils.isEmpty(processInfo)) {
                                if (processInfo.contains(":")) {
                                    String a[] = processInfo.split(":");
                                    rProcess.add(a[0]);
                                } else {
                                    rProcess.add(processInfo);
                                }
                            }
                            
                        }
                    }
                }
                index++;
            }
        } catch (IOException e) {
            CT_Log.logE("getRunningApk err="+e.toString());
        }
        for(String pro : rProcess){
            pgkProcessAppMap += pro + ",";
        }
        if (pgkProcessAppMap.contains(",") && pgkProcessAppMap.length() > 0 ) {
            pgkProcessAppMap = pgkProcessAppMap.substring(0, pgkProcessAppMap.length()-1);
        }
        long endTime = System.currentTimeMillis();
        CT_Log.logE("do_exec pgkProcessAppMap = " + pgkProcessAppMap + "\t time = " + (endTime - startTime));
        return pgkProcessAppMap;
    }
    
    private static boolean getTimeInit(Context con) {
        SharedPreferences sp = con.getSharedPreferences("CT_CLEANTASK", Context.MODE_PRIVATE);
        if (!sp.contains("ct_last_time")) {
            return true;//Defalut value is 4 hours.
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
            CT_Log.logE("min = "+min);
            if (min >= 3) {
                return true;
            }
            return false;
        }
    }
    
    private static void setLastTime(Context con, String date) {
        CT_Log.logE("date = "+date);
        SharedPreferences sp = con.getSharedPreferences("CT_CLEANTASK", Context.MODE_PRIVATE);
        Editor et = sp.edit();
        et.putString("ct_last_time", date);
        et.commit();
    }
    
    private static String getLastTime(Context con) {
        SharedPreferences sp = con.getSharedPreferences("CT_CLEANTASK", Context.MODE_PRIVATE);
        return sp.getString("ct_last_time", "");
    }

}
