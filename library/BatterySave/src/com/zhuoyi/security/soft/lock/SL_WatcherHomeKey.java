package com.zhuoyi.security.soft.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.ios.sc.common.logs.SL_Log;

public class SL_WatcherHomeKey {

    final String TAG = "WatcherHomeKey";
    private Context mContext;
    private IntentFilter mFilter;
    private OnHomePressedListener mListener;
    private InnerRecevier mRecevier;

    public interface OnHomePressedListener {
        void onHomePressed();
        void onHomeLongPressed();
    }

    public SL_WatcherHomeKey(Context context) {
        mContext = context;
        mFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //mFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //mFilter.addAction(Intent.ACTION_SCREEN_OFF);
        //mFilter.addAction(Intent.ACTION_SCREEN_ON);
    }

    public void setOnHomePressedListener(OnHomePressedListener listener) {
        mListener = listener;
        mRecevier = new InnerRecevier();
    }


    public void startWatch() {
        try{
            if (mRecevier != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mContext.registerReceiver(mRecevier, mFilter,Context.RECEIVER_EXPORTED);
                }else  mContext.registerReceiver(mRecevier, mFilter);
            }
        }catch(Exception err){
            Log.d(TAG, "startWatch():" +err.toString());
        }
    }

    public void stopWatch() {
        try{
            if (mRecevier != null) {
                mContext.unregisterReceiver(mRecevier);
            }
        }catch(Exception err){
            SL_Log.logII(TAG + ":" + err.getMessage());
        }
    }

    class InnerRecevier extends BroadcastReceiver {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            init(context,intent);
        }


        synchronized void init(Context context, Intent intent){
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null) {
                    Log.e(TAG, "action:" + action + ",reason:" + reason);
                    if (mListener != null) {
                        if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                            mListener.onHomePressed();
                        } else if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                            mListener.onHomeLongPressed();
                        }
                    }
                }
            }
        }

    };
}
