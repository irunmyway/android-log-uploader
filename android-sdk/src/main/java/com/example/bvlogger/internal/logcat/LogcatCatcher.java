package com.example.bvlogger.internal.logcat;

import android.os.Process;
import android.util.Log;

import com.example.bvlogger.model.LogEvent;
import com.example.bvlogger.uploader.LogUploader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 方案1：通过 logcat 捕获当前进程日志
 */
public class LogcatCatcher {

    private static final String TAG = "BvLogcatCatcher";

    private final LogUploader uploader;
    private Thread logThread;
    private Process process;

    public LogcatCatcher(LogUploader uploader) {
        this.uploader = uploader;
    }

    public void start() {
        if (logThread != null) {
            return;
        }

        logThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runInternal();
            }
        }, "BvLogcatCatcher-Thread");
        logThread.start();
    }

    private void runInternal() {
        try {
            try {
                Runtime.getRuntime().exec("logcat -c");
            } catch (IOException ignore) {
                // 某些设备可能没有权限清理日志，忽略
            }

            int pid = Process.myPid();
            String cmd = "logcat -v time --pid=" + pid;
            process = Runtime.getRuntime().exec(cmd);
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while (!Thread.currentThread().isInterrupted()
                    && (line = br.readLine()) != null) {
                LogEvent event = new LogEvent(
                        LogEvent.Type.LOGCAT,
                        System.currentTimeMillis(),
                        Thread.currentThread().getName(),
                        line
                );
                uploader.enqueue(event);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading logcat", e);
        } finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }

    public void stop() {
        if (logThread != null) {
            logThread.interrupt();
            logThread = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
    }
}


