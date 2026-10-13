package com.hcdc.qualipea;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "scans")
public class ScanEntity {
    @PrimaryKey
    @NonNull
    public String id = "";          // UUID, also the Firestore document ID

    public String uid;              // Firebase user who owns the scan
    public int total;
    public int good;
    public int defect;
    public double defectRate;
    public String grade;
    public double inferenceTime;
    public long timestampMillis;

    public boolean synced;          // false until Firestore confirms

    // Ticked defect ids, comma-separated (e.g. "mouldy,shriveled").
    // null  = user was never asked yet
    // ""    = user was asked and ticked nothing
    public String selectedDefects;

    @ColumnInfo(defaultValue = "0")
    public int scanNumber;

    // Kilograms typed in the price calculator for this scan (null = not entered yet)
    public Double kilograms;
}