package com.zhuoyi.security.soft.lock;


import android.app.Application;
import android.content.Intent;

import com.ios.sc.common.utils.C_SC_Service_Communication;

public class SL_Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //startService(new Intent(getApplicationContext(), SL_LockSoftService.class));
        Intent myIntent = new Intent();
        C_SC_Service_Communication.startServiceForIntent(getApplicationContext(), myIntent);
    }
}