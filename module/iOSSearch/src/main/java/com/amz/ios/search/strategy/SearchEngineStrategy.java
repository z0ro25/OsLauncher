package com.amz.ios.search.strategy;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.util.PreferencesUtil;
import com.amz.ios.search.config.SearchConfiguration;
import com.amz.ios.search.provider.CheetBDSearchEngine;
import com.amz.ios.search.provider.DefaultSearchEngine;
import com.amz.ios.search.provider.ISearchEngine;

/**
 * Created by liaozhongjun on 2017/2/15.
 */

public class SearchEngineStrategy {
    private ISearchEngine mSearchEngine;

    public String getSearchUrl(int action, String keyWord) {
        return mSearchEngine.getSearchUrl(action, keyWord);
    }

    public static void updateStrategy(Context context, String url) {
        PreferencesUtil.putString(context, SearchConfiguration.KEY_SEARCH_ENGINE, url);
    }

    public static final SearchEngineStrategy newSearchEngineStragery(Context context) {
        SearchEngineStrategy searchEngineStrategy = new SearchEngineStrategy();
        String engine = PreferencesUtil.getString(context, SearchConfiguration.KEY_SEARCH_ENGINE, "");
        if (TextUtils.isEmpty(engine) || engine.equals(DefaultSearchEngine.DEFAULT_SEARCH)) {
            searchEngineStrategy.mSearchEngine = new DefaultSearchEngine(context, "");
        } else if (engine.equals(CheetBDSearchEngine.YAHOO_SEARCH)) {
            searchEngineStrategy.mSearchEngine = new CheetBDSearchEngine(context);
        } else if (engine.startsWith("http://") || engine.startsWith("https://")) {
            searchEngineStrategy.mSearchEngine = new DefaultSearchEngine(context, engine);
        } else {
            searchEngineStrategy.mSearchEngine = new DefaultSearchEngine(context, "");
        }

        return searchEngineStrategy;
    }

}
