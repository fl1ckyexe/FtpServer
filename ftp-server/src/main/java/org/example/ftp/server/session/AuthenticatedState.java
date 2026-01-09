package org.example.ftp.server.session;


public class AuthenticatedState implements SessionState {

    
    @Override
    public boolean canExecute(String command) {
        return true;  
    }
}
