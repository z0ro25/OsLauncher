
package com.amz.ios.launcher.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.launcher.AppFilter;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.InvariantDeviceProfile;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.compat.AlphabeticIndexCompat;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;

/**
 * Widgets data model that is used by the adapters of the widget views and controllers.
 * <p>
 * <p> The widgets and shortcuts are organized using package name as its index.
 */
public class WidgetsModel {

    private static final String TAG = "WidgetsModel";
    private static final boolean DEBUG = false;

    /* List of packages that is tracked by this model. */
    private ArrayList<PackageItemInfo> mPackageItemInfos = new ArrayList<>();
    public ArrayList<PackageItemInfo> mFilteredPackageInfo = new ArrayList<>();

    /* Map of widgets and shortcuts that are tracked per package. */
    private HashMap<PackageItemInfo, ArrayList<Object>> mSystemWidgetsList = new HashMap<>();
    public HashMap<PackageItemInfo, ArrayList<Object>> mFilteredWidgetList = new HashMap<>();

    private ArrayList<Object> mIOSWidgetsList = new ArrayList<Object>();

    private ArrayList<Object> mRawList;

    private final AppWidgetManagerCompat mAppWidgetMgr;
    private final WidgetsAndShortcutNameComparator mWidgetAndShortcutNameComparator;
    private final Comparator<ItemInfo> mAppNameComparator;
    private final IconCache mIconCache;
    private final AppFilter mAppFilter;
    private AlphabeticIndexCompat mIndexer;

    private String mIOSPkgName;
    private Context mContext;

    public WidgetsModel(Context context, IconCache iconCache, AppFilter appFilter) {
        mContext = context;
        mAppWidgetMgr = AppWidgetManagerCompat.getInstance(context);
        mWidgetAndShortcutNameComparator = new WidgetsAndShortcutNameComparator(context);
        mAppNameComparator = (new AppNameComparator(context)).getAppInfoComparator();
        mIconCache = iconCache;
        mAppFilter = appFilter;
        mIndexer = new AlphabeticIndexCompat(context);
        mIOSPkgName = context.getPackageName();
    }

    @SuppressWarnings("unchecked")
    private WidgetsModel(WidgetsModel model) {
        mAppWidgetMgr = model.mAppWidgetMgr;
        mPackageItemInfos = (ArrayList<PackageItemInfo>) model.mPackageItemInfos.clone();
        mSystemWidgetsList = (HashMap<PackageItemInfo, ArrayList<Object>>) model.mSystemWidgetsList.clone();
        mIOSWidgetsList = (ArrayList<Object>) model.mIOSWidgetsList.clone();
        mRawList = (ArrayList<Object>) model.mRawList.clone();
        mWidgetAndShortcutNameComparator = model.mWidgetAndShortcutNameComparator;
        mAppNameComparator = model.mAppNameComparator;
        mIconCache = model.mIconCache;
        mAppFilter = model.mAppFilter;
    }

    // Access methods that may be deleted if the private fields are made package-private.
    public int getPackageSize() {
        if (mPackageItemInfos == null) {
            return 0;
        }
        return mPackageItemInfos.size();
    }

    // Access methods that may be deleted if the private fields are made package-private.
    public PackageItemInfo getPackageItemInfo(int pos) {
        if (pos >= mPackageItemInfos.size() || pos < 0) {
            return null;
        }
        return mPackageItemInfos.get(pos);
    }

    public List<Object> getSortedWidgets(int pos) {
        return mSystemWidgetsList.get(mPackageItemInfos.get(pos));
    }

    public List<Object> getSortedWidgets(PackageItemInfo key) {
        return mSystemWidgetsList.get(key);
    }

    public List<PackageItemInfo> getPackageItemInfos() {
        return mPackageItemInfos;
    }

    public ArrayList<Object> getIOSWidgets() {
        return mIOSWidgetsList;
    }

