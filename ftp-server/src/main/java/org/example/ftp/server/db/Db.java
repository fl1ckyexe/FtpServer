package org.example.ftp.server.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {

    private final String url;

    public Db(Path dbFile) {
        this.url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    public Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(url);
        configurePragmas(conn);
        return conn;
    }

    private void configurePragmas(Connection conn) throws SQLException {
        
        
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            
            st.execute("PRAGMA journal_mode = WAL");
            
            st.execute("PRAGMA synchronous = NORMAL");
            st.execute("PRAGMA cache_size = -10000"); 
            st.execute("PRAGMA temp_store = MEMORY");
            st.execute("PRAGMA mmap_size = 268435456"); 
        }
    }

    public void initSchema() {
        
        try (Connection c = getConnection();
             Statement st = c.createStatement()) {
            
            
            try {
                st.execute("ALTER TABLE shared_folders ADD COLUMN r INTEGER NOT NULL DEFAULT 1");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE shared_folders ADD COLUMN w INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE shared_folders ADD COLUMN e INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {}
            
            
            try {
                st.execute("ALTER TABLE users ADD COLUMN upload_speed INTEGER");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE users ADD COLUMN download_speed INTEGER");
            } catch (SQLException ignored) {}
            
            
            try {
                st.execute("UPDATE users SET upload_speed = rate_limit WHERE upload_speed IS NULL AND rate_limit IS NOT NULL AND rate_limit > 0");
                st.execute("UPDATE users SET download_speed = rate_limit WHERE download_speed IS NULL AND rate_limit IS NOT NULL AND rate_limit > 0");
            } catch (SQLException ignored) {}
            
            
            try {
                st.execute("ALTER TABLE server_settings ADD COLUMN global_upload_limit INTEGER");
                st.execute("UPDATE server_settings SET global_upload_limit = 200000 WHERE global_upload_limit IS NULL");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE server_settings ADD COLUMN global_download_limit INTEGER");
                st.execute("UPDATE server_settings SET global_download_limit = 200000 WHERE global_download_limit IS NULL");
            } catch (SQLException ignored) {}
            
            
            try {
                st.execute("ALTER TABLE server_settings ADD COLUMN admin_token TEXT");
            } catch (SQLException ignored) {}
            
            
            try {
                st.execute("""
                    INSERT INTO permissions(user_id, r, w, e)
                    SELECT u.id, 1, 1, 1
                    FROM users u
                    WHERE NOT EXISTS (
                        SELECT 1 FROM permissions p WHERE p.user_id = u.id
                    )
                    """);
            } catch (SQLException ignored) {}
            
        } catch (Exception e) {
            
        }
        
        
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              username TEXT NOT NULL UNIQUE,
              password_hash TEXT NOT NULL,
              enabled INTEGER NOT NULL DEFAULT 1,
              rate_limit INTEGER,
              upload_speed INTEGER,
              download_speed INTEGER
            );

            CREATE TABLE IF NOT EXISTS permissions (
              user_id INTEGER PRIMARY KEY,
              r INTEGER NOT NULL DEFAULT 1,
              w INTEGER NOT NULL DEFAULT 1,
              e INTEGER NOT NULL DEFAULT 1,
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS stats (
              user_id INTEGER PRIMARY KEY,
              logins INTEGER NOT NULL DEFAULT 0,
              bytes_uploaded INTEGER NOT NULL DEFAULT 0,
              bytes_downloaded INTEGER NOT NULL DEFAULT 0,
              last_login TEXT,
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS folders (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              path TEXT NOT NULL UNIQUE,
              owner_user_id INTEGER,
              is_global INTEGER NOT NULL DEFAULT 1,
              FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL
            );

            CREATE TABLE IF NOT EXISTS folder_permissions (
              user_id INTEGER NOT NULL,
              folder_id INTEGER NOT NULL,
              r INTEGER NOT NULL DEFAULT 0,
              w INTEGER NOT NULL DEFAULT 0,
              e INTEGER NOT NULL DEFAULT 0,
              PRIMARY KEY (user_id, folder_id),
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
              FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS shared_folders (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              owner_user_id INTEGER NOT NULL,
              user_to_share_id INTEGER NOT NULL,
              folder_name TEXT NOT NULL,
              folder_path TEXT NOT NULL,
              r INTEGER NOT NULL DEFAULT 1,
              w INTEGER NOT NULL DEFAULT 0,
              e INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
              FOREIGN KEY (user_to_share_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS server_settings (
              id INTEGER PRIMARY KEY CHECK (id = 1),
              global_max_connections INTEGER NOT NULL DEFAULT 20,
              global_rate_limit INTEGER NOT NULL DEFAULT 200000,
              global_upload_limit INTEGER NOT NULL DEFAULT 200000,
              global_download_limit INTEGER NOT NULL DEFAULT 200000,
              admin_token TEXT
            );

            INSERT OR IGNORE INTO server_settings(id, global_max_connections, global_rate_limit, global_upload_limit, global_download_limit)
            VALUES (1, 20, 200000, 200000, 200000);

            INSERT OR IGNORE INTO folders(path, owner_user_id, is_global) VALUES ('/', NULL, 1);
            INSERT OR IGNORE INTO folders(path, owner_user_id, is_global) VALUES ('/shared', NULL, 1);
            
            -- Create indexes for better query performance
            CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
            CREATE INDEX IF NOT EXISTS idx_shared_folders_user_to_share ON shared_folders(user_to_share_id);
            CREATE INDEX IF NOT EXISTS idx_shared_folders_owner ON shared_folders(owner_user_id);
            CREATE INDEX IF NOT EXISTS idx_shared_folders_path ON shared_folders(folder_path);
            CREATE INDEX IF NOT EXISTS idx_folder_permissions_user ON folder_permissions(user_id);
            CREATE INDEX IF NOT EXISTS idx_folder_permissions_folder ON folder_permissions(folder_id);
            """;

        try (Connection c = getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init database", e);
        }
    }

    public int execute(String sql, Object... params) {
        try (
                var conn = getConnection();
                var ps = conn.prepareStatement(sql)
        ) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("DB execute failed", e);
        }
    }

    public <T> T queryOne(String sql, ResultSetMapper<T> mapper, Object... params) {
        try (
                var conn = getConnection();
                var ps = conn.prepareStatement(sql)
        ) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (var rs = ps.executeQuery()) {
                if (rs.next()) return mapper.map(rs);
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("DB queryOne failed", e);
        }
    }
}