package com.amz.newspage.newssource;


import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;


/**
 * Author       : yizhihao
 * Create time  : 2016-10-27 上午11:59
 * mutil load
 * whichever ad loaded will start use and stop other ad load.
 * only all ad load faulure cause logic failure.
 */
public class ThirdPartAdManager {

    private static final String TAG = ThirdPartAdManager.class.getSimpleName();
    private Context mContext;

    public static void initNewsSDKAndAd(Context context) {
        Log.e(TAG, ">>>>>>30_initNewsSDKAndAd : start");
        initialDroiUpdate(context);
        checkConfigChange(context);
        Log.e(TAG, ">>>>>>30_initNewsSDKAndAd : over");
    }

    public static void checkConfigChange(Context context) {
//        NewsConfigration.setScenario(context);
    }

    public ThirdPartAdManager(@NonNull Context context) {
        mContext = context;
    }


    private static void initialDroiUpdate(Context context) {
        // Droi BaaS initialization
//        DroiObject.registerCustomClass(NewsActor.class);
//        DroiObject.registerCustomClass(AdsActor.class);
//        DroiObject.registerCustomClass(CloudCodeUtils.NewsDataRequest.class);
//        DroiObject.registerCustomClass(CloudCodeUtils.AdRequest.class);
//        DroiObject.registerCustomClass(CloudCodeUtils.NewsDataResponse.class);
//        DroiObject.registerCustomClass(CloudCodeUtils.AdResponse.class);
//        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(context)
//                .setDownsampleEnabled(true)
//                .build();
//        Fresco.initialize(context, config);
    }
}
