package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class SharedFoldersAdminHandler implements HttpHandler {

    private final SqliteUserRepository userRepo;
    private final SqliteSharedFolderRepository sharedFolderRepo;

    public SharedFoldersAdminHandler(
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

            var sharedFolders = sharedFolderRepo.findAll();

            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;

            for (var folder : sharedFolders) {
                if (!first) json.append(",");
                first = false;

                String ownerUsername = userRepo.findById(folder.ownerUserId())
                        .map(u -> u.username())
                        .orElse("unknown");
                String userToShareUsername = userRepo.findById(folder.userToShareId())
                        .map(u -> u.username())
                        .orElse("unknown");

                json.append("""
                    {
                      "id":%d,
                      "folderName":"%s",
                      "folderPath":"%s",
                      "ownerUsername":"%s",
                      "userToShareUsername":"%s",
                      "read":%s,
                      "write":%s,
                      "execute":%s
                    }
                    """.formatted(
                        folder.id(),
                        escapeJson(folder.folderName()),
                        escapeJson(folder.folderPath()),
                        escapeJson(ownerUsername),
                        escapeJson(userToShareUsername),
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

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

