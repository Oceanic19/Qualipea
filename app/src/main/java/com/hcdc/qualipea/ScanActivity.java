package com.hcdc.qualipea;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private enum State { READY, SCANNING, COMPLETE }
    private State state = State.READY;

    private TextView tvSubtitle;
    private TextView tvOverlay;
    private TextView tvKernelsDetected;
    private TextView tvStreamError;
    private ProgressBar scanProgress;
    private MaterialButton btnAction;
    private MaterialButton btnSaveImage;
    private MaterialButton btnViewDetails;
    private MaterialButton btnScanAgain;
    private View layoutResultButtons;
    private View statusDot;
    private TextView tvDeviceStatus;
    private WebView webView;
    private String currentStreamUrl;   // the stream/image we are loading right now

    private JSONObject lastResult;
    private long lastScanMillis;
    private String lastScanId;
    private View viewTapTarget;
    private View tvTapHint;
    private View progressCamera;
    private View btnRefreshCamera;
    private final android.os.Handler uiHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingReload;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvOverlay = findViewById(R.id.tvOverlay);
        tvKernelsDetected = findViewById(R.id.tvKernelsDetected);
        tvStreamError = findViewById(R.id.tvStreamError);
        scanProgress = findViewById(R.id.scanProgress);
        btnAction = findViewById(R.id.btnAction);
        btnSaveImage = findViewById(R.id.btnSaveImage);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        layoutResultButtons = findViewById(R.id.layoutResultButtons);
        statusDot = findViewById(R.id.statusDot);
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus);
        webView = findViewById(R.id.webViewStream);
        webView.setBackgroundColor(android.graphics.Color.BLACK);
        viewTapTarget = findViewById(R.id.viewTapTarget);
        tvTapHint = findViewById(R.id.tvTapHint);
        progressCamera = findViewById(R.id.progressCamera);
        btnRefreshCamera = findViewById(R.id.btnRefreshCamera);

        setUpLiveStream();

        btnAction.setOnClickListener(v -> {
            if (state == State.READY) runScan();
        });
        btnSaveImage.setOnClickListener(v -> saveResultImage());
        btnViewDetails.setOnClickListener(v -> showDetailsDialog());
        viewTapTarget.setOnClickListener(v ->
                ImageViewerOverlay.show(this, AppConfig.RESULT_IMAGE_URL));
        btnRefreshCamera.setOnClickListener(v -> refreshCamera());


        NavHelper.setup(this, findViewById(R.id.bottomNav), NavHelper.Tab.SCAN);
        renderState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        checkPiConnection();
        if (state == State.READY) refreshCamera();   // fresh connection every visit
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelPendingReload();
        webView.onPause();
        // Close the old stream so the Pi frees the camera while you're on another tab.
        if (state == State.READY) webView.loadUrl("about:blank");
    }

    @Override
    protected void onDestroy() {
        cancelPendingReload();
        super.onDestroy();
    }

    private void checkPiConnection() {
        tvDeviceStatus.setText("Checking...");
        statusDot.setBackgroundResource(R.drawable.circle_gray);

        PiClient.checkReachable(reachable -> {
            if (reachable) {
                statusDot.setBackgroundResource(R.drawable.circle_green);
                tvDeviceStatus.setText("Connected");
            } else {
                statusDot.setBackgroundResource(R.drawable.circle_red);
                tvDeviceStatus.setText("Disconnected");
            }
        });
    }

    // ------------------------------------------------------------------
    // Live stream / result image (both use the same WebView + same fit)
    // ------------------------------------------------------------------

    private void setUpLiveStream() {
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                // Ignore errors while refreshing (the spinner is showing)...
                if (pendingReload != null) return;
                // ...and errors from old/aborted connections (not the stream we want now).
                String url = request.getUrl().toString();
                if (currentStreamUrl == null || !url.equals(currentStreamUrl)) return;

                tvStreamError.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Restarts the live feed (the Pi stops it during a scan). */
    private void reloadLiveStream() {
        tvStreamError.setVisibility(View.GONE);
        loadInStream(AppConfig.VIDEO_STREAM_URL + "?restart=" + System.currentTimeMillis());
    }

    /** Spinner for ~1 second, then reconnect the live stream. */
    /** Spinner for ~1 second, then reconnect the live stream. */
    private void refreshCamera() {
        if (state != State.READY) return;

        cancelPendingReload();
        tvStreamError.setVisibility(View.GONE);
        btnRefreshCamera.setEnabled(false);
        btnRefreshCamera.setAlpha(0.4f);
        progressCamera.setVisibility(View.VISIBLE);

        webView.loadUrl("about:blank");          // drop the stuck connection
        currentStreamUrl = null;
        webView.setVisibility(View.INVISIBLE);   // hide the blank page so only the black box shows

        pendingReload = () -> {
            pendingReload = null;
            progressCamera.setVisibility(View.GONE);
            btnRefreshCamera.setEnabled(true);
            btnRefreshCamera.setAlpha(1f);
            webView.setVisibility(View.VISIBLE);
            if (state == State.READY) reloadLiveStream();
        };
        uiHandler.postDelayed(pendingReload, 1000);
    }

    private void cancelPendingReload() {
        if (pendingReload != null) {
            uiHandler.removeCallbacks(pendingReload);
            pendingReload = null;
        }
        progressCamera.setVisibility(View.GONE);
        btnRefreshCamera.setEnabled(true);
        btnRefreshCamera.setAlpha(1f);
        webView.setVisibility(View.VISIBLE);
    }

    /** Shows the annotated result image (boxes + labels) in the same area. */
    private void showResultImage() {
        tvStreamError.setVisibility(View.GONE);
        loadInStream(AppConfig.RESULT_IMAGE_URL + "?t=" + System.currentTimeMillis());
    }

    private void loadInStream(String src) {
        currentStreamUrl = src;
        webView.loadDataWithBaseURL(null, streamHtml(src), "text/html", "utf-8", null);
    }

    /** YOUR CAMERA FIX LIVES HERE. Same fit is used for the live feed and the result. */

    /** Keeps the camera preview and annotated result using the same working layout. */
    private String streamHtml(String src) {
        return "<html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, "
                + "maximum-scale=1.0, user-scalable=no'>"
                + "<style>"
                + "html,body{margin:0;padding:0;height:100%;width:100%;"
                + "overflow:hidden;background:#000;}"
                + "img{position:absolute;top:50%;left:50%;"
                + "width:100vh;height:100vw;"
                + "transform:translate(-50%,-50%) rotate(90deg);"
                + "object-fit:fill;border:0;}"
                + "</style></head><body>"
                + "<img src='" + src + "' /></body></html>";
    }

    // ------------------------------------------------------------------
    // Scan
    // ------------------------------------------------------------------

    private void runScan() {
        state = State.SCANNING;
        renderState();

        PiClient.getJson(AppConfig.SCAN_ENDPOINT, new PiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                boolean success = json.optBoolean("success", false);
                if (!success) {
                    onError("Pi reported the scan failed.");
                    return;
                }
                lastResult = json;
                lastScanMillis = System.currentTimeMillis();

                lastScanId = autoSaveToHistory(json);
                showResultImage();

                state = State.COMPLETE;
                renderState();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ScanActivity.this, message, Toast.LENGTH_LONG).show();
                reloadLiveStream();
                state = State.READY;
                renderState();
            }
        });
    }

    /** Every successful scan goes straight to History (Room, then Firestore sync). */
    /** Every successful scan goes straight to History (Room, then Firestore sync). */
    private String autoSaveToHistory(JSONObject json) {
        return ScanRepository.save(
                this,
                json.optInt("total", 0),
                json.optInt("normal", 0),
                json.optInt("defect", 0),
                json.optDouble("defect_rate", 0),
                json.optString("grade", "Unknown"),
                json.optDouble("inference_time", 0),
                null);
    }

    private void resetToReady() {
        lastScanId = null;
        lastResult = null;
        state = State.READY;
        reloadLiveStream();
        renderState();
    }

    private void renderState() {
        switch (state) {
            case READY:
                tvSubtitle.setText("Place kernels inside the imaging chamber.");
                tvOverlay.setVisibility(View.GONE);
                tvKernelsDetected.setVisibility(View.INVISIBLE);
                scanProgress.setVisibility(View.INVISIBLE);
                layoutResultButtons.setVisibility(View.GONE);
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Start Scan");
                btnAction.setEnabled(true);
                viewTapTarget.setVisibility(View.GONE);
                tvTapHint.setVisibility(View.GONE);
                btnRefreshCamera.setVisibility(View.VISIBLE);
                break;

            case SCANNING:
                tvSubtitle.setText("Scanning...");
                tvOverlay.setText("Scanning...");
                tvOverlay.setVisibility(View.VISIBLE);
                tvKernelsDetected.setVisibility(View.INVISIBLE);
                scanProgress.setVisibility(View.VISIBLE);
                layoutResultButtons.setVisibility(View.GONE);
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Scanning...");
                btnAction.setEnabled(false);
                viewTapTarget.setVisibility(View.GONE);
                tvTapHint.setVisibility(View.GONE);
                btnRefreshCamera.setVisibility(View.GONE);
                break;

            case COMPLETE:
                tvSubtitle.setText("Scan Complete");
                tvOverlay.setVisibility(View.GONE);
                scanProgress.setVisibility(View.GONE);
                viewTapTarget.setVisibility(View.VISIBLE);
                tvTapHint.setVisibility(View.VISIBLE);
                if (lastResult != null) {
                    int total = lastResult.optInt("total", 0);
                    tvKernelsDetected.setVisibility(View.VISIBLE);
                    tvKernelsDetected.setText(total + " kernels detected");
                }
                btnAction.setVisibility(View.GONE);
                layoutResultButtons.setVisibility(View.VISIBLE);
                btnRefreshCamera.setVisibility(View.GONE);
                break;
        }
    }

    // ------------------------------------------------------------------
    // Save Image (annotated result -> phone gallery, like a screenshot)
    // ------------------------------------------------------------------

    private void saveResultImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Saving images needs Android 10 or newer", Toast.LENGTH_LONG).show();
            return;
        }

        btnSaveImage.setEnabled(false);

        io.execute(() -> {
            boolean ok = false;
            try {
                URL url = new URL(AppConfig.RESULT_IMAGE_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(20000);

                String name = "QualiPea_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
                        + ".jpg";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/QualiPea");

                Uri uri = getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (InputStream in = conn.getInputStream();
                         OutputStream out = getContentResolver().openOutputStream(uri)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        ok = true;
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                ok = false;
            }

            final boolean saved = ok;
            runOnUiThread(() -> {
                btnSaveImage.setEnabled(true);
                Toast.makeText(this,
                        saved ? "Image saved to Pictures/QualiPea" : "Couldn't save the image",
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    // ------------------------------------------------------------------
    // View Details (floating modal)
    // ------------------------------------------------------------------

    private void showDetailsDialog() {
        if (lastResult == null) return;

        int total = lastResult.optInt("total", 0);
        int good = lastResult.optInt("normal", 0);
        int defect = lastResult.optInt("defect", 0);
        double defectRate = lastResult.optDouble("defect_rate", 0);
        String grade = lastResult.optString("grade", "Unknown");
        double inferenceTime = lastResult.optDouble("inference_time", 0);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scan_results);
        dialog.setCanceledOnTouchOutside(true);

        TextView tvTimestamp = dialog.findViewById(R.id.tvTimestamp);
        TextView tvGrade = dialog.findViewById(R.id.tvGrade);
        TextView tvAnalyzedCount = dialog.findViewById(R.id.tvAnalyzedCount);
        TextView tvInferenceTime = dialog.findViewById(R.id.tvInferenceTime);
        TextView tvGoodCount = dialog.findViewById(R.id.tvGoodCount);
        TextView tvDefectCount = dialog.findViewById(R.id.tvDefectCount);

        SimpleDateFormat fmt = new SimpleDateFormat("MMMM d, yyyy \u2022 h:mm a", Locale.getDefault());
        tvTimestamp.setText(fmt.format(new Date(lastScanMillis)));
        tvGrade.setText(grade);
        tvAnalyzedCount.setText(total + " kernels analyzed");
        tvInferenceTime.setText(String.format(Locale.getDefault(), "Inference time: %.2fs", inferenceTime));
        tvGoodCount.setText(good + " kernels");
        tvDefectCount.setText(String.format(Locale.getDefault(),
                "%d kernels \u2022 %.2f%% defect rate", defect, defectRate));

        if ("Reject".equalsIgnoreCase(grade)) {
            tvGrade.setTextColor(getResources().getColor(R.color.brand_primary));
        }

        dialog.findViewById(R.id.btnDialogScanAgain).setOnClickListener(v -> {
            dialog.dismiss();
            resetToReady();
        });

        dialog.findViewById(R.id.btnDialogRecommendations).setOnClickListener(v ->
                RecommendationFlow.open(this, lastScanId, total, good, defect,
                        defectRate, grade, inferenceTime));

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}