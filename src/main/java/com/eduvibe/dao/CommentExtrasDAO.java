package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * com.eduvibe.dao.CommentExtrasDAO  (NEW FILE)
 * Likes and replies on comments, stored in the new comment_likes / comment_replies tables.
 */
public class CommentExtrasDAO {

    public static final class Reply {
        public int id;
        public int commentId;
        public int userId;
        public String username;
        public String content;
        public Timestamp createdAt;
    }

    /** Everything the comment list needs, loaded in one go. */
    public static final class Bundle {
        public boolean available;
        public final Map<Integer, Integer> likeCounts = new HashMap<>();
        public final Set<Integer> likedByMe = new HashSet<>();
        public final Map<Integer, List<Reply>> replies = new HashMap<>();
    }

    public Bundle load(List<Integer> commentIds, int viewerId) throws SQLException {

        Bundle b = new Bundle();
        b.available = ExtrasSchema.available();
        if (!b.available || commentIds.isEmpty()) {
            return b;
        }

        String in = placeholders(commentIds.size());

        try (Connection c = DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT comment_id, COUNT(*) n FROM comment_likes WHERE comment_id IN (" + in
                            + ") GROUP BY comment_id")) {
                bind(ps, commentIds, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        b.likeCounts.put(rs.getInt(1), rs.getInt(2));
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT comment_id FROM comment_likes WHERE user_id = ? AND comment_id IN (" + in + ")")) {
                ps.setInt(1, viewerId);
                bind(ps, commentIds, 2);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        b.likedByMe.add(rs.getInt(1));
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.id, r.comment_id, r.user_id, u.username, r.content, r.created_at
                    FROM comment_replies r JOIN users u ON u.id = r.user_id
                    WHERE r.comment_id IN (""" + in + ") ORDER BY r.created_at ASC")) {
                bind(ps, commentIds, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Reply r = new Reply();
                        r.id = rs.getInt(1);
                        r.commentId = rs.getInt(2);
                        r.userId = rs.getInt(3);
                        r.username = rs.getString(4);
                        r.content = rs.getString(5);
                        r.createdAt = rs.getTimestamp(6);
                        b.replies.computeIfAbsent(r.commentId, k -> new ArrayList<>()).add(r);
                    }
                }
            }
        }
        return b;
    }

    /** Likes the comment, or removes the like if it was already there. Returns true if now liked. */
    public boolean toggleLike(int commentId, int userId) throws SQLException {

        try (Connection c = DatabaseConnection.getConnection()) {

            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?")) {
                del.setInt(1, commentId);
                del.setInt(2, userId);
                if (del.executeUpdate() > 0) {
                    return false;
                }
            }

            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)")) {
                ins.setInt(1, commentId);
                ins.setInt(2, userId);
                ins.executeUpdate();
            }
            return true;
        }
    }

    public void addReply(int commentId, int userId, String content) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO comment_replies (comment_id, user_id, content) VALUES (?, ?, ?)")) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public boolean deleteReply(int replyId, int userId, boolean admin) throws SQLException {
        String sql = admin
                ? "DELETE FROM comment_replies WHERE id = ?"
                : "DELETE FROM comment_replies WHERE id = ? AND user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, replyId);
            if (!admin) {
                ps.setInt(2, userId);
            }
            return ps.executeUpdate() > 0;
        }
    }

    private static String placeholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    private static void bind(PreparedStatement ps, List<Integer> ids, int from) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            ps.setInt(from + i, ids.get(i));
        }
    }
}
