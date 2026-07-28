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
 * Created by server on 17-8-21.
 */
public class LockScreenDetailRequest extends JSONObject {
    private Context mContext;
    private int mId;

    public LockScreenDetailRequest(Context context, int id) {
        mContext = context;
        mId = id;
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
            paramInfo.put("id", mId);
            paramInfo.put("isSupport", 0);
            paramInfo.put("resourceVersion", lockscreenVersion);
            String str = mId + 0 + lockscreenVersion + AppConfig.API_KEY;
            String sign = MD5Util.encypt(str);
            paramInfo.put("common", RequestArgs.getCommonJson(sign));
            paramInfo.put("tag", RequestArgs.getTagJson(mContext));

            obj.put("head", NetWorkUtils.buildHeadData(AppConfig.MessageCode.MESSAGECODE_LOCKSCREEN_DETAIL));
            obj.put("body", paramInfo.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }
}
