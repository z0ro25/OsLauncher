package com.amz.ios.search.provider;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.search.config.SearchConfiguration;
import com.amz.ios.search.config.Urls;

/**
 * Created by Administrator on 2017/2/8.
 */

public class DefaultSearchEngine implements ISearchEngine {

    public static final String DEFAULT_SEARCH = "default";
    private String mSimpleUrl;

    public DefaultSearchEngine(Context context, String simpleUrl) {
        mSimpleUrl = simpleUrl;
    }

    @Override
    public String getSearchUrl(int action, String keyWord) {
        String url;

        if (action != SearchConfiguration.TYPE_DEFAULT_SEARCH) {
            StringBuilder urlBuilder = new StringBuilder(Urls.URL_GOOGLE_SEARCH);
            if (urlBuilder.length() > Urls.URL_GOOGLE_SEARCH.length()) {
                urlBuilder.substring(Urls.URL_GOOGLE_SEARCH.length() + 1);
            }

            if (action == SearchConfiguration.TYPE_VIDEO_SERACH) {
                urlBuilder.append("&tbm=vid&q=");
            } else {
                urlBuilder.append("&tbm=isch&q=");
            }
            urlBuilder.append(keyWord).append("&gws_rd=ssl");
            url = urlBuilder.toString();
        } else {
            if (!TextUtils.isEmpty(mSimpleUrl)) {
                url = mSimpleUrl + keyWord;
            } else {
                url = Urls.DEFAULT_SEARCH_ENGINE + keyWord;
            }
        }
        return url;
    }
}
