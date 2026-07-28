package com.amz.ios.themeclub.bean.request;

import android.content.Context;

import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.NetWorkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ZhangMingZhe on 11/22/16.
 */

public class ThemeSelectionRequest extends JSONObject{
    private Context mContext;
    private int mMessageCode = AppConfig.MessageCode.MESSAGECODE_THEME_SELECTION;
    private int mFrom;
    private int mTo;

    public ThemeSelectionRequest(Context mContext, int mFrom, int mTo) {
        this.mContext = mContext;
        this.mFrom = mFrom;
        this.mTo = mTo;
    }

    public int getmFrom() {
        return mFrom;
    }

    @Override
    public String toString() {
        JSONObject obj = null;
        JSONObject paramInfo = null;
        obj = new JSONObject();
        paramInfo = new JSONObject();
        try {
            paramInfo.put("from",mFrom);
            paramInfo.put("to",mTo);
            paramInfo.put("common", NetWorkUtils.getCommonData(mContext,true));
            paramInfo.put("from",mFrom);
            obj.put("head",NetWorkUtils.buildHeadData(mMessageCode));
            obj.put("body",paramInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }
}
