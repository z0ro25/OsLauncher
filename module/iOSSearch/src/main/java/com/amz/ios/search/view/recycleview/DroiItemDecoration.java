package com.amz.ios.search.view.recycleview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * draw padding for recycleview item
 * <p>
 * top = mInsertsPadding/2
 * left = mInsertsPadding
 * bottom = mInsertsPadding/2
 * right = mInsertsPadding
 * ____________________________________________
 * |                    ^                       |
 * |          mInsertsPadding/2                 |
 * |                                            |
 * |                                            |
 * |                                            |
 * |                                            |
 * | <-mInsertsPadding        mInsertsPadding ->|
 * |                                            |
 * |                                            |
 * |                                            |
 * |          mInsertsPadding/2                 |
 * |                    v                       |
 * --------------------------------------------
 * <p>
 * Create time  : 2016-11-18 下午9:39
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class DroiItemDecoration extends RecyclerView.ItemDecoration {

    private int mInsertsPadding = 0;

    public DroiItemDecoration(Context context) {
        mInsertsPadding = (int) context.getResources().getDimension(R.dimen.fmsearch_dp_item_inner_common_padding);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(mInsertsPadding, mInsertsPadding / 2, mInsertsPadding, mInsertsPadding / 2);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
    }
}
