package com.amz.ios.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.amz.ios.launcher.ItemInfo;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class HiddenAppDao_Impl implements HiddenAppDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ItemInfo> __insertionAdapterOfItemInfo;

  private final Converter __converter = new Converter();

  private final EntityDeletionOrUpdateAdapter<ItemInfo> __deletionAdapterOfItemInfo;

  public HiddenAppDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfItemInfo = new EntityInsertionAdapter<ItemInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `ItemInfo` (`id`,`itemType`,`container`,`screenId`,`cellX`,`cellY`,`spanX`,`spanY`,`minSpanX`,`minSpanY`,`rank`,`requiresDbUpdate`,`title`,`contentDescription`,`wasMovedDueToReducedSpace`,`dropPos`,`user`,`unreadNum`,`newInstalled`,`calledNum`,`lastCalledTime`,`url`,`isHidden`,`packageName`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final ItemInfo entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.itemType);
        statement.bindLong(3, entity.container);
        statement.bindLong(4, entity.screenId);
        statement.bindLong(5, entity.cellX);
        statement.bindLong(6, entity.cellY);
        statement.bindLong(7, entity.spanX);
        statement.bindLong(8, entity.spanY);
        statement.bindLong(9, entity.minSpanX);
        statement.bindLong(10, entity.minSpanY);
        statement.bindLong(11, entity.rank);
        final int _tmp = entity.requiresDbUpdate ? 1 : 0;
        statement.bindLong(12, _tmp);
        final String _tmp_1 = __converter.fromChar(entity.title);
        if (_tmp_1 == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, _tmp_1);
        }
        final String _tmp_2 = __converter.fromChar(entity.contentDescription);
        if (_tmp_2 == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, _tmp_2);
        }
        final int _tmp_3 = entity.wasMovedDueToReducedSpace ? 1 : 0;
        statement.bindLong(15, _tmp_3);
        final String _tmp_4 = __converter.fromIntArray(entity.dropPos);
        if (_tmp_4 == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, _tmp_4);
        }
        final String _tmp_5 = __converter.fromUser(entity.user);
        if (_tmp_5 == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, _tmp_5);
        }
        statement.bindLong(18, entity.unreadNum);
        final int _tmp_6 = entity.newInstalled ? 1 : 0;
        statement.bindLong(19, _tmp_6);
        statement.bindLong(20, entity.calledNum);
        statement.bindLong(21, entity.lastCalledTime);
        if (entity.url == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.url);
        }
        final int _tmp_7 = entity.isHidden ? 1 : 0;
        statement.bindLong(23, _tmp_7);
        if (entity.packageName == null) {
          statement.bindNull(24);
        } else {
          statement.bindString(24, entity.packageName);
        }
      }
    };
    this.__deletionAdapterOfItemInfo = new EntityDeletionOrUpdateAdapter<ItemInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `ItemInfo` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final ItemInfo entity) {
        statement.bindLong(1, entity.id);
      }
    };
  }

  @Override
  public void hideApp(final ItemInfo data) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfItemInfo.insert(data);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void unHideApp(final ItemInfo data) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfItemInfo.handle(data);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<ItemInfo> getAllHiddenApp() {
    final String _sql = "SELECT * FROM iteminfo";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "itemType");
      final int _cursorIndexOfContainer = CursorUtil.getColumnIndexOrThrow(_cursor, "container");
      final int _cursorIndexOfScreenId = CursorUtil.getColumnIndexOrThrow(_cursor, "screenId");
      final int _cursorIndexOfCellX = CursorUtil.getColumnIndexOrThrow(_cursor, "cellX");
      final int _cursorIndexOfCellY = CursorUtil.getColumnIndexOrThrow(_cursor, "cellY");
      final int _cursorIndexOfSpanX = CursorUtil.getColumnIndexOrThrow(_cursor, "spanX");
      final int _cursorIndexOfSpanY = CursorUtil.getColumnIndexOrThrow(_cursor, "spanY");
      final int _cursorIndexOfMinSpanX = CursorUtil.getColumnIndexOrThrow(_cursor, "minSpanX");
      final int _cursorIndexOfMinSpanY = CursorUtil.getColumnIndexOrThrow(_cursor, "minSpanY");
      final int _cursorIndexOfRank = CursorUtil.getColumnIndexOrThrow(_cursor, "rank");
      final int _cursorIndexOfRequiresDbUpdate = CursorUtil.getColumnIndexOrThrow(_cursor, "requiresDbUpdate");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfContentDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "contentDescription");
      final int _cursorIndexOfWasMovedDueToReducedSpace = CursorUtil.getColumnIndexOrThrow(_cursor, "wasMovedDueToReducedSpace");
      final int _cursorIndexOfDropPos = CursorUtil.getColumnIndexOrThrow(_cursor, "dropPos");
      final int _cursorIndexOfUser = CursorUtil.getColumnIndexOrThrow(_cursor, "user");
      final int _cursorIndexOfUnreadNum = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadNum");
      final int _cursorIndexOfNewInstalled = CursorUtil.getColumnIndexOrThrow(_cursor, "newInstalled");
      final int _cursorIndexOfCalledNum = CursorUtil.getColumnIndexOrThrow(_cursor, "calledNum");
      final int _cursorIndexOfLastCalledTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCalledTime");
      final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
      final int _cursorIndexOfIsHidden = CursorUtil.getColumnIndexOrThrow(_cursor, "isHidden");
      final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
      final List<ItemInfo> _result = new ArrayList<ItemInfo>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final ItemInfo _item;
        _item = new ItemInfo();
        _item.id = _cursor.getLong(_cursorIndexOfId);
        _item.itemType = _cursor.getInt(_cursorIndexOfItemType);
        _item.container = _cursor.getLong(_cursorIndexOfContainer);
        _item.screenId = _cursor.getLong(_cursorIndexOfScreenId);
        _item.cellX = _cursor.getInt(_cursorIndexOfCellX);
        _item.cellY = _cursor.getInt(_cursorIndexOfCellY);
        _item.spanX = _cursor.getInt(_cursorIndexOfSpanX);
        _item.spanY = _cursor.getInt(_cursorIndexOfSpanY);
        _item.minSpanX = _cursor.getInt(_cursorIndexOfMinSpanX);
        _item.minSpanY = _cursor.getInt(_cursorIndexOfMinSpanY);
        _item.rank = _cursor.getInt(_cursorIndexOfRank);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfRequiresDbUpdate);
        _item.requiresDbUpdate = _tmp != 0;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfTitle);
        }
        _item.title = __converter.toChar(_tmp_1);
        final String _tmp_2;
        if (_cursor.isNull(_cursorIndexOfContentDescription)) {
          _tmp_2 = null;
        } else {
          _tmp_2 = _cursor.getString(_cursorIndexOfContentDescription);
        }
        _item.contentDescription = __converter.toChar(_tmp_2);
        final int _tmp_3;
        _tmp_3 = _cursor.getInt(_cursorIndexOfWasMovedDueToReducedSpace);
        _item.wasMovedDueToReducedSpace = _tmp_3 != 0;
        final String _tmp_4;
        if (_cursor.isNull(_cursorIndexOfDropPos)) {
          _tmp_4 = null;
        } else {
          _tmp_4 = _cursor.getString(_cursorIndexOfDropPos);
        }
        _item.dropPos = __converter.toIntArray(_tmp_4);
        final String _tmp_5;
        if (_cursor.isNull(_cursorIndexOfUser)) {
          _tmp_5 = null;
        } else {
          _tmp_5 = _cursor.getString(_cursorIndexOfUser);
        }
        _item.user = __converter.toUser(_tmp_5);
        _item.unreadNum = _cursor.getInt(_cursorIndexOfUnreadNum);
        final int _tmp_6;
        _tmp_6 = _cursor.getInt(_cursorIndexOfNewInstalled);
        _item.newInstalled = _tmp_6 != 0;
        _item.calledNum = _cursor.getLong(_cursorIndexOfCalledNum);
        _item.lastCalledTime = _cursor.getLong(_cursorIndexOfLastCalledTime);
        if (_cursor.isNull(_cursorIndexOfUrl)) {
          _item.url = null;
        } else {
          _item.url = _cursor.getString(_cursorIndexOfUrl);
        }
        final int _tmp_7;
        _tmp_7 = _cursor.getInt(_cursorIndexOfIsHidden);
        _item.isHidden = _tmp_7 != 0;
        if (_cursor.isNull(_cursorIndexOfPackageName)) {
          _item.packageName = null;
        } else {
          _item.packageName = _cursor.getString(_cursorIndexOfPackageName);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
