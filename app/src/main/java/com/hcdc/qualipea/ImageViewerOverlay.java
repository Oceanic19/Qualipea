package com.hcdc.qualipea;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Floating image viewer drawn on top of the current screen (no new Activity),
 * so closing it returns to exactly the same Scan Complete screen.
 * Background = blurred + dimmed copy of the screen behind it.
 */
public final class ImageViewerOverlay {

    // ---- easy knobs ----
    /** Clockwise degrees applied to the downloaded image. 0 = as the Pi saves it (horizontal). */
    private static final int ROTATION_DEG = 0;
    private static final float BOX_WIDTH_FRACTION = 0.92f;  // box width vs screen width
    private static final int BLUR_DOWNSCALE = 6;            // bigger = blurrier
    private static final int BLUR_RADIUS = 2;
    private static final int BLUR_PASSES = 2;
    private static final int DIM_WITH_BLUR = 0x73000000;    // ~45% black
    private static final int DIM_NO_BLUR = 0xA6000000;      // ~65% black (Android 7 fallback)

    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    private final AppCompatActivity activity;
    private final String imageUrl;
    private final ViewGroup content;

    private View root;
    private ZoomImageView zoomView;
    private View progress;
    private TextView errorView;
    private View card;
    private int boxWidthPx;
    private boolean closed;
    private OnBackPressedCallback backCallback;

    public static void show(AppCompatActivity activity, String imageUrl) {
        new ImageViewerOverlay(activity, imageUrl).start();
    }

    private ImageViewerOverlay(AppCompatActivity activity, String imageUrl) {
        this.activity = activity;
        this.imageUrl = imageUrl;
        this.content = activity.findViewById(android.R.id.content);
    }

