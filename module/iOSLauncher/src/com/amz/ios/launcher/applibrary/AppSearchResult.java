package com.amz.ios.launcher.applibrary;

import com.amz.ios.launcher.AppInfo;

public class AppSearchResult implements SearchResult {

    AppInfo mAppInfo;

    public AppSearchResult(AppInfo info){
        this.mAppInfo = info;
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public String getName() {
        if (mAppInfo != null) {
            return String.valueOf(mAppInfo.title);
        }
        return null;
    }

    @Override
    public AppInfo getAppInfo() {
        return mAppInfo;
    }
}
