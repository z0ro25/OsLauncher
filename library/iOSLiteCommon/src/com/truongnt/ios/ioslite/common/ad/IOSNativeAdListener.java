package com.truongnt.ios.ioslite.common.ad;


public interface IOSNativeAdListener {

    void onError(IOSAdError error);

    void onAdLoaded(IOSNAdResponse response);

    void onClick();
}