    /**
     * Danh sách widget nổi bật hiển thị ở lưới 2 cột trên cùng khay Add Widget.
     * Curated đúng 5 widget cơ bản theo thiết kế, đúng thứ tự:
     * Calendar, Photos (Picture), Batteries, Weather, Clock. Match theo tên class
     * provider nên không phụ thuộc nhãn/locale. Widget nào chưa phát hiện được thì
     * bỏ qua (không chèn ô rỗng). Đây là 1 điểm sửa duy nhất.
     */
    public ArrayList<Object> getFeaturedWidgets() {
        final String[] ORDER = {
                "CalendarWidgetProvider",
                "PictureAppWidgetProvider",
                "BatteryWidgetProvider",
                "WeatherWidgetProvider",
                // Thẻ Clock nổi bật = World Clock ngang (kiểu #3), hiển thị rộng 2 cột.
                "WorldClockWidgetProvider",
        };
        ArrayList<Object> all = getIOSWidgets();
        ArrayList<Object> featured = new ArrayList<>();
        if (all == null) return featured;
        for (String simpleName : ORDER) {
            for (Object o : all) {
                if (!(o instanceof LauncherAppWidgetProviderInfo)) continue;
                ComponentName provider = ((LauncherAppWidgetProviderInfo) o).provider;
                if (provider != null && provider.getClassName().endsWith("." + simpleName)) {
                    featured.add(o);
                    break;
                }
            }
        }
        return featured;
    }

    /**
     * 4 size đồng hồ làm sẵn (tĩnh) để user thêm ngay, đúng thứ tự carousel iOS:
     * Digital, Analog đơn, World Clock ngang, World Clock lưới 2x2. Match theo tên
     * class provider nên không phụ thuộc nhãn/locale. Dùng cho carousel level-2 khi
     * bấm thẻ Clock ở lưới featured.
     */
    public ArrayList<Object> getClockVariants() {
        final String[] ORDER = {
                "ClockWidgetProvider",
                "AnalogClockWidgetProvider",
                "WorldClockWidgetProvider",
                "MiniAnalogClockWidgetProvider",
        };
        ArrayList<Object> all = getIOSWidgets();
        ArrayList<Object> variants = new ArrayList<>();
        if (all == null) return variants;
        for (String simpleName : ORDER) {
            for (Object o : all) {
                if (!(o instanceof LauncherAppWidgetProviderInfo)) continue;
                ComponentName provider = ((LauncherAppWidgetProviderInfo) o).provider;
                if (provider != null && provider.getClassName().endsWith("." + simpleName)) {
                    variants.add(o);
                    break;
                }
            }
        }
        return variants;
    }


    public ArrayList<Object> getRawList() {
        return mRawList;
    }

