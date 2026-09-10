package com.truongnt.ios.launcher.applibrary;

import com.truongnt.ios.launcher.AppInfo;

public interface SearchResult {
    int getType();
    String getName();
    AppInfo getAppInfo();
}
