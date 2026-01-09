package org.example.ftp.server.stats.model;

import java.time.Instant;

public class UserStats {

    private final String username;
    private final int logins;
    private final long bytesUploaded;
    private final long bytesDownloaded;
    private final Instant lastLogin;

    public UserStats(
            String username,
            int logins,
            long bytesUploaded,
            long bytesDownloaded,
            Instant lastLogin
    ) {
        this.username = username;
        this.logins = logins;
        this.bytesUploaded = bytesUploaded;
        this.bytesDownloaded = bytesDownloaded;
        this.lastLogin = lastLogin;
    }

    public String username() { return username; }
    public int logins() { return logins; }
    public long bytesUploaded() { return bytesUploaded; }
    public long bytesDownloaded() { return bytesDownloaded; }
    public Instant lastLogin() { return lastLogin; }
}
