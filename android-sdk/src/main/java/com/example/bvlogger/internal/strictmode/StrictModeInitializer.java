package com.example.bvlogger.internal.strictmode;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.example.bvlogger.model.LogEvent;
import com.example.bvlogger.uploader.LogUploader;

/**
 * 方案2：StrictMode + 全局未捕获异常
 */
public class StrictModeInitializer {

    private static final String TAG = "BvStrictMode";

    public static void setup(Application app, final LogUploader uploader) {
        if (app == null || uploader == null) {
            return;
        }

        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build();
        StrictMode.setThreadPolicy(threadPolicy);

        StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build();
        StrictMode.setVmPolicy(vmPolicy);

        final Thread.UncaughtExceptionHandler originalHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                StringBuilder sb = new StringBuilder();
                sb.append("Uncaught exception in thread: ")
                        .append(thread.getName())
                        .append("\n")
                        .append(Log.getStackTraceString(throwable));

                LogEvent event = new LogEvent(
                        LogEvent.Type.CRASH,
                        System.currentTimeMillis(),
                        thread.getName(),
                        sb.toString()
                );
                uploader.enqueue(event);

                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable);
                }
            }
        });

        LogEvent startEvent = new LogEvent(
                LogEvent.Type.STRICTMODE,
                System.currentTimeMillis(),
                Thread.currentThread().getName(),
                "App start with StrictMode enabled"
        );
        uploader.enqueue(startEvent);
    }
}


