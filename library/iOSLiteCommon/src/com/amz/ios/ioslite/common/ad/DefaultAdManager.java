package com.amz.ios.ioslite.common.ad;

import android.content.Context;
import androidx.annotation.Nullable;

public class DefaultAdManager extends IOSAdManager {
    public DefaultAdManager(Context context) {
    }

    @Override
    public void init() {
    }

    @Override
    public IOSNativeAd getNativeAd(int id) {
        return null;
    }

    @Nullable
    @Override
    public IOSAppAd getAppAd(int id) {
        return null;
    }

    @Override
    public IOSListAd getListAd(int id) {
        return null;
    }
}
