package com.hcdc.qualipea;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Splash: logo and wordmark ease in, then the app opens Home (signed in) or
 * Sign In / Sign Up (signed out). Tapping the screen skips the wait.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1800;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable proceed = this::goNext;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        playIntro();

        findViewById(R.id.splashRoot).setOnClickListener(v -> goNext());
        handler.postDelayed(proceed, SPLASH_DELAY_MS);
    }

    private void goNext() {
        if (navigated) return;
        navigated = true;
        handler.removeCallbacks(proceed);

        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        Class<?> next = loggedIn ? MainActivity.class : OptionsActivity.class;
        startActivity(new Intent(this, next));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(proceed);
        super.onDestroy();
    }

    private void playIntro() {
        View logo = findViewById(R.id.ivLogo);
        logo.setAlpha(0f);
        logo.setScaleX(0.85f);
        logo.setScaleY(0.85f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(450)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        fadeUp(findViewById(R.id.wordmarkRow), 150);
        fadeUp(findViewById(R.id.tvTagline), 250);

        View progress = findViewById(R.id.splashProgress);
        progress.setAlpha(0f);
        progress.animate().alpha(1f).setStartDelay(450).setDuration(300).start();
    }

    private void fadeUp(View v, long delayMs) {
        float offset = 12 * getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(offset);
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}