package com.amz.ios.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.amz.ios.ioslite.common.Router;
import com.amz.ios.launcher.Launcher;


public class SearchWidgetView extends LinearLayout implements View.OnClickListener {

    private Launcher mLauncher;
    public SearchWidgetView(Context context) {
        this(context, null);
    }

    public SearchWidgetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchWidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(this);
        mLauncher = (Launcher) context;
    }

    @Override
    public void onClick(View v) {
        Router.startSearchActivity(getContext());
        mLauncher.setTempAppAnimationStyle(Launcher.APP_ANIM_FADE);

    }
}
