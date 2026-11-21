
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
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Intro animation plays only once per app launch.
    private static boolean introPlayed = false;
    private boolean statsIntroPending;

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

        // Exit confirmation.
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("Exit QualiPea?")
                                .setMessage("Are you sure you want to close the app?")
                                .setNegativeButton("Cancel", (dialog, which) ->
                                        dialog.dismiss())
                                .setPositiveButton("Yes", (dialog, which) ->
                                        finishAffinity())
                                .setCancelable(true)
                                .show();
                    }
                });

        // Existing dashboard views.
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

        // Start Scan button.
        MaterialButton btnStartScan = findViewById(R.id.btnStartScan);
        btnStartScan.setOnClickListener(v ->
                startActivity(new Intent(this, ScanActivity.class)));

        // View History button.
        btnViewAllHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });

        // NEW: Set up the four independent reference image carousels.
        setupReferenceCarousels();

        // Bottom navigation.
        NavHelper.setup(this, findViewById(R.id.bottomNav), NavHelper.Tab.HOME);

        SyncScheduler.syncNow(this);

        // Intro animations.
        boolean playIntro = !introPlayed;
        introPlayed = true;
        statsIntroPending = playIntro;

        if (playIntro) {
            playCardIntro();
        }

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

    // ------------------------------------------------------------------
    // Raspberry Pi connection
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // NEW: Peanut Quality Reference Guide carousels
    // ------------------------------------------------------------------

    private void setupReferenceCarousels() {

        // Extra Class images.
        setupImageCarousel(
                findViewById(R.id.ivExtraClass),
                findViewById(R.id.tvExtraCounter),
                findViewById(R.id.btnExtraPrev),
                findViewById(R.id.btnExtraNext),
                "Extra Class",
                new int[]{
                        R.drawable.logo_peanut,
                        R.drawable.broken_kernel,
                        R.drawable.discolored_kernel,
                        R.drawable.shriveled_kernel
                }
        );

        // Class I images.
        setupImageCarousel(
                findViewById(R.id.ivClassOne),
                findViewById(R.id.tvClassOneCounter),
                findViewById(R.id.btnClassOnePrev),
                findViewById(R.id.btnClassOneNext),
                "Class I",
                new int[]{
                        R.drawable.broken_kernel,
                        R.drawable.discolored_kernel,
                        R.drawable.shriveled_kernel,
                        R.drawable.logo_peanut
                }
        );

        // Class II images.
        setupImageCarousel(
                findViewById(R.id.ivClassTwo),
                findViewById(R.id.tvClassTwoCounter),
                findViewById(R.id.btnClassTwoPrev),
                findViewById(R.id.btnClassTwoNext),
                "Class II",
                new int[]{
                        R.drawable.discolored_kernel,
                        R.drawable.shriveled_kernel,
                        R.drawable.broken_kernel,
                        R.drawable.logo_peanut
                }
        );

        // Defected images.
        setupImageCarousel(
                findViewById(R.id.ivDefected),
                findViewById(R.id.tvDefectedCounter),
                findViewById(R.id.btnDefectedPrev),
                findViewById(R.id.btnDefectedNext),
                "Defected",
                new int[]{
                        R.drawable.defect_moldy,
                        R.drawable.defect_insect,
                        R.drawable.defect_damaged,
                        R.drawable.defect_shriveled
                }
        );
    }

    /**
     * Sets up one independent image carousel.
     * Each call has its own current image index.
     */
    private void setupImageCarousel(
            ImageView imageView,
            TextView counter,
            MaterialButton previousButton,
            MaterialButton nextButton,
            String category,
            int[] images) {

        final int[] currentIndex = {0};

        // Displays the current image and counter.
        Runnable updateCarousel = () -> {
            imageView.setImageResource(images[currentIndex[0]]);

            counter.setText(
                    (currentIndex[0] + 1) + "/" + images.length
            );

            imageView.setContentDescription(
                    category + " sample image "
                            + (currentIndex[0] + 1)
                            + " of " + images.length
            );
        };

        // Previous image.
        previousButton.setOnClickListener(v -> {
            currentIndex[0]--;

            if (currentIndex[0] < 0) {
                currentIndex[0] = images.length - 1;
            }

            updateCarousel.run();
        });

        // Next image.
        nextButton.setOnClickListener(v -> {
            currentIndex[0]++;

            if (currentIndex[0] >= images.length) {
                currentIndex[0] = 0;
            }

            updateCarousel.run();
        });

        // Show the first image initially.
        updateCarousel.run();
    }

    // ------------------------------------------------------------------
    // Database observation
    // ------------------------------------------------------------------

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
                findViewById(R.id.cardReferenceGuide),
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
    private ValueAnimator countUp(
            TextView tv,
            int target,
            long delay,
            long duration) {

        tv.setText("0");

        ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setStartDelay(delay);
        a.setDuration(duration);
        a.setInterpolator(new DecelerateInterpolator());

        a.addUpdateListener(anim ->
                tv.setText(num((Integer) anim.getAnimatedValue())));

        a.start();
        return a;
    }

    /** Reveals the good/defective bar from left to right. */
    private ValueAnimator growBar(long delay, long duration) {
        View bar = (View) barGood.getParent();

        bar.setClipBounds(new Rect(0, 0, 0, 0));

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setStartDelay(delay);
        a.setDuration(duration);
        a.setInterpolator(new DecelerateInterpolator());

        a.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();

            bar.setClipBounds(new Rect(
                    0,
                    0,
                    Math.round(bar.getWidth() * f),
                    bar.getHeight()
            ));
        });

        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bar.setClipBounds(null);
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
        LinearLayout.LayoutParams p =
                (LinearLayout.LayoutParams) v.getLayoutParams();

        p.weight = weight;
        v.setLayoutParams(p);
    }

    private void renderStats(ScanStats s) {

        cancelStatAnimators();

        boolean animate = statsIntroPending;
        statsIntroPending = false;

        View track = (View) barGood.getParent();

        activityBreakdown.setVisibility(View.VISIBLE);

        boolean empty = s == null
                || s.totalScans == 0
                || (s.totalGood + s.totalDefects) == 0;

        if (empty) {
            tvStatScans.setText("0");
            tvStatKernels.setText("0");

            setWeight(barGood, 1);
            setWeight(barDefect, 0);

            barGood.setBackgroundColor(EMPTY_GRAY);

            dotGood.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(EMPTY_GRAY));

            dotDefect.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(EMPTY_GRAY));

            tvLegendGood.setText("Good  0 (0%)");
            tvLegendDefect.setText("Defective  0 (0%)");

            track.setClipBounds(null);
            return;
        }

        barGood.setBackgroundColor(
                getResources().getColor(R.color.status_good));

        dotGood.setBackgroundTintList(null);
        dotDefect.setBackgroundTintList(null);

        if (animate) {
            scansAnim = countUp(tvStatScans, s.totalScans, 300, 1100);
            kernelsAnim = countUp(tvStatKernels, s.totalKernels, 300, 1100);
        } else {
            tvStatScans.setText(num(s.totalScans));
            tvStatKernels.setText(num(s.totalKernels));
        }

        int good = s.totalGood;
        int bad = s.totalDefects;

        setWeight(barGood, good);
        setWeight(barDefect, bad);

        int goodPct = Math.round(good * 100f / (good + bad));

        tvLegendGood.setText(
                "Good  " + num(good) + " (" + goodPct + "%)");

        tvLegendDefect.setText(
                "Defective  " + num(bad) + " (" + (100 - goodPct) + "%)");

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
        if ("Reject".equalsIgnoreCase(grade)) {
            return getResources().getColor(R.color.delete_red);
        }

        if ("Class II".equalsIgnoreCase(grade)) {
            return getResources().getColor(R.color.brand_primary);
        }

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

            View item = inflater.inflate(
                    R.layout.item_scan_home,
                    recentScansContainer,
                    false
            );

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

            item.setContentDescription(
                    "View details: " + scanLabel(e.scanNumber)
                            + ", " + e.total + " kernels, "
                            + grade + ", "
                            + rowFmt.format(e.timestampMillis)
            );

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
        intent.putExtra(
                ScanKeys.GRADE,
                e.grade != null ? e.grade : "Unknown"
        );
        intent.putExtra(ScanKeys.INFERENCE_TIME, e.inferenceTime);
        intent.putExtra(ScanKeys.SCAN_NUMBER, e.scanNumber);

        startActivity(intent);
    }
}
