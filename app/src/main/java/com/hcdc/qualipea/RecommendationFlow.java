package com.hcdc.qualipea;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Decides what happens when the user taps "View Recommendations":
 *  - never ticked defects for this scan -> DefectSelectionActivity (saves the ticks)
 *  - already ticked                     -> straight to RecommendationsActivity
 */
public final class RecommendationFlow {

    private RecommendationFlow() {}

    public static void open(Activity activity, String scanId, int total, int good, int defect,
                            double defectRate, String grade, double inferenceTime) {
        Bundle data = new Bundle();
        data.putInt(ScanKeys.TOTAL, total);
        data.putInt(ScanKeys.GOOD, good);
        data.putInt(ScanKeys.DEFECT, defect);
        data.putDouble(ScanKeys.DEFECT_RATE, defectRate);
        data.putString(ScanKeys.GRADE, grade);
        data.putDouble(ScanKeys.INFERENCE_TIME, inferenceTime);
        data.putString(ScanKeys.DOC_ID, scanId);

        // No defects detected: nothing to tick.
        if (defect == 0) {
            startGuide(activity, data, new ArrayList<>());
            return;
        }

        // No saved scan (e.g. not signed in): can't remember, so just ask.
        if (scanId == null) {
            startSelection(activity, data);
            return;
        }

        ScanRepository.getSelectedDefects(activity, scanId, saved -> {
            if (saved == null) {
                startSelection(activity, data);
            } else {
                startGuide(activity, data, parse(saved));
            }
        });
    }

    private static void startSelection(Activity activity, Bundle data) {
        Intent intent = new Intent(activity, DefectSelectionActivity.class);
        intent.putExtras(data);
        activity.startActivity(intent);
    }

    private static void startGuide(Activity activity, Bundle data, ArrayList<String> selected) {
        Intent intent = new Intent(activity, RecommendationsActivity.class);
        intent.putExtras(data);
        intent.putStringArrayListExtra(ScanKeys.SELECTED_DEFECTS, selected);
        activity.startActivity(intent);
    }

    private static ArrayList<String> parse(String saved) {
        ArrayList<String> list = new ArrayList<>();
        if (saved.isEmpty()) return list;
        for (String s : saved.split(",")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        return list;
    }
}