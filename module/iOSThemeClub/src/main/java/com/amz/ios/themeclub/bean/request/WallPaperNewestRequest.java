package com.amz.ios.themeclub.bean.request;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.NetWorkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ZhangMingZhe on 11/17/16.
 */

public class WallPaperNewestRequest extends JSONObject {
    private Context mContext;
    private int mFrom;
    private int mTo;
    private int mId;
    private String mSource;
    private final int mMessageCode = AppConfig.MessageCode.MESSAGECODE_NEWEST_WALLPAPER;

    public WallPaperNewestRequest(Context mContext ,int mFrom, int mTo, int mId, String mSource) {
        this.mContext = mContext;
        this.mFrom = mFrom;
        this.mTo = mTo;
        this.mId = mId;
        this.mSource = mSource;
        Log.e("arguments","mFrom="+mFrom+"mto="+mTo+"msource="+mSource+"mid="+mId);
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
            paramInfo.put("common", NetWorkUtils.getCommonData(mContext,false));
            if(mId!=-1){
                paramInfo.put("id",mId);
            }
            if(!TextUtils.isEmpty(mSource)){
                paramInfo.put("source",mSource);
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
