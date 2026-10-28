
package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        MaterialButton btnProceed = findViewById(R.id.btnProceed);

        btnProceed.setOnClickListener(v -> {
            Intent intent = new Intent(
                    OptionsActivity.this,
                    MainActivity.class
            );
            startActivity(intent);
        });

        UiAnim.staggerIn(
                findViewById(R.id.cardLogo),
                findViewById(R.id.tvBrand),
                findViewById(R.id.tvTagline),
                findViewById(R.id.rowPills),
                btnProceed,
                findViewById(R.id.tvFooter)
        );
    }
}
