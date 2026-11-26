
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.model.Post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    private static final int POST_POINTS = 15;

    private static final String BASE_QUERY = """
        SELECT
            p.id,
            p.user_id,
            u.username,
            u.profile_pic AS author_pic,
            p.category,
            p.title,
            p.description,
            p.media_type,
            p.media_path,
            p.is_reel,
            p.created_at,

            (SELECT COUNT(*)
             FROM post_likes pl
             WHERE pl.post_id = p.id) AS like_count,

            (SELECT COUNT(*)
             FROM comments c
             WHERE c.post_id = p.id) AS comment_count,

            (SELECT COUNT(*)
             FROM reports rp
             WHERE rp.post_id = p.id
               AND rp.status = 'pending'
               AND rp.source = 'user') AS pending_reports,

            EXISTS(
                SELECT 1
                FROM post_likes mylike
                WHERE mylike.post_id = p.id
                  AND mylike.user_id = ?
            ) AS liked_by_me

        FROM posts p
        INNER JOIN users u ON u.id = p.user_id

        WHERE p.moderation_status IN ('clean', 'approved')
          AND u.status = 'active'
          AND p.category IN ('academic', 'entertainment')
        """;

    // HOME FEED

    public List<Post> findHomeFeed(int currentUserId)
            throws SQLException {

        String sql = BASE_QUERY
                + " ORDER BY p.created_at DESC LIMIT 100";

        return executeFeedQuery(sql, currentUserId, null);
    }

    // FOLLOWING FEED

    public List<Post> findFollowingFeed(int currentUserId)
            throws SQLException {

        if (currentUserId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        String sql = BASE_QUERY + """
            AND EXISTS (
                SELECT 1
                FROM follows f
                WHERE f.follower_id = ?
                  AND f.following_id = p.user_id
            )
            ORDER BY p.created_at DESC
            LIMIT 100
            """;

        List<Post> posts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs));
                }
            }
        }

        return posts;
    }

    // CATEGORY FEED

    public List<Post> findByCategory(
            int currentUserId,
            String category
    ) throws SQLException {

        if (category == null
                || (!"academic".equalsIgnoreCase(category)
                && !"entertainment".equalsIgnoreCase(category))) {

            throw new IllegalArgumentException("Invalid post category.");
        }

        // Reels have their own page: a post published as a Reel never appears in Academic / Entertainment,
        // while its stored category is kept (it still shows as Academic or Entertainment on the Reel itself).
        String sql = BASE_QUERY
                + " AND p.category = ?"
                + " AND p.is_reel = 0"
                + " ORDER BY p.created_at DESC LIMIT 100";

        return executeFeedQuery(sql, currentUserId, category);
    }

    // POSTS BY ONE AUTHOR (profile page)

    public List<Post> findByUser(int viewerId, int authorId)
            throws SQLException {

        String sql = BASE_QUERY
                + " AND p.user_id = ?"
                + " ORDER BY p.created_at DESC LIMIT 100";

        List<Post> posts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, viewerId);
            ps.setInt(2, authorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs));
                }
            }
        }

        return posts;
    }

    // ACADEMIC REELS / ENTERTAINMENT REELS: category = ? AND is_reel = 1, decided by the database

    public List<Post> findReelsByCategory(int currentUserId, String category)
            throws SQLException {

        requireCategory(category);

        String sql = BASE_QUERY
                + " AND p.category = ? AND p.is_reel = 1"
                + " ORDER BY p.created_at DESC LIMIT 100";

        return executeFeedQuery(sql, currentUserId, category);
    }

    /** Reels of one category from the people this user follows. */
    public List<Post> findFollowingReelsByCategory(int currentUserId, String category)
            throws SQLException {

        requireCategory(category);

        String sql = BASE_QUERY + """
            AND p.category = ? AND p.is_reel = 1
            AND EXISTS (
                SELECT 1
                FROM follows f
                WHERE f.follower_id = ?
                  AND f.following_id = p.user_id
            )
            ORDER BY p.created_at DESC
            LIMIT 100
            """;

        List<Post> posts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentUserId);
            ps.setString(2, category.toLowerCase());
            ps.setInt(3, currentUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs));
                }
            }
        }

        return posts;
    }

    /** Administrator correction: changes only the category; the format (post / reel) is not touched. */
    public boolean updateCategory(int postId, String category) throws SQLException {

        if (postId <= 0) {
            throw new IllegalArgumentException("Invalid post ID.");
        }
        requireCategory(category);

        String sql = "UPDATE posts SET category = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.toLowerCase());
            ps.setInt(2, postId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * The author fixes the category of their OWN post (for example Academic chosen by mistake).
     * The database only changes it when the post belongs to this user; the format (post / reel) never changes.
     */
    public boolean updateCategoryForOwner(int postId, int userId, String category) throws SQLException {

        if (postId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("Invalid post or user.");
        }
        requireCategory(category);

        String sql = "UPDATE posts SET category = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.toLowerCase());
            ps.setInt(2, postId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private static void requireCategory(String category) {
        if (category == null
                || (!"academic".equalsIgnoreCase(category)
                && !"entertainment".equalsIgnoreCase(category))) {
            throw new IllegalArgumentException("Invalid post category.");
        }
    }

    // ALL REELS (both categories); the app itself uses the two category methods above

    public List<Post> findReels(int currentUserId)
            throws SQLException {

        String sql = BASE_QUERY
                + " AND p.is_reel = 1"
                + " ORDER BY p.created_at DESC LIMIT 100";

        return executeFeedQuery(sql, currentUserId, null);
    }

    // CREATE POST + AWARD POINTS + NOTIFY ADMINS

    public boolean createPost(
            int userId,
            String category,
            String title,
            String description,
            String mediaType,
            String mediaPath,
            boolean isReel
    ) throws SQLException {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        if (category == null
                || (!"academic".equalsIgnoreCase(category)
                && !"entertainment".equalsIgnoreCase(category))) {
            throw new IllegalArgumentException("Invalid post category.");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Post description cannot be empty."
            );
        }

        if (mediaType == null
                || (!"none".equalsIgnoreCase(mediaType)
                && !"image".equalsIgnoreCase(mediaType)
                && !"video".equalsIgnoreCase(mediaType))) {
            throw new IllegalArgumentException("Invalid media type.");
        }

        String insertPostSQL = """
            INSERT INTO posts (
                user_id, category, title, description,
                media_type, media_path, is_reel
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String awardPointsSQL = """
            UPDATE users
            SET points = points + ?
            WHERE id = ?
            """;

        String transactionSQL = """
            INSERT INTO point_transactions (
                user_id, points, action_type, ref_key
            )
            VALUES (?, ?, 'post', ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int postId;

                // Insert post.
                try (PreparedStatement ps = conn.prepareStatement(
                        insertPostSQL, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, userId);
                    ps.setString(2, category.toLowerCase());
                    ps.setString(3,
                            title == null || title.isBlank()
                                    ? null : title.trim());
                    ps.setString(4, description.trim());
                    ps.setString(5, mediaType.toLowerCase());
                    ps.setString(6, mediaPath);
                    ps.setBoolean(7, isReel);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Post could not be saved.");
                    }

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve the new post ID."
                            );
                        }

                        postId = keys.getInt(1);
                    }
                }

                // Award 15 points.
                try (PreparedStatement ps =
                             conn.prepareStatement(awardPointsSQL)) {

                    ps.setInt(1, POST_POINTS);
                    ps.setInt(2, userId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("User account was not found.");
                    }
                }

                // Record point transaction.
                try (PreparedStatement ps =
                             conn.prepareStatement(transactionSQL)) {

                    ps.setInt(1, userId);
                    ps.setInt(2, POST_POINTS);
                    ps.setString(3, "post:" + postId);
                    ps.executeUpdate();
                }

                // Get username for the admin notification.
                String username = "User";

                String usernameSQL =
                        "SELECT username FROM users WHERE id = ?";

                try (PreparedStatement ps =
                             conn.prepareStatement(usernameSQL)) {

                    ps.setInt(1, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            username = rs.getString("username");
                        }
                    }
                }

                // Notify active admins.
                String notificationMessage =
                        "@" + username
                        + " published a new "
                        + category.toUpperCase()
                        + (isReel ? " reel." : " post.")
                        + " Post ID: " + postId;

                AdminNotificationDAO.notifyAdmins(
                        conn,
                        notificationMessage
                );

                conn.commit();
                return true;

            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // EDIT OWN POST

    public boolean updatePost(
            int postId,
            int userId,
            String title,
            String description
    ) throws SQLException {

        if (postId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("Invalid post or user ID.");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Post description cannot be empty."
            );
        }

        String sql = """
            UPDATE posts
            SET title = ?, description = ?
            WHERE id = ? AND user_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,
                    title == null || title.isBlank()
                            ? null : title.trim());
            ps.setString(2, description.trim());
            ps.setInt(3, postId);
            ps.setInt(4, userId);

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE OWN POST

    public boolean deletePost(
            int postId,
            int userId
    ) throws SQLException {

        if (postId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("Invalid post or user ID.");
        }

        String sql = """
            DELETE FROM posts
            WHERE id = ? AND user_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, postId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        }
    }

    // EXECUTE STANDARD FEED QUERY

    private List<Post> executeFeedQuery(
            String sql,
            int currentUserId,
            String category
    ) throws SQLException {

        List<Post> posts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentUserId);

            if (category != null) {
                ps.setString(2, category.toLowerCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs));
                }
            }
        }

        return posts;
    }

    // MAP DATABASE RESULT TO POST

    private Post mapPost(ResultSet rs) throws SQLException {

        Post post = new Post();

        post.setId(rs.getInt("id"));
        post.setUserId(rs.getInt("user_id"));
        post.setUsername(rs.getString("username"));
        post.setAuthorProfilePic(rs.getString("author_pic"));
        post.setCategory(rs.getString("category"));
        post.setTitle(rs.getString("title"));
        post.setDescription(rs.getString("description"));
        post.setMediaType(rs.getString("media_type"));
        post.setMediaPath(rs.getString("media_path"));
        post.setReel(rs.getBoolean("is_reel"));

        java.sql.Timestamp timestamp = rs.getTimestamp("created_at");

        if (timestamp != null) {
            post.setCreatedAt(timestamp.toLocalDateTime());
        }

        post.setLikeCount(rs.getInt("like_count"));
        post.setCommentCount(rs.getInt("comment_count"));
        post.setLikedByCurrentUser(rs.getBoolean("liked_by_me"));

        // Only present in queries built on BASE_QUERY.
        try {
            post.setPendingReportCount(rs.getInt("pending_reports"));
        } catch (SQLException ignored) {
            post.setPendingReportCount(0);
        }

        return post;
    }
}
