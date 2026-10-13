package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail, etPassword, etConfirm;
    private TextInputLayout tilEmail, tilPassword, tilConfirm;
    private MaterialButton btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);
        btnSignup = findViewById(R.id.btnSignup);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        clearErrorOnType(etEmail, tilEmail);
        clearErrorOnType(etPassword, tilPassword);
        clearErrorOnType(etConfirm, tilConfirm);

        btnSignup.setOnClickListener(v -> attemptSignup());
        etConfirm.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSignup();
                return true;
            }
            return false;
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        UiAnim.staggerIn(
                findViewById(R.id.headerBlock),
                findViewById(R.id.cardForm),
                findViewById(R.id.rowSwitch));
    };

    private void attemptSignup() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        boolean ok = true;
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            ok = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            ok = false;
        }
        if (!password.equals(confirm)) {
            tilConfirm.setError("Passwords do not match");
            ok = false;
        }
        if (!ok) return;

        btnSignup.setEnabled(false);
        btnSignup.setText("Creating account\u2026");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    btnSignup.setText("Create Account");
                    String msg = AuthErrors.friendly(e, true);
                    if (AuthErrors.isNetwork(e)) {
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    } else if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException
                            || e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                        tilEmail.setError(msg);
                    } else if (e instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
                        tilPassword.setError(msg);
                    } else {
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearErrorOnType(EditText et, TextInputLayout til) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { til.setError(null); }
        });
    }
}