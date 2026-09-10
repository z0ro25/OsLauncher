package com.truongnt.ios.ioslite.common.ad;

import java.util.List;


public interface IOSAppAdListener {
    void onError(IOSAdError error);

    void onAdLoaded(List<? extends IOSNAdResponse> responses);
}

