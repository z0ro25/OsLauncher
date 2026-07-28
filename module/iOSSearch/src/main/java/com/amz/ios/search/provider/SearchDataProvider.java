package com.amz.ios.search.provider;

import android.content.Context;
import android.util.Log;

import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.strategy.SearchEngineStrategy;
import com.amz.ios.search.view.IWebViewContent;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-19 上午11:00
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class SearchDataProvider extends DataFlowProvider {

    private static final String TAG = SearchDataProvider.class.getSimpleName();

    private static final boolean DEBUG = MSCConfiguration.DEBUG;

    private IWebViewContent mSearchWebView;

    SearchEngineStrategy searchEngineStrategy;

    public SearchDataProvider(Context context, IWebViewContent searchWebView) {
        super(context);
        mSearchWebView = searchWebView;
        searchEngineStrategy = SearchEngineStrategy.newSearchEngineStragery(context);
    }


    public void loadUrl(int action, String keyword) {

        final String url = searchEngineStrategy.getSearchUrl(action, keyword);
        Log.e(TAG, ">>>>>>SearchDataProvider#loadUrl : " + url);
        mSearchWebView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSearchWebView = null;
    }
}