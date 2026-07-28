package com.amz.ios.launcher.leftpage.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.amz.ios.launcher.IShakeInterface;

public class WidgetBaseLayout extends ConstraintLayout implements IShakeInterface {

    public WidgetBaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetBaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, 0);
    }

    public void r() {

    }

    public void setAppSuggestionViewMargin() {

    }

    @Override
    public void beginOrAdjustHintAnimations() {

    }

    @Override
    public void beginOrAdjustHintAnimations(int i) {

    }

    @Override
    public void completeAndClearReorderHintAnimations() {

    }

    @Override
    public void joinAnimations(View view) {

    }
}
