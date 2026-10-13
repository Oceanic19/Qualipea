package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

/** Shown after Splash when no user is currently signed in. */
public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        MaterialButton btnSignIn = findViewById(R.id.btnSignIn);
        MaterialButton btnSignUp = findViewById(R.id.btnSignUp);

        btnSignIn.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));

        UiAnim.staggerIn(
                findViewById(R.id.cardLogo),
                findViewById(R.id.tvBrand),
                findViewById(R.id.tvTagline),
                findViewById(R.id.rowPills),
                btnSignUp,
                btnSignIn,
                findViewById(R.id.tvFooter));
    }
}