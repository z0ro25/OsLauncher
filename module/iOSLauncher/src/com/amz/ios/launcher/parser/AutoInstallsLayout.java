/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.launcher.parser;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.launcher.provider.AppTypeProvider;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetInfo;
import com.amz.ios.launcher.LauncherProvider;
import com.amz.ios.launcher.LauncherProvider.SqlArguments;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.LauncherSettings.Favorites;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ShortcutInfo;
import com.amz.ios.launcher.Shortcuts;
import com.amz.ios.launcher.util.Thunk;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Layout parsing code for auto installs layout
 */
public class AutoInstallsLayout {
    private static final String TAG = "AutoInstalls";
    private static final boolean LOGD = false;

    // Object Tags
    private static final String TAG_FAVORITES = "favorites";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_FAVORITE = "favorite";
    private static final String TAG_AUTO_INSTALL = "autoinstall";
    private static final String TAG_FOLDER = "folder";
    private static final String TAG_APPWIDGET = "appwidget";
    private static final String TAG_RESOLVER = "resolver";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_VIRTUALAPP = "virtualapp";
    private static final String TAG_EXTRA = "extra";

    private static final String ATTR_CONTAINER = "container";
    private static final String ATTR_RANK = "rank";

    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_CLASS_NAME = "className";
    private static final String ATTR_APP_TYPE = "appType";
    private static final String ATTR_TITLE = "favoriteTitle";
    private static final String ATTR_FOLDER_CATEGORY = "folderCategoryType";
    private static final String ATTR_SCREEN = "screen";
    private static final String ATTR_X = "x";
    private static final String ATTR_Y = "y";
    private static final String ATTR_SPAN_X = "spanX";
    private static final String ATTR_SPAN_Y = "spanY";
    private static final String ATTR_ICON = "favoriteIcon";
    private static final String ATTR_URL = "url";

    // Attrs for "Include"
    private static final String ATTR_WORKSPACE = "workspace";

    // Style attrs -- "Extra"
    private static final String ATTR_KEY = "key";
    private static final String ATTR_VALUE = "value";

    private static final String HOTSEAT_CONTAINER_NAME =
            Favorites.containerToString(Favorites.CONTAINER_HOTSEAT);

    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE =
            "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";

    @Thunk
    final Context mContext;
    @Thunk
    final AppWidgetHost mAppWidgetHost;
    protected final LayoutParserCallback mCallback;

    protected final PackageManager mPackageManager;
    protected final Resources mSourceRes;
    protected final int mLayoutId;
    private final long[] mTemp = new long[2];
    @Thunk
    protected final ContentValues mValues;
    protected final String mRootTag;

    protected SQLiteDatabase mDb;
    protected List<ComponentName> mAllLauncherComps;
    protected AppTypeProvider mAppTypeProvider;

    public AutoInstallsLayout(Context context, AppWidgetHost appWidgetHost,
                              LayoutParserCallback callback, Resources res,
                              int layoutId) {
        this(context, appWidgetHost, callback, res, layoutId, TAG_FAVORITES);
    }

    public AutoInstallsLayout(Context context, AppWidgetHost appWidgetHost,
                              LayoutParserCallback callback, Resources res,
                              int layoutId, String rootTag) {
        mContext = context;
        mAppWidgetHost = appWidgetHost;
        mCallback = callback;

        mPackageManager = context.getPackageManager();
        mValues = new ContentValues();
        mRootTag = rootTag;

        mSourceRes = res;
        mLayoutId = layoutId;
        mAppTypeProvider = AppTypeProvider.getAppTypeProvider();
    }

    /**
     * Loads the layout in the db and returns the number of entries added on the desktop.
     */
    public int loadLayout(SQLiteDatabase db, ArrayList<Long> screenIds, List<ComponentName> componentNames) {
        mAllLauncherComps = componentNames;
        mDb = db;
        try {
            return parseLayout(mLayoutId, screenIds);
        } catch (Exception e) {
            Log.w(TAG, "Got exception parsing layout.", e);
            return -1;
        }
    }

