package org.example.ftp.server.auth.db;

import org.example.ftp.server.auth.model.FolderPermission;
import org.example.ftp.server.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteFolderPermissionRepository {

    private final Db db;

    public SqliteFolderPermissionRepository(Db db) {
        this.db = db;
    }

    public FolderPermission find(long userId, long folderId) {

        String sql = """
            SELECT r, w, e
            FROM folder_permissions
            WHERE user_id = ? AND folder_id = ?
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, folderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new FolderPermission(false, false, false);
                }

                return new FolderPermission(
                        rs.getInt("r") == 1,
                        rs.getInt("w") == 1,
                        rs.getInt("e") == 1
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setPermissions(
            long userId,
            long folderId,
            boolean r,
            boolean w,
            boolean e
    ) {

        String update = """
            UPDATE folder_permissions
            SET r = ?, w = ?, e = ?
            WHERE user_id = ? AND folder_id = ?
            """;

        String insert = """
            INSERT INTO folder_permissions(user_id, folder_id, r, w, e)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection c = db.getConnection()) {

            int updated;
            try (PreparedStatement ps = c.prepareStatement(update)) {
                ps.setInt(1, r ? 1 : 0);
                ps.setInt(2, w ? 1 : 0);
                ps.setInt(3, e ? 1 : 0);
                ps.setLong(4, userId);
                ps.setLong(5, folderId);
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement ps = c.prepareStatement(insert)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, folderId);
                    ps.setInt(3, r ? 1 : 0);
                    ps.setInt(4, w ? 1 : 0);
                    ps.setInt(5, e ? 1 : 0);
                    ps.executeUpdate();
                }
            }

        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
