package com.amz.ios.launcher.applibrary;

import com.amz.ios.launcher.AppInfo;

public class NormalSearchResult implements SearchResult {

    public String mName;

    public NormalSearchResult(String str){
        mName = str;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public AppInfo getAppInfo() {
        return null;
    }
}
