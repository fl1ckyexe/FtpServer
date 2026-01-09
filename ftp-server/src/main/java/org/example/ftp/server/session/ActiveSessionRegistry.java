package org.example.ftp.server.session;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

 
public class ActiveSessionRegistry {

    private final Set<Socket> controlSockets = ConcurrentHashMap.newKeySet();

    public void register(Socket controlSocket) {
        if (controlSocket == null) return;
        controlSockets.add(controlSocket);
    }

    public void unregister(Socket controlSocket) {
        if (controlSocket == null) return;
        controlSockets.remove(controlSocket);
    }

    public void disconnectAll() {
        for (Socket s : controlSockets) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
        controlSockets.clear();
    }
}

