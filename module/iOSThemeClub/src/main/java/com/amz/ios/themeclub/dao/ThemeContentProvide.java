package com.amz.ios.themeclub.dao;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.Nullable;

/**
 * Created by GL on 16-12-28.
 */

public class ThemeContentProvide extends ContentProvider {

    private static final int THEME_SINGLE = 1;
    private static final int THEME_MULTIPLE = 2;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private DataBaseOpenHelper helper = null;

    static {
        sURIMatcher.addURI("com.amz.ios.themeclub.provide", "theme/#", THEME_SINGLE);
        sURIMatcher.addURI("com.amz.ios.themeclub.provide", "theme", THEME_MULTIPLE);
    }

    @Override
    public boolean onCreate() {
        helper = new DataBaseOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;
        int match = sURIMatcher.match(uri);
        switch (match){
            case THEME_SINGLE:
                long id = ContentUris.parseId(uri);
                String where = "_id=" + id;
                if (selection != null && !"".equals(selection)) {
                    where = selection + " and " + where;
                }
                cursor = db.query(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE,
                        projection, where, selectionArgs, null, null, sortOrder);
                break;
            case THEME_MULTIPLE:
                cursor = db.query(DataBaseOpenHelper.DOWNLOAD_THEME_TABLE,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
        }
        db.close();
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        int flag = sURIMatcher.match(uri);
        switch (flag) {
            case THEME_SINGLE:
                return "vnd.android.cursor.item/vnd.com.amz.ios.themeclub.provide."
                        +DataBaseOpenHelper.DOWNLOAD_THEME_TABLE;
            case THEME_MULTIPLE:
                return "vnd.android.cursor.dir/vnd.com.amz.ios.themeclub.provide."
                        +DataBaseOpenHelper.DOWNLOAD_THEME_TABLE;
        }
        return null;
    }

    @Nullable
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
}
