package com.amz.ios.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class RecommandLayout extends LinearLayout {
    public RecommandLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public RecommandLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }
}
