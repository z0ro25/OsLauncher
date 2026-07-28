package com.amz.ios.themeclub.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amz.ios.themeclub.R;

/**
 * Created by lideqian on 16-12-6.
 */
public class PermissionUtils {

    public static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    public static final int READ_PHONE_STATE_CODE = 2;
    private static final String PACKAGE_URL_SCHEME = "package:";
    //获取读取手机的权限请求
    public static boolean getReadPhonePermission(final Activity activity){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)) {
                showMessageOKCancel(activity,activity.getResources().getString(R.string.themeclub_get_phone_permission),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE},
                                        READ_PHONE_STATE_CODE);
                            }
                        });
                return false;
            }
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE},
                    READ_PHONE_STATE_CODE);
            return false;
        }else {
            return true;
        }
    }

    //获取读取sdCard的权限请求
    public static boolean getSdPermission(final Activity activity){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessageOKCancel(activity,activity.getResources().getString(R.string.themeclub_get_sdcard_permission),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                            }
                        });
                return false;
            }
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            return false;
        }else {
            return true;
        }
    }


    private static void showMessageOKCancel(final Activity activity,String message, DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder builder =new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setPositiveButton(activity.getResources().getString(R.string.themeclub_ok), okListener)
                .setNegativeButton(activity.getResources().getString(R.string.themeclub_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }


}
