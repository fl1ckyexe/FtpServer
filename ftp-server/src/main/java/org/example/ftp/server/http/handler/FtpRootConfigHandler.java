package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class FtpRootConfigHandler implements HttpHandler {

    private final Path currentFtpRoot;

    public FtpRootConfigHandler(Path currentFtpRoot) {
        this.currentFtpRoot = currentFtpRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            switch (exchange.getRequestMethod()) {
                case "GET" -> handleGet(exchange);
                case "PUT" -> handlePut(exchange);
                default -> exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            byte[] msg = ("ERROR: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(500, msg.length);
            exchange.getResponseBody().write(msg);
        } finally {
            exchange.close();
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        Path cfgPath = defaultConfigPath();
        String json = "{"
                + "\"currentFtpRoot\":\"" + escapeJson(currentFtpRoot.toAbsolutePath().normalize().toString()) + "\","
                + "\"currentDbPath\":\"" + escapeJson(currentFtpRoot.resolve("ftp.db").toAbsolutePath().normalize().toString()) + "\","
                + "\"configPath\":\"" + escapeJson(cfgPath.toAbsolutePath().normalize().toString()) + "\""
                + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void handlePut(HttpExchange exchange) throws Exception {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String ftpRoot = extractString(body, "ftpRoot");
        if (ftpRoot == null || ftpRoot.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        Path ftpRootPath = Path.of(ftpRoot).toAbsolutePath().normalize();
        Files.createDirectories(ftpRootPath);

        Path cfgPath = defaultConfigPath();
        Files.createDirectories(cfgPath.getParent());
        Files.writeString(cfgPath, ftpRootPath.toString() + System.lineSeparator(), StandardCharsets.UTF_8);

        String json = "{"
                + "\"ok\":true,"
                + "\"restartRequired\":true,"
                + "\"ftpRoot\":\"" + escapeJson(ftpRootPath.toString()) + "\","
                + "\"configPath\":\"" + escapeJson(cfgPath.toString()) + "\""
                + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
    }

    private static Path defaultConfigPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "FtpServer", "ftp-root.path").toAbsolutePath().normalize();
        }
        String home = System.getProperty("user.home");
        return Path.of(home == null ? "." : home, "FtpServer", "ftp-root.path").toAbsolutePath().normalize();
    }

    private String extractString(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

