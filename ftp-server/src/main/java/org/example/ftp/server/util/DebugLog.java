package org.example.ftp.server.util;

 
public final class DebugLog {

    private static final boolean ENABLED = Boolean.getBoolean("ftp.debug");

    private DebugLog() {}

    public static void d(String msg) {
        if (!ENABLED) return;
        System.out.println(msg);
    }
}

