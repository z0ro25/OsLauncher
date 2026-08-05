package com.amz.ios.ioslite.common.ad;

import android.app.Activity;
import android.content.Context;

/**
 * Quảng cáo toàn màn: interstitial (chèn khi mở app) và app-open (khi quay lại
 * desktop). Phân biệt loại cụ thể qua ad-id truyền vào {@link IOSAdManager}.
 *
 * Cùng phong cách với {@link IOSNativeAd}/{@link IOSListAd}: đây là lớp trừu
 * tượng thuộc tầng chung, không phụ thuộc SDK ad nào. Implementation thật (AdMob,
 * AppLovin...) sẽ được cung cấp ở module {@code :app} qua một lớp con
 * {@link IOSAdManager} rồi {@link IOSAdManager#setInstance(IOSAdManager)}.
 */
public abstract class IOSFullScreenAd {

    /** Bắt đầu tải quảng cáo. */
    public abstract void load(Context context);

    /** Đã tải xong và sẵn sàng hiển thị. */
    public abstract boolean isReady();

    /** Hiển thị nếu đã sẵn sàng. Không sẵn sàng thì bỏ qua. */
    public abstract void show(Activity activity);

    public abstract void setListener(IOSFullScreenAdListener listener);

    protected abstract int getIOSAdId();

    protected boolean shouldShowAd() {
        return IOSAdManager.shouldShowAd(getIOSAdId());
    }

    public void afterShowAd() {
        IOSAdManager.afterShowAd(getIOSAdId());
    }

    public abstract void destroy();
}
