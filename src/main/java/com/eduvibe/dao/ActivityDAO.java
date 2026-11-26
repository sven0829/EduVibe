package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * com.eduvibe.dao.ActivityDAO  (NEW FILE)
 * Daily activity counts for the profile calendar. Read-only: counts the user's posts, comments
 * and daily logins (point_transactions.action_type = 'daily_login') per day.
 */
public class ActivityDAO {

    public Map<LocalDate, Integer> countsSince(int userId, LocalDate from) throws SQLException {

        String sql = """
            SELECT d, COUNT(*) n FROM (
                SELECT DATE(created_at) d FROM posts WHERE user_id = ? AND created_at >= ?
                UNION ALL
                SELECT DATE(created_at) FROM comments WHERE user_id = ? AND created_at >= ?
                UNION ALL
                SELECT DATE(created_at) FROM point_transactions
                    WHERE user_id = ? AND action_type = 'daily_login' AND created_at >= ?
            ) t GROUP BY d
            """;

        Map<LocalDate, Integer> map = new HashMap<>();
        Timestamp since = Timestamp.valueOf(from.atStartOfDay());

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < 3; i++) {
                ps.setInt(1 + i * 2, userId);
                ps.setTimestamp(2 + i * 2, since);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate(1);
                    if (d != null) {
                        map.put(d.toLocalDate(), rs.getInt(2));
                    }
                }
            }
        }
        return map;
    }

    /** Comments this user wrote today (server date). */
    public int commentsWrittenToday(int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM comments WHERE user_id = ? AND DATE(created_at) = CURDATE()")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
