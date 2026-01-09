package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SharedFoldersPostHandler implements HttpHandler {

    private final SqliteUserRepository userRepo;
    private final SqliteSharedFolderRepository sharedFolderRepo;

    public SharedFoldersPostHandler(
            SqliteUserRepository userRepo,
            SqliteSharedFolderRepository sharedFolderRepo
    ) {
        this.userRepo = userRepo;
        this.sharedFolderRepo = sharedFolderRepo;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        if (!"POST".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        
        String authenticatedUsername = (String) ex.getAttribute("authenticatedUsername");
        if (authenticatedUsername == null) {
            ex.sendResponseHeaders(401, -1);
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        String ownerUsername = extract(body, "owner");
        String userToShareUsername = extract(body, "userToShare");
        String folderName = extract(body, "folderName");
        String folderPath = extract(body, "folderPath");
        boolean write = extractBoolean(body, "write");
        boolean execute = extractBoolean(body, "execute");

        if (ownerUsername == null || userToShareUsername == null || 
            folderName == null || folderPath == null) {
            ex.sendResponseHeaders(400, -1);
            return;
        }

        
        if (!authenticatedUsername.equals(ownerUsername)) {
            ex.sendResponseHeaders(403, -1); 
            return;
        }

        var ownerOpt = userRepo.findByUsername(ownerUsername);
        var userToShareOpt = userRepo.findByUsername(userToShareUsername);

        if (ownerOpt.isEmpty() || userToShareOpt.isEmpty()) {
            ex.sendResponseHeaders(404, -1);
            return;
        }

        long ownerId = ownerOpt.get().id();
        long userToShareId = userToShareOpt.get().id();

        
        if (sharedFolderRepo.exists(ownerId, userToShareId, folderPath)) {
            ex.sendResponseHeaders(409, -1); 
            return;
        }

        
        sharedFolderRepo.create(ownerId, userToShareId, folderName, folderPath, true, write, execute);

        ex.sendResponseHeaders(204, -1);
    }

    private String extract(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();
        int j = json.indexOf('"', i);
        if (j < 0) return null;
        return json.substring(i, j);
    }

    private boolean extractBoolean(String json, String key) {
        String p = "\"" + key + "\":";
        int i = json.indexOf(p);
        if (i < 0) return false;
        i += p.length();
        return json.startsWith("true", i);
    }
}

