package com.amz.ios.serverswitchcontrol;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.serverswitchcontrol.bean.response.SwitchResponseBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class SwitchResponseSubject {
    private static Vector<SwitchResponseObserver> sObservers = new Vector<>();
    private static SwitchResponseBean mSwitchResponseBean;
    private final static String LAST_RESPONSE = "last_response";

    public static void registerObserver(Context context, SwitchResponseObserver observer) {
        sObservers.add(observer);
        if (mSwitchResponseBean == null) {
            String json = PreferencesUtil.getString(context, LAST_RESPONSE);
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new GsonBuilder().serializeNulls().create();
                try {
                    mSwitchResponseBean = gson.fromJson(json, SwitchResponseBean.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (mSwitchResponseBean != null) {
            handleSwitchResponse(mSwitchResponseBean, observer);
        }
    }

    public static void unRegisterObserver(SwitchResponseObserver observer) {
        sObservers.remove(observer);
    }

    public static void handleSwitchResponse(Context context, SwitchResponseBean adSwitchResponseBean) {
        if (adSwitchResponseBean != null && adSwitchResponseBean.getData() != null) {
            mSwitchResponseBean = adSwitchResponseBean;
            Gson gson = new GsonBuilder().serializeNulls().create();
            String json = gson.toJson(adSwitchResponseBean);
            PreferencesUtil.putString(context, LAST_RESPONSE, json);

            for (SwitchResponseObserver observer : sObservers) {
                handleSwitchResponse(adSwitchResponseBean, observer);
            }
        }
    }

    private static void handleSwitchResponse(SwitchResponseBean adSwitchResponseBean, SwitchResponseObserver observer) {
        if (adSwitchResponseBean != null && adSwitchResponseBean.getData() != null) {
            HashMap<String, SwitchResponseBean.ServerResponseBean> map = new HashMap<>();
            List<String> list = observer.onRequestKey();
            if (list != null) {
                a:for (String s : list) {
                    for (SwitchResponseBean.ServerResponseBean serverResponseBean : adSwitchResponseBean.getData()) {
                        if (TextUtils.equals(serverResponseBean.getKey(), s)) {
                            map.put(s, serverResponseBean);
                            continue a;
                        }
                    }
                }
            }
            if (map.size() > 0) {
                observer.onSwitchCallback(map);
            }
        }
    }
}
