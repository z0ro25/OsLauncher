package com.amz.ios.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class HiddenDatabase_Impl extends HiddenDatabase {
  private volatile HiddenAppDao _hiddenAppDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ItemInfo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemType` INTEGER NOT NULL, `container` INTEGER NOT NULL, `screenId` INTEGER NOT NULL, `cellX` INTEGER NOT NULL, `cellY` INTEGER NOT NULL, `spanX` INTEGER NOT NULL, `spanY` INTEGER NOT NULL, `minSpanX` INTEGER NOT NULL, `minSpanY` INTEGER NOT NULL, `rank` INTEGER NOT NULL, `requiresDbUpdate` INTEGER NOT NULL, `title` TEXT, `contentDescription` TEXT, `wasMovedDueToReducedSpace` INTEGER NOT NULL, `dropPos` TEXT, `user` TEXT, `unreadNum` INTEGER NOT NULL, `newInstalled` INTEGER NOT NULL, `calledNum` INTEGER NOT NULL, `lastCalledTime` INTEGER NOT NULL, `url` TEXT, `isHidden` INTEGER NOT NULL, `packageName` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2bfde018623897fc80f67154b1ab5a39')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `ItemInfo`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsItemInfo = new HashMap<String, TableInfo.Column>(24);
        _columnsItemInfo.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("itemType", new TableInfo.Column("itemType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("container", new TableInfo.Column("container", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("screenId", new TableInfo.Column("screenId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("cellX", new TableInfo.Column("cellX", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("cellY", new TableInfo.Column("cellY", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("spanX", new TableInfo.Column("spanX", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("spanY", new TableInfo.Column("spanY", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("minSpanX", new TableInfo.Column("minSpanX", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("minSpanY", new TableInfo.Column("minSpanY", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("rank", new TableInfo.Column("rank", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("requiresDbUpdate", new TableInfo.Column("requiresDbUpdate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("title", new TableInfo.Column("title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("contentDescription", new TableInfo.Column("contentDescription", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("wasMovedDueToReducedSpace", new TableInfo.Column("wasMovedDueToReducedSpace", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("dropPos", new TableInfo.Column("dropPos", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("user", new TableInfo.Column("user", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("unreadNum", new TableInfo.Column("unreadNum", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("newInstalled", new TableInfo.Column("newInstalled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("calledNum", new TableInfo.Column("calledNum", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("lastCalledTime", new TableInfo.Column("lastCalledTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("url", new TableInfo.Column("url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("isHidden", new TableInfo.Column("isHidden", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemInfo.put("packageName", new TableInfo.Column("packageName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysItemInfo = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesItemInfo = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoItemInfo = new TableInfo("ItemInfo", _columnsItemInfo, _foreignKeysItemInfo, _indicesItemInfo);
        final TableInfo _existingItemInfo = TableInfo.read(db, "ItemInfo");
        if (!_infoItemInfo.equals(_existingItemInfo)) {
          return new RoomOpenHelper.ValidationResult(false, "ItemInfo(com.amz.ios.launcher.ItemInfo).\n"
                  + " Expected:\n" + _infoItemInfo + "\n"
                  + " Found:\n" + _existingItemInfo);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2bfde018623897fc80f67154b1ab5a39", "43d7e230172c2391544bd09ec5fea32d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "ItemInfo");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `ItemInfo`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(HiddenAppDao.class, HiddenAppDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public HiddenAppDao databaseDao() {
    if (_hiddenAppDao != null) {
      return _hiddenAppDao;
    } else {
      synchronized(this) {
        if(_hiddenAppDao == null) {
          _hiddenAppDao = new HiddenAppDao_Impl(this);
        }
        return _hiddenAppDao;
      }
    }
  }
}
