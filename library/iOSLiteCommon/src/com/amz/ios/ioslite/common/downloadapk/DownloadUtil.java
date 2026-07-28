package com.amz.ios.ioslite.common.downloadapk;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.R;
import com.amz.ios.ioslite.common.util.NetworkStateUtil;
import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.ioslite.common.util.ToastUtil;

import static com.amz.ios.ioslite.common.CommonSdk.getApplicationContext;

/**
 * Created by server on 17-11-21.
 */

public class DownloadUtil {
    public static void download(Context context,String downloadUrl,String packageName) {
        if(ConstantConfig.downLoadState.containsKey(packageName)&&ConstantConfig.downLoadState.get(packageName)== ConstantConfig.DownLoadState.LOADING){
            return;
        }else {
        }
        if (!TextUtils.isEmpty(downloadUrl) && downloadUrl.startsWith("http")) {
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(downloadUrl));
            request.allowScanningByMediaScanner();
            request.setTitle(packageName);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName);
            long refernece = manager.enqueue(request);
            ConstantConfig.downLoadState.put(packageName, ConstantConfig.DownLoadState.LOADING);
            PreferencesUtil.putString(getApplicationContext(), String.valueOf(refernece) , Environment.DIRECTORY_DOWNLOADS.concat(fileName));
            PreferencesUtil.putString(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS.concat(fileName) , packageName);
        }
    }

    public static void showFlowTips(Context context, final Runnable okRunnable,final Runnable cancelRunnable) {
        if(NetworkStateUtil.isMobileConnected(context)){
            new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getString(R.string.discovery_data_warn_tips))
                    .setPositiveButton(context.getResources().getText(R.string.discovery_sure), new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface diaLog, int which) {
                                    if(okRunnable!=null){
                                        okRunnable.run();
                                    }
                                    diaLog.dismiss();
                                }
                            })
                    .setNegativeButton(context.getResources().getText(R.string.discovery_cancel), new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface diaLog, int which) {
                                    if(cancelRunnable!=null){
                                        cancelRunnable.run();
                                    }
                                    diaLog.dismiss();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if(cancelRunnable!=null){
                                cancelRunnable.run();
                            }
                            dialog.dismiss();
                        }
                    })
                    .show();
        }else if(NetworkStateUtil.isWifiConnected(context)){
            okRunnable.run();
        }else {
            ToastUtil.show(context,R.string.network_error_tip);
        }
    }
}
