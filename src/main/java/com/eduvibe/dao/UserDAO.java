
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByUsername(String username)
            throws SQLException {

        String sql = """
            SELECT id, full_name, username, email, bio,
                   profile_pic, role, status, points,
                   entertainment_limit_minutes, created_at
            FROM users
            WHERE username = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<User> findByEmail(String email)
            throws SQLException {

        String sql = """
            SELECT id, full_name, username, email, bio,
                   profile_pic, role, status, points,
                   entertainment_limit_minutes, created_at
            FROM users
            WHERE email = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<User> findById(int id) throws SQLException {

        String sql = """
            SELECT id, full_name, username, email, bio,
                   profile_pic, role, status, points,
                   entertainment_limit_minutes, created_at
            FROM users
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }

        return Optional.empty();
    }

    public boolean usernameExists(String username)
            throws SQLException {

        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean emailExists(String email)
            throws SQLException {

        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int create(String fullName, String username,
                      String email, String passwordHash)
            throws SQLException {

        String sql = """
            INSERT INTO users
            (full_name, username, email, password_hash)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setString(4, passwordHash);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("User ID was not generated.");
    }

    // ==========================================
    // DAILY LOGIN REWARD
    // ==========================================

    public boolean claimDailyLoginReward(int userId)
            throws SQLException {

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Manila")
        );

        String refKey = "login:" + today;

        String lockUserSql = """
            SELECT id
            FROM users
            WHERE id = ?
            FOR UPDATE
            """;

        String checkSql = """
            SELECT id
            FROM point_transactions
            WHERE user_id = ?
              AND action_type = 'daily_login'
              AND ref_key = ?
            LIMIT 1
            """;

        String updatePointsSql = """
            UPDATE users
            SET points = points + 20
            WHERE id = ?
            """;

        String insertTransactionSql = """
            INSERT INTO point_transactions
            (user_id, points, action_type, ref_key)
            VALUES (?, 20, 'daily_login', ?)
            """;

        String insertNotificationSql = """
            INSERT INTO notifications
            (user_id, type, message, is_read)
            VALUES (?, 'reward', ?, 0)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                // Lock the user row to prevent duplicate rewards.
                try (PreparedStatement ps =
                             conn.prepareStatement(lockUserSql)) {

                    ps.setInt(1, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "User account not found."
                            );
                        }
                    }
                }

                // Check if today's reward was already claimed.
                try (PreparedStatement ps =
                             conn.prepareStatement(checkSql)) {

                    ps.setInt(1, userId);
                    ps.setString(2, refKey);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            conn.commit();
                            return false;
                        }
                    }
                }

                // Add 20 points.
                try (PreparedStatement ps =
                             conn.prepareStatement(updatePointsSql)) {

                    ps.setInt(1, userId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not update user points."
                        );
                    }
                }

                // Save the points transaction.
                try (PreparedStatement ps =
                             conn.prepareStatement(insertTransactionSql)) {

                    ps.setInt(1, userId);
                    ps.setString(2, refKey);
                    ps.executeUpdate();
                }

                // Create the daily login notification.
                try (PreparedStatement ps =
                             conn.prepareStatement(insertNotificationSql)) {

                    ps.setInt(1, userId);
                    ps.setString(
                            2,
                            "Daily Login Reward! You earned 20 points "
                            + "for logging in today."
                    );

                    ps.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException | RuntimeException ex) {

                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }

                throw ex;

            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection closes after this method.
                }
            }
        }
    }

    // ==========================================
    // PASSWORD
    // ==========================================

    public Optional<String> findPasswordHash(String username)
            throws SQLException {

        String sql = """
            SELECT password_hash
            FROM users
            WHERE username = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("password_hash"));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Sets a new password hash only when the username and email both match
     * an ACTIVE account. Returns the user id, or empty when nothing matched.
     */
    public Optional<Integer> resetPasswordIfMatches(String username,
                                                    String email,
                                                    String newHash)
            throws SQLException {

        String findSql = """
            SELECT id
            FROM users
            WHERE username = ?
              AND LOWER(email) = LOWER(?)
              AND status = 'active'
            LIMIT 1
            """;

        String updateSql = """
            UPDATE users
            SET password_hash = ?
            WHERE id = ?
            """;

        String notifySql = """
            INSERT INTO notifications
            (user_id, type, message, is_read)
            VALUES (?, 'system', ?, 0)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int userId;

                try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                    ps.setString(1, username);
                    ps.setString(2, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Optional.empty();
                        }
                        userId = rs.getInt("id");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, newHash);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(notifySql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, "Your password was reset. "
                            + "If this was not you, change it again in Settings.");
                    ps.executeUpdate();
                }

                conn.commit();
                return Optional.of(userId);

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public Optional<String> findPasswordHashById(int userId)
            throws SQLException {

        String sql = """
            SELECT password_hash
            FROM users
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("password_hash"));
                }
            }
        }

        return Optional.empty();
    }

    // ==========================================
    // USER MAPPING
    // ==========================================

    private User mapUser(ResultSet rs) throws SQLException {

        User user = new User();

        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setBio(rs.getString("bio"));
        user.setProfilePic(rs.getString("profile_pic"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        user.setPoints(rs.getInt("points"));

        user.setEntertainmentLimitMinutes(
                rs.getInt("entertainment_limit_minutes")
        );

        Timestamp timestamp = rs.getTimestamp("created_at");

        if (timestamp != null) {
            user.setCreatedAt(timestamp.toLocalDateTime());
        }

        return user;
    }
}
