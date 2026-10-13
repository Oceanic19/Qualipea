package com.hcdc.qualipea;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the Pi's Flask endpoints over plain HTTP. Runs network
 * calls on a background thread and delivers results back on the
 * main thread.
 */
public class PiClient {

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String message);
    }

    public interface ReachabilityCallback {
        void onResult(boolean reachable);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Calls GET /scan (or any GET endpoint returning JSON) and parses the response. */
    public static void getJson(String url, Callback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                // Inference can take several seconds on the Pi - allow a generous read timeout.
                conn.setReadTimeout(30000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    postError(callback, "Pi responded with HTTP " + code);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

                JSONObject json = new JSONObject(sb.toString());
                MAIN.post(() -> callback.onSuccess(json));

            } catch (Exception e) {
                postError(callback, "Couldn't reach the Pi: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    /**
     * Lightweight check for whether the Pi is reachable at all - just
     * opens a raw socket to host:port rather than hitting a specific
     * Flask route (so it doesn't trigger a real scan/inference just to
     * check status).
     */
    public static void checkReachable(ReachabilityCallback callback) {
        EXECUTOR.execute(() -> {
            boolean reachable;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(AppConfig.PI_HOST, AppConfig.PI_PORT), 3000);
                reachable = true;
            } catch (Exception e) {
                reachable = false;
            }
            boolean finalReachable = reachable;
            MAIN.post(() -> callback.onResult(finalReachable));
        });
    }

    private static void postError(Callback callback, String message) {
        MAIN.post(() -> callback.onError(message));
    }
}
