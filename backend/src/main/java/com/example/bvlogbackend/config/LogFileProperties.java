package com.example.bvlogbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bvlog.file")
public class LogFileProperties {

    /**
     * 日志基础目录，默认 logs
     */
    private String baseDir = "logs";

    /**
     * 文件名模式，例如 android-log-%s.txt，%s 为日期 yyyy-MM-dd
     */
    private String fileNamePattern = "android-log-%s.txt";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }
}


