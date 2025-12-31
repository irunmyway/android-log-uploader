package com.example.bvlogger;

/**
 * SDK 配置类
 */
public class BvLoggerConfig {

    public static final int TYPE_LOGCAT = 1;
    public static final int TYPE_STRICTMODE = 2;

    private final String serverUrl;
    private final int type;
    private final boolean enable;

    private BvLoggerConfig(Builder builder) {
        this.serverUrl = builder.serverUrl;
        this.type = builder.type;
        this.enable = builder.enable;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public int getType() {
        return type;
    }

    public boolean isEnable() {
        return enable;
    }

    public static class Builder {
        private String serverUrl;
        private int type = TYPE_LOGCAT;
        private boolean enable = true;

        public Builder setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setEnable(boolean enable) {
            this.enable = enable;
            return this;
        }

        public BvLoggerConfig build() {
            if (serverUrl == null || serverUrl.isEmpty()) {
                throw new IllegalArgumentException("serverUrl is required");
            }
            return new BvLoggerConfig(this);
        }
    }
}


