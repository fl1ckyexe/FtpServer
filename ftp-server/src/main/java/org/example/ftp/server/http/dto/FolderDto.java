package org.example.ftp.server.http.dto;

public record FolderDto(
        String path,
        boolean global
) {}
