package com.hcdc.qualipea;

/**
 * One saved scan, loaded from Firestore at
 * users/{uid}/scans/{id}
 */
public class ScanRecord {
    public final String id;
    public final int scanNumber;
    public final int total;
    public final int good;
    public final int defect;
    public final double defectRate;
    public final String grade;
    public final double inferenceTime;
    public final long timestampMillis;

    public ScanRecord(String id, int scanNumber, int total, int good, int defect, double defectRate,
                      String grade, double inferenceTime, long timestampMillis) {
        this.id = id;
        this.scanNumber = scanNumber;
        this.total = total;
        this.good = good;
        this.defect = defect;
        this.defectRate = defectRate;
        this.grade = grade;
        this.inferenceTime = inferenceTime;
        this.timestampMillis = timestampMillis;
    }
}
