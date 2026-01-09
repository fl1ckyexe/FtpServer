package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.db.Db;
import org.example.ftp.server.db.SqliteServerSettingsRepository;
import org.example.ftp.server.http.AdminTokenService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class BootstrapHandler implements HttpHandler {

    private final AdminTokenService tokenService;

    public BootstrapHandler(AdminTokenService tokenService) {
        this.tokenService = tokenService;
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
        String suggested = suggestedDefaultFtpRoot();
        String json = "{\"suggestedFtpRoot\":\"" + escapeJson(suggested) + "\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void handlePut(HttpExchange exchange) throws Exception {
        
        if (tokenService != null && tokenService.isTokenSet()) {
            exchange.sendResponseHeaders(409, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String token = extractString(body, "token");
        String ftpRoot = extractString(body, "ftpRoot");

        if (token == null || token.isBlank() || ftpRoot == null || ftpRoot.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        Path ftpRootPath = Path.of(ftpRoot).toAbsolutePath().normalize();
        Files.createDirectories(ftpRootPath);

        
        Db db = new Db(ftpRootPath.resolve("ftp.db"));
        db.initSchema();
        SqliteServerSettingsRepository settingsRepo = new SqliteServerSettingsRepository(db);
        settingsRepo.saveAdminToken(token.trim());

        
        Path cfgPath = defaultConfigPath();
        Files.createDirectories(cfgPath.getParent());
        Files.writeString(cfgPath, ftpRootPath.toString() + System.lineSeparator(), StandardCharsets.UTF_8);

        
        if (tokenService != null) {
            tokenService.setToken(token);
        }

        String json = "{"
                + "\"ok\":true,"
                + "\"restartRequired\":true,"
                + "\"ftpRoot\":\"" + escapeJson(ftpRootPath.toString()) + "\","
                + "\"configPath\":\"" + escapeJson(cfgPath.toString()) + "\""
                + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static Path defaultConfigPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "FtpServer", "ftp-root.path").toAbsolutePath().normalize();
        }
        String home = System.getProperty("user.home");
        return Path.of(home == null ? "." : home, "FtpServer", "ftp-root.path").toAbsolutePath().normalize();
    }

    private static String suggestedDefaultFtpRoot() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "FtpServer", "ftp-root").toAbsolutePath().normalize().toString();
        }
        String home = System.getProperty("user.home");
        return Path.of(home == null ? "." : home, "FtpServer", "ftp-root").toAbsolutePath().normalize().toString();
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