    /**
     * Parses the layout and returns the number of elements added on the homescreen.
     */
    protected int parseLayout(int layoutId, ArrayList<Long> screenIds)
            throws XmlPullParserException, IOException {
        XmlResourceParser parser = mSourceRes.getXml(layoutId);
        beginDocument(parser, mRootTag);
        final int depth = parser.getDepth();
        int type;
        HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();
        int count = 0;

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            count += parseAndAddNode(parser, tagParserMap, screenIds);
        }
        return count;
    }

    /**
     * Parses container and screenId attribute from the current tag, and puts it in the out.
     *
     * @param out array of size 2.
     */
    protected void parseContainerAndScreen(XmlResourceParser parser, long[] out) {
        out[0] = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        out[1] = Long.parseLong(getAttributeValue(parser, ATTR_SCREEN));

        String strContainer = getAttributeValue(parser, ATTR_CONTAINER);
        if (strContainer != null) {
            out[0] = Long.valueOf(strContainer);
        }

    }

    /**
     * Parses the current node and returns the number of elements added.
     */
    protected int parseAndAddNode(
            XmlResourceParser parser,
            HashMap<String, TagParser> tagParserMap,
            ArrayList<Long> screenIds)
            throws XmlPullParserException, IOException {

        if (TAG_INCLUDE.equals(parser.getName())) {
            final int resId = getAttributeResourceValue(parser, ATTR_WORKSPACE, 0);
            if (resId != 0) {
                // recursively load some more favorites, why not?
                return parseLayout(resId, screenIds);
            } else {
                return 0;
            }
        }

        mValues.clear();
        parseContainerAndScreen(parser, mTemp);
        final long container = mTemp[0];
        final long screenId = mTemp[1];

        mValues.put(Favorites.CONTAINER, container);
        mValues.put(Favorites.SCREEN, screenId);
        mValues.put(Favorites.CELLX, getAttributeValue(parser, ATTR_X));
        mValues.put(Favorites.CELLY, getAttributeValue(parser, ATTR_Y));

        TagParser tagParser = tagParserMap.get(parser.getName());
        if (tagParser == null) {
            if (LOGD) Log.d(TAG, "Ignoring unknown element tag: " + parser.getName());
            return 0;
        }
        long newElementId = tagParser.parseAndAdd(parser);
        if (newElementId >= 0) {
            // Keep track of the set of screens which need to be added to the db.
            if (!screenIds.contains(screenId) &&
                    container == Favorites.CONTAINER_DESKTOP) {
                screenIds.add(screenId);
            }
            return 1;
        }
        return 0;
    }

    protected long addShortcut(String title, Intent intent, int type) {
        long id = mCallback.generateNewItemId();
        mValues.put(Favorites.INTENT, intent.toUri(0));
        mValues.put(Favorites.TITLE, title);
        mValues.put(Favorites.ITEM_TYPE, type);
        mValues.put(Favorites.SPANX, 1);
        mValues.put(Favorites.SPANY, 1);
        mValues.put(Favorites._ID, id);
        if (mCallback.insertAndCheck(mDb, mValues) < 0) {
            return -1;
        } else {
            return id;
        }
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_FAVORITE, new AppShortcutParser());
        parsers.put(TAG_APPWIDGET, new AppWidgetParser());
        parsers.put(TAG_SHORTCUT, new ShortcutParser());
        parsers.put(TAG_FOLDER, new FolderParser());
        parsers.put(TAG_RESOLVER, new ResolveParser());
        parsers.put(TAG_VIRTUALAPP, new VirtualAppParser());
        parsers.put(TAG_AUTO_INSTALL, new AutoInstallParser());
        return parsers;
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_FAVORITE, new AppShortcutParser());
        parsers.put(TAG_SHORTCUT, new ShortcutParser());
        parsers.put(TAG_VIRTUALAPP, new VirtualAppParser());
        parsers.put(TAG_AUTO_INSTALL, new AutoInstallParser());
        return parsers;
    }

    protected HashMap<String, TagParser> getResolveElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_FAVORITE, new AppShortcutParser());
        parsers.put(TAG_SHORTCUT, new ShortcutParser());
        parsers.put(TAG_VIRTUALAPP, new VirtualAppParser());
        parsers.put(TAG_FOLDER, new FolderParser());
        return parsers;
    }


    protected interface TagParser {
        /**
         * Parses the tag and adds to the db
         *
         * @return the id of the row added or -1;
         */
        long parseAndAdd(XmlResourceParser parser)
                throws XmlPullParserException, IOException;
    }

    /**
     * App shortcuts: required attributes packageName and className
     */
    protected class AppShortcutParser implements TagParser {

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);
            final String appType = getAttributeValue(parser, ATTR_APP_TYPE);

            ActivityInfo info;
            ComponentName cn = null;

            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
                cn = new ComponentName(packageName, className);
            } else if (!TextUtils.isEmpty(appType)) {
                cn = mAppTypeProvider.getComponentNameForAppType(appType);
            }

            if (cn == null || !mAllLauncherComps.contains(cn)) {
                return -1;
            } else {
                mAllLauncherComps.remove(cn);
            }

            try {

                try {
                    info = mPackageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    String[] packages = mPackageManager.currentToCanonicalPackageNames(
                            new String[]{packageName});
                    cn = new ComponentName(packages[0], className);
                    info = mPackageManager.getActivityInfo(cn, 0);
                }
                final Intent intent = new Intent(Intent.ACTION_MAIN, null)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(cn)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                return addShortcut(info.loadLabel(mPackageManager).toString(),
                        intent, Favorites.ITEM_TYPE_APPLICATION);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to add favorite: " + packageName + "/" + className, e);
            }

            return -1;
        }

    }

    /**
     * Virtual App Parser: this parser is one for hidden folder
     */
    protected class VirtualAppParser implements TagParser {

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            final String appType = getAttributeValue(parser, ATTR_APP_TYPE);

            try {
                Integer appId = Integer.valueOf(appType);
                final Intent intent = new Intent(Shortcuts.SHORTCUT_ACTION);
                intent.putExtra(Shortcuts.SHORTCUT_ID, appId);
                return addShortcut(Shortcuts.getShortcutTitle(appId, mContext), intent, Favorites.ITEM_TYPE_VIRTUAL_APP);
            } catch (Exception e) {
                Log.e(TAG, "Unable to add virtual app: ", e);
            }

            return -1;
        }

    }

    /**
     * AutoInstall: required attributes packageName and className
     */
    protected class AutoInstallParser implements TagParser {

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                if (LOGD) Log.d(TAG, "Skipping invalid <favorite> with no component");
                return -1;
            }

            mValues.put(Favorites.RESTORED, ShortcutInfo.FLAG_AUTOINTALL_ICON);
            final Intent intent = new Intent(Intent.ACTION_MAIN, null)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(new ComponentName(packageName, className))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            return addShortcut(mContext.getString(R.string.package_state_unknown), intent,
                    Favorites.ITEM_TYPE_APPLICATION);
        }
    }

    /**
     * Parses a web shortcut. Required attributes url, icon, title
     */
    protected class ShortcutParser implements TagParser {

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);

            ComponentName cn = null;
            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
                cn = new ComponentName(packageName, className);
            }

            if (cn == null) {
                return -1;
            }

            ActivityInfo info;
            try {
                try {
                    info = mPackageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    String[] packages = mPackageManager.currentToCanonicalPackageNames(
                            new String[]{packageName});
                    cn = new ComponentName(packages[0], className);
                    info = mPackageManager.getActivityInfo(cn, 0);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to add shortcut: " + packageName + "/" + className, e);
                return -1;
            }


            Intent intent = new Intent().setComponent(cn);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            return addShortcut(info.loadLabel(mPackageManager).toString(),
                    intent, Favorites.ITEM_TYPE_SHORTCUT);
        }
    }

    /**
     * Contains a list of <favorite> nodes, and accepts the first successfully parsed node.
     */
    protected class ResolveParser implements TagParser {

        private final HashMap<String, TagParser> mResolveElements;

        public ResolveParser() {
            this(getResolveElementsMap());
        }

        public ResolveParser(HashMap<String, TagParser> elements) {
            mResolveElements = elements;
        }


        @Override
        public long parseAndAdd(XmlResourceParser parser) throws XmlPullParserException, IOException {
            final int groupDepth = parser.getDepth();
            int type;
            long addedId = -1;
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > groupDepth) {
                if (type != XmlPullParser.START_TAG || addedId > -1) {
                    continue;
                }
                TagParser tagParser = mResolveElements.get(parser.getName());
                if (tagParser != null) {
                    addedId = tagParser.parseAndAdd(parser);
                } else {
                    throw new RuntimeException("Invalid folder item " + parser.getName());
                }
            }
            return addedId;
        }
    }

    /**
     * AppWidget parser: Required attributes packageName, className, spanX and spanY.
     * Options child nodes: <extra key=... value=... />
     */
    protected class AppWidgetParser implements TagParser {

        @Override
        public long parseAndAdd(XmlResourceParser parser)
                throws XmlPullParserException, IOException {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                if (LOGD) Log.d(TAG, "Skipping invalid <favorite> with no component");
                return -1;
            }

            ComponentName cn = new ComponentName(packageName, className);
            try {
                mPackageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                String[] packages = mPackageManager.currentToCanonicalPackageNames(
                        new String[]{packageName});
                cn = new ComponentName(packages[0], className);
                try {
                    mPackageManager.getReceiverInfo(cn, 0);
                } catch (Exception e1) {
                    if (LOGD) Log.d(TAG, "Can't find widget provider: " + className);
                    return -1;
                }
            }

            mValues.put(Favorites.SPANX, getAttributeValue(parser, ATTR_SPAN_X));
            mValues.put(Favorites.SPANY, getAttributeValue(parser, ATTR_SPAN_Y));

            // Read the extras
            Bundle extras = new Bundle();
            int widgetDepth = parser.getDepth();
            int type;
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > widgetDepth) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                if (TAG_EXTRA.equals(parser.getName())) {
                    String key = getAttributeValue(parser, ATTR_KEY);
                    String value = getAttributeValue(parser, ATTR_VALUE);
                    if (key != null && value != null) {
                        extras.putString(key, value);
                    } else {
                        throw new RuntimeException("Widget extras must have a key and value");
                    }
                } else {
                    throw new RuntimeException("Widgets can contain only extras");
                }
            }

            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            long insertedId = -1;
            try {
                int appWidgetId;
                if (packageName.equals(mContext.getPackageName())) {
                    appWidgetId = LauncherAppWidgetInfo.IOS_WIDGET_ID;
                } else {
                    appWidgetId = mAppWidgetHost.allocateAppWidgetId();

                    if (!appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                        if (LOGD) Log.e(TAG, "Unable to bind app widget id " + cn);
                        return -1;
                    }
                }

                mValues.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPWIDGET);
                mValues.put(Favorites.APPWIDGET_ID, appWidgetId);
                mValues.put(Favorites.APPWIDGET_PROVIDER, cn.flattenToString());
                mValues.put(Favorites._ID, mCallback.generateNewItemId());
                insertedId = mCallback.insertAndCheck(mDb, mValues);
                if (insertedId < 0) {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    return insertedId;
                }

                // Send a broadcast to configure the widget
                if (!extras.isEmpty()) {
                    Intent intent = new Intent(ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE);
                    intent.setComponent(cn);
                    intent.putExtras(extras);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    mContext.sendBroadcast(intent);
                }
            } catch (RuntimeException ex) {
                if (LOGD) Log.e(TAG, "Problem allocating appWidgetId", ex);
            }
            return insertedId;
        }
    }

    protected class FolderParser implements TagParser {
        private final HashMap<String, TagParser> mFolderElements;

        public FolderParser() {
            this(getFolderElementsMap());
        }

        public FolderParser(HashMap<String, TagParser> elements) {
            mFolderElements = elements;
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser)
                throws XmlPullParserException, IOException {
            String title;
            final int titleResId = getAttributeResourceValue(parser, ATTR_TITLE, 0);
            if (titleResId != 0) {
                title = mSourceRes.getString(titleResId);
            } else {
                title = getAttributeValue(parser, ATTR_TITLE);
                if (TextUtils.isEmpty(title)) {
                    title = mContext.getResources().getString(R.string.folder_name);
                }
            }

            String categoryType = getAttributeValue(parser, ATTR_FOLDER_CATEGORY);
            if (!TextUtils.isEmpty(categoryType)) {
                mValues.put(Favorites.FOLDER_CATEGORY_TYPE, categoryType);
            }

            mValues.put(Favorites.TITLE, title);
            mValues.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_FOLDER);
            mValues.put(Favorites.SPANX, 1);
            mValues.put(Favorites.SPANY, 1);
            mValues.put(Favorites._ID, mCallback.generateNewItemId());
            long folderId = mCallback.insertAndCheck(mDb, mValues);
            if (folderId < 0) {
                if (LOGD) Log.e(TAG, "Unable to add folder");
                return -1;
            }

            final ContentValues myValues = new ContentValues(mValues);
            ArrayList<Long> folderItems = new ArrayList<Long>();

            int type;
            int folderDepth = parser.getDepth();
            int rank = 0;
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > folderDepth) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                mValues.clear();
                mValues.put(Favorites.CONTAINER, folderId);
                mValues.put(Favorites.RANK, rank);

                TagParser tagParser = mFolderElements.get(parser.getName());
                if (tagParser != null) {
                    final long id = tagParser.parseAndAdd(parser);
                    if (id >= 0) {
                        folderItems.add(id);
                        rank++;
                    }
                } else {
                    throw new RuntimeException("Invalid folder item " + parser.getName());
                }
            }

            long addedId = folderId;

            // We can only have folders with >= 2 items, so we need to remove the
            // folder and clean up if less than 2 items were included, or some
            // failed to add, and less than 2 were actually added
            if (folderItems.size() < 1) {
                // Delete the folder
                Uri uri = Favorites.getContentUri(folderId);
                SqlArguments args = new SqlArguments(uri, null, null);
                mDb.delete(args.table, args.where, args.args);
                addedId = -1;
            }
            return addedId;
        }
    }

    protected static final void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) ;

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    /**
     * Return attribute value, attempting launcher-specific namespace first
     * before falling back to anonymous attribute.
     */
    protected static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue(
                "http://schemas.android.com/apk/res-auto/com.ios.launcher", attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    /**
     * Return attribute resource value, attempting launcher-specific namespace
     * first before falling back to anonymous attribute.
     */
    protected static int getAttributeResourceValue(XmlResourceParser parser, String attribute,
                                                   int defaultValue) {
        int value = parser.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto/com.ios.launcher", attribute,
                defaultValue);
        if (value == defaultValue) {
            value = parser.getAttributeResourceValue(null, attribute, defaultValue);
        }
        return value;
    }

    public interface LayoutParserCallback {
        long generateNewItemId();

        long insertAndCheck(SQLiteDatabase db, ContentValues values);
    }

    @Thunk
    static void copyInteger(ContentValues from, ContentValues to, String key) {
        to.put(key, from.getAsInteger(key));
    }
}
