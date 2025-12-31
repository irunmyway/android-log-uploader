package com.example.bvlogger;

import android.app.Application;
import android.content.Context;

import com.example.bvlogger.internal.logcat.LogcatCatcher;
import com.example.bvlogger.internal.strictmode.StrictModeInitializer;
import com.example.bvlogger.uploader.LogUploader;

/**
 * SDK 对外入口类
 */
public final class BvLogger {

    private static volatile boolean initialized = false;
    private static Application application;
    private static BvLoggerConfig config;
    private static LogUploader uploader;
    private static LogcatCatcher logcatCatcher;

    private BvLogger() {
    }

    /**
     * 初始化 SDK
     */
    public static synchronized void init(Context context, BvLoggerConfig cfg) {
        if (initialized) {
            return;
        }
        if (context == null || cfg == null) {
            throw new IllegalArgumentException("context and config must not be null");
        }
        if (!(context instanceof Application)) {
            context = context.getApplicationContext();
        }
        if (!(context instanceof Application)) {
            throw new IllegalArgumentException("Context must be Application or have applicationContext");
        }
        application = (Application) context;
        config = cfg;

        if (!config.isEnable()) {
            initialized = true;
            return;
        }

        uploader = new LogUploader(config.getServerUrl());

        int type = config.getType();
        if (type == BvLoggerConfig.TYPE_LOGCAT) {
            logcatCatcher = new LogcatCatcher(uploader);
            logcatCatcher.start();
        } else if (type == BvLoggerConfig.TYPE_STRICTMODE) {
            StrictModeInitializer.setup(application, uploader);
        }

        initialized = true;
    }

    /**
     * 运行时切换方案类型
     */
    public static synchronized void setType(int type) {
        if (!initialized || config == null) {
            return;
        }
        BvLoggerConfig.Builder builder = new BvLoggerConfig.Builder()
                .setServerUrl(config.getServerUrl())
                .setEnable(config.isEnable())
                .setType(type);
        config = builder.build();
        stop();
        init(application, config);
    }

    /**
     * 停止日志采集与上报
     */
    public static synchronized void stop() {
        if (!initialized) {
            return;
        }
        if (logcatCatcher != null) {
            logcatCatcher.stop();
            logcatCatcher = null;
        }
        if (uploader != null) {
            uploader.shutdown();
            uploader = null;
        }
        initialized = false;
    }
}


