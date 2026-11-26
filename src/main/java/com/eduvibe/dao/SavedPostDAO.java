
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.model.Post;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavedPostDAO {

    public boolean savePost(int userId, int postId)
            throws SQLException {

        String sql = """
            INSERT IGNORE INTO saved_posts (user_id, post_id)
            VALUES (?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, postId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeSavedPost(int userId, int postId)
            throws SQLException {

        String sql = """
            DELETE FROM saved_posts
            WHERE user_id = ? AND post_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, postId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean isPostSaved(int userId, int postId)
            throws SQLException {

        String sql = """
            SELECT 1 FROM saved_posts
            WHERE user_id = ? AND post_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, postId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Post> getSavedPosts(int userId)
            throws SQLException {

        List<Post> posts = new ArrayList<>();

        String sql = """
            SELECT
                p.id,
                p.user_id,
                u.username,
                p.category,
                p.title,
                p.description,
                p.media_type,
                p.media_path,
                p.is_reel,
                p.created_at,
                (SELECT COUNT(*) FROM post_likes pl
                 WHERE pl.post_id = p.id) AS like_count,
                (SELECT COUNT(*) FROM comments c
                 WHERE c.post_id = p.id) AS comment_count,
                (SELECT COUNT(*) FROM reports rp
                 WHERE rp.post_id = p.id
                   AND rp.status = 'pending'
                   AND rp.source = 'user') AS pending_reports,
                EXISTS(
                    SELECT 1 FROM post_likes pl
                    WHERE pl.post_id = p.id AND pl.user_id = ?
                ) AS liked_by_me
            FROM saved_posts sp
            INNER JOIN posts p ON p.id = sp.post_id
            INNER JOIN users u ON u.id = p.user_id
            WHERE sp.user_id = ?
              AND p.moderation_status IN ('clean', 'approved')
              AND u.status = 'active'
            ORDER BY sp.saved_at DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Post post = new Post();

                    post.setId(rs.getInt("id"));
                    post.setUserId(rs.getInt("user_id"));
                    post.setUsername(rs.getString("username"));
                    post.setCategory(rs.getString("category"));
                    post.setTitle(rs.getString("title"));
                    post.setDescription(rs.getString("description"));
                    post.setMediaType(rs.getString("media_type"));
                    post.setMediaPath(rs.getString("media_path"));
                    post.setReel(rs.getBoolean("is_reel"));

                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        post.setCreatedAt(timestamp.toLocalDateTime());
                    }

                    post.setLikeCount(rs.getInt("like_count"));
                    post.setCommentCount(rs.getInt("comment_count"));
                    post.setLikedByCurrentUser(
                            rs.getBoolean("liked_by_me")
                    );
                    post.setPendingReportCount(rs.getInt("pending_reports"));

                    posts.add(post);
                }
            }
        }

        return posts;
    }
}
