package org.example.ftp.server.http.dto;

public record FolderPermissionDto(
        String folder,
        boolean r,
        boolean w,
        boolean e
) {}
