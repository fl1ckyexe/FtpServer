package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class SharedFoldersAdminDeleteHandler implements HttpHandler {

    private final SqliteSharedFolderRepository sharedFolderRepo;

    public SharedFoldersAdminDeleteHandler(SqliteSharedFolderRepository sharedFolderRepo) {
        this.sharedFolderRepo = sharedFolderRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String rawId = extractQueryParam(query, "id");
            String rawFolderPath = extractQueryParam(query, "folderPath");
            
            
            if (rawId != null && !rawId.isBlank()) {
                try {
                    long id = Long.parseLong(rawId);
                    sharedFolderRepo.deleteById(id);
                } catch (NumberFormatException e) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
            } else if (rawFolderPath != null && !rawFolderPath.isBlank()) {
                String folderPath = urlDecode(rawFolderPath);
                
                sharedFolderRepo.deleteByFolderPath(folderPath);
            } else {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            exchange.sendResponseHeaders(204, -1);

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

    private String extractQueryParam(String query, String key) {
        if (query == null) return null;
        String prefix = key + "=";
        int i = query.indexOf(prefix);
        if (i < 0) return null;
        i += prefix.length();
        int j = query.indexOf('&', i);
        return j < 0 ? query.substring(i) : query.substring(i, j);
    }

    private String urlDecode(String s) {
        if (s == null) return null;
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}

