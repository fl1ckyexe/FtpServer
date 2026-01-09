package org.example.ftp.server.stats;

public interface StatsRepository {
    void onLogin(String username);
    void addUploaded(String username, long bytes);
    void addDownloaded(String username, long bytes);
}
