package org.example.ftp.server.session.memento;

import org.example.ftp.server.session.SessionState;

import java.nio.file.Path;


public class SessionMemento {

    private final SessionState state;
    private final String pendingUsername;
    private final String username;
    private final boolean authenticated;
    private final Path currentDirectory;
    private final Path homeDirectory;

    public SessionMemento(
            SessionState state,
            String pendingUsername,
            String username,
            boolean authenticated,
            Path currentDirectory,
            Path homeDirectory
    ) {
        this.state = state;
        this.pendingUsername = pendingUsername;
        this.username = username;
        this.authenticated = authenticated;
        this.currentDirectory = currentDirectory;
        this.homeDirectory = homeDirectory;
    }

    public SessionState getState() {
        return state;
    }

    public String getPendingUsername() {
        return pendingUsername;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public Path getHomeDirectory() {
        return homeDirectory;
    }
    
}

