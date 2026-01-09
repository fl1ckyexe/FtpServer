package org.example.ftp.server.auth.db;

import org.example.ftp.server.auth.User;
import org.example.ftp.server.db.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteUserRepository {

    private final Db db;

    public SqliteUserRepository(Db db) {
        this.db = db;
    }

    public Optional<User> findByUsername(String username) {
        String sql = """
            SELECT id, username, password_hash, enabled
            FROM users
            WHERE username = ?
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                return Optional.of(
                        new User(
                                rs.getLong("id"),
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getInt("enabled") == 1
                        )
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = """
            SELECT id, username, password_hash, enabled
            FROM users
            WHERE id = ?
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                return Optional.of(
                        new User(
                                rs.getLong("id"),
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getInt("enabled") == 1
                        )
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> findAll() {

        String sql = "SELECT * FROM users";
        List<User> result = new ArrayList<>();

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(
                        new User(
                                rs.getLong("id"),
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getInt("enabled") == 1
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


    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getInt("enabled") == 1
        );
    }

    public void delete(String username) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void update(String username, boolean enabled, Long rateLimit) {
        String sql = """
        UPDATE users
        SET enabled = ?, rate_limit = ?
        WHERE username = ?
        """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, enabled ? 1 : 0);

            if (rateLimit == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setLong(2, rateLimit);
            }

            ps.setString(3, username);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(String username, boolean enabled, Long rateLimit, Long uploadSpeed, Long downloadSpeed) {
        String sql = """
        UPDATE users
        SET enabled = ?, rate_limit = ?, upload_speed = ?, download_speed = ?
        WHERE username = ?
        """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, enabled ? 1 : 0);

            if (rateLimit == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setLong(2, rateLimit);
            }

            if (uploadSpeed == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setLong(3, uploadSpeed);
            }

            if (downloadSpeed == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setLong(4, downloadSpeed);
            }

            ps.setString(5, username);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Long getRateLimit(String username) {
        return db.queryOne(
                "SELECT rate_limit FROM users WHERE username = ?",
                rs -> {
                    long v = rs.getLong("rate_limit");
                    return rs.wasNull() ? null : v;
                },
                username
        );
    }

    
    public Long getUploadSpeed(String username) {
        return db.queryOne(
                "SELECT upload_speed FROM users WHERE username = ?",
                rs -> {
                    long v = rs.getLong("upload_speed");
                    return rs.wasNull() ? null : v;
                },
                username
        );
    }

    
    public Long getDownloadSpeed(String username) {
        return db.queryOne(
                "SELECT download_speed FROM users WHERE username = ?",
                rs -> {
                    long v = rs.getLong("download_speed");
                    return rs.wasNull() ? null : v;
                },
                username
        );
    }


    public void create(String username, String passwordHash) {
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
