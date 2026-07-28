package com.amz.ios.ioslite.common.ad;

import java.util.List;

/**
 * Created by Administrator on 2017/3/23.
 */

public interface IOSListAdListener {

    void onError(IOSAdError error);

    void onAdLoaded(List<? extends IOSNAdResponse> responses);
}
