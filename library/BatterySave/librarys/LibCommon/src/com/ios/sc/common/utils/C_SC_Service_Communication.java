package com.ios.sc.common.utils;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.ios.sc.common.logs.SL_Log;


public class C_SC_Service_Communication {


    private final static String CLASSNAME    = "com.zhuoyi.security.soft.lock.SL_LockSoftService";
    //private final static String PACKAGENAME    = "com.ios.security.center";
    //private final static String SERVICE_ACTION = "com.zhuoyi.security.ACTION_SERVICE_COMMUNICATION";
    public  final static String KEY_OPERATION  = "operation";

    /*soft lock start*/
    public final static int SOFT_LOCK_LIST_UPDATE_DATA = 5100;
    public final static int SOFT_LOCK_STATA_UPDATE     = 5101;
    public final static int SOFT_LOCK_SWITCH     = 5102;
    /*soft lock end*/

    /*light start*/
    public final static String   KEY_EXTRA_AUTO_STATE        = "isFilter";
    public final static String   KEY_EXTRA_AUTO_PACKAGENAME  = "packageName";
    public final static int      LIGHT_SET_STATE_AUTO        = 5201;
    /*light end*/

    /*network manager start*/
    public final static int NWM_CLEAN_ALERTANDCLOSE_COUNT_OP     = 6100;
    public static final int NWM_SMS_SND_OP                       = 6101;
    public static final int NWM_SMS_REC_OP                       = 6102;
    public static final int NWM_AUTO_ADJUST_OP                   = 6103;
    public static final int NWM_CONNECT_STATE_CHANGE_OP          = 6104;
    public static final int NWM_START_MONTIOR_OP                 = 6105;
    public static final int NWM_OVER_SUIT_OP                     = 6106;
    public static final int NWM_ALERT_OP                         = 6107;
    public static final int NWM_AUTO_CLOSE_OP                    = 6108;
    /*network manager end*/
    
   /**
     * get service intent
     * @param operation
     * @return
     */
    public static Intent getServiceIntent (int operation){
        /*Intent myIntent = new Intent(SERVICE_ACTION);
        myIntent.setPackage(PACKAGENAME);*/
        Intent myIntent = new Intent();
        myIntent.putExtra(KEY_OPERATION, operation);
        return myIntent;
    }

    /**
     * start service for intent
     * @param mCtx
     * @param intent
     */
    @TargetApi(Build.VERSION_CODES.DONUT)
    public static void startServiceForIntent (Context mCtx , Intent intent){
        try{
            intent.setPackage(mCtx.getPackageName());
            intent.setComponent(new ComponentName(mCtx, CLASSNAME));
            mCtx.startService(intent);
        }catch(Exception err){
            SL_Log.logE("C_SC_Service_Communication err:" + err.toString());
        }
    }

}
