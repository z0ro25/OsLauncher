package com.amz.ios.launcher.bounce;

public interface OnOverPullListener {
    void onOverPulledTop(float deltaDistance);
    void onOverPulledBottom(float deltaDistance);
    void onRelease();
}
