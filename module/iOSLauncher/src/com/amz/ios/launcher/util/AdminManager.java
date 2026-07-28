package com.amz.ios.launcher.util;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import com.amz.ios.launcher.DeviceProfile;

/**
 * Created by 37 on 11/8/2019.
 */

public class AdminManager extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }
}
