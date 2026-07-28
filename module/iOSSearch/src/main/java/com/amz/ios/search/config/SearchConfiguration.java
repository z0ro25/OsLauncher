package com.amz.ios.search.config;

import android.net.Uri;
import androidx.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-21 下午9:40
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 * configuration and util for filter search info
 * iosSearch://droi.com/${type}$/$keyword$
 */
public class SearchConfiguration {
    private static final String TAG = SearchConfiguration.class.getSimpleName();
    private static final String SCHEME = "iosSearch://";
    private static final String HOST = "droi.com";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            {TYPE_VIDEO_SERACH, TYPE_PIC_SEARCH, TYPE_DEFAULT_SEARCH}
    )
    public @interface SearchType {
    }

    public static final int TYPE_VIDEO_SERACH = 0x1;
    public static final int TYPE_PIC_SEARCH = 0x2;
    public static final int TYPE_DEFAULT_SEARCH = 0x3;

    public static final String AUTHORITY = SCHEME.concat("://").concat(HOST);

    public static SearchConfiguration filterConfig(Uri data) {
        SearchConfiguration filterConfiguration = new SearchConfiguration();
        if (data == null || TextUtils.isEmpty(data.getPath())) return filterConfiguration;
        String[] infos = data.getPath().split("/");
        if (infos != null && infos.length > 1) {
            try {
                final int type = Integer.valueOf(infos[1]);
                filterConfiguration.type = type;
                if (infos.length > 2)
                    filterConfiguration.keyword = infos[2];
            } catch (Exception e) {
                Log.e(TAG, ">>>>>>SearchConfiguration#filterConfig : ");
            }
        }
        return filterConfiguration;
    }

    public boolean shouldSkipConfig() {
        return TextUtils.isEmpty(keyword);
    }

    public String keyword = "";

    public int type = 0;

    public boolean picSearchEnable = true;

    public boolean videoSearchEnable = true;

    public static final String KEY_SEARCH_ENGINE = "search_engine";

}