package org.example.ftp.server.auth;

public record User(
        long id,
        String username,
        String passwordHash,
        boolean enabled
) {}
