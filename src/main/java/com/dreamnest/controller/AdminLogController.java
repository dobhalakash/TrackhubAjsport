package com.dreamnest.controller;

import com.dreamnest.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Lets the super admin download the live application log file - useful for
 * reviewing user behaviour, diagnosing errors, or auditing activity without
 * needing direct server/SSH access. Restricted to SUPER_ADMIN (see
 * SecurityConfig: {@code /admin/**} requires that role).
 */
@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    @Value("${logging.file.name:logs/trackhub.log}")
    private String logFilePath;

    /** Downloads the entire current log file as an attachment. */
    @GetMapping("/download")
    public ResponseEntity<FileSystemResource> downloadFullLog() {
        Path path = Path.of(logFilePath);
        FileSystemResource resource = new FileSystemResource(path);

        String filename = "trackhub-log-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".log";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Returns just the last N lines of the log (default 500) as plain JSON-wrapped
     * text, for a quick "tail -f"-style peek in the admin UI without downloading
     * the whole file.
     */
    @GetMapping("/tail")
    public ApiResponse<Map<String, Object>> tailLog(@RequestParam(defaultValue = "500") int lines) {
        Path path = Path.of(logFilePath);
        if (!Files.exists(path)) {
            return ApiResponse.success(Map.of("lines", java.util.List.of(), "totalLines", 0));
        }

        java.util.List<String> tail = readLastLines(path, Math.min(lines, 2000));
        return ApiResponse.success(Map.of("lines", tail, "totalLines", tail.size()));
    }

    private java.util.List<String> readLastLines(Path path, int maxLines) {
        java.util.LinkedList<String> result = new java.util.LinkedList<>();
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long length = file.length();
            long pointer = length - 1;
            StringBuilder line = new StringBuilder();

            while (pointer >= 0 && result.size() < maxLines) {
                file.seek(pointer);
                int readByte = file.read();
                if (readByte == '\n') {
                    if (line.length() > 0) {
                        result.addFirst(line.reverse().toString());
                        line.setLength(0);
                    }
                } else if (readByte != '\r') {
                    line.append((char) readByte);
                }
                pointer--;
            }
            if (line.length() > 0 && result.size() < maxLines) {
                result.addFirst(line.reverse().toString());
            }
        } catch (IOException e) {
            return java.util.List.of("Could not read log file: " + e.getMessage());
        }
        return result;
    }
}
