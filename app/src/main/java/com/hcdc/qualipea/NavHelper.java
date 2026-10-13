package com.hcdc.qualipea;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;

/**
 * Wires up the shared bottom_nav.xml include. The second icon
 * (navSearch in the layout, visually a scan-corners icon) opens the
 * Scan screen directly - there's no separate Search feature.
 */
public final class NavHelper {

    public enum Tab { HOME, SCAN, HISTORY, SETTINGS }

    private NavHelper() {}

    public static void setup(Activity activity, View navRoot, Tab activeTab) {
        ImageButton navHome = navRoot.findViewById(R.id.navHome);
        ImageButton navScan = navRoot.findViewById(R.id.navSearch);
        ImageButton navHistory = navRoot.findViewById(R.id.navHistory);
        ImageButton navSettings = navRoot.findViewById(R.id.navSettings);

        tint(navHome, activeTab == Tab.HOME, activity);
        tint(navScan, activeTab == Tab.SCAN, activity);
        tint(navHistory, activeTab == Tab.HISTORY, activity);
        tint(navSettings, activeTab == Tab.SETTINGS, activity);

        navHome.setOnClickListener(v -> {
            if (activeTab != Tab.HOME) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
            }
        });

        navScan.setOnClickListener(v -> {
            if (activeTab != Tab.SCAN) {
                activity.startActivity(new Intent(activity, ScanActivity.class));
                activity.finish();
            }
        });

        navHistory.setOnClickListener(v -> {
            if (activeTab != Tab.HISTORY) {
                activity.startActivity(new Intent(activity, HistoryActivity.class));
                activity.finish();
            }
        });

        navSettings.setOnClickListener(v -> {
            if (activeTab != Tab.SETTINGS) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                activity.finish();
            }
        });
    }

    private static void tint(ImageButton button, boolean active, Activity activity) {
        int color = active
                ? activity.getResources().getColor(R.color.brand_primary)
                : activity.getResources().getColor(R.color.nav_inactive);
        button.setColorFilter(color);
    }
}
