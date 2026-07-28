package com.amz.ios.launcher.applibrary;

import com.amz.ios.launcher.AppInfo;

import java.util.ArrayList;

public class AppCategory {

    public String mCategoryName;
    public ArrayList<AppInfo> mApps;

    public AppCategory(String category, ArrayList<AppInfo> apps) {
        this.mCategoryName = category;
        this.mApps = apps;
    }
}
