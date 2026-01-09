package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.PermissionService;
import org.example.ftp.server.auth.db.SqlitePermissionsRepository.PermissionRow;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PermissionsHandler implements HttpHandler {

    private final PermissionService permissionService;

    public PermissionsHandler(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            switch (exchange.getRequestMethod()) {
                case "GET" -> handleGet(exchange);
                case "POST" -> handleSave(exchange);
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
        String query = exchange.getRequestURI().getQuery();
        String rawUsername = extractQueryParam(query, "user");
        String username = urlDecode(rawUsername);

        if (username == null || username.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        PermissionRow p = permissionService.getPermissions(username);

        
        if (p == null) {
            try {
                permissionService.setPermissions(username, true, true, true);
                p = permissionService.getPermissions(username);
            } catch (IllegalArgumentException ex) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
        }

        if (p == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String json = """
            {
              "username":"%s",
              "read":%s,
              "write":%s,
              "execute":%s
            }
            """.formatted(
                escapeJson(username),
                p.read(),
                p.write(),
                p.execute()
        );

        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
    }

    
    private void handleSave(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        String user = extractString(body, "username");
        if (user == null || user.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        boolean r = extractBoolean(body, "read");
        boolean w = extractBoolean(body, "write");
        boolean e = extractBoolean(body, "execute");

        try {
            permissionService.setPermissions(user, r, w, e);
        } catch (IllegalArgumentException ex) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        exchange.sendResponseHeaders(204, -1);
    }

    private String extractQueryParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    private String urlDecode(String s) {
        if (s == null) return null;
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String extractString(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }

    private boolean extractBoolean(String json, String key) {
        String p = "\"" + key + "\":";
        int i = json.indexOf(p);
        if (i < 0) return false;
        i += p.length();
        return json.startsWith("true", i);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}