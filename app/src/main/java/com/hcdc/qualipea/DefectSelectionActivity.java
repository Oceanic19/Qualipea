package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 1 of the recommendations flow: "Check the following seen on
 * tray". The user ticks which defect types they can see; tapping a
 * thumbnail shows an example photo. Confirm sends the selection (plus
 * the scan data) on to RecommendationsActivity.
 */
public class DefectSelectionActivity extends AppCompatActivity {

    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private TextView tvSelectedCount;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defect_selection);

        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnConfirm = findViewById(R.id.btnConfirm);
        LinearLayout container = findViewById(R.id.defectListContainer);

        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
        findViewById(R.id.btnUncheckAll).setOnClickListener(v -> {
            for (CheckBox cb : checkBoxes) cb.setChecked(false);
        });
        btnConfirm.setOnClickListener(v -> confirm());

        LayoutInflater inflater = LayoutInflater.from(this);
        for (DefectType defect : DefectCatalog.ALL) {
            View row = inflater.inflate(R.layout.item_defect_checkbox, container, false);

            CheckBox cb = row.findViewById(R.id.cbDefect);
            TextView tvName = row.findViewById(R.id.tvDefectName);
            TextView tvSummary = row.findViewById(R.id.tvDefectSummary);
            ImageView ivThumb = row.findViewById(R.id.ivThumb);

            tvName.setText(defect.name);
            tvSummary.setText(defect.summary);
            ivThumb.setImageResource(defect.imageRes);
            ivThumb.setContentDescription("Show example of " + defect.name + " kernel");
            cb.setTag(defect.id);

            // Whole row toggles the checkbox (bigger touch target);
            // the thumbnail opens the example instead.
            row.setOnClickListener(v -> cb.toggle());
            ivThumb.setOnClickListener(v -> showExample(defect));
            cb.setOnCheckedChangeListener((button, checked) -> updateCount());

            checkBoxes.add(cb);
            container.addView(row);
        }
        updateCount();
    }

    private void updateCount() {
        int n = 0;
        for (CheckBox cb : checkBoxes) if (cb.isChecked()) n++;
        tvSelectedCount.setText(n + " selected");
        btnConfirm.setText(n > 0 ? "Confirm (" + n + ")" : "Confirm");
    }

    private void showExample(DefectType defect) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_defect_example, null);
        ((ImageView) view.findViewById(R.id.ivThumb)).setImageResource(defect.imageRes);
        ((TextView) view.findViewById(R.id.tvExampleName)).setText(defect.name);
        ((TextView) view.findViewById(R.id.tvExampleLookFor)).setText(defect.lookFor);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirm() {
        ArrayList<String> selected = new ArrayList<>();
        for (CheckBox cb : checkBoxes) {
            if (cb.isChecked()) selected.add((String) cb.getTag());
        }

        // Require at least one defect before moving on.
        if (selected.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Select a defect")
                    .setMessage("Please select at least one defect you see on the tray before proceeding.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Remember the ticks for this scan so we never ask again.
        String scanId = getIntent().getStringExtra(ScanKeys.DOC_ID);
        if (scanId != null) {
            ScanRepository.updateDefects(this, scanId, selected);
        }

        Intent intent = new Intent(this, RecommendationsActivity.class);
        intent.putExtras(getIntent()); // carries the scan numbers forward
        intent.putStringArrayListExtra(ScanKeys.SELECTED_DEFECTS, selected);
        startActivity(intent);
        finish();
    }
}
