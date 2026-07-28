package com.amz.ios.launcher.applibrary;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;

public class SearchResultDecoration extends RecyclerView.ItemDecoration {

    SearchResultAdapter mAdapter;
    int mMeasuredHeight;

    public SearchResultDecoration(SearchResultAdapter adapter){
        this.mAdapter = adapter;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onDraw(Canvas canvas, RecyclerView recyclerView) {
        View view;
        int i;
        View childAt = recyclerView.getChildAt(0);
        if (childAt == null) {
            return;
        }
        int N = recyclerView.getChildAdapterPosition(childAt);
        int i2 = -1;
        if (N == -1) {
            return;
        }
        SearchResultAdapter searchResultAdapter = this.mAdapter;
        while (true) {
            if (!searchResultAdapter.isNormalItem(N)) {
                N--;
                if (N < 0) {
                    break;
                }
            } else {
                i2 = N;
                break;
            }
        }

        if (i2 >= 0) {
            TextViewCustomFont inflate = (TextViewCustomFont) LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.item_header_apps_library_search_view, (ViewGroup) recyclerView, false);
            inflate.setText((searchResultAdapter.mSearchedResult.get(i2)).getName());
            inflate.measure(ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), 1073741824), recyclerView.getPaddingRight() + recyclerView.getPaddingLeft(), inflate.getLayoutParams().width), ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(recyclerView.getHeight(), 0), recyclerView.getPaddingBottom() + recyclerView.getPaddingTop(), inflate.getLayoutParams().height));
            int measuredWidth = inflate.getMeasuredWidth();
            int measuredHeight = inflate.getMeasuredHeight();
            this.mMeasuredHeight = measuredHeight;
            inflate.layout(0, 0, measuredWidth, measuredHeight);
            int bottom = inflate.getBottom();
            int paddingLeft = recyclerView.getPaddingLeft();
            int paddingTop = recyclerView.getPaddingTop();
            int i3 = 0;
            while (true) {
                if (i3 >= recyclerView.getChildCount()) {
                    view = null;
                    break;
                }
                view = recyclerView.getChildAt(i3);
                if (i2 != i3) {
                    if (searchResultAdapter.isNormalItem(recyclerView.getChildLayoutPosition(view))) {
                        i = this.mMeasuredHeight - view.getHeight();
                        if ((view.getTop() <= 0 ? view.getBottom() + i : view.getBottom()) <= bottom && view.getTop() <= bottom) {
                            break;
                        }
                        i3++;
                    }
                }
                i = 0;
                if ((view.getTop() <= 0 ? view.getBottom() + i : view.getBottom()) <= bottom) {
                }
                i3++;
            }
            if (view != null) {
                if (searchResultAdapter.isNormalItem(recyclerView.getChildLayoutPosition(view))) {
                    canvas.save();
                    canvas.translate(paddingLeft, (view.getTop() + paddingTop) - inflate.getHeight());
                    inflate.draw(canvas);
                    canvas.restore();
                    return;
                }
            }
            canvas.save();
            canvas.translate(paddingLeft, paddingTop);
            inflate.draw(canvas);
            canvas.restore();
        }
    }
}
