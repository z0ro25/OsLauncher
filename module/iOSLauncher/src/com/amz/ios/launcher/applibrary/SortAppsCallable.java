package com.amz.ios.launcher.applibrary;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class SortAppsCallable implements Callable<ArrayList<AppCategory>> {

    int type;
    ConstraintLayout constraintLayout;
    Object object;

    public SortAppsCallable(ConstraintLayout constraintLayout,Object object, int type){
        this.type = type;
        this.constraintLayout = constraintLayout;
        this.object = object;
    }

    @Override
    public ArrayList<AppCategory> call() {
        switch (type){
            case 0:
                AppsLibraryLayout layout = (AppsLibraryLayout) this.constraintLayout;
                ArrayList<AppInfo> arrayList =(ArrayList) object;

                ArrayList<AppInfo> gameApps = new ArrayList<>();
                ArrayList<AppInfo> audioApps = new ArrayList<>();
                ArrayList<AppInfo> videoApps = new ArrayList<>();
                ArrayList<AppInfo> photoApps = new ArrayList<>();
                ArrayList<AppInfo> socialApps = new ArrayList<>();
                ArrayList<AppInfo> newsApps = new ArrayList<>();
                ArrayList<AppInfo> mapsApps = new ArrayList<>();
                ArrayList<AppInfo> productApps = new ArrayList<>();
                ArrayList<AppInfo> otherApps = new ArrayList<>();
                ArrayList<AppInfo> recentApps = new ArrayList<>();

                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    AppInfo appInfo = (AppInfo) it.next();
                    if (appInfo != null && appInfo.mApplicationInfo != null) {
                        switch (layout.getAppType(appInfo)) {
                            case AppsLibraryLayout.CATEGORY_APPS_GAME:
                                if (!gameApps.contains(appInfo)) {
                                    gameApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_AUDIO:
                                if (!audioApps.contains(appInfo)) {
                                    audioApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_VIDEO:
                                if (!videoApps.contains(appInfo)) {
                                    videoApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_PHOTO:
                                if (!photoApps.contains(appInfo)) {
                                    photoApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_SOCIAL:
                                if (!socialApps.contains(appInfo)) {
                                    socialApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_NEWS:
                                if (!newsApps.contains(appInfo)) {
                                    newsApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_MAPS:
                                if (!mapsApps.contains(appInfo)) {
                                    mapsApps.add(appInfo);
                                }
                                break;
                            case AppsLibraryLayout.CATEGORY_APPS_PRODUCTIVITY:
                                if (!productApps.contains(appInfo)) {
                                    productApps.add(appInfo);
                                }
                                break;
                            default:
                                if (!otherApps.contains(appInfo)) {
                                    otherApps.add(appInfo);
                                }
                                break;
                        }
                    }
                }

                int size = arrayList.size();
                for (int i = 1; i <= 4; i++) {
                    int index = size - i;
                    if (index >= 0) {
                        recentApps.add(arrayList.get(index));
                    }
                }

                Collections.sort(recentApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.recent), recentApps));
                Collections.sort(gameApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.game), gameApps));
                Collections.sort(audioApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.audio), audioApps));
                Collections.sort(videoApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.video), videoApps));
                Collections.sort(photoApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.image), photoApps));
                Collections.sort(socialApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.social), socialApps));
                Collections.sort(newsApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.news), newsApps));
                Collections.sort(mapsApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.maps), mapsApps));
                Collections.sort(productApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.productivity), productApps));
                Collections.sort(otherApps, layout.mAppNameComparator.getAppInfoComparator());
                layout.mCategories.add(new AppCategory(layout.getContext().getResources().getString(R.string.other), otherApps));

                Collections.sort(arrayList,layout.mAppNameComparator.getAppInfoComparator());
                ArrayList<SearchResult> results = getSearchResult(arrayList);
                layout.mSearchResultAdapter = new SearchResultAdapter(results);
                layout.mSearchResultRV.setAdapter(layout.mSearchResultAdapter);
                layout.mSearchResultAdapter.notifyDataSetChanged();
                layout.mSearchResultRV.addItemDecoration(new SearchResultDecoration(layout.mSearchResultAdapter));
                return layout.mCategories;
                default:
        }
        return null;
    }

    public ArrayList<SearchResult> getSearchResult(ArrayList<AppInfo> arrayList) {
        ArrayList<SearchResult> searchResults = new ArrayList<>();
        Iterator<AppInfo> it = arrayList.iterator();
        String str = "";
        while (it.hasNext()) {
            AppInfo appInfo = it.next();
            CharSequence title = appInfo != null ? appInfo.title : null;
            if (title != null && title.length() > 0) {
                String substring = title.toString().toUpperCase().trim().substring(0, 1);
                if (!str.equals(substring)) {
                    searchResults.add(new NormalSearchResult(substring));
                    str = substring;
                }
                searchResults.add(new AppSearchResult(appInfo));
            }
        }
        return searchResults;
    }
}
