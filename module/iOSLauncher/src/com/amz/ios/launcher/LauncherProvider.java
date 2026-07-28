/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.amz.ios.launcher;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.launcher.LauncherSettings.Favorites;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.GridConfig;
import com.amz.ios.launcher.config.ProviderConfig;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.parser.AutoInstallsLayout;
import com.amz.ios.launcher.parser.AutoInstallsLayout.LayoutParserCallback;
import com.amz.ios.launcher.parser.CategoryInstallLayout;
import com.amz.ios.launcher.provider.AppTypeProvider;
import com.amz.ios.launcher.util.ManagedProfileHeuristic;
import com.amz.ios.launcher.util.Thunk;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LauncherProvider extends ContentProvider {
    private static final String TAG = "LauncherProvider";
    private static final boolean LOGD = false;

    private static int DATABASE_VERSION = 9;

    public static final String AUTHORITY = ProviderConfig.AUTHORITY;

    public static final String TABLE_FAVORITES = LauncherSettings.Favorites.TABLE_NAME;
    static final String TABLE_WORKSPACE_SCREENS = LauncherSettings.WorkspaceScreens.TABLE_NAME;
    static final String TABLE_EMPTY_WORKSPACE_SCREENS = LauncherSettings.EmptyWorkspaceScreens.TABLE_NAME;
    static final String TABLE_NEW_INSTALL = LauncherSettings.NewInstall.TABLE_NAME;

    static final String TABLE_BACKUPSETTING = LauncherSettings.BackupSettings.TABLE_NAME;

    static final String EMPTY_DATABASE_CREATED = "LauncherProvider.empty_database_created";
    private static final String RESTRICTION_PACKAGE_NAME = "workspace.configuration.package.name";

    @Thunk
    LauncherProviderChangeListener mListener;
    @Thunk
    DatabaseHelper mOpenHelper;

    public static String sDatabaseName;

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState == null || !appState.getModel().isModelLoaded()) {
            return;
        }
        appState.getModel().dumpState("", fd, writer, args);
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        DATABASE_VERSION = Partner.getInteger(context, Partner.DEF_LAUNCHER_DB_VERSION, DATABASE_VERSION);
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        sDatabaseName = LauncherFiles.LAUNCHER_DB;
        mOpenHelper = new DatabaseHelper(context);
        StrictMode.setThreadPolicy(oldPolicy);
        LauncherAppState.setLauncherProvider(this);
        return true;
    }


    public void setLauncherProviderChangeListener(LauncherProviderChangeListener listener) {
        mListener = listener;
        mOpenHelper.mListener = mListener;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    @Thunk
    static long dbInsertAndCheck(DatabaseHelper helper,
                                 SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        if (!values.containsKey(LauncherSettings.ChangeLogColumns._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        helper.checkId(table, values);
        return db.insert(table, nullColumnHack, values);
    }

    private void reloadLauncherIfExternal() {
        if (Utilities.ATLEAST_MARSHMALLOW && Binder.getCallingPid() != Process.myPid()) {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app != null) {
                app.reloadWorkspace();
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        try {
            SqlArguments args = new SqlArguments(uri);

            // In very limited cases, we support system|signature permission apps to modify the db.
            if (Binder.getCallingPid() != Process.myPid()) {
                if (!mOpenHelper.initializeExternalAdd(initialValues)) {
                    return null;
                }
            }

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            addModifiedTime(initialValues);
            final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
            if (rowId < 0) return null;

            uri = ContentUris.withAppendedId(uri, rowId);
            notifyListeners();

            if (Utilities.ATLEAST_MARSHMALLOW) {
                reloadLauncherIfExternal();
            } else {
                // Deprecated behavior to support legacy devices which rely on provider callbacks.
                LauncherAppState app = LauncherAppState.getInstanceNoCreate();
                if (app != null && "true".equals(uri.getQueryParameter("isExternalAdd"))) {
                    app.reloadWorkspace();
                }

                String notify = uri.getQueryParameter("notify");
                if (notify == null || "true".equals(notify)) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            }
        } catch (Exception e) {

        }
        return uri;
    }


    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                addModifiedTime(values[i]);
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyListeners();
        reloadLauncherIfExternal();
        return values.length;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            db.setTransactionSuccessful();
            reloadLauncherIfExternal();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            count = db.delete(args.table, args.where, args.args);
            if (count > 0) notifyListeners();

            reloadLauncherIfExternal();
        } catch (Exception e) {

        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
            addModifiedTime(values);
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            count = db.update(args.table, values, args.where, args.args);
            if (count > 0) notifyListeners();

            reloadLauncherIfExternal();
        } catch (Exception e) {

        }

        return count;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (Binder.getCallingUid() != Process.myUid()) {
            return null;
        }

        switch (method) {
            case LauncherSettings.Settings.METHOD_GET_BOOLEAN: {
                Bundle result = new Bundle();
                result.putBoolean(LauncherSettings.Settings.EXTRA_VALUE,
                        getContext().getSharedPreferences(
                                LauncherAppState.getSharedPreferencesKey(), Context.MODE_PRIVATE)
                                .getBoolean(arg, extras.getBoolean(
                                        LauncherSettings.Settings.EXTRA_DEFAULT_VALUE)));
                return result;
            }
            case LauncherSettings.Settings.METHOD_SET_BOOLEAN: {
                boolean value = extras.getBoolean(LauncherSettings.Settings.EXTRA_VALUE);
                getContext().getSharedPreferences(
                        LauncherAppState.getSharedPreferencesKey(), Context.MODE_PRIVATE)
                        .edit().putBoolean(arg, value).apply();
                if (mListener != null) {
                    mListener.onSettingsChanged(arg, value);
                }
                Bundle result = new Bundle();
                result.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, value);
                return result;
            }
        }
        return null;
    }

    /**
     * Deletes any empty folder from the DB.
     *
     * @return Ids of deleted folders.
     */
    public List<Long> deleteEmptyFolders() {
        ArrayList<Long> folderIds = new ArrayList<Long>();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Select folders whose id do not match any container value.
            String selection = LauncherSettings.Favorites.ITEM_TYPE + " = "
                    + LauncherSettings.Favorites.ITEM_TYPE_FOLDER + " AND "
                    + LauncherSettings.Favorites._ID + " NOT IN (SELECT " +
                    LauncherSettings.Favorites.CONTAINER + " FROM "
                    + TABLE_FAVORITES + ")";
            Cursor c = db.query(TABLE_FAVORITES,
                    new String[]{LauncherSettings.Favorites._ID},
                    selection, null, null, null, null);
            while (c.moveToNext()) {
                folderIds.add(c.getLong(0));
            }
            c.close();
            if (folderIds.size() > 0) {
                db.delete(TABLE_FAVORITES, Utilities.createDbSelectionQuery(
                        LauncherSettings.Favorites._ID, folderIds), null);
            }
            db.setTransactionSuccessful();
        } catch (SQLException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            folderIds.clear();
        } finally {
            db.endTransaction();
        }
        return folderIds;
    }

    private void notifyListeners() {
        // always notify the backup agent
        if (mListener != null) {
            mListener.onLauncherProviderChange();
        }
    }

    @Thunk
    static void addModifiedTime(ContentValues values) {
        values.put(LauncherSettings.ChangeLogColumns.MODIFIED, System.currentTimeMillis());
    }

    public long generateNewItemId() {
        return mOpenHelper.generateNewItemId();
    }

    public void updateMaxItemId(long id) {
        mOpenHelper.updateMaxItemId(id);
    }

    public long generateNewScreenId() {
        return mOpenHelper.generateNewScreenId();
    }

    /**
     * Clears all the data for a fresh start.
     */
    synchronized public void createEmptyDB() {
        mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
    }

    public void clearFlagEmptyDbCreated() {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE)
                .edit()
                .remove(EMPTY_DATABASE_CREATED)
                .commit();
    }

    public void clearDatabase() {
        mOpenHelper.clearDatabase(mOpenHelper.getWritableDatabase());
    }

    /**
     * Loads the default workspace based on the following priority scheme:
     * 1) From the app restrictions
     * 2) From a package provided by play store
     * 3) From a partner configuration APK, already in the system image
     * 4) The default configuration for the particular device
     */
    synchronized public boolean loadDefaultFavoritesIfNecessary() {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE);

        boolean emptyWorkspaceDb = sp.getBoolean(EMPTY_DATABASE_CREATED, false);
        boolean emptyAppTypeDb = sp.getBoolean(AppTypeProvider.EMPTY_DATABASE_CREATED, false);

        if (emptyWorkspaceDb || emptyAppTypeDb) {
            DebugUtil.debugLaunch(TAG, "loading default workspace");
            // load all launcher components;
            final ArrayList<ComponentName> allLauncherComponentNames = new ArrayList<ComponentName>();
            final List<UserHandleCompat> profiles = UserManagerCompat.getInstance(getContext()).getUserProfiles();
            LauncherAppsCompat launcherAppsCompat = LauncherAppsCompat.getInstance(getContext());
            for (UserHandleCompat user : profiles) {
                final List<LauncherActivityInfoCompat> apps = launcherAppsCompat.getActivityList(null, user);
                if (apps == null || apps.isEmpty()) {
                    continue;
                }

                ComponentName componentName;
                for (int i = 0; i < apps.size(); i++) {
                    LauncherActivityInfoCompat app = apps.get(i);
                    componentName = app.getComponentName();
                    if (AppFilter.filterLoadApp(getContext(), componentName)) {
                        continue;
                    }

                    allLauncherComponentNames.add(app.getComponentName());
                }
            }

            if (emptyAppTypeDb) {
                AppTypeParser appTypeParser = new AppTypeParser(getContext(), AppTypeProvider.getAppTypeProvider(), allLauncherComponentNames);
                appTypeParser.initAllAppType();
                AppTypeProvider.getAppTypeProvider().clearFlagEmptyDbCreated();
            }


            if (emptyWorkspaceDb) {
                for (int i = 0; i < 3; i++) {
                    try {
                        createEmptyDB();
                        mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase(),
                                getLayoutParser(), allLauncherComponentNames);
                        clearFlagEmptyDbCreated();
                        break;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            return true;
        }

        return false;
    }


    private AutoInstallsLayout getLayoutParser() {
        AutoInstallsLayout layoutParser = null;
        if (BuildUtil.isCustomerBuild()) {
            layoutParser = getCustomLayoutParser();
        }

        if (layoutParser == null) {
            layoutParser = getDefaultLayoutParser();
        }

        return layoutParser;
    }


    private AutoInstallsLayout getDefaultLayoutParser() {
        int defaultLayout = LauncherAppState.getInstance()
                .getInvariantDeviceProfile().defaultLayoutId;
        return new AutoInstallsLayout(getContext(), mOpenHelper.mAppWidgetHost,
                mOpenHelper, getContext().getResources(), defaultLayout);
    }

    private AutoInstallsLayout getCustomLayoutParser() {
        int customLayout = Partner.getXmlResId(getContext(), GridConfig.getGridWorkspaceName(getContext()));
        if (customLayout <= 0) {
            customLayout = Partner.getXmlResId(getContext(), GridConfig.getDefaultWorkspaceName(getContext()));
        }

        if (customLayout > 0) {
            return new AutoInstallsLayout(getContext(), mOpenHelper.mAppWidgetHost,
                    mOpenHelper, Partner.getResources(getContext()), customLayout);
        }

        return null;
    }


    public void deleteDatabase() {
        File standardDbFile = getContext().getDatabasePath(LauncherFiles.LAUNCHER_DB);

        if (standardDbFile.exists()) {
            SQLiteDatabase.deleteDatabase(standardDbFile);
        }

        mOpenHelper = new DatabaseHelper(getContext());
        mOpenHelper.mListener = mListener;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper implements LayoutParserCallback {
        private final Context mContext;
        @Thunk
        final AppWidgetHost mAppWidgetHost;
        private long mMaxItemId = -1;
        private long mMaxScreenId = -1;

        @Thunk
        LauncherProviderChangeListener mListener;

        DatabaseHelper(Context context) {
            super(context, sDatabaseName, null, DATABASE_VERSION);
            mContext = context;
            mAppWidgetHost = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);

            try {
                // Table creation sometimes fails silently, which leads to a crash loop.
                // This way, we will try to create a table every time after crash, so the device
                // would eventually be able to recover.
                if (!tableExists(TABLE_FAVORITES) || !tableExists(TABLE_WORKSPACE_SCREENS)) {
                    Log.e(TAG, "Tables are missing after onCreate has been called. Trying to recreate");
                    // This operation is a no-op if the table already exists.
                    onCreate(getWritableDatabase());
                }

                // In the case where neither onCreate nor onUpgrade gets called, we read the maxId from
                // the DB here
                if (mMaxItemId == -1) {
                    mMaxItemId = initializeMaxItemId(getWritableDatabase());
                }
                if (mMaxScreenId == -1) {
                    mMaxScreenId = initializeMaxScreenId(getWritableDatabase());
                }
            } catch (Exception e) {
                //
             }
        }

        private boolean tableExists(String tableName) {
            Cursor c = getReadableDatabase().query(
                    true, "sqlite_master", new String[] {"tbl_name"},
                    "tbl_name = ?", new String[] {tableName},
                    null, null, null, null, null);
            try {
                return c.getCount() > 0;
            } finally {
                c.close();
            }
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOGD) Log.d(TAG, "creating new launcher database");

            mMaxItemId = 1;
            mMaxScreenId = 0;

            UserManagerCompat userManager = UserManagerCompat.getInstance(mContext);
            long userSerialNumber = userManager.getSerialNumberForUser(UserHandleCompat.myUserHandle());

            createTableNewInstall(db);
            createTableFavorites(db, userSerialNumber);
            addWorkspacesTable(db);
            addEmptyWorkspacesTable(db);
            addBackupSettingTable(db);

            // Database was just created, so wipe any previous widgets
            if (mAppWidgetHost != null) {

                // delete it , if not after restore widgets unavliable;
                /**
                 mAppWidgetHost.deleteHost();
                 *

                 /**
                 * Send notification that we've deleted the {@link AppWidgetHost},
                 * probably as part of the initial database creation. The receiver may
                 * want to re-call {@link AppWidgetHost#startListening()} to ensure
                 * callbacks are correctly set.
                 */
//                new MainThreadExecutor().execute(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        if (mListener != null) {
//                            mListener.onAppWidgetHostReset();
//                        }
//                    }
//                });
            }

            // Fresh and clean launcher DB.
            mMaxItemId = initializeMaxItemId(db);
            setFlagEmptyDbCreated();

            // When a new DB is created, remove all previously stored managed profile information.
            ManagedProfileHeuristic.processAllUsers(Collections.<UserHandleCompat>emptyList(), mContext);
        }

        private void createTableFavorites(SQLiteDatabase db, long userSerialNumber) {
            try {
                db.execSQL("CREATE TABLE favorites (" +
                        "_id INTEGER PRIMARY KEY," +
                        "title TEXT," +
                        "intent TEXT," +
                        "container INTEGER," +
                        "screen INTEGER," +
                        "cellX INTEGER," +
                        "cellY INTEGER," +
                        "spanX INTEGER," +
                        "spanY INTEGER," +
                        "itemType INTEGER," +
                        "folderCategoryType INTEGER NOT NULL DEFAULT -1," +
                        "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                        "iconType INTEGER," +
                        "iconPackage TEXT," +
                        "iconResource TEXT," +
                        "icon BLOB," +
                        "appWidgetProvider TEXT," +
                        "modified INTEGER NOT NULL DEFAULT 0," +
                        "restored INTEGER NOT NULL DEFAULT 0," +
                        "profileId INTEGER DEFAULT " + userSerialNumber + "," +
                        "rank INTEGER NOT NULL DEFAULT 0," +
                        "options INTEGER NOT NULL DEFAULT 0," +
                        "called_num INTEGER NOT NULL DEFAULT 0," +
                        "last_called_time INTEGER NOT NULL DEFAULT 0," +
                        "url TEXT," +
                        "hidden INTEGER NOT NULL DEFAULT 0" +
                        ");");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void createTableNewInstall(SQLiteDatabase db) {
            try {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEW_INSTALL);
                db.execSQL("CREATE TABLE " + TABLE_NEW_INSTALL + " (" +
                        LauncherSettings.NewInstall._ID + " INTEGER PRIMARY KEY," +
                        LauncherSettings.NewInstall.PACKAGENAME + " TEXT," +
                        LauncherSettings.NewInstall.NEW_INSTALL_FLAG + " INTEGER," +
                        LauncherSettings.NewInstall.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                        ");");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addWorkspacesTable(SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE " + TABLE_WORKSPACE_SCREENS + " (" +
                        LauncherSettings.WorkspaceScreens._ID + " INTEGER PRIMARY KEY," +
                        LauncherSettings.WorkspaceScreens.SCREEN_RANK + " INTEGER," +
                        LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                        ");");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addEmptyWorkspacesTable(SQLiteDatabase db) {
            Log.d(TAG, "addEmptyWorkspacesTable");
            try {
                db.execSQL("CREATE TABLE " + TABLE_EMPTY_WORKSPACE_SCREENS + " (" +
                        LauncherSettings.EmptyWorkspaceScreens._ID + " INTEGER PRIMARY KEY," +
                        LauncherSettings.EmptyWorkspaceScreens.SCREEN_RANK + " INTEGER," +
                        LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                        ");");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        private void addBackupSettingTable(SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE " + TABLE_BACKUPSETTING + " (" +
                        LauncherSettings.BackupSettings._ID + " INTEGER AUTO INCREMENT," +
                        LauncherSettings.BackupSettings.SETTING_NAME + " TEXT," +
                        LauncherSettings.BackupSettings.SETTING_VALUE + " TEXT," +
                        LauncherSettings.BackupSettings.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                        ");");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        private void setFlagEmptyDbCreated() {
            String spKey = LauncherAppState.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            sp.edit().putBoolean(EMPTY_DATABASE_CREATED, true).commit();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOGD) Log.d(TAG, "onUpgrade triggered: " + oldVersion);
            // DB was not upgraded
            Log.w(TAG, "Destroying all old data.");
            createEmptyDB(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This shouldn't happen -- throw our hands up in the air and start over.
            Log.w(TAG, "Database version downgrade from: " + oldVersion + " to " + newVersion +
                    ". Wiping databse.");
            createEmptyDB(db);
        }

        /**
         * Clears all the data for a fresh start.
         */
        public void createEmptyDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKSPACE_SCREENS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPTY_WORKSPACE_SCREENS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEW_INSTALL);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BACKUPSETTING);
            onCreate(db);
        }

        /**
         * Clears all the data for a fresh start.
         */
        public void clearDatabase(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKSPACE_SCREENS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPTY_WORKSPACE_SCREENS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEW_INSTALL);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BACKUPSETTING);
            onCreate(db);
        }

        /**
         * Replaces all shortcuts of type {@link Favorites#ITEM_TYPE_SHORTCUT} which have a valid
         * launcher activity target with {@link Favorites#ITEM_TYPE_APPLICATION}.
         */
        @Thunk
        void convertShortcutsToLauncherActivities(SQLiteDatabase db) {
            db.beginTransaction();
            Cursor c = null;
            SQLiteStatement updateStmt = null;

            try {
                // Only consider the primary user as other users can't have a shortcut.
                long userSerial = UserManagerCompat.getInstance(mContext)
                        .getSerialNumberForUser(UserHandleCompat.myUserHandle());
                c = db.query(TABLE_FAVORITES, new String[]{
                                Favorites._ID,
                                Favorites.INTENT,
                        }, "itemType=" + Favorites.ITEM_TYPE_SHORTCUT + " AND profileId=" + userSerial,
                        null, null, null, null);

                updateStmt = db.compileStatement("UPDATE favorites SET itemType="
                        + Favorites.ITEM_TYPE_APPLICATION + " WHERE _id=?");

                final int idIndex = c.getColumnIndexOrThrow(Favorites._ID);
                final int intentIndex = c.getColumnIndexOrThrow(Favorites.INTENT);

                while (c.moveToNext()) {
                    String intentDescription = c.getString(intentIndex);
                    Intent intent;
                    try {
                        intent = Intent.parseUri(intentDescription, 0);
                    } catch (URISyntaxException e) {
                        Log.e(TAG, "Unable to parse intent", e);
                        continue;
                    }

                    if (!Utilities.isLauncherAppTarget(intent)) {
                        continue;
                    }

                    long id = c.getLong(idIndex);
                    updateStmt.bindLong(1, id);
                    updateStmt.executeUpdateDelete();
                }
                db.setTransactionSuccessful();
            } catch (SQLException ex) {
                Log.w(TAG, "Error deduping shortcuts", ex);
            } finally {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
                if (updateStmt != null) {
                    updateStmt.close();
                }
            }
        }

        /**
         * Recreates workspace table and migrates data to the new table.
         */
        public boolean recreateWorkspaceTable(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                Cursor c = db.query(TABLE_WORKSPACE_SCREENS,
                        new String[]{LauncherSettings.WorkspaceScreens._ID},
                        null, null, null, null,
                        LauncherSettings.WorkspaceScreens.SCREEN_RANK);
                ArrayList<Long> sortedIDs = new ArrayList<Long>();
                long maxId = 0;
                try {
                    while (c.moveToNext()) {
                        Long id = c.getLong(0);
                        if (!sortedIDs.contains(id)) {
                            sortedIDs.add(id);
                            maxId = Math.max(maxId, id);
                        }
                    }
                } finally {
                    c.close();
                }

                db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKSPACE_SCREENS);
                addWorkspacesTable(db);

                db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPTY_WORKSPACE_SCREENS);
                addEmptyWorkspacesTable(db);

                db.execSQL("DROP TABLE IF EXISTS " + TABLE_BACKUPSETTING);
                addBackupSettingTable(db);

                // Add all screen ids back
                int total = sortedIDs.size();
                for (int i = 0; i < total; i++) {
                    ContentValues values = new ContentValues();
                    values.put(LauncherSettings.WorkspaceScreens._ID, sortedIDs.get(i));
                    values.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, i);
                    addModifiedTime(values);
                    db.insertOrThrow(TABLE_WORKSPACE_SCREENS, null, values);
                }
                db.setTransactionSuccessful();
                mMaxScreenId = maxId;
            } catch (SQLException ex) {
                // Old version remains, which means we wipe old data
                Log.e(TAG, ex.getMessage(), ex);
                return false;
            } finally {
                db.endTransaction();
            }
            return true;
        }

        @Thunk
        boolean updateFolderItemsRank(SQLiteDatabase db, boolean addRankColumn) {
            db.beginTransaction();
            try {
                if (addRankColumn) {
                    // Insert new column for holding rank
                    db.execSQL("ALTER TABLE favorites ADD COLUMN rank INTEGER NOT NULL DEFAULT 0;");
                }

                // Get a map for folder ID to folder width
                Cursor c = db.rawQuery("SELECT container, MAX(cellX) FROM favorites"
                                + " WHERE container IN (SELECT _id FROM favorites WHERE itemType = ?)"
                                + " GROUP BY container;",
                        new String[]{Integer.toString(LauncherSettings.Favorites.ITEM_TYPE_FOLDER)});

                while (c.moveToNext()) {
                    db.execSQL("UPDATE favorites SET rank=cellX+(cellY*?) WHERE "
                                    + "container=? AND cellX IS NOT NULL AND cellY IS NOT NULL;",
                            new Object[]{c.getLong(1) + 1, c.getLong(0)});
                }

                c.close();
                db.setTransactionSuccessful();
            } catch (SQLException ex) {
                // Old version remains, which means we wipe old data
                Log.e(TAG, ex.getMessage(), ex);
                return false;
            } finally {
                db.endTransaction();
            }
            return true;
        }

        private boolean addProfileColumn(SQLiteDatabase db) {
            UserManagerCompat userManager = UserManagerCompat.getInstance(mContext);
            // Default to the serial number of this user, for older
            // shortcuts.
            long userSerialNumber = userManager.getSerialNumberForUser(
                    UserHandleCompat.myUserHandle());
            return addIntegerColumn(db, Favorites.PROFILE_ID, userSerialNumber);
        }

        private boolean addIntegerColumn(SQLiteDatabase db, String columnName, long defaultValue) {
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE favorites ADD COLUMN "
                        + columnName + " INTEGER NOT NULL DEFAULT " + defaultValue + ";");
                db.setTransactionSuccessful();
            } catch (SQLException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                return false;
            } finally {
                db.endTransaction();
            }
            return true;
        }

        // Generates a new ID to use for an object in your database. This method should be only
        // called from the main UI thread. As an exception, we do call it when we call the
        // constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        @Override
        public long generateNewItemId() {
            if (mMaxItemId < 0) {
                throw new RuntimeException("Error: max item id was not initialized");
            }
            mMaxItemId += 1;
            return mMaxItemId;
        }

        @Override
        public long insertAndCheck(SQLiteDatabase db, ContentValues values) {
            return dbInsertAndCheck(this, db, TABLE_FAVORITES, null, values);
        }

        public void updateMaxItemId(long id) {
            mMaxItemId = id + 1;
        }

        public void checkId(String table, ContentValues values) {
            long id = values.getAsLong(LauncherSettings.BaseLauncherColumns._ID);
            if (table == LauncherProvider.TABLE_WORKSPACE_SCREENS) {
                mMaxScreenId = Math.max(id, mMaxScreenId);
            } else {
                mMaxItemId = Math.max(id, mMaxItemId);
            }
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            return getMaxId(db, TABLE_FAVORITES);
        }

        // Generates a new ID to use for an workspace screen in your database. This method
        // should be only called from the main UI thread. As an exception, we do call it when we
        // call the constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        public long generateNewScreenId() {
            if (mMaxScreenId < 0) {
                throw new RuntimeException("Error: max screen id was not initialized");
            }
            mMaxScreenId += 1;
            return mMaxScreenId;
        }

        private long initializeMaxScreenId(SQLiteDatabase db) {
            return getMaxId(db, TABLE_WORKSPACE_SCREENS);
        }

        @Thunk
        boolean initializeExternalAdd(ContentValues values) {
            // 1. Ensure that externally added items have a valid item id
            long id = generateNewItemId();
            values.put(LauncherSettings.Favorites._ID, id);

            // 2. In the case of an app widget, and if no app widget id is specified, we
            // attempt allocate and bind the widget.
            Integer itemType = values.getAsInteger(LauncherSettings.Favorites.ITEM_TYPE);
            if (itemType != null &&
                    itemType.intValue() == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET &&
                    !values.containsKey(LauncherSettings.Favorites.APPWIDGET_ID)) {

                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
                ComponentName cn = ComponentName.unflattenFromString(
                        values.getAsString(Favorites.APPWIDGET_PROVIDER));

                if (cn != null) {
                    try {
                        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                        values.put(LauncherSettings.Favorites.APPWIDGET_ID, appWidgetId);
                        if (!appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                            return false;
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to initialize external widget", e);
                        return false;
                    }
                } else {
                    return false;
                }
            }

            // Add screen id if not present
            long screenId = values.getAsLong(LauncherSettings.Favorites.SCREEN);
            return addScreenIdIfNecessary(screenId);
        }

        // Returns true of screen id exists, or if successfully added
        private boolean addScreenIdIfNecessary(long screenId) {
            if (!hasScreenId(screenId)) {
                int rank = getMaxScreenRank() + 1;

                ContentValues v = new ContentValues();
                v.put(LauncherSettings.WorkspaceScreens._ID, screenId);
                v.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, rank);
                if (dbInsertAndCheck(this, getWritableDatabase(),
                        TABLE_WORKSPACE_SCREENS, null, v) < 0) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasScreenId(long screenId) {
            SQLiteDatabase db = getWritableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_WORKSPACE_SCREENS + " WHERE "
                    + LauncherSettings.WorkspaceScreens._ID + " = " + screenId, null);
            if (c != null) {
                int count = c.getCount();
                c.close();
                return count > 0;
            } else {
                return false;
            }
        }

        private int getMaxScreenRank() {
            SQLiteDatabase db = getWritableDatabase();
            Cursor c = db.rawQuery("SELECT MAX(" + LauncherSettings.WorkspaceScreens.SCREEN_RANK
                    + ") FROM " + TABLE_WORKSPACE_SCREENS, null);

            // get the result
            final int maxRankIndex = 0;
            int rank = -1;
            if (c != null && c.moveToNext()) {
                rank = c.getInt(maxRankIndex);
            }
            if (c != null) {
                c.close();
            }

            return rank;
        }

        @Thunk
        void loadFavorites(SQLiteDatabase db, AutoInstallsLayout loader, List<ComponentName> allLauncherComps) {
            ArrayList<Long> screenIds = new ArrayList<Long>();
            // TODO: Use multiple loaders with fall-back and transaction.
            loader.loadLayout(db, screenIds, allLauncherComps);

            // Add the screens specified by the items above
            long categoryScreenId;

            boolean autoCategoty = Partner.getBoolean(mContext, Partner.DEF_CATEGORY_DEFAULT_LAYOUT, false);
            if (autoCategoty) {
                long maxId = 0;
                for (long id : screenIds) {
                    maxId = (maxId > id ? maxId : id);
                }
                categoryScreenId = maxId + 1;
                screenIds.add(categoryScreenId);

                CategoryInstallLayout categoryLoader = new CategoryInstallLayout(mContext, mAppWidgetHost, this, mContext.getResources(), 0);
                categoryLoader.loadLayout(db, categoryScreenId, allLauncherComps);
            }

            Collections.sort(screenIds);

            int rank = 0;
            ContentValues values = new ContentValues();
            for (Long id : screenIds) {
                values.clear();
                values.put(LauncherSettings.WorkspaceScreens._ID, id);
                values.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, rank);
                if (dbInsertAndCheck(this, db, TABLE_WORKSPACE_SCREENS, null, values) < 0) {
                    throw new RuntimeException("Failed initialize screen table"
                            + "from default layout");
                }
                rank++;
            }

            // Ensure that the max ids are initialized
            mMaxItemId = initializeMaxItemId(db);
            mMaxScreenId = initializeMaxScreenId(db);
        }

    }

    /**
     * @return the max _id in the provided table.
     */
    @Thunk
    static long getMaxId(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + table, null);
        // get the result
        long id = -1;
        if (c != null && c.moveToNext()) {
            id = c.getLong(0);
        }
        if (c != null) {
            c.close();
        }

        if (id == -1) {
            throw new RuntimeException("Error: could not query max id in " + table);
        }

        return id;
    }

    public static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        public SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}
