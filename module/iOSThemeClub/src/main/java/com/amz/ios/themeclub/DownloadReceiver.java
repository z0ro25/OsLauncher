package com.amz.ios.themeclub;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.PreferencesUtils;

/**
 * Created by ubuntu on 18/08/17.
 */

public class DownloadReceiver extends BroadcastReceiver{
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        DebugLog.w(TAG, "============onReceive");
        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            final long downLoadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DebugLog.w(TAG, "==============downLoadId:" + downLoadId);
            String path = PreferencesUtils.getString(context, downLoadId + "");
            AppUtils.AppInstall(path,context.getApplicationContext());
        }
    }
}
