package com.amz.ios.launcher.searchview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class ExpandableHeightRecyclerView extends RecyclerView {
    public ExpandableHeightRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public final void onMeasure(int widthSpec, int heightSpec) {
        try {
            int makeMeasureSpec = View.MeasureSpec.makeMeasureSpec(536870911,MeasureSpec.AT_MOST);
            setMeasuredDimension(widthSpec, makeMeasureSpec);
            super.onMeasure(widthSpec, makeMeasureSpec);
        } catch (Throwable th) {
            th.getMessage();
        }
    }
}