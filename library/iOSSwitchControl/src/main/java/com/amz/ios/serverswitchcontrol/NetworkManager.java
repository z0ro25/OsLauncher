package com.amz.ios.serverswitchcontrol;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.serverswitchcontrol.bean.response.RegistResponseBean;
import com.amz.ios.serverswitchcontrol.bean.response.SwitchResponseBean;

import okhttp3.Call;
import okhttp3.Response;

public class NetworkManager {
    private static final String TAG = "NetworkManager";

    private static final int WIFI_UPDATE_TIME_INTERVAL = 2 * 60 * 60 * 1000;
    private static final int MOBILE_UPDATE_TIME_INTERVAL = 6 * 60 * 60 * 1000;

    private static final String SWITCH_STATE_URL = "http://control.yy845.com/config/desktopInfo";
    private static final String SWITCH_STATE_URL_TEST = "http://192.168.0.52:2012/config/desktopInfo";
    private static final String REGIST_TO_SERVER_URL = "http://control.yy845.com/connectNet/register";
    private static final String REGIST_TO_SERVER_URL_TEST = "http://192.168.0.52:2012/connectNet/register";

    public static final String SWITCH_NEWS_PAGER = "newspage_switch_show";
    public static final String SWITCH_FOLDER_DISCOVERY = "folder_discovery_show";
    public static final String SWITCH_NOT_UNINSTALL_APP_LIST = "not_uninstall_app_list";
    public static final String DESKTOP_SHORTCUT_BLACKLIST = "desktop_shortcut_blacklist";
    private static NetConnectedReceiver sNetConnectedReceiver;

    public static void registerNetworkChangeReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        sNetConnectedReceiver = new NetConnectedReceiver();
        context.registerReceiver(sNetConnectedReceiver, intentFilter);
    }

    public static void unRegisterNetworkChangeReceiver(Context context) {
        if (sNetConnectedReceiver != null) {
            context.unregisterReceiver(sNetConnectedReceiver);
        }
    }


    public static void requestSwitchStateByPost(final Context context) {
        String postContent = SwitchTools.getRequestContent(context, null);
        SwitchTools.executeNetworkTask(getSwitchStateUrl(), postContent, SwitchResponseBean.class, new SwitchTools.ResultCallback<SwitchResponseBean>() {
            @Override
            public void onFailure(Call call, Exception e, String message) {
                Log.d(TAG, "requestSwitchStateByPost:onFailure:".concat(message));
            }

            @Override
            public void onResponse(Call call, Response response, SwitchResponseBean adSwitchResponseBean) {
                Log.d(TAG, "requestSwitchStateByPost:onResponse:".concat(adSwitchResponseBean.toString()));
                PreferencesUtil.putLong(context, Constants.SharedPreferencesConstants.REQUEST_FROM_SERVER_TIME, System.currentTimeMillis());
                SwitchResponseSubject.handleSwitchResponse(context.getApplicationContext(), adSwitchResponseBean);
            }
        });
    }

    public static void registToServerByPost(final Context context) {
        String postContent = SwitchTools.getRequestContent(context, CommonDeviceInfo.getIPAddress(context));
        SwitchTools.executeNetworkTask(getRegistToServerUrl(), postContent, RegistResponseBean.class, new SwitchTools.ResultCallback<RegistResponseBean>() {
            @Override
            public void onFailure(Call call, Exception e, String message) {
                Log.d(TAG, "registToServerByPost:onFailure:".concat(message));
            }

            @Override
            public void onResponse(Call call, Response response, RegistResponseBean registResponseBean) {
                if (registResponseBean == null) return;
                boolean isSuccess = registResponseBean.isSuccess();
                if (isSuccess) {
                    PreferencesUtil.putBoolean(context, Constants.SharedPreferencesConstants.REGIST_TO_SERVER_STATE, registResponseBean.isSuccess());
                }
                if (isSuccess) {
                    requestSwitchStateByPost(context);
                }
                Log.d(TAG, "registToServerByPost:onResponse:".concat(isSuccess + ""));
            }
        });
    }

    public static void handleNetConnect(Context context) {
        if (BuildUtil.isHWBuild()) {
            return;
        }
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo.State wifiState = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        NetworkInfo.State mobileState = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        final long updateTime = PreferencesUtil.getLong(context, Constants.SharedPreferencesConstants.REQUEST_FROM_SERVER_TIME, 0);
        if ((BuildUtil.DEBUG && (wifiState == NetworkInfo.State.CONNECTED || mobileState == NetworkInfo.State.CONNECTED))
                || (wifiState == NetworkInfo.State.CONNECTED && (System.currentTimeMillis() - updateTime) > WIFI_UPDATE_TIME_INTERVAL)
                || (mobileState == NetworkInfo.State.CONNECTED && (System.currentTimeMillis() - updateTime) > MOBILE_UPDATE_TIME_INTERVAL)) {
            if (PreferencesUtil.getBoolean(context, Constants.SharedPreferencesConstants.REGIST_TO_SERVER_STATE, false)) {
                requestSwitchStateByPost(context);
            } else {
                registToServerByPost(context);
            }
        }
    }

    public static String getSwitchStateUrl() {
        if (BuildUtil.DEBUG) {
            return SWITCH_STATE_URL_TEST;
        }
        return SWITCH_STATE_URL;
    }

    public static String getRegistToServerUrl() {
        if (BuildUtil.DEBUG) {
            return REGIST_TO_SERVER_URL_TEST;
        }
        return REGIST_TO_SERVER_URL;
    }
}
