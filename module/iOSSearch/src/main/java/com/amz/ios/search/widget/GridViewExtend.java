package com.amz.ios.search.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-18 上午11:32
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class GridViewExtend extends GridView {

    public GridViewExtend(Context context) {
        super(context);
    }

    public GridViewExtend(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridViewExtend(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GridViewExtend(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
