package com.amz.ios.search.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;
import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-29 下午1:12
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class ListViewExtend extends ListView {
    public ListViewExtend(Context context) {
        this(context, null);
    }

    public ListViewExtend(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListViewExtend(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setDivider(getResources().getDrawable(R.drawable.fmsearch_dw_divider, getContext().getTheme()));
        } else {
            setDivider(getResources().getDrawable(R.drawable.fmsearch_dw_divider));
        }
        setVerticalScrollBarEnabled(false);
        setDividerHeight(DisplayUtil.dip2px(context, 1));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
