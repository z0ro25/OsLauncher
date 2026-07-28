package com.amz.ios.ioslite.common.anim;

import android.animation.ObjectAnimator;
import android.view.View;

/**
 * Created by server on 17-5-11.
 */
public class AnimUtils {


    public static ObjectAnimator ofViewAlpha(View target,
                                             float alpha) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(PropertyHolderUtis.alpha(alpha));
        return anim;
    }
}
