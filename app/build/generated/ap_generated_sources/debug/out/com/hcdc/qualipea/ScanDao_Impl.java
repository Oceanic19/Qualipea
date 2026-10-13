package com.hcdc.qualipea;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ScanDao_Impl implements ScanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ScanEntity> __insertionAdapterOfScanEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDefects;

  private final SharedSQLiteStatement __preparedStmtOfUpdateKilograms;

  public ScanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScanEntity = new EntityInsertionAdapter<ScanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `scans` (`id`,`uid`,`total`,`good`,`defect`,`defectRate`,`grade`,`inferenceTime`,`timestampMillis`,`synced`,`selectedDefects`,`scanNumber`,`kilograms`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final ScanEntity entity) {
        if (entity.id == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.id);
        }
        if (entity.uid == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.uid);
        }
        statement.bindLong(3, entity.total);
        statement.bindLong(4, entity.good);
        statement.bindLong(5, entity.defect);
        statement.bindDouble(6, entity.defectRate);
        if (entity.grade == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.grade);
        }
        statement.bindDouble(8, entity.inferenceTime);
        statement.bindLong(9, entity.timestampMillis);
        final int _tmp = entity.synced ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.selectedDefects == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.selectedDefects);
        }
        statement.bindLong(12, entity.scanNumber);
        if (entity.kilograms == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.kilograms);
        }
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE scans SET synced = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM scans WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateDefects = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE scans SET selectedDefects = ?, synced = 0 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateKilograms = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE scans SET kilograms = ?, synced = 0 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(final ScanEntity e) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfScanEntity.insert(e);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void markSynced(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfMarkSynced.release(_stmt);
    }
  }

  @Override
  public void delete(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDelete.release(_stmt);
    }
  }

  @Override
  public void updateDefects(final String id, final String defects) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDefects.acquire();
    int _argIndex = 1;
    if (defects == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, defects);
    }
    _argIndex = 2;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateDefects.release(_stmt);
    }
  }

  @Override
  public void updateKilograms(final String id, final Double kg) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateKilograms.acquire();
    int _argIndex = 1;
    if (kg == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindDouble(_argIndex, kg);
    }
    _argIndex = 2;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateKilograms.release(_stmt);
    }
  }

  @Override
  public LiveData<List<ScanEntity>> observeForUser(final String uid) {
    final String _sql = "SELECT * FROM scans WHERE uid = ? ORDER BY timestampMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (uid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, uid);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"scans"}, false, new Callable<List<ScanEntity>>() {
      @Override
      @Nullable
      public List<ScanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "total");
          final int _cursorIndexOfGood = CursorUtil.getColumnIndexOrThrow(_cursor, "good");
          final int _cursorIndexOfDefect = CursorUtil.getColumnIndexOrThrow(_cursor, "defect");
          final int _cursorIndexOfDefectRate = CursorUtil.getColumnIndexOrThrow(_cursor, "defectRate");
          final int _cursorIndexOfGrade = CursorUtil.getColumnIndexOrThrow(_cursor, "grade");
          final int _cursorIndexOfInferenceTime = CursorUtil.getColumnIndexOrThrow(_cursor, "inferenceTime");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSelectedDefects = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedDefects");
          final int _cursorIndexOfScanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "scanNumber");
          final int _cursorIndexOfKilograms = CursorUtil.getColumnIndexOrThrow(_cursor, "kilograms");
          final List<ScanEntity> _result = new ArrayList<ScanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanEntity _item;
            _item = new ScanEntity();
            if (_cursor.isNull(_cursorIndexOfId)) {
              _item.id = null;
            } else {
              _item.id = _cursor.getString(_cursorIndexOfId);
            }
            if (_cursor.isNull(_cursorIndexOfUid)) {
              _item.uid = null;
            } else {
              _item.uid = _cursor.getString(_cursorIndexOfUid);
            }
            _item.total = _cursor.getInt(_cursorIndexOfTotal);
            _item.good = _cursor.getInt(_cursorIndexOfGood);
            _item.defect = _cursor.getInt(_cursorIndexOfDefect);
            _item.defectRate = _cursor.getDouble(_cursorIndexOfDefectRate);
            if (_cursor.isNull(_cursorIndexOfGrade)) {
              _item.grade = null;
            } else {
              _item.grade = _cursor.getString(_cursorIndexOfGrade);
            }
            _item.inferenceTime = _cursor.getDouble(_cursorIndexOfInferenceTime);
            _item.timestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _item.synced = _tmp != 0;
            if (_cursor.isNull(_cursorIndexOfSelectedDefects)) {
              _item.selectedDefects = null;
            } else {
              _item.selectedDefects = _cursor.getString(_cursorIndexOfSelectedDefects);
            }
            _item.scanNumber = _cursor.getInt(_cursorIndexOfScanNumber);
            if (_cursor.isNull(_cursorIndexOfKilograms)) {
              _item.kilograms = null;
            } else {
              _item.kilograms = _cursor.getDouble(_cursorIndexOfKilograms);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<ScanEntity> getUnsynced() {
    final String _sql = "SELECT * FROM scans WHERE synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
      final int _cursorIndexOfTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "total");
      final int _cursorIndexOfGood = CursorUtil.getColumnIndexOrThrow(_cursor, "good");
      final int _cursorIndexOfDefect = CursorUtil.getColumnIndexOrThrow(_cursor, "defect");
      final int _cursorIndexOfDefectRate = CursorUtil.getColumnIndexOrThrow(_cursor, "defectRate");
      final int _cursorIndexOfGrade = CursorUtil.getColumnIndexOrThrow(_cursor, "grade");
      final int _cursorIndexOfInferenceTime = CursorUtil.getColumnIndexOrThrow(_cursor, "inferenceTime");
      final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
      final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
      final int _cursorIndexOfSelectedDefects = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedDefects");
      final int _cursorIndexOfScanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "scanNumber");
      final int _cursorIndexOfKilograms = CursorUtil.getColumnIndexOrThrow(_cursor, "kilograms");
      final List<ScanEntity> _result = new ArrayList<ScanEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final ScanEntity _item;
        _item = new ScanEntity();
        if (_cursor.isNull(_cursorIndexOfId)) {
          _item.id = null;
        } else {
          _item.id = _cursor.getString(_cursorIndexOfId);
        }
        if (_cursor.isNull(_cursorIndexOfUid)) {
          _item.uid = null;
        } else {
          _item.uid = _cursor.getString(_cursorIndexOfUid);
        }
        _item.total = _cursor.getInt(_cursorIndexOfTotal);
        _item.good = _cursor.getInt(_cursorIndexOfGood);
        _item.defect = _cursor.getInt(_cursorIndexOfDefect);
        _item.defectRate = _cursor.getDouble(_cursorIndexOfDefectRate);
        if (_cursor.isNull(_cursorIndexOfGrade)) {
          _item.grade = null;
        } else {
          _item.grade = _cursor.getString(_cursorIndexOfGrade);
        }
        _item.inferenceTime = _cursor.getDouble(_cursorIndexOfInferenceTime);
        _item.timestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfSynced);
        _item.synced = _tmp != 0;
        if (_cursor.isNull(_cursorIndexOfSelectedDefects)) {
          _item.selectedDefects = null;
        } else {
          _item.selectedDefects = _cursor.getString(_cursorIndexOfSelectedDefects);
        }
        _item.scanNumber = _cursor.getInt(_cursorIndexOfScanNumber);
        if (_cursor.isNull(_cursorIndexOfKilograms)) {
          _item.kilograms = null;
        } else {
          _item.kilograms = _cursor.getDouble(_cursorIndexOfKilograms);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public String getSelectedDefects(final String id) {
    final String _sql = "SELECT selectedDefects FROM scans WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final String _result;
      if (_cursor.moveToFirst()) {
        if (_cursor.isNull(0)) {
          _result = null;
        } else {
          _result = _cursor.getString(0);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<ScanEntity>> observeRecent(final String uid) {
    final String _sql = "SELECT * FROM scans WHERE uid = ? ORDER BY timestampMillis DESC LIMIT 3";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (uid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, uid);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"scans"}, false, new Callable<List<ScanEntity>>() {
      @Override
      @Nullable
      public List<ScanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "total");
          final int _cursorIndexOfGood = CursorUtil.getColumnIndexOrThrow(_cursor, "good");
          final int _cursorIndexOfDefect = CursorUtil.getColumnIndexOrThrow(_cursor, "defect");
          final int _cursorIndexOfDefectRate = CursorUtil.getColumnIndexOrThrow(_cursor, "defectRate");
          final int _cursorIndexOfGrade = CursorUtil.getColumnIndexOrThrow(_cursor, "grade");
          final int _cursorIndexOfInferenceTime = CursorUtil.getColumnIndexOrThrow(_cursor, "inferenceTime");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSelectedDefects = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedDefects");
          final int _cursorIndexOfScanNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "scanNumber");
          final int _cursorIndexOfKilograms = CursorUtil.getColumnIndexOrThrow(_cursor, "kilograms");
          final List<ScanEntity> _result = new ArrayList<ScanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanEntity _item;
            _item = new ScanEntity();
            if (_cursor.isNull(_cursorIndexOfId)) {
              _item.id = null;
            } else {
              _item.id = _cursor.getString(_cursorIndexOfId);
            }
            if (_cursor.isNull(_cursorIndexOfUid)) {
              _item.uid = null;
            } else {
              _item.uid = _cursor.getString(_cursorIndexOfUid);
            }
            _item.total = _cursor.getInt(_cursorIndexOfTotal);
            _item.good = _cursor.getInt(_cursorIndexOfGood);
            _item.defect = _cursor.getInt(_cursorIndexOfDefect);
            _item.defectRate = _cursor.getDouble(_cursorIndexOfDefectRate);
            if (_cursor.isNull(_cursorIndexOfGrade)) {
              _item.grade = null;
            } else {
              _item.grade = _cursor.getString(_cursorIndexOfGrade);
            }
            _item.inferenceTime = _cursor.getDouble(_cursorIndexOfInferenceTime);
            _item.timestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _item.synced = _tmp != 0;
            if (_cursor.isNull(_cursorIndexOfSelectedDefects)) {
              _item.selectedDefects = null;
            } else {
              _item.selectedDefects = _cursor.getString(_cursorIndexOfSelectedDefects);
            }
            _item.scanNumber = _cursor.getInt(_cursorIndexOfScanNumber);
            if (_cursor.isNull(_cursorIndexOfKilograms)) {
              _item.kilograms = null;
            } else {
              _item.kilograms = _cursor.getDouble(_cursorIndexOfKilograms);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<ScanStats> observeStats(final String uid) {
    final String _sql = "SELECT COUNT(*) AS totalScans, COALESCE(SUM(total), 0) AS totalKernels, COALESCE(SUM(good), 0) AS totalGood, COALESCE(SUM(defect), 0) AS totalDefects FROM scans WHERE uid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (uid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, uid);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"scans"}, false, new Callable<ScanStats>() {
      @Override
      @Nullable
      public ScanStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalScans = 0;
          final int _cursorIndexOfTotalKernels = 1;
          final int _cursorIndexOfTotalGood = 2;
          final int _cursorIndexOfTotalDefects = 3;
          final ScanStats _result;
          if (_cursor.moveToFirst()) {
            _result = new ScanStats();
            _result.totalScans = _cursor.getInt(_cursorIndexOfTotalScans);
            _result.totalKernels = _cursor.getInt(_cursorIndexOfTotalKernels);
            _result.totalGood = _cursor.getInt(_cursorIndexOfTotalGood);
            _result.totalDefects = _cursor.getInt(_cursorIndexOfTotalDefects);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public int getMaxScanNumber(final String uid) {
    final String _sql = "SELECT COALESCE(MAX(scanNumber), 0) FROM scans WHERE uid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (uid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, uid);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<String> getIdsForUser(final String uid) {
    final String _sql = "SELECT id FROM scans WHERE uid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (uid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, uid);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final String _item;
        if (_cursor.isNull(0)) {
          _item = null;
        } else {
          _item = _cursor.getString(0);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Double getKilograms(final String id) {
    final String _sql = "SELECT kilograms FROM scans WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final Double _result;
      if (_cursor.moveToFirst()) {
        if (_cursor.isNull(0)) {
          _result = null;
        } else {
          _result = _cursor.getDouble(0);
        }
      } else {
        _result = null;
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
