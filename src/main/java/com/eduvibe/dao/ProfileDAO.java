package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * com.eduvibe.dao.ProfileDAO  (NEW FILE)
 *
 * Profile edits and counters. Uses only users.full_name / bio / profile_pic /
 * points and posts.user_id, which the existing code already reads.
 */
public class ProfileDAO {

    public static final class Counters {
        public final int posts;
        public final int points;

        public Counters(int posts, int points) {
            this.posts = posts;
            this.points = points;
        }
    }

    public Counters loadCounters(int userId) throws SQLException {

        String sql = """
            SELECT (SELECT COUNT(*) FROM posts WHERE user_id = ?) AS posts,
                   (SELECT points FROM users WHERE id = ?) AS points
            """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Counters(rs.getInt("posts"), rs.getInt("points"));
                }
            }
        }
        return new Counters(0, 0);
    }

    public boolean updateProfile(int userId, String fullName, String bio) throws SQLException {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }
        if (fullName.trim().length() > 100) {
            throw new IllegalArgumentException("Full name is too long (max 100 characters).");
        }
        if (bio != null && bio.length() > 500) {
            throw new IllegalArgumentException("Bio is too long (max 500 characters).");
        }

        String sql = "UPDATE users SET full_name = ?, bio = ? WHERE id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, bio == null || bio.isBlank() ? null : bio.trim());
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateProfilePic(int userId, String relativePath) throws SQLException {

        String sql = "UPDATE users SET profile_pic = ? WHERE id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, relativePath);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** 0 means "no limit". Allowed range 0-600 minutes. */
    public boolean updateEntertainmentLimit(int userId, int minutes) throws SQLException {

        if (minutes < 0 || minutes > 600) {
            throw new IllegalArgumentException("Limit must be between 0 and 600 minutes.");
        }

        String sql = "UPDATE users SET entertainment_limit_minutes = ? WHERE id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
