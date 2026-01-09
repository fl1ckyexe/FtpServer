package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.http.AdminTokenService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;


public class RuntimeInfoHandler implements HttpHandler {

    private final Path ftpRoot;
    private final AdminTokenService tokenService;

    public RuntimeInfoHandler(Path ftpRoot, AdminTokenService tokenService) {
        this.ftpRoot = ftpRoot;
        this.tokenService = tokenService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            boolean tokenSet = tokenService != null && tokenService.isTokenSet();
            String json = "{"
                    + "\"ftpRoot\":\"" + escapeJson(ftpRoot.toAbsolutePath().normalize().toString()) + "\","
                    + "\"dbPath\":\"" + escapeJson(ftpRoot.resolve("ftp.db").toAbsolutePath().normalize().toString()) + "\","
                    + "\"tokenSet\":" + tokenSet
                    + "}";

            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
        } finally {
            exchange.close();
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

