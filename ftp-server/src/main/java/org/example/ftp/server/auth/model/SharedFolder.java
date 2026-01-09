package org.example.ftp.server.auth.model;

public record SharedFolder(
    long id,
    long ownerUserId,
    long userToShareId,
    String folderName,
    String folderPath,
    boolean read,
    boolean write,
    boolean execute
) {}

