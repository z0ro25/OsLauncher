package com.amz.ios.ioslite.common.location;

/**
 * Created by server on 17-3-22.
 */
public interface OnGpsResulListener {
    //gps is not enable
    void onGpsFail();

    //gps is enable
    void onGpsSuccessful();
}
