package com.hcdc.qualipea;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

public final class UiAnim {
    private UiAnim() {}

    /** Views fade in and slide up one after another. */
    public static void staggerIn(View... views) {
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            float offset = 24 * v.getResources().getDisplayMetrics().density;
            v.setAlpha(0f);
            v.setTranslationY(offset);
            v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(i * 90L)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
}