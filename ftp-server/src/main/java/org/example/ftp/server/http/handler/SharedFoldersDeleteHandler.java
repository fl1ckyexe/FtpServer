package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SharedFoldersDeleteHandler implements HttpHandler {

    private final SqliteSharedFolderRepository sharedFolderRepo;
    private final SqliteUserRepository userRepo;

    public SharedFoldersDeleteHandler(
            SqliteUserRepository userRepo,
            SqliteSharedFolderRepository sharedFolderRepo) {
        this.userRepo = userRepo;
        this.sharedFolderRepo = sharedFolderRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            
            String authenticatedUsername = (String) exchange.getAttribute("authenticatedUsername");
            if (authenticatedUsername == null) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            var authenticatedUserOpt = userRepo.findByUsername(authenticatedUsername);
            if (authenticatedUserOpt.isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            long authenticatedUserId = authenticatedUserOpt.get().id();

            String query = exchange.getRequestURI().getQuery();
            String rawFolderPath = extractQueryParam(query, "folderPath");
            String folderPath = urlDecode(rawFolderPath);

            if (folderPath == null || folderPath.isBlank()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            
            Long ownerId = sharedFolderRepo.findOwnerByFolderPath(folderPath);
            if (ownerId == null) {
                
                
                String[] pathParts = folderPath.split("/");
                if (pathParts.length < 2) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                String ownerUsername = pathParts[1];
                
                var ownerOpt = userRepo.findByUsername(ownerUsername);
                if (ownerOpt.isEmpty()) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                
                ownerId = ownerOpt.get().id();
            }
            
            
            if (authenticatedUserId != ownerId) {
                exchange.sendResponseHeaders(403, -1); 
                return;
            }

            
            sharedFolderRepo.deleteByFolderPath(folderPath);

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