    public void setWidgetsAndShortcuts(ArrayList<Object> rawWidgetsShortcuts) {
        Utilities.assertWorkerThread();
        mRawList = rawWidgetsShortcuts;
        if (DEBUG) {
            Log.d(TAG, "addWidgetsAndShortcuts, widgetsShortcuts#=" + rawWidgetsShortcuts.size());
        }

        // Temporary list for {@link PackageItemInfos} to avoid having to go through
        // {@link mPackageItemInfos} to locate the key to be used for {@link #mSystemWidgetsList}
        HashMap<String, PackageItemInfo> tmpPackageItemInfos = new HashMap<>();

        // clear the lists.
        mSystemWidgetsList.clear();
        mIOSWidgetsList.clear();
        mPackageItemInfos.clear();
        mWidgetAndShortcutNameComparator.reset();

        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();

        PackageManager manager = LauncherAppState.getInstance().getContext().getPackageManager();

        // add and update.
        for (Object o : rawWidgetsShortcuts) {
            String packageName = "";
            UserHandleCompat userHandle = null;
            ComponentName componentName = null;
            if (o instanceof LauncherAppWidgetProviderInfo) {
                LauncherAppWidgetProviderInfo widgetInfo = (LauncherAppWidgetProviderInfo) o;

                // Ensure that all widgets we show can be added on a workspace of this size
                int minSpanX = Math.min(widgetInfo.spanX, widgetInfo.minSpanX);
                int minSpanY = Math.min(widgetInfo.spanY, widgetInfo.minSpanY);
                if (minSpanX <= idp.numColumns &&
                        minSpanY <= idp.numRows) {
                    componentName = widgetInfo.provider;
                    packageName = widgetInfo.provider.getPackageName();
                    userHandle = mAppWidgetMgr.getUser(widgetInfo);
                } else {
                    if (DEBUG) {
                        Log.d(TAG, String.format(
                                "Widget %s : (%d X %d) can't fit on this device",
                                widgetInfo.provider, minSpanX, minSpanY));
                    }
                    continue;
                }
            } else if (o instanceof ResolveInfo) {
                ResolveInfo resolveInfo = (ResolveInfo) o;
                componentName = new ComponentName(resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);
                packageName = resolveInfo.activityInfo.packageName;
                userHandle = UserHandleCompat.myUserHandle();
            }

            if (isInBlackList(mContext, packageName)) {
                continue;
            }

            if (componentName == null || userHandle == null) {
                Log.e(TAG, String.format("Widget cannot be set for %s.", o.getClass().toString()));
                continue;
            }
            if (mAppFilter != null && !mAppFilter.shouldShowApp(componentName)) {
                if (DEBUG) {
                    Log.d(TAG, String.format("%s is filtered and not added to the widget tray.",
                            packageName));
                }
                continue;
            }

            if (packageName.equals(mIOSPkgName)) {
                mIOSWidgetsList.add(o);
            } else {
                PackageItemInfo pInfo = tmpPackageItemInfos.get(packageName);
                ArrayList<Object> widgetsShortcutsList = mSystemWidgetsList.get(pInfo);
                if (widgetsShortcutsList != null) {
                    widgetsShortcutsList.add(o);
                } else {
                    widgetsShortcutsList = new ArrayList<>();
                    widgetsShortcutsList.add(o);
                    pInfo = new PackageItemInfo(packageName);
                    mIconCache.getTitleAndIconForApp(packageName, userHandle,
                            false /* userLowResIcon */, pInfo);

                    try {
                        Intent intent = manager.getLaunchIntentForPackage(packageName);
                        if (intent != null && intent.getComponent() != null) {
                            Bitmap bitmap = mIconCache.getThemeIconForComponent(intent.getComponent(), userHandle);
                            if (bitmap != null) {
                                pInfo.iconBitmap = bitmap;
                            }
                        }
                    } catch (Exception e) {
                    }

                    pInfo.titleSectionName = mIndexer.computeSectionName(pInfo.title);
                    mSystemWidgetsList.put(pInfo, widgetsShortcutsList);
                    tmpPackageItemInfos.put(packageName, pInfo);
                    mPackageItemInfos.add(pInfo);
                }
            }

        }

        // sort.

        /*
        for (PackageItemInfo p : mPackageItemInfos) {
            Collections.sort(mSystemWidgetsList.get(p), mWidgetAndShortcutNameComparator);
        }

         */


        try {
            synchronized (WidgetsModel.class){

                Collections.sort(mIOSWidgetsList, mWidgetAndShortcutNameComparator);
                Collections.sort(mPackageItemInfos, mAppNameComparator);

//                int size  = mPackageItemInfos.size();
//                for (int i = 0 ; i < size - 1 ; i++){
//                    PackageItemInfo p = mPackageItemInfos.get(i);
//                    if (p == null) return;
//                    Collections.sort(mSystemWidgetsList.get(p),mWidgetAndShortcutNameComparator);
//                }

                for (PackageItemInfo pack : mPackageItemInfos) {
                    if (pack == null) return;
                    Collections.sort(mSystemWidgetsList.get(pack),mWidgetAndShortcutNameComparator);
                }
            }
        }
        catch (ConcurrentModificationException e){

        }


    }

    /**
     * Create a snapshot of the widgets model.
     * <p>
     * Usage case: view binding without being modified from package updates.
     */
    @Override
    public WidgetsModel clone() {
        return new WidgetsModel(this);
    }

    private static String[] mBlackNames;

    private boolean isInBlackList(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            if (mBlackNames == null) {
                mBlackNames = Partner.getStringArray(context, Partner.DEF_WIDGET_BLACK_LIST);
            }

            if (mBlackNames == null) {
                return false;
            }

            for (int i = 0; i < mBlackNames.length; i++) {
                if (packageName.equals(mBlackNames[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setFilterNull(){
        mFilteredPackageInfo.clear();
        mFilteredPackageInfo.addAll(mPackageItemInfos);
        mFilteredWidgetList.clear();
        mFilteredWidgetList.putAll(mSystemWidgetsList);
    }

}