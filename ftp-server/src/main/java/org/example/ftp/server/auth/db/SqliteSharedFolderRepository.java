package org.example.ftp.server.auth.db;

import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.auth.model.SharedFolder;
import org.example.ftp.server.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteSharedFolderRepository {

    private final Db db;

    public SqliteSharedFolderRepository(Db db) {
        this.db = db;
    }

    public List<SharedFolder> findAll() {
        String sql = """
            SELECT id, owner_user_id, user_to_share_id, folder_name, folder_path, r, w, e
            FROM shared_folders
            ORDER BY folder_path
            """;

        List<SharedFolder> out = new ArrayList<>();

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return out;
    }

    public List<SharedFolder> findByUserToShare(long userToShareId) {
        String sql = """
            SELECT id, owner_user_id, user_to_share_id, folder_name, folder_path, r, w, e
            FROM shared_folders
            WHERE user_to_share_id = ?
            ORDER BY folder_path
            """;

        List<SharedFolder> out = new ArrayList<>();

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userToShareId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return out;
    }

    public void create(long ownerUserId, long userToShareId, String folderName, String folderPath, boolean read, boolean write, boolean execute) {
        db.execute(
            "INSERT INTO shared_folders(owner_user_id, user_to_share_id, folder_name, folder_path, r, w, e) VALUES (?, ?, ?, ?, ?, ?, ?)",
            ownerUserId, userToShareId, folderName, folderPath, read ? 1 : 0, write ? 1 : 0, execute ? 1 : 0
        );
    }

    public boolean exists(long ownerUserId, long userToShareId, String folderPath) {
        Integer count = db.queryOne(
            "SELECT COUNT(*) as cnt FROM shared_folders WHERE owner_user_id = ? AND user_to_share_id = ? AND folder_path = ?",
            rs -> rs.getInt("cnt"),
            ownerUserId, userToShareId, folderPath
        );
        return count != null && count > 0;
    }
    
    
    public boolean hasAccess(long userToShareId, String folderPath) {
        Integer count = db.queryOne(
            """
            SELECT COUNT(*) as cnt FROM shared_folders
            WHERE user_to_share_id = ? 
            AND (folder_path = ? OR (? LIKE folder_path || '/%'))
            """,
            rs -> rs.getInt("cnt"),
            userToShareId, folderPath, folderPath
        );
        return count != null && count > 0;
    }

    
    public boolean hasPermission(long userToShareId, String folderPath, Permission permission) {
        String sql = """
            SELECT r, w, e, LENGTH(folder_path) AS len
            FROM shared_folders
            WHERE user_to_share_id = ?
              AND (folder_path = ? OR (? LIKE folder_path || '/%'))
            ORDER BY len DESC
            LIMIT 1
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userToShareId);
            ps.setString(2, folderPath);
            ps.setString(3, folderPath);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                return switch (permission) {
                    case READ -> rs.getInt("r") == 1;
                    case WRITE -> rs.getInt("w") == 1;
                    case EXECUTE -> rs.getInt("e") == 1;
                };
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
    public Long findOwnerByFolderPath(String folderPath) {
        String sql = """
            SELECT owner_user_id FROM shared_folders
            WHERE folder_path = ?
            LIMIT 1
            """;
        
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, folderPath);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("owner_user_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return null;
    }
    
    
    public void deleteById(long id) {
        String sql = "DELETE FROM shared_folders WHERE id = ?";
        
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    public void deleteByFolderPath(String folderPath) {
        String sql = """
            DELETE FROM shared_folders
            WHERE folder_path = ? OR folder_path LIKE ? || '/%'
            """;
        
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, folderPath);
            ps.setString(2, folderPath);
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SharedFolder map(ResultSet rs) throws SQLException {
        return new SharedFolder(
            rs.getLong("id"),
            rs.getLong("owner_user_id"),
            rs.getLong("user_to_share_id"),
            rs.getString("folder_name"),
            rs.getString("folder_path"),
            rs.getInt("r") == 1,
            rs.getInt("w") == 1,
            rs.getInt("e") == 1
        );
    }
}

