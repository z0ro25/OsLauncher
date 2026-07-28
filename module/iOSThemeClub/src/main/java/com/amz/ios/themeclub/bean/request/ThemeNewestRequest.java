package com.amz.ios.themeclub.bean.request;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.NetWorkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ZhangMingZhe on 11/21/16.
 */

public class ThemeNewestRequest extends JSONObject{
    private Context mContext;
    private int mFrom;
    private int mTo;
    private int mID;
    private String mSource;
    private int mColumn = -1;
    private int mMessageCode = AppConfig.MessageCode.MESSAGECODE_THEME_NEWEST;

    public ThemeNewestRequest(Context mContext, int mFrom, int mTo, int mID, String mSource, int mColumn) {
        this.mContext = mContext;
        this.mFrom = mFrom;
        this.mTo = mTo;
        this.mID = mID;
        this.mSource = mSource;
        this.mColumn = mColumn;
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
            if(mID != -1){
                paramInfo.put("id",mID);
            }
            if(!TextUtils.isEmpty(mSource)){
                paramInfo.put("source",mSource);
            }
            if(mColumn!=-1){
                paramInfo.put("column",mColumn);
            }
            paramInfo.put("from",mFrom);
            obj.put("head",NetWorkUtils.buildHeadData(mMessageCode));
            obj.put("body",paramInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }
}
