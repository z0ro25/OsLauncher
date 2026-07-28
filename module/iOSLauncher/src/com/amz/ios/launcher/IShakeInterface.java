package com.amz.ios.launcher;

import android.view.View;

public interface IShakeInterface {

    void beginOrAdjustHintAnimations();

    void beginOrAdjustHintAnimations(int i);

    void completeAndClearReorderHintAnimations();

    void joinAnimations(View view);
}
