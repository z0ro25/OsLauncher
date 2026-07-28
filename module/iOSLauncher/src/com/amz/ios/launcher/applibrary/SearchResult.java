package com.amz.ios.launcher.applibrary;

import com.amz.ios.launcher.AppInfo;

public interface SearchResult {
    int getType();
    String getName();
    AppInfo getAppInfo();
}
