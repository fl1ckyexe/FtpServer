package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.db.Db;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class RootHandler implements HttpHandler {

    private final Path currentFtpRoot;

    public RootHandler(Path currentFtpRoot) {
        this.currentFtpRoot = currentFtpRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            if (path.endsWith("/create") && "POST".equals(method)) {
                handleCreate(exchange);
            } else if ("GET".equals(method)) {
                handleGet(exchange);
            } else if ("PUT".equals(method)) {
                handlePut(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
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
        Path dbPath = currentFtpRoot.resolve("ftp.db");
        Path sharedPath = currentFtpRoot.resolve("shared");
        Path usersPath = currentFtpRoot.resolve("users");
        
        String json = String.format(
            """
            {
              "currentFtpRoot": "%s",
              "currentDbPath": "%s",
              "sharedPath": "%s",
              "usersPath": "%s",
              "dbExists": %s,
              "sharedExists": %s,
              "usersExists": %s
            }
            """,
            escapeJson(currentFtpRoot.toAbsolutePath().normalize().toString()),
            escapeJson(dbPath.toAbsolutePath().normalize().toString()),
            escapeJson(sharedPath.toAbsolutePath().normalize().toString()),
            escapeJson(usersPath.toAbsolutePath().normalize().toString()),
            Files.exists(dbPath),
            Files.exists(sharedPath),
            Files.exists(usersPath)
        );
        
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

        String json = String.format(
            """
            {
              "ok": true,
              "restartRequired": true,
              "ftpRoot": "%s",
              "configPath": "%s"
            }
            """,
            escapeJson(ftpRootPath.toString()),
            escapeJson(cfgPath.toString())
        );
        
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
    }

    private void handleCreate(HttpExchange exchange) throws Exception {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String ftpRoot = extractString(body, "ftpRoot");
        if (ftpRoot == null || ftpRoot.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        Path ftpRootPath = Path.of(ftpRoot).toAbsolutePath().normalize();
        
        
        if (Files.exists(ftpRootPath)) {
            Path dbPath = ftpRootPath.resolve("ftp.db");
            if (Files.exists(dbPath)) {
                exchange.sendResponseHeaders(409, -1);
                String error = "{\"error\":\"Root already exists and contains ftp.db\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
                return;
            }
        }

        
        Files.createDirectories(ftpRootPath);

        
        Path dbPath = ftpRootPath.resolve("ftp.db");
        Db db = new Db(dbPath);
        db.initSchema();

        
        Path sharedPath = ftpRootPath.resolve("shared");
        Files.createDirectories(sharedPath);

        
        Path usersPath = ftpRootPath.resolve("users");
        Files.createDirectories(usersPath);

        
        Path cfgPath = defaultConfigPath();
        Files.createDirectories(cfgPath.getParent());
        Files.writeString(cfgPath, ftpRootPath.toString() + System.lineSeparator(), StandardCharsets.UTF_8);

        String json = String.format(
            """
            {
              "ok": true,
              "restartRequired": true,
              "ftpRoot": "%s",
              "dbPath": "%s",
              "sharedPath": "%s",
              "usersPath": "%s",
              "configPath": "%s"
            }
            """,
            escapeJson(ftpRootPath.toString()),
            escapeJson(dbPath.toString()),
            escapeJson(sharedPath.toString()),
            escapeJson(usersPath.toString()),
            escapeJson(cfgPath.toString())
        );
        
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

