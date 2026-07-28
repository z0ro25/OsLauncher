package com.amz.ios.search.view;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.amz.ios.search.DataFlowAdapter;
import com.amz.ios.search.view.recycleview.DroiItemDecoration;
import com.amz.ios.search.widget.ScrollViewExtend;
import com.amz.ios.search.view.recycleview.SlideInDownAnimator;
import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-28 下午7:54
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface ISearchContent {

    void hideContent();

    void showContent();

    void setOnScrollChangeLintener(ScrollViewExtend.OnScrollChangeLintener l);

    class SearchContentProxy implements ISearchContent {
        private Context mContext;

        private ScrollViewExtend mScroller;
        private RecyclerView mRecyclerView;
        private View mSearchContent;


        public SearchContentProxy(Context context, View searchContent, DataFlowAdapter dataFlowAdapter) {
            mScroller = (ScrollViewExtend) searchContent;

            mContext = context;
            mSearchContent = searchContent;
            mRecyclerView = (RecyclerView) searchContent.findViewById(R.id.rclv_search_preview_container);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            mRecyclerView.addItemDecoration(new DroiItemDecoration(context));

            SlideInDownAnimator animator = new SlideInDownAnimator();
            animator.setAddDuration(5000);
            mRecyclerView.setItemAnimator(animator);
            mRecyclerView.setAdapter(dataFlowAdapter);
        }

        @Override
        public void hideContent() {
            mSearchContent.setVisibility(View.GONE);
        }

        @Override
        public void showContent() {
            mSearchContent.setVisibility(View.VISIBLE);
        }

        @Override
        public void setOnScrollChangeLintener(ScrollViewExtend.OnScrollChangeLintener l) {
            mScroller.setOnScrollChangeLintener(l);
        }
    }
}


