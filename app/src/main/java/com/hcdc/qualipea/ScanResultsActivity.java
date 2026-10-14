package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Details page for a saved scan (opened from History or the Home recent card). */
public class ScanResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_results);

        int total = getIntent().getIntExtra(ScanKeys.TOTAL, 0);
        int good = getIntent().getIntExtra(ScanKeys.GOOD, 0);
        int defect = getIntent().getIntExtra(ScanKeys.DEFECT, 0);
        double defectRate = getIntent().getDoubleExtra(ScanKeys.DEFECT_RATE, 0);
        String gradeExtra = getIntent().getStringExtra(ScanKeys.GRADE);
        final String grade = gradeExtra != null ? gradeExtra : "Unknown";
        double inferenceTime = getIntent().getDoubleExtra(ScanKeys.INFERENCE_TIME, 0);
        String savedDate = getIntent().getStringExtra(ScanKeys.DATE);
        String scanId = getIntent().getStringExtra(ScanKeys.DOC_ID);
        int scanNumber = getIntent().getIntExtra(ScanKeys.SCAN_NUMBER, 0);
        TextView tvTitle = findViewById(R.id.tvTitle);

        tvTitle.setText(scanNumber > 0
                ? String.format(Locale.US, "Scan #%04d", scanNumber)
                : "Scan Result");

        String when = savedDate != null ? savedDate
                : new SimpleDateFormat("MMMM d, yyyy \u2022 h:mm a", Locale.getDefault()).format(new Date());
        ((TextView) findViewById(R.id.tvTimestamp)).setText(when);

        // Overall quality
        int color = gradeColor(grade);
        TextView tvGrade = findViewById(R.id.tvGrade);
        ImageView ivIcon = findViewById(R.id.ivGradeIcon);
        tvGrade.setText(grade);
        tvGrade.setTextColor(color);
        ivIcon.setColorFilter(color);
        boolean positive = !"Reject".equalsIgnoreCase(grade) && !"Class II".equalsIgnoreCase(grade);
        ivIcon.setImageResource(positive ? R.drawable.ic_check_circle : R.drawable.ic_warning_circle);

        ((TextView) findViewById(R.id.tvAnalyzedCount)).setText(total + " kernels analyzed");
        ((TextView) findViewById(R.id.tvInferenceTime)).setText(
                String.format(Locale.getDefault(), "Inference time: %.2fs", inferenceTime));

        // Breakdown
        int sum = good + defect;
        int goodPct = sum == 0 ? 0 : Math.round(good * 100f / sum);
        ((TextView) findViewById(R.id.tvGoodCount)).setText(
                String.format(Locale.getDefault(), "%d kernels \u2022 %d%%", good, goodPct));
        ((TextView) findViewById(R.id.tvDefectCount)).setText(
                String.format(Locale.getDefault(), "%d kernels \u2022 %.2f%%", defect, defectRate));
        ((DonutChartView) findViewById(R.id.donutChart)).setData(good, defect);

        // Inspection summary (short explanation)
        ((TextView) findViewById(R.id.tvSummaryText)).setText(
                buildSummary(total, good, defect, defectRate, grade, inferenceTime));

        // Buttons
        MaterialButton btnRecs = findViewById(R.id.btnViewRecommendations);
        btnRecs.setOnClickListener(v ->
                RecommendationFlow.open(this, scanId, total, good, defect, defectRate,
                        grade, inferenceTime));

        findViewById(R.id.btnReturnHistory).setOnClickListener(v -> {
            Intent i = new Intent(this, HistoryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });

        playIntro();
    }

    /** Plain-language explanation of the scan, with key numbers in bold. */
    private CharSequence buildSummary(int total, int good, int defect, double rate,
                                      String grade, double inferenceTime) {
        int sum = good + defect;
        float goodPct = sum == 0 ? 0f : good * 100f / sum;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "Out of <b>%d</b> kernels analyzed, <b>%d</b> (%.1f%%) passed inspection",
                total, good, goodPct));

        if (defect == 0) {
            sb.append(" and no defects were found. ");
        } else {
            sb.append(String.format(Locale.US,
                    " and <b>%d</b> showed visible defects (<b>%.2f%%</b> defect rate). ",
                    defect, rate));
        }

        if ("Reject".equalsIgnoreCase(grade)) {
            sb.append("The defect level is too high, so this batch is <b>not suitable</b> "
                    + "for sale until it is re-sorted. ");
        } else if ("Class II".equalsIgnoreCase(grade)) {
            sb.append("A noticeable share of defects was found, so removing the defective "
                    + "kernels before packing is recommended. ");
        } else if (defect == 0) {
            sb.append("This is a clean batch of <b>" + grade + "</b> quality, ready for packing. ");
        } else {
            sb.append("The defect level is low and the batch grades as <b>" + grade
                    + "</b>. Removing the defective kernels would improve it further. ");
        }

        sb.append(String.format(Locale.US, "The analysis took %.2f seconds.", inferenceTime));
        return HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    private int gradeColor(String grade) {
        if ("Reject".equalsIgnoreCase(grade)) return getResources().getColor(R.color.delete_red);
        if ("Class II".equalsIgnoreCase(grade)) return getResources().getColor(R.color.brand_primary);
        return getResources().getColor(R.color.brand_primary);
    }

    /** Cards fade/slide in one after another. */
    private void playIntro() {
        View[] cards = {
                findViewById(R.id.cardOverall),
                findViewById(R.id.cardBreakdown),
                findViewById(R.id.cardSummary)
        };
        float offset = 24 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < cards.length; i++) {
            cards[i].setAlpha(0f);
            cards[i].setTranslationY(offset);
            cards[i].animate().alpha(1f).translationY(0f)
                    .setStartDelay(i * 160L)
                    .setDuration(550)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
}