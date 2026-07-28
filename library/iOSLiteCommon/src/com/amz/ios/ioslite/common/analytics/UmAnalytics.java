package com.amz.ios.ioslite.common.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
//import com.umeng.analytics.MobclickAgent;

/**
 * 友盟数据统计
 */
public class UmAnalytics extends AbsAnalytics {
    private final String UMENG_APPKEY_HW = "599163191c5dd04d570004ee";
    private final String UMENG_APPKEY_CN = "57a94dcce0f55a2a47002d9f";

    @Override
    protected void initalize(Application application) {
//        String channel = DeviceInfoUtil.getChannel(application);
//        if (BuildUtil.isCNBuild()){
//            MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(application, UMENG_APPKEY_CN, channel));
//        }else {
//            MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(application, UMENG_APPKEY_HW, channel));
//        }
//        MobclickAgent.setDebugMode(false);
//        MobclickAgent.setScenarioType(application, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }

    @Override
    protected void onCreate(Activity activity) {

    }


    @Override
    protected void onResume(Activity activity) {
//        MobclickAgent.onResume(activity);
    }

    @Override
    protected void onPause(Activity activity) {
//        MobclickAgent.onPause(activity);
    }

    @Override
    protected void onEvent(Context context, String event, String type) {
//        MobclickAgent.onEvent(context.getApplicationContext(), event, type);
    }
}
