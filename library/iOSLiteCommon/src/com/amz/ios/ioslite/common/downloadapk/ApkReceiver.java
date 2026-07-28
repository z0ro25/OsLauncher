package com.amz.ios.ioslite.common.downloadapk;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.util.PreferencesUtil;

import java.io.File;

/**
 * Created by ubuntu on 18/08/17.
 */

public class ApkReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            long downLoadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            String path = PreferencesUtil.getString(context,String.valueOf(downLoadId));
            String packageName = PreferencesUtil.getString(context,path);
            if (ConstantConfig.downLoadState.containsKey(packageName)) {
                String serviceString = Context.DOWNLOAD_SERVICE;
                DownloadManager dManager = (DownloadManager) context.getSystemService(serviceString);
                Uri downloadFileUri = dManager.getUriForDownloadedFile(downLoadId);
                if (downloadFileUri != null) {
                    installNormal(context, downloadFileUri, path);
                    AnalyticsDelegate.onEvent(context, UMEventConstants.CN_APPS_DOWNLOAD, packageName);
                }
                ConstantConfig.downLoadState.put(packageName, ConstantConfig.DownLoadState.STOP);
            }
        }
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if(ConstantConfig.downLoadState.containsKey(packageName)){
                AnalyticsDelegate.onEvent(context, UMEventConstants.CN_APPS_INSTALL, packageName);
                ConstantConfig.downLoadState.remove(packageName);
            }
        }
    }

    private static void installNormal(Context context,Uri uri,String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }else {
            File file = new File(Environment.getExternalStorageDirectory(),path);
            path = file.getAbsolutePath();
            uri = Uri.parse("file://" + path);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
