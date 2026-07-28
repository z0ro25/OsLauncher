package com.amz.ios.ioslite.common.location;

/**
 * Created by server on 17-7-10.
 */
public interface LocationState<DLocation> {
    void handleResult(int state,DLocation locationInfo);
}
