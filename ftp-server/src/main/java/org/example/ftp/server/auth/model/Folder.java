package org.example.ftp.server.auth.model;

public record Folder(
        long id,
        String path,
        Long ownerUserId,
        boolean global
) {}
