package com.hcdc.qualipea;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard: slim Pi status, "Your Activity" (totals + Kernel Quality bar),
 * ONE Recent Activity card, and the Ready To Grade card. Reads from the local
 * Room database, so it works offline and refreshes when a scan is saved or deleted.
 *
 * Intro animations (staggered cards, count-up numbers, growing bar) play ONCE
 * per app launch, not every time the dashboard is shown.
 */
public class MainActivity extends AppCompatActivity {

    // static = survives this Activity being recreated (tab switches), so the intro
    // only plays on the first Home screen of an app launch.
    private static boolean introPlayed = false;
    private boolean statsIntroPending;

    // How many recent scans the Recent Activity card shows (1 = just the latest).
    private static final int MAX_RECENT = 1;

    private View statusDot;
    private TextView tvDeviceStatus;
    private TextView tvStatScans;
    private TextView tvStatKernels;
    private View activityBreakdown;
    private View barGood;
    private View barDefect;
    private TextView tvLegendGood;
    private TextView tvLegendDefect;
    private LinearLayout recentScansContainer;
    private View emptyRecent;
    private MaterialButton btnViewAllHistory;

    private ValueAnimator scansAnim;
    private ValueAnimator kernelsAnim;
    private ValueAnimator barAnim;
    private View dotGood;
    private View dotDefect;
    private static final int EMPTY_GRAY = 0xFFEADBC6;

    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
    private final SimpleDateFormat rowFmt =
            new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusDot = findViewById(R.id.statusDot);
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus);
        tvStatScans = findViewById(R.id.tvStatScans);
        tvStatKernels = findViewById(R.id.tvStatKernels);
        activityBreakdown = findViewById(R.id.activityBreakdown);
        barGood = findViewById(R.id.barGood);
        barDefect = findViewById(R.id.barDefect);
        tvLegendGood = findViewById(R.id.tvLegendGood);
        tvLegendDefect = findViewById(R.id.tvLegendDefect);
        dotGood = findViewById(R.id.dotGood);
        dotDefect = findViewById(R.id.dotDefect);
        recentScansContainer = findViewById(R.id.recentScansContainer);
        emptyRecent = findViewById(R.id.emptyRecent);
        btnViewAllHistory = findViewById(R.id.btnViewAllHistory);

        MaterialButton btnStartScan = findViewById(R.id.btnStartScan);
        btnStartScan.setOnClickListener(v ->
                startActivity(new Intent(this, ScanActivity.class)));

        // Same behaviour as the History tab in the bottom nav.
        btnViewAllHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });

        NavHelper.setup(this, findViewById(R.id.bottomNav), NavHelper.Tab.HOME);
        SyncScheduler.syncNow(this);

        // First Home screen of this app launch? Then play the intro animations.
        boolean playIntro = !introPlayed;
        introPlayed = true;
        statsIntroPending = playIntro;
        if (playIntro) playCardIntro();

        observeData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPiConnection();
    }

    @Override
    protected void onDestroy() {
        cancelStatAnimators();
        super.onDestroy();
    }

    private void checkPiConnection() {
        tvDeviceStatus.setText("Checking Raspberry Pi…");
        statusDot.setBackgroundResource(R.drawable.circle_gray);

        PiClient.checkReachable(reachable -> {
            if (reachable) {
                statusDot.setBackgroundResource(R.drawable.circle_green);
                tvDeviceStatus.setText("Raspberry Pi connected");
            } else {
                statusDot.setBackgroundResource(R.drawable.circle_red);
                tvDeviceStatus.setText("Raspberry Pi disconnected");
            }
        });
    }

    private void observeData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            renderStats(null);
            renderRecent(new ArrayList<>());
            return;
        }
        ScanDao dao = AppDatabase.get(this).scanDao();
        dao.observeStats(uid).observe(this, this::renderStats);
        dao.observeRecent(uid).observe(this, this::renderRecent);
    }

    // ------------------------------------------------------------------
    // Animations
    // ------------------------------------------------------------------

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    /** Cards fade in and slide up one after another. */
    private void playCardIntro() {
        View[] cards = {
                findViewById(R.id.cardActivity),
                findViewById(R.id.cardRecent),
                findViewById(R.id.cardReady)
        };
        float offset = dp(24);
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            card.setAlpha(0f);
            card.setTranslationY(offset);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 180L)
                    .setDuration(650)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    /** Counts a number up from 0 to target. */
    private ValueAnimator countUp(TextView tv, int target, long delay, long duration) {
        tv.setText("0");
        ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setStartDelay(delay);
        a.setDuration(duration);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> tv.setText(num((Integer) anim.getAnimatedValue())));
        a.start();
        return a;
    }

    /** Reveals the good/defective bar from left to right (no stretching). */
    private ValueAnimator growBar(long delay, long duration) {
        View bar = (View) barGood.getParent();
        bar.setClipBounds(new Rect(0, 0, 0, 0)); // hidden until the animation starts

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setStartDelay(delay);
        a.setDuration(duration);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            bar.setClipBounds(new Rect(0, 0, Math.round(bar.getWidth() * f), bar.getHeight()));
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bar.setClipBounds(null); // back to normal
            }
        });
        a.start();
        return a;
    }

    private void cancelStatAnimators() {
        if (scansAnim != null) scansAnim.cancel();
        if (kernelsAnim != null) kernelsAnim.cancel();
        if (barAnim != null) barAnim.cancel();
    }

    // ------------------------------------------------------------------
    // Your Activity
    // ------------------------------------------------------------------

    private String num(int n) {
        return String.format(Locale.US, "%,d", n);
    }

    private void setWeight(View v, float weight) {
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams();
        p.weight = weight;
        v.setLayoutParams(p);
    }

    private void renderStats(ScanStats s) {
        // Any later refresh (e.g. after a sync) just shows the final numbers.
        cancelStatAnimators();

        boolean animate = statsIntroPending;
        statsIntroPending = false;

        View track = (View) barGood.getParent();
        activityBreakdown.setVisibility(View.VISIBLE);   // always shown

        boolean empty = s == null || s.totalScans == 0 || (s.totalGood + s.totalDefects) == 0;

        if (empty) {
            // Default state: zeros and a gray bar.
            tvStatScans.setText("0");
            tvStatKernels.setText("0");

            setWeight(barGood, 1);
            setWeight(barDefect, 0);
            barGood.setBackgroundColor(EMPTY_GRAY);
            dotGood.setBackgroundTintList(android.content.res.ColorStateList.valueOf(EMPTY_GRAY));
            dotDefect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(EMPTY_GRAY));
            tvLegendGood.setText("Good  0 (0%)");
            tvLegendDefect.setText("Defective  0 (0%)");
            track.setClipBounds(null);
            return;
        }

        // Real data: restore the normal colors.
        barGood.setBackgroundColor(getResources().getColor(R.color.status_good));
        dotGood.setBackgroundTintList(null);
        dotDefect.setBackgroundTintList(null);

        if (animate) {
            scansAnim = countUp(tvStatScans, s.totalScans, 300, 1100);
            kernelsAnim = countUp(tvStatKernels, s.totalKernels, 300, 1100);
        } else {
            tvStatScans.setText(num(s.totalScans));
            tvStatKernels.setText(num(s.totalKernels));
        }

        // Good vs defective bar across ALL scans.
        int good = s.totalGood;
        int bad = s.totalDefects;
        setWeight(barGood, good);
        setWeight(barDefect, bad);
        int goodPct = Math.round(good * 100f / (good + bad));
        tvLegendGood.setText("Good  " + num(good) + " (" + goodPct + "%)");
        tvLegendDefect.setText("Defective  " + num(bad) + " (" + (100 - goodPct) + "%)");

        if (animate) {
            barAnim = growBar(600, 1200);
        } else {
            track.setClipBounds(null);
        }
    }

    // ------------------------------------------------------------------
    // Recent Activity
    // ------------------------------------------------------------------

    private String scanLabel(int number) {
        return String.format(Locale.US, "Scan #%04d", number);
    }

    private int gradeColor(String grade) {
        if ("Reject".equalsIgnoreCase(grade)) return getResources().getColor(R.color.delete_red);
        if ("Class II".equalsIgnoreCase(grade)) return getResources().getColor(R.color.brand_primary);
        return getResources().getColor(R.color.brand_primary);
    }

    private void renderRecent(List<ScanEntity> scans) {
        recentScansContainer.removeAllViews();

        if (scans == null || scans.isEmpty()) {
            emptyRecent.setVisibility(View.VISIBLE);
            btnViewAllHistory.setVisibility(View.INVISIBLE);
            return;
        }
        emptyRecent.setVisibility(View.GONE);
        btnViewAllHistory.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        int count = Math.min(scans.size(), MAX_RECENT);
        for (int i = 0; i < count; i++) {
            ScanEntity e = scans.get(i);
            View item = inflater.inflate(R.layout.item_scan_home, recentScansContainer, false);

            ImageView ivIcon = item.findViewById(R.id.ivHomeIcon);
            TextView tvScanNumber = item.findViewById(R.id.tvHomeScanNumber);
            TextView tvKernels = item.findViewById(R.id.tvHomeKernels);
            TextView tvGrade = item.findViewById(R.id.tvHomeGrade);
            TextView tvDate = item.findViewById(R.id.tvHomeDate);

            String grade = e.grade != null ? e.grade : "Unknown";
            int color = gradeColor(grade);

            ivIcon.setColorFilter(color);
            tvScanNumber.setText(scanLabel(e.scanNumber));
            tvKernels.setText(e.total + " kernels");
            tvGrade.setText(grade);
            tvGrade.setTextColor(color);
            tvDate.setText(rowFmt.format(e.timestampMillis));

            item.setContentDescription("View details: " + scanLabel(e.scanNumber) + ", "
                    + e.total + " kernels, " + grade + ", " + rowFmt.format(e.timestampMillis));
            item.setOnClickListener(v -> openDetail(e));

            recentScansContainer.addView(item);
        }
    }

    private void openDetail(ScanEntity e) {
        Intent intent = new Intent(this, ScanResultsActivity.class);
        intent.putExtra(ScanKeys.ALREADY_SAVED, true);
        intent.putExtra(ScanKeys.DOC_ID, e.id);
        intent.putExtra(ScanKeys.DATE, dateFmt.format(e.timestampMillis));
        intent.putExtra(ScanKeys.TOTAL, e.total);
        intent.putExtra(ScanKeys.GOOD, e.good);
        intent.putExtra(ScanKeys.DEFECT, e.defect);
        intent.putExtra(ScanKeys.DEFECT_RATE, e.defectRate);
        intent.putExtra(ScanKeys.GRADE, e.grade != null ? e.grade : "Unknown");
        intent.putExtra(ScanKeys.INFERENCE_TIME, e.inferenceTime);
        intent.putExtra(ScanKeys.SCAN_NUMBER, e.scanNumber);
        startActivity(intent);
    }
}