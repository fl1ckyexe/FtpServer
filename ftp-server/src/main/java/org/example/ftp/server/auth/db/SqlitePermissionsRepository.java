package org.example.ftp.server.auth.db;

import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlitePermissionsRepository {

    private final Db db;

    public SqlitePermissionsRepository(Db db) {
        this.db = db;
    }

    public boolean hasPermission(String username, Permission permission) {

        String sql = """
            SELECT p.r, p.w, p.e
            FROM users u
            JOIN permissions p ON p.user_id = u.id
            WHERE u.username = ?
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    
                    
                    return false;
                }

                int r = rs.getInt("r");
                int w = rs.getInt("w");
                int e = rs.getInt("e");
                
                boolean result = switch (permission) {
                    case READ -> r == 1;
                    case WRITE -> w == 1;
                    case EXECUTE -> e == 1;
                };
                
                return result;
            }

        } catch (SQLException exe) {
            throw new RuntimeException(exe);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public PermissionRow findByUsername(String username) {

        String sql = """
            SELECT p.r, p.w, p.e
            FROM users u
            JOIN permissions p ON p.user_id = u.id
            WHERE u.username = ?
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new PermissionRow(
                        rs.getInt("r") == 1,
                        rs.getInt("w") == 1,
                        rs.getInt("e") == 1
                );
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (Exception exe) {
            throw new RuntimeException(exe);
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
                throw new IllegalArgumentException(
                        "User not found: " + username
                );
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

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public record PermissionRow(
            boolean read,
            boolean write,
            boolean execute
    ) {}
}
