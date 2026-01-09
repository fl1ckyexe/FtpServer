package org.example.ftp.server.stats;

import org.example.ftp.server.stats.db.SqliteStatsRepository;
import org.example.ftp.server.stats.model.ConnectionStat;
import org.example.ftp.server.stats.model.UserStats;

import java.util.List;

public class StatsService {

    private final SqliteStatsRepository repository;

    public StatsService(SqliteStatsRepository repository) {
        this.repository = repository;
    }

    public void onLogin(String username) {
        repository.insertConnection(username);
    }
    public void onUpload(String username, long bytes) {
        repository.addUploadedBytes(username, bytes);
    }
    public List<UserStats> getAllUserStats() {
        return repository.findAllUserStats();
    }


    public void onDownload(String username, long bytes) {
        repository.insertDownload(username, bytes);
    }


}

