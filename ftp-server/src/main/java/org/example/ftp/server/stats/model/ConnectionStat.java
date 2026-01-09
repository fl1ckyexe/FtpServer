package org.example.ftp.server.stats.model;

import java.time.Instant;

public class ConnectionStat {

    private final String username;
    private final String ip;
    private final Instant connectedAt;

    public ConnectionStat(String username, String ip, Instant connectedAt) {
        this.username = username;
        this.ip = ip;
        this.connectedAt = connectedAt;
    }

    public String username() {
        return username;
    }

    public String ip() {
        return ip;
    }

    public Instant connectedAt() {
        return connectedAt;
    }
}
