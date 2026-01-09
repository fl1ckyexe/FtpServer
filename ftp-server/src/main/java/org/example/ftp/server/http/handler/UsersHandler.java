package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.AuthService;
import org.example.ftp.server.auth.PermissionService;
import org.example.ftp.server.auth.User;
import org.example.ftp.server.auth.UserRateLimitService;
import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class UsersHandler implements HttpHandler {

    private final AuthService authService;
    private final PermissionService permissionService;
    private final UserRateLimitService rateLimitService;

    public UsersHandler(AuthService authService, PermissionService permissionService, SqliteUserRepository userRepository) {
        this.authService = authService;
        this.permissionService = permissionService;
        this.rateLimitService = new UserRateLimitService(userRepository);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            switch (exchange.getRequestMethod()) {
                case "GET" -> handleGet(exchange);
                case "POST" -> handlePost(exchange);
                case "DELETE" -> handleDelete(exchange);
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
        String username = extractUsernameFromPath(exchange);
        
        
        if (username != null && !username.isBlank()) {
            var userOpt = authService.getAllUsers().stream()
                    .filter(u -> u.username().equals(username))
                    .findFirst();
            
            if (userOpt.isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            
            User user = userOpt.get();
            var limits = rateLimitService.getRateLimits(username);
            
            String json = String.format(
                    "{\"username\":\"%s\",\"enabled\":%s,\"rateLimit\":%s}",
                    HttpHandlerUtils.escapeJson(user.username()),
                    user.enabled(),
                    limits.legacyRateLimit() == null ? "null" : String.valueOf(limits.legacyRateLimit())
            );
            
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
            return;
        }
        
        
        List<User> users = authService.getAllUsers();

        String json = users.stream()
                .map(u -> String.format(
                        "{\"username\":\"%s\",\"enabled\":%s}",
                        HttpHandlerUtils.escapeJson(u.username()),
                        u.enabled()
                ))
                .collect(Collectors.joining(",", "[", "]"));

        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        String username = extractString(body, "username");
        String password = extractString(body, "password");

        if (username == null || username.isBlank() || password == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        try {
            authService.createUser(username, password);

            
            permissionService.setPermissions(username, true, true, true);

            exchange.sendResponseHeaders(201, -1);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(409, -1);
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String username = extractUsernameFromPath(exchange);
        if (username == null || username.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        authService.deleteUser(username);
        exchange.sendResponseHeaders(204, -1);
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String username = extractUsernameFromPath(exchange);
        if (username == null || username.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        Boolean enabled = extractBoolean(body, "enabled");
        Long rateLimit = extractLong(body, "rateLimit");

        if (enabled == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        authService.updateUser(username, enabled, rateLimit);
        exchange.sendResponseHeaders(200, -1);
    }

    private String extractUsernameFromPath(HttpExchange exchange) {
        
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length != 4) return null;
        return urlDecode(parts[3]);
    }

    private String urlDecode(String s) {
        return HttpHandlerUtils.urlDecode(s);
    }

    private String extractString(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }

    private Boolean extractBoolean(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return null;

        start += pattern.length();
        if (json.startsWith("true", start)) return true;
        if (json.startsWith("false", start)) return false;
        return null;
    }

    private Long extractLong(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return null;

        start += pattern.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        if (end < 0) return null;

        try {
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) {
            return null;
        }
    }
}