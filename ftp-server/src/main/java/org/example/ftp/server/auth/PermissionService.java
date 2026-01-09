package org.example.ftp.server.auth;

import org.example.ftp.server.auth.db.SqlitePermissionsRepository;

public class PermissionService {

    private final SqlitePermissionsRepository repo;

    public PermissionService(SqlitePermissionsRepository repo) {
        this.repo = repo;
    }

    public boolean has(String username, Permission p) {
        return repo.hasPermission(username, p);
    }

    public void setPermissions(
            String username,
            boolean r,
            boolean w,
            boolean e
    ) {
        repo.setPermissions(username, r, w, e);
    }
    public SqlitePermissionsRepository.PermissionRow getPermissions(String username) {
        return repo.findByUsername(username);
    }

}
