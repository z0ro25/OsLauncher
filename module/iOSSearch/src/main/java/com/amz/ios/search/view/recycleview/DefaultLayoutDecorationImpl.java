package com.amz.ios.search.view.recycleview;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.amz.ioslauncher.iossearch.R;


/**
 * Author       : yizhihao
 * Create time  : 2016-11-15 上午10:54
 */
public class DefaultLayoutDecorationImpl implements LayoutDecoration {

    private static final String TAG = DefaultLayoutDecorationImpl.class.getSimpleName();

    private LayoutInflater mInflater;

    private int mParentLayoutId;

    public DefaultLayoutDecorationImpl(Context context) {
        this(context, R.layout.fmsearch_layout_default_wraper);
    }

    public DefaultLayoutDecorationImpl(Context context, int parentLayoutId) {
        mInflater = LayoutInflater.from(context);
        mParentLayoutId = parentLayoutId;
    }

    @Override
    public View wrap(View view) {
        if (view == null) {
            Log.e(TAG, ">>>>DefaultLayoutWraperImp#wrap : the wraped view is null! warp failure!");
            return view;
        }
        ViewGroup viewGroup = (ViewGroup) mInflater.inflate(mParentLayoutId, null);
        viewGroup.addView(view,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return viewGroup;
    }

    @Override
    public ViewGroup newWrapView() {
        return (ViewGroup) mInflater.inflate(mParentLayoutId, null);
    }
}
