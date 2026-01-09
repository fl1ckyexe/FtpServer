package org.example.ftp.server.fs;

import java.nio.file.Path;


public final class PathContext {
    private final Path ftpRoot;
    private final Path sharedDirectory;
    private final Path usersDirectory;
    private final Path homeDirectory;

    public PathContext(Path ftpRoot, Path homeDirectory) {
        this.ftpRoot = ftpRoot.normalize().toAbsolutePath();
        this.sharedDirectory = this.ftpRoot.resolve("shared").normalize().toAbsolutePath();
        this.usersDirectory = this.ftpRoot.resolve("users").normalize().toAbsolutePath();
        this.homeDirectory = homeDirectory != null ? homeDirectory.normalize().toAbsolutePath() : null;
    }

    public Path ftpRoot() {
        return ftpRoot;
    }

    public Path sharedDirectory() {
        return sharedDirectory;
    }

    public Path usersDirectory() {
        return usersDirectory;
    }

    public Path homeDirectory() {
        return homeDirectory;
    }
}

