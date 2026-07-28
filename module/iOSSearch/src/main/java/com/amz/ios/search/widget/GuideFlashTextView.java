package com.amz.ios.search.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

import com.amz.ios.launcher.views.CustomTextView;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-03 下午5:08
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class GuideFlashTextView extends CustomTextView {

    public GuideFlashTextView(Context context) {
        super(context, null);
    }

    public GuideFlashTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GuideFlashTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWithContext(context);
    }

    private void initWithContext(Context context) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }


}
