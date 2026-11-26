package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * com.eduvibe.dao.CommentDAO  (NEW FILE)
 *
 * Reads, edits and deletes comments. Only columns already used by the
 * project are touched: comments.id/post_id/user_id/content/created_at.
 */
public class CommentDAO {

    public static final class CommentRow {
        public int id;
        public int userId;
        public String username;
        public String content;
        public Timestamp createdAt;
    }

    public List<CommentRow> findByPost(int postId) throws SQLException {

        String sql = """
            SELECT c.id, c.user_id, u.username, c.content, c.created_at
            FROM comments c
            JOIN users u ON c.user_id = u.id
            WHERE c.post_id = ?
            ORDER BY c.created_at ASC
            """;

        List<CommentRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CommentRow r = new CommentRow();
                    r.id = rs.getInt("id");
                    r.userId = rs.getInt("user_id");
                    r.username = rs.getString("username");
                    r.content = rs.getString("content");
                    r.createdAt = rs.getTimestamp("created_at");
                    rows.add(r);
                }
            }
        }
        return rows;
    }

    /** Only the comment's author can edit it. Returns false if nothing changed. */
    public boolean update(int commentId, int editorUserId, String newContent) throws SQLException {

        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }

        String sql = "UPDATE comments SET content = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newContent.trim());
            ps.setInt(2, commentId);
            ps.setInt(3, editorUserId);
            return ps.executeUpdate() > 0;
        }
    }

    /** The author or an administrator can delete. Returns false if nothing was deleted. */
    public boolean delete(int commentId, int userId, boolean isAdmin) throws SQLException {

        String sql = isAdmin
                ? "DELETE FROM comments WHERE id = ?"
                : "DELETE FROM comments WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            if (!isAdmin) {
                ps.setInt(2, userId);
            }
            return ps.executeUpdate() > 0;
        }
    }

    public int countByPost(int postId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM comments WHERE post_id = ?")) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean isAdmin(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT role FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "admin".equalsIgnoreCase(rs.getString(1));
            }
        }
    }
}
