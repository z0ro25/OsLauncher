package com.amz.ios.ioslite.common.ad;


public abstract class IOSAppAd {
    public abstract void load(int count);

    public abstract void setAppAdListener(IOSAppAdListener listener);

    protected abstract int getIOSAdId();

    protected boolean shouldShowAd() {
        return IOSAdManager.shouldShowAd(getIOSAdId());
    }

    public void afterShowAd() {
        IOSAdManager.afterShowAd(getIOSAdId());
    }

    public abstract void destory();
}

