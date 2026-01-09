package org.example.ftp.server.session;


public class UnauthenticatedState implements SessionState {

    
    @Override
    public boolean canExecute(String command) {
        return command.equals("USER")
                || command.equals("PASS")
                || command.equals("QUIT")
                || command.equals("NOOP")
                || command.equals("FEAT")
                || command.equals("OPTS")
                || command.equals("PWD");
    }
}