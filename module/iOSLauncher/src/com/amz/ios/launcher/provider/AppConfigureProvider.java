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

package com.amz.ios.launcher.provider;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherFiles;
import com.amz.ios.launcher.LauncherSettings;
import com.amz.ios.launcher.Utilities;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.GestureSettings;
import com.amz.ios.launcher.config.ProviderConfig;
import com.amz.ios.launcher.parser.AutoInstallsLayout.LayoutParserCallback;
import com.amz.ios.launcher.util.ManagedProfileHeuristic;
import com.amz.ios.launcher.util.Thunk;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppConfigureProvider extends ContentProvider {
    private static final String TAG = "AppConfigureProvider";
    private static final boolean LOGD = false;

    private static int DATABASE_VERSION = 9;

    public static final String AUTHORITY = ProviderConfig.AUTHORITY_APPCONFIGURE;

    static final String TABLE_GESTURE = GestureSettings.Gesture.TABLE_NAME;

    static final String EMPTY_DATABASE_CREATED = "LauncherProvider.empty_database_created";
    private static final String RESTRICTION_PACKAGE_NAME = "workspace.configuration.package.name";

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
        sDatabaseName = LauncherFiles.LAUNCHER_APPCONFIGURE_DB;
        mOpenHelper = new DatabaseHelper(context);
        StrictMode.setThreadPolicy(oldPolicy);
        return true;
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
//            if (Binder.getCallingPid() != Process.myPid()) {
//                if (!mOpenHelper.initializeExternalAdd(initialValues)) {
//                    return null;
//                }
//            }

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            addModifiedTime(initialValues);
            final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
            if (rowId < 0) return null;

            uri = ContentUris.withAppendedId(uri, rowId);

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

    private void notifyListeners() {
        // always notify the backup agent
        Log.d(TAG, "notifyListeners");
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
                Bundle result = new Bundle();
                result.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, value);
                return result;
            }
        }
        return null;
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

    private static class DatabaseHelper extends SQLiteOpenHelper implements LayoutParserCallback {
        private final Context mContext;

        private long mMaxItemId = -1;
        private long mMaxScreenId = -1;

        DatabaseHelper(Context context) {
            super(context, sDatabaseName, null, DATABASE_VERSION);
            mContext = context;

            try {
                // Table creation sometimes fails silently, which leads to a crash loop.
                // This way, we will try to create a table every time after crash, so the device
                // would eventually be able to recover.
                Log.e(TAG, "Tables are missing after onCreate has been called. Trying to recreate");
                // This operation is a no-op if the table already exists.
                onCreate(getWritableDatabase());

                // In the case where neither onCreate nor onUpgrade gets called, we read the maxId from
                // the DB here
                if (mMaxItemId == -1) {
                    mMaxItemId = initializeMaxItemId(getWritableDatabase());
                }

            } catch (Exception e) {
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
            addGestureUsageTable(db);

            // Fresh and clean launcher DB.
            mMaxItemId = initializeMaxItemId(db);
//            setFlagEmptyDbCreated();

            // When a new DB is created, remove all previously stored managed profile information.
            ManagedProfileHeuristic.processAllUsers(Collections.<UserHandleCompat>emptyList(), mContext);
        }

        private void addGestureUsageTable(SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GESTURE + " (" +
                        GestureSettings.Gesture._ID + " INTEGER PRIMARY KEY," +
                        GestureSettings.Gesture.GESTURE_EVENT + " INTEGER NOT NULL DEFAULT 0," +
                        GestureSettings.Gesture.ACTION_INTENT + " TEXT," +
                        GestureSettings.Gesture.ACTION_DES + " TEXT," +
                        GestureSettings.Gesture.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
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
            onCreate(db);
        }

        /**
         * Clears all the data for a fresh start.
         */
        public void clearDatabase(SQLiteDatabase db) {
            onCreate(db);
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
            return -1;
        }

        public void updateMaxItemId(long id) {
            mMaxItemId = id + 1;
        }

        public void checkId(String table, ContentValues values) {
            long id = values.getAsLong(LauncherSettings.BaseLauncherColumns._ID);
            mMaxItemId = Math.max(id, mMaxItemId);
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            return -1;
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
