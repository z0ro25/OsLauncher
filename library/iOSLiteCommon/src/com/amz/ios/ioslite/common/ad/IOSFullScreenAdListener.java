package com.amz.ios.ioslite.common.ad;

/**
 * Lắng nghe vòng đời quảng cáo toàn màn ({@link IOSFullScreenAd}).
 * Tái dùng {@link IOSAdError} như các listener khác trong package.
 */
public interface IOSFullScreenAdListener {

    void onLoaded();

    void onError(IOSAdError error);

    void onShown();

    void onClosed();
}
