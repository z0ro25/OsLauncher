package com.amz.ios.search.strategy;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.search.utils.IntentUtils;

/**
 * Created by liaozhongjun on 2017/2/10.
 */

public class DroiClickStrategy {

    private Context context;
    private int action;
    private String url;
    private String packageName;
    private String apkId;

    private DroiClickStrategy() {

    }

    public void performClickStrategy() {
        if (action == 1) {
            IntentUtils.openUrl(context, url);
        } else if (action == 2) {
            if (!IntentUtils.toApp(context, packageName, "")) {
                if (TextUtils.isEmpty(apkId)) {
                    IntentUtils.downloadApp(context, url);
                } else {
                    IntentUtils.toDroiAppDetail(context, apkId, packageName);
                }
            }
        }
    }

    public static DroiClickStrategy newDroiClickStrategy(Context context, int action, String url, String packageName, String apkId) {
        DroiClickStrategy droiClickStrategy = new DroiClickStrategy();
        droiClickStrategy.action = action;
        droiClickStrategy.context = context;
        droiClickStrategy.url = url;
        droiClickStrategy.packageName = packageName;
        droiClickStrategy.apkId = apkId;
        return droiClickStrategy;
    }

}
