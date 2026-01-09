package org.example.ftp.server.db;

public class SqliteServerSettingsRepository {

    public record ServerSettings(
            int globalMaxConnections,
            long globalRateLimit,
            long globalUploadLimit,
            long globalDownloadLimit
    ) {}

    private final Db db;

    public SqliteServerSettingsRepository(Db db) {
        this.db = db;
    }

    private void ensureRowExists() {
        db.execute(
                "INSERT OR IGNORE INTO server_settings(id, global_max_connections, global_rate_limit, global_upload_limit, global_download_limit) VALUES (1, 20, 200000, 200000, 200000)"
        );
    }

    public ServerSettings get() {
        ensureRowExists();
        return db.queryOne(
                "SELECT global_max_connections, global_rate_limit, global_upload_limit, global_download_limit FROM server_settings WHERE id = 1",
                rs -> new ServerSettings(
                        rs.getInt("global_max_connections"),
                        rs.getLong("global_rate_limit"),
                        rs.getLong("global_upload_limit"),
                        rs.getLong("global_download_limit")
                )
        );
    }

    public String getAdminToken() {
        ensureRowExists();
        return db.queryOne(
                "SELECT admin_token FROM server_settings WHERE id = 1",
                rs -> rs.getString("admin_token")
        );
    }

    public void saveAdminToken(String token) {
        ensureRowExists();
        db.execute(
                "UPDATE server_settings SET admin_token = ? WHERE id = 1",
                token
        );
    }

    
    public void save(Integer globalMaxConnections, Long globalRateLimit) {
        save(globalMaxConnections, globalRateLimit, null, null);
    }

    public void save(
            Integer globalMaxConnections,
            Long globalRateLimit,
            Long globalUploadLimit,
            Long globalDownloadLimit
    ) {
        ensureRowExists();

        if (globalMaxConnections != null) {
            db.execute(
                    "UPDATE server_settings SET global_max_connections = ? WHERE id = 1",
                    globalMaxConnections
            );
        }

        if (globalRateLimit != null) {
            db.execute(
                    "UPDATE server_settings SET global_rate_limit = ? WHERE id = 1",
                    globalRateLimit
            );
            
            db.execute(
                    "UPDATE server_settings SET global_upload_limit = ? WHERE id = 1",
                    globalRateLimit
            );
            db.execute(
                    "UPDATE server_settings SET global_download_limit = ? WHERE id = 1",
                    globalRateLimit
            );
        }

        
        if (globalUploadLimit != null) {
            db.execute(
                    "UPDATE server_settings SET global_upload_limit = ? WHERE id = 1",
                    globalUploadLimit
            );
        }

        if (globalDownloadLimit != null) {
            db.execute(
                    "UPDATE server_settings SET global_download_limit = ? WHERE id = 1",
                    globalDownloadLimit
            );
        }
    }
}

