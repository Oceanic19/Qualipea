package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Generic "Scan #NNN" detail screen. Populated entirely from Intent
 * extras (see ScanKeys) so the same Activity/layout serves every
 * entry in History without needing a screen per scan.
 */
public class ScanDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_detail);

        String title = getIntent().getStringExtra(ScanKeys.TITLE);
        String date = getIntent().getStringExtra(ScanKeys.DATE);
        int total = getIntent().getIntExtra(ScanKeys.TOTAL, 0);
        int good = getIntent().getIntExtra(ScanKeys.GOOD, 0);
        int damaged = getIntent().getIntExtra(ScanKeys.DAMAGED, 0);
        int moldy = getIntent().getIntExtra(ScanKeys.MOLDY, 0);
        String price = getIntent().getStringExtra(ScanKeys.PRICE);

        ((TextView) findViewById(R.id.tvTitle)).setText(title);
        ((TextView) findViewById(R.id.tvDate)).setText(date);
        ((TextView) findViewById(R.id.tvAnalyzedCount)).setText(total + " kernels analyzed");
        ((TextView) findViewById(R.id.tvGoodCount)).setText(good + " kernels");
        ((TextView) findViewById(R.id.tvDamagedCount)).setText(damaged + " kernel" + (damaged == 1 ? "" : "s"));
        ((TextView) findViewById(R.id.tvMoldyCount)).setText(moldy + " kernels");
        ((TextView) findViewById(R.id.tvPrice)).setText(price);

        MaterialButton btnViewRecommendations = findViewById(R.id.btnViewRecommendations);
        MaterialButton btnReturn = findViewById(R.id.btnReturn);

        btnViewRecommendations.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecommendationsActivity.class);
            intent.putExtra(ScanKeys.TOTAL, total);
            intent.putExtra(ScanKeys.GOOD, good);
            intent.putExtra(ScanKeys.DAMAGED, damaged);
            intent.putExtra(ScanKeys.MOLDY, moldy);
            startActivity(intent);
        });

        btnReturn.setOnClickListener(v -> finish());
    }
}
