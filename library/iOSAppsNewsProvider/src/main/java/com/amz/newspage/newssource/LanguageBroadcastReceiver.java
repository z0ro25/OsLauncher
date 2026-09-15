package com.amz.newspage.newssource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-30 下午5:16
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class LanguageBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LanguageBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, ">>>>>>LanguageBroadcastReceiver#onReceive : local changed!");
        ThirdPartAdManager.checkConfigChange(context);
    }
}
