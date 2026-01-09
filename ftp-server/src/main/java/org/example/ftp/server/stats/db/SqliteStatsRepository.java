package org.example.ftp.server.stats.db;

import org.example.ftp.server.db.Db;
import org.example.ftp.server.stats.model.UserStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SqliteStatsRepository {

    private final Db db;

    public SqliteStatsRepository(Db db) {
        this.db = db;
    }


    public void insertConnection(String username) {

        String selectUserId = """
            SELECT id FROM users WHERE username = ?
            """;

        String updateStats = """
            UPDATE stats
            SET logins = logins + 1,
                last_login = CURRENT_TIMESTAMP
            WHERE user_id = ?
            """;

        String insertStats = """
            INSERT INTO stats(user_id, logins, bytes_uploaded, bytes_downloaded, last_login)
            VALUES (?, 1, 0, 0, CURRENT_TIMESTAMP)
            """;

        try (Connection c = db.getConnection()) {

            Long userId = getUserId(c, username);
            if (userId == null) return;

            int updated;
            try (PreparedStatement ps = c.prepareStatement(updateStats)) {
                ps.setLong(1, userId);
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement ps = c.prepareStatement(insertStats)) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void addUploadedBytes(String username, long bytes) {

        String updateStats = """
            UPDATE stats
            SET bytes_uploaded = bytes_uploaded + ?
            WHERE user_id = ?
            """;

        try (Connection c = db.getConnection()) {

            Long userId = getUserId(c, username);
            if (userId == null) return;

            try (PreparedStatement ps = c.prepareStatement(updateStats)) {
                ps.setLong(1, bytes);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void setPermissions(
            String username,
            boolean r,
            boolean w,
            boolean e
    ) {

        String selectUserId = """
            SELECT id FROM users WHERE username = ?
            """;

        String updateSql = """
            UPDATE permissions
            SET r = ?, w = ?, e = ?
            WHERE user_id = ?
            """;

        String insertSql = """
            INSERT INTO permissions(user_id, r, w, e)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection c = db.getConnection()) {

            Long userId = null;

            try (PreparedStatement ps = c.prepareStatement(selectUserId)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong("id");
                    }
                }
            }

            if (userId == null) {
                throw new IllegalArgumentException("User not found: " + username);
            }

            int updated;
            try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                ps.setInt(1, r ? 1 : 0);
                ps.setInt(2, w ? 1 : 0);
                ps.setInt(3, e ? 1 : 0);
                ps.setLong(4, userId);
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setLong(1, userId);
                    ps.setInt(2, r ? 1 : 0);
                    ps.setInt(3, w ? 1 : 0);
                    ps.setInt(4, e ? 1 : 0);
                    ps.executeUpdate();
                }
            }

        }catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public void addDownloadedBytes(String username, long bytes) {

        String updateStats = """
            UPDATE stats
            SET bytes_downloaded = bytes_downloaded + ?
            WHERE user_id = ?
            """;

        try (Connection c = db.getConnection()) {

            Long userId = getUserId(c, username);
            if (userId == null) return;

            try (PreparedStatement ps = c.prepareStatement(updateStats)) {
                ps.setLong(1, bytes);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public List<UserStats> findAllUserStats() {

        String sql = """
            SELECT u.username,
                   s.logins,
                   s.bytes_uploaded,
                   s.bytes_downloaded,
                   s.last_login
            FROM stats s
            JOIN users u ON u.id = s.user_id
            ORDER BY s.last_login DESC
            """;

        List<UserStats> result = new ArrayList<>();

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String ts = rs.getString("last_login");
                Instant lastLogin = parseSqliteInstant(ts);

                result.add(
                        new UserStats(
                                rs.getString("username"),
                                rs.getInt("logins"),
                                rs.getLong("bytes_uploaded"),
                                rs.getLong("bytes_downloaded"),
                                lastLogin
                        )
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    private Long getUserId(Connection c, String username) throws SQLException {

        String sql = "SELECT id FROM users WHERE username = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        return null;
    }


    private Instant parseSqliteInstant(String value) {
        if (value == null) return null;

        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime
                    .parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }

        return null;
    }

    public void insertDownload(String username, long bytes) {

        String updateStats = """
        UPDATE stats
        SET bytes_downloaded = bytes_downloaded + ?
        WHERE user_id = (
            SELECT id FROM users WHERE username = ?
        )
        """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(updateStats)) {

            ps.setLong(1, bytes);
            ps.setString(2, username);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
