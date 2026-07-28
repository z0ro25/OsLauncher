package com.amz.ios.ioslite.common.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.Nullable;
import android.text.TextUtils;


public class AppUsagesProvider extends ContentProvider {
    private static final boolean DEBUG = false;
    private static final String TAG = "AppUsagesProvider";

    public static final String AUTHORITY = "com.amz.ios.ioslite.provider.usages";
    private static final String APP_USAGES_DB = "launcher_app_usages.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_USAGE = Usages.TABLE_NAME;

    private DatabaseHelper mOpenHelper;


    public static final class Usages implements BaseColumns {

        public static final String TABLE_NAME = "usages";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id) {
            return Uri.parse(CONTENT_URI + "/" + id);
        }

        public static final String APP_COMPONENTNAME = "componentName";

        public static final String APP_LAUNCH_TIMES = "launchTimes";

        public static final String APP_LAUNCH_LATEST_TIME = "launchLatestTime";

        /**
         * The time of the last update to this row.
         */
        public static final String MODIFIED = "modified";
    }


    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new DatabaseHelper(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);

        return result;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, values);
        if (rowId < 0) return null;
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        return count;
    }

    static long dbInsertAndCheck(DatabaseHelper helper, SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }

        return db.insert(table, nullColumnHack, values);
    }


    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;

        DatabaseHelper(Context context) {
            super(context, APP_USAGES_DB, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            addAppUsagesTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            createEmptyDB(db);
        }

        public void createEmptyDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USAGE);
            onCreate(db);
        }

        /**
         * @return the max _id in the provided table.
         */
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


        private void addAppUsagesTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_USAGE + " (" +
                    Usages._ID + " INTEGER PRIMARY KEY," +
                    Usages.APP_COMPONENTNAME + " TEXT," +
                    Usages.APP_LAUNCH_TIMES + " INTEGER NOT NULL DEFAULT 0," +
                    Usages.APP_LAUNCH_LATEST_TIME + " INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
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
