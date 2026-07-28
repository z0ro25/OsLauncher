package com.amz.ios.themeclub.bean.request;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.util.encrypt.MD5Util;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.util.RequestArgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 14/06/17.
 */

public class LockScreenNewestRequest extends JSONObject {
    private Context mContext;
    private int mColumn;
    private int mResourceTypeId;
    private int mStartNum;
    private int mRequestNum;

    public LockScreenNewestRequest(Context context, int column, int resourceTypeId, int startNum, int requestNum) {
        mContext =context;
        mColumn = column;
        mStartNum = startNum;
        mRequestNum = requestNum;
        mResourceTypeId =resourceTypeId;
    }

    public int getmFrom() {
        return mStartNum;
    }

    @Override
    public String toString() {
        JSONObject obj = null;
        JSONObject paramInfo = null;

        obj = new JSONObject();
        paramInfo = new JSONObject();
        String lockscreenVersion = Partner.getString(mContext, Partner.FEATURE_LOCK_SCREEN_VERSION);
        if (TextUtils.isEmpty(lockscreenVersion)) {
            lockscreenVersion = "v700";
        }
        try {
            paramInfo.put("column",mColumn);
            paramInfo.put("isSupport",0);
            paramInfo.put("resourceVersion", lockscreenVersion);
            paramInfo.put("from",mStartNum);
            paramInfo.put("to",mRequestNum);
            if(mResourceTypeId!=-1){
                paramInfo.put("resourceTypeId",mResourceTypeId);
            }
            String str = mColumn + 0 + lockscreenVersion + AppConfig.API_KEY;
            String sign =  MD5Util.encypt(str);
            paramInfo.put("common", RequestArgs.getCommonJson(sign));
            paramInfo.put("tag", RequestArgs.getTagJson(mContext));

            obj.put("head", NetWorkUtils.buildHeadData(AppConfig.MessageCode.MESSAGECODE_LOCKSCREEN_NEWEST));
            obj.put("body",paramInfo.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }
}
