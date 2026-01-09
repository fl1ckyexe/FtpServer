package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteFolderRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FoldersHandler implements HttpHandler {

    private final SqliteFolderRepository folderRepo;

    public FoldersHandler(SqliteFolderRepository folderRepo) {
        this.folderRepo = folderRepo;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }

            var folders = folderRepo.findAll();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (var f : folders) {
                if (!first) json.append(",");
                first = false;

                json.append("""
                    {
                      "path":"%s",
                      "global":%s
                    }
                    """.formatted(
                        HttpHandlerUtils.escapeJson(f.path()),
                        f.global()
                ));
            }

            json.append("]");

            byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);

        } catch (Exception e) {
            e.printStackTrace();
            byte[] msg = ("ERROR: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(500, msg.length);
            ex.getResponseBody().write(msg);
        } finally {
            ex.close();
        }
    }
}