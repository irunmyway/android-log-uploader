package com.example.bvlogger.model;

/**
 * SDK 内部使用的日志事件对象
 */
public class LogEvent {

    public enum Type {
        LOGCAT,
        STRICTMODE,
        CRASH
    }

    private final Type type;
    private final long timestamp;
    private final String threadName;
    private final String message;

    public LogEvent(Type type, long timestamp, String threadName, String message) {
        this.type = type;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }
}


