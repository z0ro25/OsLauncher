package com.amz.ios.themeclub.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.amz.ios.ioslite.common.util.DeviceInfoUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 15/06/17.
 */

public class RequestArgs {

    public static JSONObject getCommonJson(String sign) {
        JSONObject commonInfo = new JSONObject();
        try {
            commonInfo.put("language", "zh_CN");
            commonInfo.put("requestVersion", "1");
            commonInfo.put("sign", sign);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return commonInfo;
    }

    public static JSONObject getTagJson(Context context) {
        return getTagJson(context,true);
    }

    public static JSONObject getTagJson(Context context,boolean isWidth) {
        JSONObject tagInfo = new JSONObject();
        try {
            tagInfo.put("channel", DeviceInfoUtil.getChannel(context));
            tagInfo.put("customer", DeviceInfoUtil.getCustomer(context));
            tagInfo.put("brand", DeviceInfoUtil.getBrand());
            tagInfo.put("project", DeviceInfoUtil.getProject());
            tagInfo.put("cpu", android.os.Build.HARDWARE);
            tagInfo.put("osVersion", DeviceInfoUtil.getOsVersion());
            tagInfo.put("appVersion", AppUtils.getVersion(context));
            tagInfo.put("resolution", AppUtils.getLCD(context,isWidth));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tagInfo;
    }
}