    // ------------------------------------------------------------
    // 1) Grab + blur the screen behind the viewer
    // ------------------------------------------------------------
    private void start() {
        if (content.findViewById(R.id.viewerRoot) != null) return; // already open

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || content.getWidth() == 0 || content.getHeight() == 0) {
            attach(null); // no blur on Android 7, just a darker dim
            return;
        }
        try {
            int[] loc = new int[2];
            content.getLocationInWindow(loc);
            Rect src = new Rect(loc[0], loc[1],
                    loc[0] + content.getWidth(), loc[1] + content.getHeight());
            Bitmap full = Bitmap.createBitmap(content.getWidth(), content.getHeight(),
                    Bitmap.Config.ARGB_8888);

            PixelCopy.request(activity.getWindow(), src, full, result -> {
                if (result != PixelCopy.SUCCESS) {
                    attach(null);
                    return;
                }
                IO.execute(() -> {
                    Bitmap blurred = null;
                    try {
                        Bitmap small = Bitmap.createScaledBitmap(full,
                                Math.max(1, full.getWidth() / BLUR_DOWNSCALE),
                                Math.max(1, full.getHeight() / BLUR_DOWNSCALE), true);
                        blurred = blur(small, BLUR_RADIUS, BLUR_PASSES);
                    } catch (Throwable ignored) {
                    }
                    Bitmap b = blurred;
                    main.post(() -> attach(b));
                });
            }, main);
        } catch (Throwable t) {
            attach(null);
        }
    }

    // ------------------------------------------------------------
    // 2) Show the overlay
    // ------------------------------------------------------------
    private void attach(Bitmap blurred) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (content.findViewById(R.id.viewerRoot) != null) return;

        root = LayoutInflater.from(activity).inflate(R.layout.overlay_image_viewer, content, false);

        ImageView ivBlur = root.findViewById(R.id.ivBlurBg);
        View dim = root.findViewById(R.id.viewDim);
        if (blurred != null) {
            ivBlur.setImageBitmap(blurred);
            dim.setBackgroundColor(DIM_WITH_BLUR);
        } else {
            ivBlur.setVisibility(View.GONE);
            dim.setBackgroundColor(DIM_NO_BLUR);
        }

        boxWidthPx = (int) (activity.getResources().getDisplayMetrics().widthPixels * BOX_WIDTH_FRACTION);
        View column = root.findViewById(R.id.viewerColumn);
        column.getLayoutParams().width = boxWidthPx;

        card = root.findViewById(R.id.viewerCard);
        card.getLayoutParams().height = (int) (boxWidthPx * 0.75f); // until the image loads

        zoomView = root.findViewById(R.id.zoomView);
        progress = root.findViewById(R.id.viewerProgress);
        errorView = root.findViewById(R.id.viewerError);

        root.findViewById(R.id.btnViewerClose).setOnClickListener(v -> close());
        root.setOnClickListener(v -> close());          // tap outside the box
        errorView.setOnClickListener(v -> load());      // tap to retry

        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);

        root.setAlpha(0f);
        content.addView(root);
        root.animate().alpha(1f).setDuration(180).start();

        load();
    }

    private void close() {
        if (closed) return;
        closed = true;
        if (backCallback != null) backCallback.remove();
        root.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> content.removeView(root)).start();
    }

    // ------------------------------------------------------------
    // 3) Download + show the annotated image (horizontal, zoomable)
    // ------------------------------------------------------------
    private void load() {
        progress.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        // Cache-buster so we never show a previous scan's image.
        String url = imageUrl + (imageUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();

        IO.execute(() -> {
            Bitmap bmp = null;
            try {
                bmp = decodeScaled(download(url), 2048);
            } catch (Exception ignored) {
            }
            Bitmap result = bmp;
            main.post(() -> onLoaded(result));
        });
    }

    private void onLoaded(Bitmap bmp) {
        if (closed) return;
        progress.setVisibility(View.GONE);

        if (bmp == null) {
            errorView.setVisibility(View.VISIBLE);
            return;
        }

        if (ROTATION_DEG != 0) {
            Matrix m = new Matrix();
            m.postRotate(ROTATION_DEG);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }

        // Box follows the image shape, but stays boxy (never a thin strip).
        float aspect = (float) bmp.getWidth() / bmp.getHeight();
        int h = Math.round(Math.max(0.62f * boxWidthPx, Math.min(boxWidthPx, boxWidthPx / aspect)));
        card.getLayoutParams().height = h;
        card.requestLayout();

        zoomView.setImageBitmap(bmp);
    }

    private byte[] download(String address) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        try {
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                return out.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }

    /** Decodes the image, shrinking huge camera frames so they can't run out of memory. */
    private Bitmap decodeScaled(byte[] data, int maxSide) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);

        int longest = Math.max(o.outWidth, o.outHeight);
        int sample = 1;
        while (longest / (sample * 2) >= maxSide) sample *= 2;

        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o);
    }

    // ------------------------------------------------------------
    // Simple box blur (runs on a tiny downscaled copy, so it's instant)
    // ------------------------------------------------------------
    private static Bitmap blur(Bitmap src, int radius, int passes) {
        int w = src.getWidth(), h = src.getHeight();
        int[] a = new int[w * h];
        int[] b = new int[w * h];
        src.getPixels(a, 0, w, 0, 0, w, h);
        for (int i = 0; i < passes; i++) {
            boxBlurH(a, b, w, h, radius);
            boxBlurV(b, a, w, h, radius);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(a, 0, w, 0, 0, w, h);
        return out;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static void boxBlurH(int[] in, int[] out, int w, int h, int r) {
        int div = 2 * r + 1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[row + clamp(i, 0, w - 1)];
                sr += (c >> 16) & 0xFF; sg += (c >> 8) & 0xFF; sb += c & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                out[row + x] = 0xFF000000 | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                int add = in[row + clamp(x + r + 1, 0, w - 1)];
                int rem = in[row + clamp(x - r, 0, w - 1)];
                sr += ((add >> 16) & 0xFF) - ((rem >> 16) & 0xFF);
                sg += ((add >> 8) & 0xFF) - ((rem >> 8) & 0xFF);
                sb += (add & 0xFF) - (rem & 0xFF);
            }
        }
    }

    private static void boxBlurV(int[] in, int[] out, int w, int h, int r) {
        int div = 2 * r + 1;
        for (int x = 0; x < w; x++) {
            int sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[clamp(i, 0, h - 1) * w + x];
                sr += (c >> 16) & 0xFF; sg += (c >> 8) & 0xFF; sb += c & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                out[y * w + x] = 0xFF000000 | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                int add = in[clamp(y + r + 1, 0, h - 1) * w + x];
                int rem = in[clamp(y - r, 0, h - 1) * w + x];
                sr += ((add >> 16) & 0xFF) - ((rem >> 16) & 0xFF);
                sg += ((add >> 8) & 0xFF) - ((rem >> 8) & 0xFF);
                sb += (add & 0xFF) - (rem & 0xFF);
            }
        }
    }
}