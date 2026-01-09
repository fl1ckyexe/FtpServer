package org.example.ftp.server.auth.model;

public record FolderPermission(
        boolean read,
        boolean write,
        boolean execute
) {}
