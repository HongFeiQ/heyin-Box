package com.github.tvbox.osc.cache;

import android.database.Cursor;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class StorageDriveDao_Impl implements StorageDriveDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StorageDrive> __insertionAdapterOfStorageDrive;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public StorageDriveDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStorageDrive = new EntityInsertionAdapter<StorageDrive>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `storageDrive` (`name`,`type`,`configJson`,`id`) VALUES (?,?,?,nullif(?, 0))";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, StorageDrive value) {
        if (value.name == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.name);
        }
        stmt.bindLong(2, value.type);
        if (value.configJson == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.configJson);
        }
        stmt.bindLong(4, value.getId());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "delete from storageDrive where `id`=?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final StorageDrive drive) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfStorageDrive.insertAndReturnId(drive);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final int id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDelete.release(_stmt);
    }
  }

  @Override
  public List<StorageDrive> getAll() {
    final String _sql = "select * from storageDrive order by id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfConfigJson = CursorUtil.getColumnIndexOrThrow(_cursor, "configJson");
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final List<StorageDrive> _result = new ArrayList<StorageDrive>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final StorageDrive _item;
        _item = new StorageDrive();
        if (_cursor.isNull(_cursorIndexOfName)) {
          _item.name = null;
        } else {
          _item.name = _cursor.getString(_cursorIndexOfName);
        }
        _item.type = _cursor.getInt(_cursorIndexOfType);
        if (_cursor.isNull(_cursorIndexOfConfigJson)) {
          _item.configJson = null;
        } else {
          _item.configJson = _cursor.getString(_cursorIndexOfConfigJson);
        }
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
