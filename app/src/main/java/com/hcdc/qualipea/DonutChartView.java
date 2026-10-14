package com.hcdc.qualipea;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.content.ContextCompat;

import java.util.Locale;

/** Simple animated donut: good vs defective kernels, with the good % in the middle. */
public class DonutChartView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private final int goodColor;
    private final int defectColor;

    private int good = 0;
    private int defect = 0;
    private float progress = 1f;
    private ValueAnimator animator;

    public DonutChartView(Context c) { this(c, null); }

    public DonutChartView(Context c, AttributeSet a) {
        super(c, a);
        goodColor = ContextCompat.getColor(c, R.color.status_good);
        defectColor = ContextCompat.getColor(c, R.color.delete_red);

        track.setStyle(Paint.Style.STROKE);
        track.setColor(0xFFEFEAD9);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.BUTT);

        float d = c.getResources().getDisplayMetrics().density;
        big.setTextAlign(Paint.Align.CENTER);
        Typeface nunito = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.nunito);
        big.setTypeface(Typeface.create(nunito != null ? nunito : Typeface.DEFAULT, Typeface.BOLD));
        big.setColor(ContextCompat.getColor(c, R.color.text_dark));

        small.setTextAlign(Paint.Align.CENTER);
        small.setTypeface(nunito != null ? nunito : Typeface.DEFAULT);
        small.setColor(ContextCompat.getColor(c, R.color.text_gray));
    }

    public void setData(int good, int defect) {
        this.good = Math.max(0, good);
        this.defect = Math.max(0, defect);

        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1100);
        animator.setStartDelay(250);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        progress = 0f;
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        big.setTextSize(size * 0.19f);
        small.setTextSize(size * 0.085f);
        float stroke = size * 0.15f;
        float pad = stroke / 2f + 2;
        oval.set((w - size) / 2f + pad, (h - size) / 2f + pad,
                (w + size) / 2f - pad, (h + size) / 2f - pad);

        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        canvas.drawArc(oval, 0, 360, false, track);

        int total = good + defect;
        if (total > 0) {
            float goodSweep = 360f * good / total;
            float defectSweep = 360f - goodSweep;
            float gap = (good > 0 && defect > 0) ? 3f : 0f;

            if (good > 0) {
                arc.setColor(goodColor);
                canvas.drawArc(oval, -90, (goodSweep - gap) * progress, false, arc);
            }
            if (defect > 0) {
                arc.setColor(defectColor);
                canvas.drawArc(oval, -90 + goodSweep, (defectSweep - gap) * progress, false, arc);
            }
        }

        float cx = w / 2f, cy = h / 2f;
        String pct = total == 0 ? "–" : String.format(Locale.US, "%.1f%%", good * 100f / total * progress);
        canvas.drawText(pct, cx, cy + big.getTextSize() * 0.15f, big);
        canvas.drawText("Good quality", cx, cy + big.getTextSize() * 0.15f + small.getTextSize() * 1.5f, small);
    }
}