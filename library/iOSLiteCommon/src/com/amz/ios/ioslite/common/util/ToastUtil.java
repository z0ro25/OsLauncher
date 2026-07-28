package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastUtil {
    private static Handler mHandler;
    private static Toast mToast;

    public static void show(Context context, String msg) {
        ToastUtil.show(context, msg, 0);
    }

    public static void show(Context context, int ResId) {
        ToastUtil.show(context, context.getResources().getString(ResId), 0);
    }

    public static void show(Context context, int ResId,int len) {
        ToastUtil.show(context, context.getResources().getString(ResId), len);
    }

    public static void show(final Context context, final String msg, final int len) {
        if (ToastUtil.mHandler == null) {
            ToastUtil.mHandler = new Handler(Looper.getMainLooper());
        }
        ToastUtil.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ToastUtil.mToast == null) {
                    ToastUtil.mToast = Toast.makeText(context, msg, len);
                }else{
                    ToastUtil.mToast.setDuration(len);
                }
                ToastUtil.mToast.setText(msg);
                ToastUtil.mToast.show();
            }
        });
    }
}
