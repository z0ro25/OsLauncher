package com.amz.ios.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;


public class DefaultHomeButton extends FrameLayout {
    private ImageView mIcon;

    public DefaultHomeButton(Context context) {
        this(context, null);
    }

    public DefaultHomeButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefaultHomeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View contentView = inflate(context, R.layout.default_home_button, this);
        mIcon = (ImageView) contentView.findViewById(R.id.icon);
    }

    public void setSelected(boolean selected) {
        if (selected) {
            mIcon.setImageResource(R.drawable.default_home_selected);
        } else {
            mIcon.setImageResource(R.drawable.default_home_unselected);
        }
    }
}
