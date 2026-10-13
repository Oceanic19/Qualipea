package com.hcdc.qualipea;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** ImageView with pinch-zoom, double-tap zoom and drag-to-pan. No external libraries. */
public class ZoomImageView extends AppCompatImageView {

    private static final float MAX_ZOOM = 5f;          // x the "fit in box" size
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private float baseScale = 1f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomImageView(Context c) { super(c); init(); }
    public ZoomImageView(Context c, AttributeSet a) { super(c, a); init(); }
    public ZoomImageView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        zoomBy(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                        return true;
                    }
                });
        scaleDetector.setQuickScaleEnabled(false);

        gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) { return true; }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                        if (relativeScale() > 1.01f && !scaleDetector.isInProgress()) {
                            matrix.postTranslate(-dx, -dy);
                            fixTranslation();
                            setImageMatrix(matrix);
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        float target = relativeScale() > 1.05f ? 1f : DOUBLE_TAP_ZOOM;
                        animateZoomTo(target, e.getX(), e.getY());
                        return true;
                    }
                });
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        fitToBox();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitToBox();
    }

    /** Shows the whole image, centered, at zoom level 1. */
    private void fitToBox() {
        if (matrix == null) return; // can be null while the super constructor runs
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) return;

        float dw = d.getIntrinsicWidth();
        float dh = d.getIntrinsicHeight();
        baseScale = Math.min(getWidth() / dw, getHeight() / dh);

        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate((getWidth() - dw * baseScale) / 2f,
                (getHeight() - dh * baseScale) / 2f);
        setImageMatrix(matrix);
    }

    private float relativeScale() {
        if (baseScale == 0f) return 1f;
        matrix.getValues(values);
        return values[Matrix.MSCALE_X] / baseScale;
    }

    private void zoomBy(float factor, float focusX, float focusY) {
        float rel = relativeScale();
        float newRel = Math.max(1f, Math.min(MAX_ZOOM, rel * factor));
        float f = newRel / rel;
        matrix.postScale(f, f, focusX, focusY);
        fixTranslation();
        setImageMatrix(matrix);
    }

    private void animateZoomTo(float targetRel, float focusX, float focusY) {
        ValueAnimator a = ValueAnimator.ofFloat(relativeScale(), targetRel);
        a.setDuration(200);
        a.addUpdateListener(anim -> {
            float value = (float) anim.getAnimatedValue();
            zoomBy(value / relativeScale(), focusX, focusY);
        });
        a.start();
    }

    /** Keeps the image from being dragged out of the box (centers it when smaller than the box). */
    private void fixTranslation() {
        Drawable d = getDrawable();
        if (d == null) return;
        matrix.getValues(values);
        float scale = values[Matrix.MSCALE_X];
        float tx = values[Matrix.MTRANS_X];
        float ty = values[Matrix.MTRANS_Y];
        float nx = clampAxis(tx, d.getIntrinsicWidth() * scale, getWidth());
        float ny = clampAxis(ty, d.getIntrinsicHeight() * scale, getHeight());
        matrix.postTranslate(nx - tx, ny - ty);
    }

    private float clampAxis(float t, float content, float view) {
        if (content <= view) return (view - content) / 2f;
        return Math.min(0f, Math.max(view - content, t));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }
}