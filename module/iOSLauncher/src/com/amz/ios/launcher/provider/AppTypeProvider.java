package com.amz.ios.launcher.provider;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.launcher.AppTypeParser;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherFiles;
import com.amz.ios.launcher.ota.IOSOtaHandler;
import com.amz.ios.launcher.util.Thunk;

import java.lang.ref.WeakReference;
import java.util.HashMap;


public class AppTypeProvider extends ContentProvider implements AppTypeParser.AppTypeParserCallback {
    private static WeakReference<AppTypeProvider> sAppTypeProvider;
    // v3: re-enable APP_TYPE_MARKET (Google Play → iOS App Store icon). Bumping the version
    // forces onUpgrade → createEmptyDB so the market type is (re)inserted on existing installs.
    // v4: classify Google Clock (com.google.android.deskclock) via app_target_app_clock.xml so
    // ic_app_clock applies; re-parse app-type xmls to pick up the new clock entry.
    // v5: thêm 6 app Google (Gmail/Maps/News/TV/Wallet/Meet) -> logo iOS; bump để re-parse
    // app-type xmls, nhận 6 type mới trên máy đã cài.
    private static final int DATABASE_VERSION = 5;
    public static final String AUTHORITY = "com.amz.ios.launcher.apptype";

    public static final String TABLE_APP_TYPE = "apptypeitems";
    public static final String APP_TYPE = "appType";
    public static final String APP_ICON_RESOURCE_NAME = "iconName";
    public static final String APP_COMPONENT_NAME = "componentName";

    public static final String EMPTY_DATABASE_CREATED = "AppTypeProvider.empty_database_created";

    @Thunk
    DatabaseHelper mOpenHelper;
    private static HashMap<String, ComponentName> mAppTypeToCompMap = new HashMap<>();
    private static HashMap<ComponentName, String> mAppIconResMap = new HashMap<>();
    // Package-level fallback: a favorite may store an abbreviated class (e.g.
    // "com.android.vending/.AssetBrowserActivity") while the app-type map is keyed by the
    // fully-qualified class resolved from PackageManager. ComponentName.equals is an exact
    // string compare, so those miss. This maps packageName -> iconResName so we can still
    // resolve the override. Packages that map to more than one distinct icon (ambiguous, e.g.
    // a dialer package that also hosts contacts) are removed to avoid cross-assigning icons.
    private static HashMap<String, String> mAppIconResByPkgMap = new HashMap<>();
    private static java.util.HashSet<String> mAmbiguousPkgs = new java.util.HashSet<>();
    private boolean mHasLoaderCompletedOnce;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new DatabaseHelper(context);
        setAppTypeProvider(this);
        return true;
    }

    static void setAppTypeProvider(AppTypeProvider provider) {
        sAppTypeProvider = new WeakReference<AppTypeProvider>(provider);
    }

    public static AppTypeProvider getAppTypeProvider() {
        return sAppTypeProvider.get();
    }

    public void loadAppTypeFromDb() {
        if (mHasLoaderCompletedOnce) {
            return;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        qb.setTables(TABLE_APP_TYPE);
        Cursor cursor = qb.query(db, new String[]{APP_COMPONENT_NAME, APP_TYPE, APP_ICON_RESOURCE_NAME}, null, null, null, null, null);
        if (cursor != null) {
            ComponentName cn;
            String appType;
            String iconResName;
            while (cursor.moveToNext()) {
                String componentName = cursor.getString(0);
                appType = cursor.getString(1);
                iconResName = cursor.getString(2);
                cn = ComponentName.unflattenFromString(componentName);
                mAppIconResMap.put(cn, iconResName);
                mAppTypeToCompMap.put(appType, cn);
                putIconResForPkg(cn, iconResName);
            }
        }

        CommonUtilities.close(cursor);
        mHasLoaderCompletedOnce = true;
    }

    @Override
    public void insertAndCheck(String type, ComponentName componentName) {
        if (IOSOtaHandler.isAppHide(getContext(), componentName.getPackageName())) {
            ComponentName appCN = IOSOtaHandler.getAppEntryRepalced(getContext(), componentName.getPackageName());
            if (appCN != null) {
                componentName = appCN;
            }
        }

        ContentValues values = new ContentValues();
        String iconResName = "ic_" + type;
        values.put(APP_COMPONENT_NAME, componentName.flattenToString());
        values.put(APP_TYPE, type);
        values.put(APP_ICON_RESOURCE_NAME, iconResName);
        mAppIconResMap.put(componentName, iconResName);
        mAppTypeToCompMap.put(type, componentName);
        putIconResForPkg(componentName, iconResName);
        mOpenHelper.getWritableDatabase().insert(TABLE_APP_TYPE, null, values);
    }

    // Record packageName -> iconResName, flagging the package ambiguous if two different
    // icons ever map to it (so we only fall back when a package unambiguously means one icon).
    private void putIconResForPkg(ComponentName cn, String iconResName) {
        if (cn == null || iconResName == null) {
            return;
        }
        String pkg = cn.getPackageName();
        String existing = mAppIconResByPkgMap.get(pkg);
        if (existing != null && !existing.equals(iconResName)) {
            mAmbiguousPkgs.add(pkg);
        } else {
            mAppIconResByPkgMap.put(pkg, iconResName);
        }
    }

    public ComponentName getComponentNameForAppType(String type) {
        return mAppTypeToCompMap.get(type);
    }

    public String getIconResNameForComp(ComponentName cn) {
        String iconResName = mAppIconResMap.get(cn);
        if (iconResName != null || cn == null) {
            return iconResName;
        }
        // Exact ComponentName miss. Fall back to package-level match so an abbreviated vs
        // fully-qualified class (e.g. "/.AssetBrowserActivity" vs the resolved full class)
        // still gets the override — but only when the package maps to a single icon.
        String pkg = cn.getPackageName();
        if (mAmbiguousPkgs.contains(pkg)) {
            return null;
        }
        return mAppIconResByPkgMap.get(pkg);
    }


    public ComponentName getCalendarComp() {
        return mAppTypeToCompMap.get(AppTypeParser.APP_TYPE_CALENDAR);
    }

    public ComponentName getClockComp() {
        return mAppTypeToCompMap.get(AppTypeParser.APP_TYPE_CLOCK);
    }

    public void clearFlagEmptyDbCreated() {
        mOpenHelper.clearFlagEmptyDbCreated();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        DatabaseHelper(Context context) {
            super(context, LauncherFiles.APP_TYPE_DB, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTableAppType(db);
            setFlagEmptyDbCreated();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < DATABASE_VERSION) {
                createEmptyDB(db);
            }
        }


        private void createTableAppType(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_APP_TYPE + " (_id INTEGER PRIMARY KEY,componentName TEXT,appType TEXT,iconName TEXT);");
        }

        public void createEmptyDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_APP_TYPE);
            onCreate(db);
        }


        private void setFlagEmptyDbCreated() {
            String spKey = LauncherAppState.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            sp.edit().putBoolean(EMPTY_DATABASE_CREATED, true).commit();
        }

        public void clearFlagEmptyDbCreated() {
            String spKey = LauncherAppState.getSharedPreferencesKey();
            mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE)
                    .edit()
                    .remove(EMPTY_DATABASE_CREATED)
                    .commit();
        }

    }
}
