package com.amz.ios.ioslite.common.ad;


public abstract class IOSNativeAd {
    public abstract void load();

    public abstract void setNativeAdListener(IOSNativeAdListener listener);

    protected abstract int getIOSAdId();

    protected boolean shouldShowAd() {
        return IOSAdManager.shouldShowAd(getIOSAdId());
    }

    public void afterShowAd() {
        IOSAdManager.afterShowAd(getIOSAdId());
    }

    public abstract void destory();
}

