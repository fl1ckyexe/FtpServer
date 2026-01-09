package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.connection.ConnectionLimiter;
import org.example.ftp.server.db.SqliteServerSettingsRepository;
import org.example.ftp.server.session.ActiveSessionRegistry;
import org.example.ftp.server.transfer.RateLimiter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LimitsHandler implements HttpHandler {

    private final ConnectionLimiter connectionLimiter;
    private final RateLimiter uploadRateLimiter;
    private final RateLimiter downloadRateLimiter;
    private final SqliteServerSettingsRepository settingsRepo;
    private final ActiveSessionRegistry sessionRegistry;

    public LimitsHandler(
            ConnectionLimiter connectionLimiter,
            RateLimiter uploadRateLimiter,
            RateLimiter downloadRateLimiter,
            SqliteServerSettingsRepository settingsRepo,
            ActiveSessionRegistry sessionRegistry
    ) {
        this.connectionLimiter = connectionLimiter;
        this.uploadRateLimiter = uploadRateLimiter;
        this.downloadRateLimiter = downloadRateLimiter;
        this.settingsRepo = settingsRepo;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        switch (exchange.getRequestMethod()) {
            case "GET" -> handleGet(exchange);
            case "PUT" -> handlePut(exchange);
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }


    private void handleGet(HttpExchange exchange) throws IOException {
        
        var settings = settingsRepo.get();
        int maxConn = settings != null ? settings.globalMaxConnections() : connectionLimiter.getMaxConnections();
        
        
        long uploadLimit = settings != null ? settings.globalUploadLimit() : uploadRateLimiter.getLimit();
        long downloadLimit = settings != null ? settings.globalDownloadLimit() : downloadRateLimiter.getLimit();
        long rateLimit = settings != null ? settings.globalRateLimit() : downloadRateLimiter.getLimit();
        
        
        Long uploadLimitJson = uploadLimit == 200000 ? null : uploadLimit;
        Long downloadLimitJson = downloadLimit == 200000 ? null : downloadLimit;

        String json = String.format(
                """
                {
                  "globalMaxConnections": %d,
                  "globalRateLimit": %d,
                  "globalUploadLimit": %s,
                  "globalDownloadLimit": %s
                }
                """,
                maxConn,
                rateLimit,
                uploadLimitJson != null ? String.valueOf(uploadLimitJson) : "null",
                downloadLimitJson != null ? String.valueOf(downloadLimitJson) : "null"
        );

        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }


    private void handlePut(HttpExchange exchange) throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Integer maxConn = extractInt(body, "globalMaxConnections");
        Long rate = extractLong(body, "globalRateLimit");
        Long upload = extractLong(body, "globalUploadLimit");
        Long download = extractLong(body, "globalDownloadLimit");

        if (maxConn != null) {
            connectionLimiter.setMaxConnections(maxConn);
        }

        if (rate != null) {
            uploadRateLimiter.setLimit(rate);
            downloadRateLimiter.setLimit(rate);
        }

        if (upload != null) {
            uploadRateLimiter.setLimit(upload);
        }

        if (download != null) {
            downloadRateLimiter.setLimit(download);
        }

        try {
            settingsRepo.save(maxConn, rate, upload, download);
        } catch (Exception ignored) {
        }

        if (sessionRegistry != null && (maxConn != null || rate != null || upload != null || download != null)) {
            sessionRegistry.disconnectAll();
        }

        exchange.sendResponseHeaders(200, -1);
    }


    private Integer extractInt(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return null;

        start += pattern.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);

        return Integer.parseInt(json.substring(start, end).trim());
    }

    private Long extractLong(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return null;

        start += pattern.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);

        return Long.parseLong(json.substring(start, end).trim());
    }
}
