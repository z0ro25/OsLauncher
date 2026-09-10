package com.truongnt.ios.launcher.applibrary;

import com.truongnt.ios.launcher.AppInfo;

import java.util.ArrayList;

public class AppCategory {

    public String mCategoryName;
    public ArrayList<AppInfo> mApps;

    public AppCategory(String category, ArrayList<AppInfo> apps) {
        this.mCategoryName = category;
        this.mApps = apps;
    }
}
