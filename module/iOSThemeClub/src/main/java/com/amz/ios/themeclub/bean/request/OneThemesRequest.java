package com.amz.ios.themeclub.bean.request;

import android.content.Context;

import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.NetWorkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ZhangMingZhe on 1/17/17.
 */

public class OneThemesRequest extends JSONObject{
    private int mMessageCode;
    private int mType;
    private int mId;
    private Context mContext;

    public OneThemesRequest(Context mContext, int mId, int mType) {
        this.mContext = mContext;
        this.mId = mId;
        this.mType = mType;
        mMessageCode = AppConfig.MessageCode.MESSAGECODE_CODE_ONE_THEME;
    }

    @Override
    public String toString() {
        JSONObject obj = null;
        JSONObject paramInfo = null;
        obj = new JSONObject();
        paramInfo = new JSONObject();
        try {
            paramInfo.put("id",mId);
            paramInfo.put("type",mType);
            paramInfo.put("common", NetWorkUtils.getCommonData(mContext,true));
            obj.put("head",NetWorkUtils.buildHeadData(mMessageCode));
            obj.put("body",paramInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }
}
