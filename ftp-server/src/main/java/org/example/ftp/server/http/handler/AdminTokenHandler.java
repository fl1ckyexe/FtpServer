package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.db.SqliteServerSettingsRepository;
import org.example.ftp.server.http.AdminTokenService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

 
public class AdminTokenHandler implements HttpHandler {

    private final SqliteServerSettingsRepository settingsRepo;
    private final AdminTokenService tokenService;

    public AdminTokenHandler(SqliteServerSettingsRepository settingsRepo, AdminTokenService tokenService) {
        this.settingsRepo = settingsRepo;
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
        } finally {
            exchange.close();
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        boolean tokenSet = tokenService != null && tokenService.isTokenSet();
        String json = "{\"tokenSet\":" + tokenSet + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String token = extractString(body, "token");
        if (token == null || token.trim().isEmpty()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        token = token.trim();

        try {
            settingsRepo.saveAdminToken(token);
        } catch (Exception e) {
            exchange.sendResponseHeaders(500, -1);
            return;
        }

        if (tokenService != null) {
            tokenService.setToken(token);
        }

        exchange.sendResponseHeaders(204, -1);
    }

    private String extractString(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }
}

