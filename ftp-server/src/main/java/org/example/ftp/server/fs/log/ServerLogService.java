package org.example.ftp.server.fs.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ServerLogService {

    private static final ConcurrentLinkedDeque<String> LOGS = new ConcurrentLinkedDeque<>();

    
    private static final int MAX_LOGS = Integer.getInteger("ftp.log.max", 2000);
    private static final boolean CONSOLE = Boolean.parseBoolean(System.getProperty("ftp.log.console", "true"));

    public static void log(String message) {
        String line = "[" + Instant.now() + "] " + message;

        if (CONSOLE) {
            System.out.println(line);
        }

        LOGS.addLast(line);
        
        while (LOGS.size() > MAX_LOGS) {
            LOGS.pollFirst();
        }

    }

    public static List<String> getLogs() {
        return new ArrayList<>(LOGS); 
    }
}
