package org.example.ftp.server.auth.db;

import org.example.ftp.server.auth.model.Folder;
import org.example.ftp.server.db.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteFolderRepository {

    private final Db db;

    public SqliteFolderRepository(Db db) {
        this.db = db;
    }

    public List<Folder> findAll() {
        String sql = "SELECT id, path, owner_user_id, is_global FROM folders ORDER BY path";
        List<Folder> out = new ArrayList<>();

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

    public Optional<Folder> findByPath(String path) {
        String sql = "SELECT id, path, owner_user_id, is_global FROM folders WHERE path = ?";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, path);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    private Folder map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String path = rs.getString("path");

        
        long owner = rs.getLong("owner_user_id");
        Long ownerUserId = rs.wasNull() ? null : owner;

        boolean global = rs.getInt("is_global") == 1;

        return new Folder(id, path, ownerUserId, global);
    }
}