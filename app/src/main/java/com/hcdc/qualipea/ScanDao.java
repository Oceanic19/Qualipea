package com.hcdc.qualipea;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ScanEntity e);

    @Query("SELECT * FROM scans WHERE uid = :uid ORDER BY timestampMillis DESC")
    LiveData<List<ScanEntity>> observeForUser(String uid);

    @Query("SELECT * FROM scans WHERE synced = 0")
    List<ScanEntity> getUnsynced();

    @Query("UPDATE scans SET synced = 1 WHERE id = :id")
    void markSynced(String id);

    @Query("DELETE FROM scans WHERE id = :id")
    void delete(String id);

    @Query("SELECT selectedDefects FROM scans WHERE id = :id")
    String getSelectedDefects(String id);

    // synced = 0 so the new ticks get uploaded to Firestore too
    @Query("UPDATE scans SET selectedDefects = :defects, synced = 0 WHERE id = :id")
    void updateDefects(String id, String defects);

    @Query("SELECT * FROM scans WHERE uid = :uid ORDER BY timestampMillis DESC LIMIT 3")
    LiveData<List<ScanEntity>> observeRecent(String uid);

    // Totals for the dashboard. COALESCE keeps the sums at 0 when there are no scans.
    @Query("SELECT COUNT(*) AS totalScans, "
            + "COALESCE(SUM(total), 0) AS totalKernels, "
            + "COALESCE(SUM(good), 0) AS totalGood, "
            + "COALESCE(SUM(defect), 0) AS totalDefects "
            + "FROM scans WHERE uid = :uid")
    LiveData<ScanStats> observeStats(String uid);

    @Query("SELECT COALESCE(MAX(scanNumber), 0) FROM scans WHERE uid = :uid")
    int getMaxScanNumber(String uid);

    @Query("SELECT id FROM scans WHERE uid = :uid")
    List<String> getIdsForUser(String uid);

    @Query("SELECT kilograms FROM scans WHERE id = :id")
    Double getKilograms(String id);

    // synced = 0 so the new weight gets uploaded to Firestore too
    @Query("UPDATE scans SET kilograms = :kg, synced = 0 WHERE id = :id")
    void updateKilograms(String id, Double kg);
}

