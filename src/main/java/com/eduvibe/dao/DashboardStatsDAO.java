package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * com.eduvibe.dao.DashboardStatsDAO  (NEW FILE)
 *
 * Read-only statistics for the dashboard sidebar and top bar.
 * Uses only columns already used elsewhere in the project:
 * users.points, posts.user_id/created_at, post_likes.post_id/user_id/created_at,
 * comments.post_id/user_id/created_at, point_transactions.user_id/points/created_at,
 * notifications.user_id/is_read.
 */
public class DashboardStatsDAO {

    public static final class UserStats {
        public final int points;
        public final int postsToday;
        public final int likesToday;
        public final int commentsToday;
        public final int pointsToday;
        public final int unreadNotifications;

        public UserStats(int points, int postsToday, int likesToday,
                         int commentsToday, int pointsToday,
                         int unreadNotifications) {
            this.points = points;
            this.postsToday = postsToday;
            this.likesToday = likesToday;
            this.commentsToday = commentsToday;
            this.pointsToday = pointsToday;
            this.unreadNotifications = unreadNotifications;
        }
    }

    /** "Today" is the database's current date (CURDATE()). */
    public UserStats loadStats(int userId) throws SQLException {

        String sql = """
            SELECT
              (SELECT points FROM users WHERE id = ?) AS points,
              (SELECT COUNT(*) FROM posts
                 WHERE user_id = ? AND DATE(created_at) = CURDATE()) AS posts_today,
              (SELECT COUNT(*) FROM post_likes pl
                 JOIN posts p ON p.id = pl.post_id
                 WHERE p.user_id = ? AND DATE(pl.created_at) = CURDATE()) AS likes_today,
              (SELECT COUNT(*) FROM comments c
                 JOIN posts p ON p.id = c.post_id
                 WHERE p.user_id = ? AND DATE(c.created_at) = CURDATE()) AS comments_today,
              (SELECT COALESCE(SUM(points), 0) FROM point_transactions
                 WHERE user_id = ? AND points > 0 AND DATE(created_at) = CURDATE()) AS points_today,
              (SELECT COUNT(*) FROM notifications
                 WHERE user_id = ? AND is_read = 0
                   AND (? = 1 OR COALESCE(type, '') <> 'like')
                   AND (? = 1 OR COALESCE(type, '') <> 'comment')
                   AND (? = 1 OR COALESCE(type, '') IN ('like', 'comment'))) AS unread
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 6; i++) {
                ps.setInt(i, userId);
            }
            ps.setInt(7, com.eduvibe.util.NotificationPrefs.isEnabled(
                    userId, com.eduvibe.util.NotificationPrefs.LIKES) ? 1 : 0);
            ps.setInt(8, com.eduvibe.util.NotificationPrefs.isEnabled(
                    userId, com.eduvibe.util.NotificationPrefs.COMMENTS) ? 1 : 0);
            ps.setInt(9, com.eduvibe.util.NotificationPrefs.isEnabled(
                    userId, com.eduvibe.util.NotificationPrefs.SYSTEM) ? 1 : 0);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserStats(
                            rs.getInt("points"),
                            rs.getInt("posts_today"),
                            rs.getInt("likes_today"),
                            rs.getInt("comments_today"),
                            rs.getInt("points_today"),
                            rs.getInt("unread"));
                }
            }
        }

        return new UserStats(0, 0, 0, 0, 0, 0);
    }

    public int countUnreadNotifications(int userId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM notifications "
                   + "WHERE user_id = ? AND is_read = 0 "
                   + "AND (? = 1 OR COALESCE(type, '') <> 'like') "
                   + "AND (? = 1 OR COALESCE(type, '') <> 'comment') "
                   + "AND (? = 1 OR COALESCE(type, '') IN ('like', 'comment'))";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, com.eduvibe.util.NotificationPrefs.isEnabled(
                    userId, com.eduvibe.util.NotificationPrefs.LIKES) ? 1 : 0);
            ps.setInt(3, com.eduvibe.util.NotificationPrefs.isEnabled(
                    userId, com.eduvibe.util.NotificationPrefs.COMMENTS) ? 1 : 0);
            ps.setInt(4, com.eduvibe.util.NotificationPrefs.isEnabled(
                    userId, com.eduvibe.util.NotificationPrefs.SYSTEM) ? 1 : 0);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Stores a 'reminder' notification (type already used by the Notifications screen). */
    public void addReminder(int userId, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, type, message, is_read) "
                   + "VALUES (?, 'reminder', ?, 0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
        }
    }
}
