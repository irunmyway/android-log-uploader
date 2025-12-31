package com.example.bvlogger.uploader;

import android.util.Log;

import com.example.bvlogger.model.LogEvent;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 负责将采集到的日志事件发送到后端
 */
public class LogUploader {

    private static final String TAG = "BvLogUploader";

    private final String serverUrl;
    private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<LogEvent>();
    private final Thread workerThread;
    private volatile boolean running = true;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    public LogUploader(String serverUrl) {
        this.serverUrl = serverUrl;
        this.workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "BvLogUploader-Worker");
        this.workerThread.start();
    }

    private void loop() {
        while (running) {
            try {
                LogEvent event = queue.take();
                send(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error sending log", e);
            }
        }
    }

    public void enqueue(LogEvent event) {
        if (!running || event == null) {
            return;
        }
        queue.offer(event);
    }

    public void shutdown() {
        running = false;
        workerThread.interrupt();
    }

    private void send(LogEvent event) {
        String body = formatEvent(event);
        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(serverUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");

                byte[] bytes = body.getBytes("UTF-8");
                conn.setFixedLengthStreamingMode(bytes.length);

                OutputStream os = null;
                try {
                    os = new BufferedOutputStream(conn.getOutputStream());
                    os.write(bytes);
                    os.flush();
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (Exception ignore) {
                        }
                    }
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    return;
                } else {
                    Log.w(TAG, "Server responded code: " + code);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send log, retry " + i, e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private String formatEvent(LogEvent event) {
        String ts = sdf.format(new Date(event.getTimestamp()));
        return "[" + ts + "] "
                + "[" + event.getType().name() + "] "
                + "[" + event.getThreadName() + "] "
                + event.getMessage();
    }
}


