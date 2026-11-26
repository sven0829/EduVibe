package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * com.eduvibe.dao.PostDownloadDAO  (NEW FILE)
 *
 * Remembers which posts have downloads switched off by their author. A post with no row can be downloaded.
 * If the small table cannot be created, downloads simply stay allowed and the switch is hidden.
 */
public class PostDownloadDAO {

    public boolean isBlocked(int postId) throws SQLException {
        if (!ExtrasSchema.available()) {
            return false;
        }
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM post_download_blocked WHERE post_id = ?")) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Only the post's author may change it (checked here against the database, not just in the screen). */
    public void setBlocked(int postId, int userId, boolean blocked) throws SQLException {
        if (!ExtrasSchema.available()) {
            throw new SQLException("Download settings are not available.");
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement own = c.prepareStatement("SELECT 1 FROM posts WHERE id = ? AND user_id = ?")) {
                own.setInt(1, postId);
                own.setInt(2, userId);
                try (ResultSet rs = own.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Only the author can change this.");
                    }
                }
            }
            String sql = blocked
                    ? "INSERT IGNORE INTO post_download_blocked (post_id) VALUES (?)"
                    : "DELETE FROM post_download_blocked WHERE post_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, postId);
                ps.executeUpdate();
            }
        }
    }
}
