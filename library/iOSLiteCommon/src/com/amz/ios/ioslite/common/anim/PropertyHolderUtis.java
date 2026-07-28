package com.amz.ios.ioslite.common.anim;

import android.animation.PropertyValuesHolder;
import android.view.View;

public class PropertyHolderUtis {

    public static PropertyValuesHolder rotation(float... paramVarArgs)
    {
        return PropertyValuesHolder.ofFloat(View.ROTATION, paramVarArgs);
    }

    public static PropertyValuesHolder scaleX(float... paramVarArgs) {
        return PropertyValuesHolder.ofFloat(View.SCALE_X, paramVarArgs);
    }

    public static PropertyValuesHolder scaleY(float... paramVarArgs) {
        return PropertyValuesHolder.ofFloat(View.SCALE_Y, paramVarArgs);
    }

    public static PropertyValuesHolder translateX(float... paramVarArgs) {
        return PropertyValuesHolder.ofFloat(View.TRANSLATION_X, paramVarArgs);
    }

    public static PropertyValuesHolder translateY(float... paramVarArgs) {
        return PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, paramVarArgs);
    }

    public static PropertyValuesHolder alpha(float... paramVarArgs)
    {
        return PropertyValuesHolder.ofFloat(View.ALPHA, paramVarArgs);
    }
}
