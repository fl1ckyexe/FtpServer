package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SharedFoldersGetHandler implements HttpHandler {

    private final SqliteUserRepository userRepo;
    private final SqliteSharedFolderRepository sharedFolderRepo;

    public SharedFoldersGetHandler(
            SqliteUserRepository userRepo,
            SqliteSharedFolderRepository sharedFolderRepo
    ) {
        this.userRepo = userRepo;
        this.sharedFolderRepo = sharedFolderRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            
            String authenticatedUsername = (String) exchange.getAttribute("authenticatedUsername");
            if (authenticatedUsername == null) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            
            var userOpt = userRepo.findByUsername(authenticatedUsername);
            if (userOpt.isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            long userId = userOpt.get().id();
            var sharedFolders = sharedFolderRepo.findByUserToShare(userId);

            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;

            for (var folder : sharedFolders) {
                if (!first) json.append(",");
                first = false;

                json.append("""
                    {
                      "folderName":"%s",
                      "folderPath":"%s",
                      "ownerUsername":"%s",
                      "read":%s,
                      "write":%s,
                      "execute":%s
                    }
                    """.formatted(
                        escapeJson(folder.folderName()),
                        escapeJson(folder.folderPath()),
                        escapeJson(getOwnerUsername(folder.ownerUserId())),
                        folder.read(),
                        folder.write(),
                        folder.execute()
                ));
            }

            json.append("]");

            byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);

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

    private String getOwnerUsername(long ownerUserId) {
        return userRepo.findById(ownerUserId)
                .map(u -> u.username())
                .orElse("unknown");
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

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

