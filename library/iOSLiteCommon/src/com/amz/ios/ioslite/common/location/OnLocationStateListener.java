package com.amz.ios.ioslite.common.location;

import android.content.Context;

/**
 * Created by server on 17-3-22.
 */
public interface OnLocationStateListener {
    //start location
    void startPositioning(Context context,String provider, boolean isLauncher);

    //stop location
    void stopPositioning();
}
