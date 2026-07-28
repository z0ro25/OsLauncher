package com.amz.ios.search.view.recycleview;

import android.view.View;
import android.view.ViewGroup;

/**
 * wrap a viewgroup for item view
 * if we want to change our outter viewgroup object
 * Author       : yizhihao
 * Create time  : 2016-11-15 上午10:50
 */
public interface LayoutDecoration {

    public View wrap(View view);

    public ViewGroup newWrapView();

}
