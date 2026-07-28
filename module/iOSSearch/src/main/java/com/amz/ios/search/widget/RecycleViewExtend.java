package com.amz.ios.search.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import com.amz.ios.search.config.MSCConfiguration;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-25 下午5:38
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class RecycleViewExtend extends RecyclerView {

    /**
     * max scroll distance
     */
    private static final int MAX_Y_OVERSCROLL_DISTANCE = 200;

    public RecycleViewExtend(Context context) {
        this(context, null);
    }

    public RecycleViewExtend(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecycleViewExtend(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private float mScrollRatio = 0;
    private int mMaxYOverscrollDistance;

    private void init() {
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final float density = metrics.density;

        mMaxYOverscrollDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);
        mScrollRatio = MSCConfiguration.OVER_SCROLL_PERCENT;
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        //This is where the magic happens, we have replaced the incoming maxOverScrollY with our own custom variable mMaxYOverscrollDistance;

        int newDeltaY = deltaY;
        int delta = (int) (deltaY * mScrollRatio);
        if (delta != 0) newDeltaY = delta;
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, mMaxYOverscrollDistance, isTouchEvent);
    }

}
