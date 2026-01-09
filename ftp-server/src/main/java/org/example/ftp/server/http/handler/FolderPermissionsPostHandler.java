package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FolderPermissionsPostHandler implements HttpHandler {

    private final SqliteUserRepository userRepo;
    private final SqliteFolderRepository folderRepo;
    private final SqliteFolderPermissionRepository permRepo;

    public FolderPermissionsPostHandler(
            SqliteUserRepository userRepo,
            SqliteFolderRepository folderRepo,
            SqliteFolderPermissionRepository permRepo
    ) {
        this.userRepo = userRepo;
        this.folderRepo = folderRepo;
        this.permRepo = permRepo;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        if (!"POST".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        String body =
                new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        String user = extract(body, "user");
        String folder = extract(body, "folder");
        boolean r = Boolean.parseBoolean(extract(body, "r"));
        boolean w = Boolean.parseBoolean(extract(body, "w"));
        boolean e = Boolean.parseBoolean(extract(body, "e"));

        var userOpt = userRepo.findByUsername(user);
        var folderOpt = folderRepo.findByPath(folder);

        if (userOpt.isEmpty() || folderOpt.isEmpty()) {
            ex.sendResponseHeaders(404, -1);
            return;
        }

        permRepo.setPermissions(
                userOpt.get().id(),
                folderOpt.get().id(),
                r, w, e
        );

        ex.sendResponseHeaders(204, -1);
    }

    private String extract(String json, String key) {
        String p = "\"" + key + "\":";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();

        if (json.charAt(i) == '"') {
            i++;
            int j = json.indexOf('"', i);
            return json.substring(i, j);
        }

        int j = json.indexOf(",", i);
        if (j < 0) j = json.indexOf("}", i);
        return json.substring(i, j).trim();
    }
}
