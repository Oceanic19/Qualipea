    package com.hcdc.qualipea;

    import android.content.res.ColorStateList;
    import android.os.Bundle;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.transition.TransitionManager;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.WindowManager;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.LinearLayout;
    import android.widget.ProgressBar;
    import android.widget.TextView;
    import android.os.Handler;
    import android.os.Looper;
    import java.util.Objects;

    import androidx.appcompat.app.AppCompatActivity;

    import java.util.ArrayList;
    import java.util.Locale;

    /**
     * Step 2 of the recommendations flow: the actual Post-Harvest
     * Recommendations page. Shows overall quality, good-kernel advice,
     * expandable how-to cards for each defect the user ticked, the defect
     * rate, and a live price calculator.
     */
    public class RecommendationsActivity extends AppCompatActivity {

        private double defectRate;
        private String grade;
        private TextView tvPriceBreakdown;
        private TextView tvPriceTotal;
        private String scanId;
        private EditText etKg;
        private final Handler saveHandler = new Handler(Looper.getMainLooper());
        private Runnable pendingSave;
        private Double lastSavedKg;      // what is stored in the database right now
        private boolean fillingField;    // true while we fill the field ourselves

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_recommendations);
            // Keep the kg field visible above the keyboard.
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            int total = getIntent().getIntExtra(ScanKeys.TOTAL, 0);
            int good = getIntent().getIntExtra(ScanKeys.GOOD, 0);
            int defect = getIntent().getIntExtra(ScanKeys.DEFECT, 0);
            defectRate = getIntent().getDoubleExtra(ScanKeys.DEFECT_RATE, 0);
            grade = getIntent().getStringExtra(ScanKeys.GRADE);
            if (grade == null) grade = "Unknown";
            scanId = getIntent().getStringExtra(ScanKeys.DOC_ID);
            ArrayList<String> selected = getIntent().getStringArrayListExtra(ScanKeys.SELECTED_DEFECTS);

            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
            findViewById(R.id.btnDone).setOnClickListener(v -> finish());

            bindQuality(total, good);
            populateDefects(defect, selected);
            bindDefectRate();
            setUpPriceCalculator();
        }

        private int gradeColor() {
            if ("Reject".equalsIgnoreCase(grade)) return getResources().getColor(R.color.delete_red);
            if ("Class II".equalsIgnoreCase(grade)) return getResources().getColor(R.color.brand_dark);
            return getResources().getColor(R.color.brand_primary);
        }

        private String goodAdvice() {
            switch (grade) {
                case "Extra Class":
                    return "Excellent batch. These kernels are suitable for selling as premium whole kernels.";
                case "Class I":
                    return "Very good batch. Suitable for selling as whole kernels once the defects are removed.";
                case "Class II":
                    return "Acceptable batch. Remove the defects first to protect the selling price.";
                case "Reject":
                    return "Too many defects to sell this batch as it is. Re-sort it, remove the defects, then scan again.";
                default:
                    return "Keep these kernels separate from the defective ones.";
            }
        }

        private void bindQuality(int total, int good) {
            TextView tvGrade = findViewById(R.id.tvGrade);
            tvGrade.setText(grade);
            tvGrade.setTextColor(gradeColor());
            ((ImageView) findViewById(R.id.ivGradeIcon)).setColorFilter(gradeColor());

            ((TextView) findViewById(R.id.tvAnalyzed)).setText(total + " kernels analyzed");
            ((TextView) findViewById(R.id.tvGoodCount)).setText(good + " kernels");
            ((TextView) findViewById(R.id.tvGoodAdvice)).setText(goodAdvice());
        }

        private void populateDefects(int defectCount, ArrayList<String> selectedIds) {
            LinearLayout container = findViewById(R.id.defectsContainer);
            container.removeAllViews();

            if (defectCount == 0) {
                addNote(container, "No defects detected",
                        "Keep the kernels in a clean, sealed container in a cool, dry place.");
                return;
            }
            if (selectedIds == null || selectedIds.isEmpty()) {
                addNote(container, "No defect type selected",
                        "Separate the " + defectCount + " defective kernels from the good ones before selling. "
                                + "Go back and tick the defects you see to get step-by-step advice.");
                return;
            }

            View contentRoot = findViewById(R.id.contentRoot);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (String id : selectedIds) {
                DefectType defect = DefectCatalog.byId(id);
                if (defect == null) continue;

                View card = inflater.inflate(R.layout.item_defect_recommendation, container, false);
                View header = card.findViewById(R.id.header);
                View body = card.findViewById(R.id.body);
                ImageView chevron = card.findViewById(R.id.ivChevron);

                ((ImageView) card.findViewById(R.id.ivThumb)).setImageResource(defect.imageRes);
                ((TextView) card.findViewById(R.id.tvName)).setText(defect.name);
                ((TextView) card.findViewById(R.id.tvSummary)).setText(defect.summary);

                StringBuilder steps = new StringBuilder();
                for (int i = 0; i < defect.steps.length; i++) {
                    if (i > 0) steps.append("\n");
                    steps.append(i + 1).append(". ").append(defect.steps[i]);
                }
                ((TextView) card.findViewById(R.id.tvSteps)).setText(steps.toString());

                header.setOnClickListener(v -> {
                    boolean show = body.getVisibility() != View.VISIBLE;
                    TransitionManager.beginDelayedTransition((LinearLayout) contentRoot);
                    body.setVisibility(show ? View.VISIBLE : View.GONE);
                    chevron.animate().rotation(show ? 180f : 0f).setDuration(150).start();
                });

                container.addView(card);
            }
        }

        private void addNote(LinearLayout container, String title, String body) {
            View note = LayoutInflater.from(this).inflate(R.layout.item_note_card, container, false);
            ((TextView) note.findViewById(R.id.tvNoteTitle)).setText(title);
            ((TextView) note.findViewById(R.id.tvNoteBody)).setText(body);
            container.addView(note);
        }

        private void bindDefectRate() {
            TextView tvDefectRate = findViewById(R.id.tvDefectRate);
            tvDefectRate.setText(String.format(Locale.US, "%.2f%%", defectRate));
            tvDefectRate.setTextColor(gradeColor());

            ProgressBar bar = findViewById(R.id.rateBar);
            bar.setProgress((int) (Math.min(defectRate, 20.0) * 10)); // bar is full at 20%
            bar.setProgressTintList(ColorStateList.valueOf(gradeColor()));
        }

        private void setUpPriceCalculator() {
            etKg = findViewById(R.id.etKg);
            tvPriceBreakdown = findViewById(R.id.tvPriceBreakdown);
            tvPriceTotal = findViewById(R.id.tvPriceTotal);

            etKg.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    updatePrice(s.toString());
                    if (!fillingField) scheduleSave();
                }
            });
            updatePrice("");

            // Bring back the weight saved for THIS scan.
            if (scanId != null) {
                ScanRepository.getKilograms(this, scanId, kg -> {
                    if (isFinishing() || isDestroyed()) return;
                    lastSavedKg = kg;
                    if (kg != null && etKg.getText().length() == 0) {
                        fillingField = true;
                        etKg.setText(fieldText(kg));   // also refreshes the estimated total
                        fillingField = false;
                    }
                });
            }
        }

        private void updatePrice(String kgText) {
            double kg;
            try {
                kg = Double.parseDouble(kgText.trim());
            } catch (NumberFormatException e) {
                kg = 0;
            }

            if (kg <= 0) {
                tvPriceBreakdown.setText("Enter the weight to see an estimate.");
                tvPriceTotal.setText("\u20B10.00");
                return;
            }

            double base = PriceConfig.BASE_PRICE_PER_KG;
            double adjusted = base * (1 - defectRate / 100.0);
            double total = adjusted * kg;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.US, "Base price: \u20B1%,.2f / kg\n", base));
            sb.append(String.format(Locale.US, "Defect rate: \u2212%.2f%%\n", defectRate));
            sb.append(String.format(Locale.US, "Adjusted price: \u20B1%,.2f / kg \u00D7 %s kg", adjusted, trimKg(kg)));
            if ("Reject".equalsIgnoreCase(grade)) {
                sb.append("\nReject grade: this batch may sell for much less, or not at all, until re-sorted.");
            }
            tvPriceBreakdown.setText(sb.toString());
            tvPriceTotal.setText(String.format(Locale.US, "\u20B1%,.2f", total));
        }

        private String trimKg(double kg) {
            return kg == Math.floor(kg) ? String.valueOf((long) kg) : String.format(Locale.US, "%.2f", kg);
        }

        /** Waits until the user stops typing, then saves. */
        private void scheduleSave() {
            if (scanId == null) return;   // nobody signed in: nothing to save to
            if (pendingSave != null) saveHandler.removeCallbacks(pendingSave);
            pendingSave = this::saveKilograms;
            saveHandler.postDelayed(pendingSave, 700);
        }

        private void saveKilograms() {
            pendingSave = null;
            if (scanId == null) return;

            Double kg = parseKg(etKg.getText().toString());
            if (Objects.equals(kg, lastSavedKg)) return;   // nothing changed

            lastSavedKg = kg;
            ScanRepository.updateKilograms(this, scanId, kg);
        }

        private Double parseKg(String text) {
            try {
                double v = Double.parseDouble(text.trim());
                if (v > 0) return v;
            } catch (NumberFormatException ignored) {
            }
            return null;   // empty, invalid, or zero = no weight
        }

        /** 10.0 -> "10", 2.5 -> "2.5" */
        private String fieldText(double kg) {
            String s = String.format(Locale.US, "%.2f", kg);
            if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
            return s;
        }

        @Override
        protected void onPause() {
            super.onPause();
            // Leaving the page (back, Done, another app): save right away.
            if (pendingSave != null) {
                saveHandler.removeCallbacks(pendingSave);
                saveKilograms();
            }
        }

        @Override
        protected void onDestroy() {
            saveHandler.removeCallbacksAndMessages(null);
            super.onDestroy();
        }
    }
