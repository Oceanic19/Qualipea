package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    // {step text, optional command shown in a code box}
    private static final String[][] STEPS = {
            {"Plug the ethernet cable of the imaging chamber into your laptop.", null},
            {"Switch on the power of the Raspberry Pi and wait about a minute for it to boot.", null},
            {"On your laptop, open Windows PowerShell.", null},
            {"Connect to the Pi with this command, then ask the project owners to type in the password:",
                    "ssh qualipearpi@192.168.137.31"},
            {"Let the owners start the camera stream, and keep the window open:",
                    "python3 camera_stream.py"},
            {"On your phone, connect to the Wi-Fi network \"qualipea\" (password: peanut123).", null},
            {"Go back to the Scan page. You can now grade freely!", null},
    };

    // {name, detail}
    private static final String[][] HARDWARE = {
            {"Raspberry Pi 4", "Quad-core ARM processor \u2022 controls the imaging device and talks to the app"},
            {"Raspberry Pi Camera Module 3", "12 MP \u2022 captures the peanut kernel images"},
            {"LED Strip Light", "Uniform white illumination inside the chamber"},
            {"Imaging Chamber", "Custom enclosure with a matte background"},
            {"microSD Card", "32 GB \u2022 stores the OS and program files"},
            {"Power Supply", "Official Raspberry Pi adapter"},
            {"Android Smartphone", "Android 10 or higher \u2022 runs the QualiPea app"},
    };

    private static final String[][] SOFTWARE = {
            {"Raspberry Pi OS", "Operating system of the imaging device"},
            {"Python 3.x", "Main programming language"},
            {"Ultralytics YOLO26n", "Peanut kernel detection model"},
            {"OpenCV", "Image processing"},
            {"Android Studio", "Mobile app development"},
            {"Firebase", "Authentication and database"},
    };

    private boolean specsOpen = false;

    private boolean troubleshootOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, OptionsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        buildTroubleshootSteps();
        findViewById(R.id.headerTroubleshoot).setOnClickListener(v -> toggleTroubleshoot());

        fillSpecs(R.id.hardwareContainer, HARDWARE);
        fillSpecs(R.id.softwareContainer, SOFTWARE);
        findViewById(R.id.headerSpecs).setOnClickListener(v -> {
            specsOpen = !specsOpen;
            setExpanded(R.id.specsContent, R.id.ivSpecsChevron, specsOpen);
        });

        NavHelper.setup(this, findViewById(R.id.bottomNav), NavHelper.Tab.SETTINGS);
        playIntro();
    }

    private void buildTroubleshootSteps() {
        LinearLayout container = findViewById(R.id.stepsContainer);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < STEPS.length; i++) {
            View row = inflater.inflate(R.layout.item_step, container, false);
            ((TextView) row.findViewById(R.id.tvStepNumber)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.tvStepText)).setText(STEPS[i][0]);

            String cmd = STEPS[i][1];
            TextView tvCode = row.findViewById(R.id.tvStepCode);
            if (cmd != null) {
                tvCode.setText(cmd);
                tvCode.setVisibility(View.VISIBLE);
            }
            container.addView(row);
        }
    }

    private void toggleTroubleshoot() {
        troubleshootOpen = !troubleshootOpen;
        setExpanded(R.id.troubleshootContent, R.id.ivTroubleshootChevron, troubleshootOpen);
    }

    private void setExpanded(int contentId, int chevronId, boolean open) {
        findViewById(contentId).setVisibility(open ? View.VISIBLE : View.GONE);
        findViewById(chevronId).animate().rotation(open ? 180f : 0f).setDuration(250).start();
    }

    private void fillSpecs(int containerId, String[][] items) {
        LinearLayout container = findViewById(containerId);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String[] item : items) {
            View row = inflater.inflate(R.layout.item_spec, container, false);
            ((TextView) row.findViewById(R.id.tvSpecName)).setText(item[0]);
            ((TextView) row.findViewById(R.id.tvSpecDetail)).setText(item[1]);
            container.addView(row);
        }
    }

    /** Cards fade/slide in one after another. */
    private void playIntro() {
        View[] cards = {
                findViewById(R.id.cardSpecs),
                findViewById(R.id.cardTroubleshoot),
                findViewById(R.id.cardAbout),
                findViewById(R.id.cardAccount)
        };
        float offset = 24 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < cards.length; i++) {
            cards[i].setAlpha(0f);
            cards[i].setTranslationY(offset);
            cards[i].animate().alpha(1f).translationY(0f)
                    .setStartDelay(i * 120L)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
}