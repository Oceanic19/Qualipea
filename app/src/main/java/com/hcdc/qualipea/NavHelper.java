
package com.hcdc.qualipea;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public final class NavHelper {

    public enum Tab {
        HOME, SCAN, HISTORY, SETTINGS
    }

    private NavHelper() {}

    public static void setup(Activity activity, View navRoot, Tab activeTab) {

        ImageButton navHome = navRoot.findViewById(R.id.navHome);
        ImageButton navScan = navRoot.findViewById(R.id.navSearch);
        ImageButton navHistory = navRoot.findViewById(R.id.navHistory);
        ImageButton navSettings = navRoot.findViewById(R.id.navSettings);

        TextView homeLabel = navRoot.findViewById(R.id.navHomeLabel);
        TextView scanLabel = navRoot.findViewById(R.id.navScanLabel);
        TextView historyLabel = navRoot.findViewById(R.id.navHistoryLabel);
        TextView settingsLabel = navRoot.findViewById(R.id.navSettingsLabel);

        tint(navHome, homeLabel, activeTab == Tab.HOME);
        tint(navScan, scanLabel, activeTab == Tab.SCAN);
        tint(navHistory, historyLabel, activeTab == Tab.HISTORY);
        tint(navSettings, settingsLabel, activeTab == Tab.SETTINGS);

        View homeItem = navRoot.findViewById(R.id.navHomeItem);
        View scanItem = navRoot.findViewById(R.id.navScanItem);
        View historyItem = navRoot.findViewById(R.id.navHistoryItem);
        View settingsItem = navRoot.findViewById(R.id.navSettingsItem);

        homeItem.setOnClickListener(v -> {
            if (activeTab != Tab.HOME) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
            }
        });

        scanItem.setOnClickListener(v -> {
            if (activeTab != Tab.SCAN) {
                activity.startActivity(new Intent(activity, ScanActivity.class));
                activity.finish();
            }
        });

        historyItem.setOnClickListener(v -> {
            if (activeTab != Tab.HISTORY) {
                activity.startActivity(new Intent(activity, HistoryActivity.class));
                activity.finish();
            }
        });

        settingsItem.setOnClickListener(v -> {
            if (activeTab != Tab.SETTINGS) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                activity.finish();
            }
        });
    }

    public static void handleBackToHome(AppCompatActivity activity) {

        activity.getOnBackPressedDispatcher().addCallback(
                activity,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        Intent intent = new Intent(activity, MainActivity.class);

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        activity.startActivity(intent);
                        activity.finish();
                    }
                }
        );
    }

    private static void tint(
            ImageButton button,
            TextView label,
            boolean active
    ) {

        int color = Color.parseColor(
                active ? "#2E7D32" : "#9E9E9E"
        );

        if (button != null) {
            button.setColorFilter(color);
        }

        if (label != null) {
            label.setTextColor(color);
        }
    }
}
