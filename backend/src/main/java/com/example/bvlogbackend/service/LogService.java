package com.example.bvlogbackend.service;

import com.example.bvlogbackend.config.LogFileProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class LogService {

    private final LogFileProperties properties;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public LogService(LogFileProperties properties) {
        this.properties = properties;
    }

    public void appendLog(String content) {
        String dateStr = dateFormat.format(new Date());
        String fileName = String.format(properties.getFileNamePattern(), dateStr);
        Path dir = Paths.get(properties.getBaseDir());
        Path file = dir.resolve(fileName);

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(
                    file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                String ts = timeFormat.format(new Date());
                writer.write("[" + ts + "] " + content);
                if (!content.endsWith("\n")) {
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


