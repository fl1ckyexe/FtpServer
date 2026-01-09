package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteFolderPermissionRepository;
import org.example.ftp.server.auth.db.SqliteFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FolderPermissionsGetHandler implements HttpHandler {

    private final SqliteUserRepository userRepo;
    private final SqliteFolderRepository folderRepo;
    private final SqliteFolderPermissionRepository permRepo;

    public FolderPermissionsGetHandler(
            SqliteUserRepository userRepo,
            SqliteFolderRepository folderRepo,
            SqliteFolderPermissionRepository permRepo
    ) {
        this.userRepo = userRepo;
        this.folderRepo = folderRepo;
        this.permRepo = permRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String rawUsername = HttpHandlerUtils.extractQueryParam(query, "username");
            String username = HttpHandlerUtils.urlDecode(rawUsername);

            if (username == null || username.isBlank()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            var userOpt = userRepo.findByUsername(username);
            if (userOpt.isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            long userId = userOpt.get().id();

            var folders = folderRepo.findAll();

            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;

            for (var folder : folders) {
                var perm = permRepo.find(userId, folder.id());

                if (!first) json.append(",");
                first = false;

                json.append("""
                    {
                      "folder":"%s",
                      "read":%s,
                      "write":%s,
                      "execute":%s
                    }
                    """.formatted(
                        HttpHandlerUtils.escapeJson(folder.path()),
                        perm.read(),
                        perm.write(),
                        perm.execute()
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
}