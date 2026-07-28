package com.amz.ios.serverswitchcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.serverswitchcontrol.Constants.SharedPreferencesConstants;

public class LocationSuccessfulReceiver extends BroadcastReceiver {
    private final String LOCATION_SUCCESSFUL_ACTION = "ios.intent.action.LOCATION.SUCCESSFUL";
    private final String CITY_NAME_TAG = "cityName";
    private final String PROVINCE_TAG = "province";


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(LOCATION_SUCCESSFUL_ACTION)) {
            final String cityName = intent.getStringExtra(CITY_NAME_TAG);
            final String province = intent.getStringExtra(PROVINCE_TAG);
            PreferencesUtil.putString(context, SharedPreferencesConstants.CITY_NAME_KEY, cityName);
            PreferencesUtil.putString(context, SharedPreferencesConstants.PROVINCE_KEY, province);
        }
    }
}
