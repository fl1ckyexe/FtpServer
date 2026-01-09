package org.example.ftp.server.session;


public interface SessionState {
    
    boolean canExecute(String commandName);
}