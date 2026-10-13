package com.hcdc.qualipea;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanRepository {

    // One single thread for all database work, so operations stay in order.
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public interface DefectsCallback {
        void onResult(String selectedDefects);   // null = never ticked
    }

    public interface KilogramsCallback {
        void onResult(Double kilograms);   // null = nothing saved yet
    }

    /**
     * Saves locally (instant, works offline) and schedules the Firestore sync.
     * Returns the new scan's id, or null if nobody is signed in.
     */
    public static String save(Context context, int total, int good, int defect,
                              double defectRate, String grade, double inferenceTime,
                              Runnable onSaved) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;

        ScanEntity e = new ScanEntity();
        e.id = UUID.randomUUID().toString();
        e.uid = user.getUid();
        e.total = total;
        e.good = good;
        e.defect = defect;
        e.defectRate = defectRate;
        e.grade = grade;
        e.inferenceTime = inferenceTime;
        e.timestampMillis = System.currentTimeMillis();
        e.synced = false;
        e.selectedDefects = null;

        Context app = context.getApplicationContext();
        IO.execute(() -> {
            e.scanNumber = AppDatabase.get(app).scanDao().getMaxScanNumber(e.uid) + 1;
            AppDatabase.get(app).scanDao().insert(e);
            SyncScheduler.syncNow(app);
            if (onSaved != null) new Handler(app.getMainLooper()).post(onSaved);
        });
        return e.id;
    }

    /** Reads the ticked defects of one scan (null if the user was never asked). */
    public static void getSelectedDefects(Activity activity, String id, DefectsCallback callback) {
        Context app = activity.getApplicationContext();
        IO.execute(() -> {
            String saved = AppDatabase.get(app).scanDao().getSelectedDefects(id);
            activity.runOnUiThread(() -> callback.onResult(saved));
        });
    }

    /** Saves the ticked defects to the scan (an empty list is saved as ""). */
    public static void updateDefects(Context context, String id, List<String> defectIds) {
        String joined = TextUtils.join(",", defectIds);
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            AppDatabase.get(app).scanDao().updateDefects(id, joined);
            SyncScheduler.syncNow(app);
        });
    }

    /** Deletes locally (list updates automatically) and from Firestore if it was synced. */
    public static void delete(Context context, String uid, String id) {
        Context app = context.getApplicationContext();
        IO.execute(() -> AppDatabase.get(app).scanDao().delete(id));
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("scans").document(id)
                .delete();
    }

    /** Reads the kilograms saved for one scan (null if none). */
    public static void getKilograms(Activity activity, String id, KilogramsCallback callback) {
        Context app = activity.getApplicationContext();
        IO.execute(() -> {
            Double saved = AppDatabase.get(app).scanDao().getKilograms(id);
            activity.runOnUiThread(() -> callback.onResult(saved));
        });
    }

    /** Saves the kilograms to the scan (null clears it) and schedules the sync. */
    public static void updateKilograms(Context context, String id, Double kg) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            AppDatabase.get(app).scanDao().updateKilograms(id, kg);
            SyncScheduler.syncNow(app);
        });
    }
}