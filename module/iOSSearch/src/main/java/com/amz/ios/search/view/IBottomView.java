package com.amz.ios.search.view;

import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.SearchActivity;
import com.amz.ios.search.config.SearchConfiguration;
import com.amz.ioslauncher.iossearch.R;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-22 下午5:18
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public interface IBottomView {

    /**
     * when {@link SearchActivity#doAutoSearch(String)} called
     */
    void showBottom();

    /**
     * when {@link SearchActivity#resetToPreloadState()} called
     */
    void hideBottom();

    void setOnClickListener(View.OnClickListener l);

    class BottomViewProxy implements IBottomView {

        private CustomTextView mSearchWebSiteView;
        private CustomTextView mSearchPicView;
        private CustomTextView mSearchVideoView;

        private ViewStub mBottomContainer;

        private View.OnClickListener l;
        private SearchConfiguration mConfiguration;

        public BottomViewProxy(ViewStub bottomContainer, SearchConfiguration configuration) {
            mBottomContainer = bottomContainer;
            this.mConfiguration = configuration;
        }

        private boolean isInflatered = false;

        private void checkInflate() {
            if (isInflatered) return;
            isInflatered = true;
            final View infater = mBottomContainer.inflate();
            mSearchPicView = (CustomTextView) infater.findViewById(R.id.btn_search_pic);
            mSearchWebSiteView = (CustomTextView) infater.findViewById(R.id.btn_search_websit);
            mSearchVideoView = (CustomTextView) infater.findViewById(R.id.btn_search_video);
            if (mConfiguration != null) {
                mSearchPicView.setVisibility(mConfiguration.picSearchEnable ? View.VISIBLE : View.GONE);
                mSearchVideoView.setVisibility(mConfiguration.videoSearchEnable ? View.VISIBLE : View.GONE);
            }
            mSearchPicView.setOnClickListener(l);
            mSearchWebSiteView.setOnClickListener(l);
            mSearchVideoView.setOnClickListener(l);
        }

        @Override
        public void showBottom() {
            checkInflate();
            mBottomContainer.setVisibility(View.VISIBLE);
        }

        @Override
        public void hideBottom() {
            mBottomContainer.setVisibility(View.GONE);
        }

        @Override
        public void setOnClickListener(View.OnClickListener l) {
            this.l = l;
        }
    }
}
