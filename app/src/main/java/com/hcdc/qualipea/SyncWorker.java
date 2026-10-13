package com.hcdc.qualipea;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        ScanDao dao = AppDatabase.get(getApplicationContext()).scanDao();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<ScanEntity> pending = dao.getUnsynced();

        for (ScanEntity e : pending) {
            if (e.uid == null) continue;

            Map<String, Object> data = new HashMap<>();
            data.put("total", e.total);
            data.put("good", e.good);
            data.put("defect", e.defect);
            data.put("defectRate", e.defectRate);
            data.put("grade", e.grade);
            data.put("inferenceTime", e.inferenceTime);
            data.put("timestamp", new Date(e.timestampMillis));
            data.put("selectedDefects", e.selectedDefects);
            data.put("scanNumber", e.scanNumber);

            if (e.kilograms != null) data.put("kilograms", e.kilograms);

            try {
                // Timeout is important: offline, Firestore writes wait forever
                Tasks.await(
                        db.collection("users").document(e.uid)
                                .collection("scans").document(e.id).set(data),
                        15, TimeUnit.SECONDS);
                dao.markSynced(e.id);
            } catch (Exception ex) {
                Log.w("SyncWorker", "Sync failed, will retry: " + ex.getMessage());
                return Result.retry();
            }
        }
        // Upload is done, now pull down anything this phone doesn't have yet
        try {
            restoreFromCloud(dao);
        } catch (Exception ex) {
            Log.w("SyncWorker", "Restore failed, will retry: " + ex.getMessage());
            return Result.retry();
        }
        return Result.success();
    }

    /** Downloads scans that exist in Firestore but not in Room (reinstall / second phone). */
    private void restoreFromCloud(ScanDao dao) throws Exception {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        QuerySnapshot snap = Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("scans")
                        .orderBy("timestamp")          // oldest first
                        .get(),
                20, TimeUnit.SECONDS);

        Set<String> have = new HashSet<>(dao.getIdsForUser(uid));
        int next = dao.getMaxScanNumber(uid) + 1;

        for (DocumentSnapshot d : snap.getDocuments()) {
            if (have.contains(d.getId())) continue;   // never overwrite local data

            ScanEntity e = new ScanEntity();
            e.id = d.getId();
            e.uid = uid;
            e.total = intOf(d, "total");
            e.good = intOf(d, "good");
            e.defect = intOf(d, "defect");
            e.defectRate = doubleOf(d, "defectRate");
            e.grade = d.getString("grade");
            e.inferenceTime = doubleOf(d, "inferenceTime");
            e.selectedDefects = d.getString("selectedDefects");
            e.synced = true;                          // it came from the cloud
            e.kilograms = d.getDouble("kilograms");   // null if the document has none

            Date ts = d.getDate("timestamp");
            e.timestampMillis = ts != null ? ts.getTime() : System.currentTimeMillis();

            // Use the stored number; if an old document has none, give it the next one
            int n = intOf(d, "scanNumber");
            e.scanNumber = n > 0 ? n : next;
            next = Math.max(next, e.scanNumber + 1);

            dao.insert(e);
        }
    }

    private static int intOf(DocumentSnapshot d, String field) {
        Long v = d.getLong(field);
        return v == null ? 0 : v.intValue();
    }

    private static double doubleOf(DocumentSnapshot d, String field) {
        Double v = d.getDouble(field);
        return v == null ? 0 : v;
    }
}