package com.amz.newspage.newssource;

import java.util.concurrent.atomic.AtomicReference;


/**
 * Author       : yizhihao
 * Create time  : 2016-11-10 上午11:27
 */
public class NewsUiHelper {

    private static final String TAG = NewsUiHelper.class.getSimpleName();

    private static final AtomicReference<NewsUiHelper> INSTANCE = new AtomicReference<NewsUiHelper>();

    private static String mAppId;

    /**
     * get news by this number
     */
    public static NewsUiHelper getInstance() {
        for (; ; ) {
            NewsUiHelper current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new NewsUiHelper();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

//    public static NewsFragmentFactory FragmentsFactoryInstance(Activity pContext, IInfoHubNewStyleSetter pInfoHubNewStyleSetter) throws IllegalArgumentException {
//        if (pContext == null) {
//            throw new IllegalArgumentException("context can't be null");
//        }
//        if (pInfoHubNewStyleSetter == null) {
//            pInfoHubNewStyleSetter = new IInfoHubNewStyleSetter.DefaultInfoHubNewStyleSetter(pContext);
//        }
//        if (mAppId == null) {
//            try {
//                ApplicationInfo app = pContext.getPackageManager().getApplicationInfo(pContext.getPackageName(), PackageManager.GET_META_DATA);
//                Bundle bundle = app.metaData;
//                mAppId = bundle.getString("com.droi.sdk.application_id");
//            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//        return new NewsFragmentFactory(
//                pContext.getApplicationContext(),
//                CacheUtils.getCategoryIndexList(pContext.getApplicationContext()),
//                BuildUtil.getIOSVersionCode(pContext),
//                mAppId,
//                pInfoHubNewStyleSetter.getNewsStyles());
//    }
}
