package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * com.eduvibe.dao.GoalsDAO  (NEW FILE)
 *
 * One-time-per-day bonus for finishing all four Daily Goals. No schema change: the bonus is a
 * point_transactions row with the already-valid action_type 'rng_spin' and ref_key
 * "goals:YYYY-MM-DD" (that key is what stops a second claim). The points are digital only.
 */
public class GoalsDAO {

    public static final int BONUS = 30;
    public static final int POINTS_GOAL = 50;

    private static String todayKey() {
        return "goals:" + LocalDate.now(ZoneId.of("Asia/Manila"));
    }

    public boolean claimedToday(int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT id FROM point_transactions
                     WHERE user_id = ? AND action_type = 'rng_spin' AND ref_key = ? LIMIT 1
                     """)) {
            ps.setInt(1, userId);
            ps.setString(2, todayKey());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Re-checks the four goals on the server side, then pays the bonus once. Returns the new balance. */
    public int claim(int userId) throws SQLException {

        boolean login = new AchievementDAO().loadStreak(userId).claimedToday;
        DashboardStatsDAO.UserStats st = new DashboardStatsDAO().loadStats(userId);
        int commented = new ActivityDAO().commentsWrittenToday(userId);

        if (!(login && st.postsToday >= 1 && commented >= 1 && st.pointsToday >= POINTS_GOAL)) {
            throw new IllegalStateException("Finish all four goals first.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                // lock this user's row so two clicks cannot both pay out
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT points FROM users WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, userId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("User not found.");
                        }
                    }
                }

                try (PreparedStatement chk = conn.prepareStatement("""
                        SELECT id FROM point_transactions
                        WHERE user_id = ? AND action_type = 'rng_spin' AND ref_key = ? LIMIT 1
                        """)) {
                    chk.setInt(1, userId);
                    chk.setString(2, todayKey());
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException("You already claimed today's goals bonus.");
                        }
                    }
                }

                try (PreparedStatement up = conn.prepareStatement(
                        "UPDATE users SET points = points + ? WHERE id = ?")) {
                    up.setInt(1, BONUS);
                    up.setInt(2, userId);
                    up.executeUpdate();
                }

                try (PreparedStatement ins = conn.prepareStatement("""
                        INSERT INTO point_transactions (user_id, points, action_type, ref_key)
                        VALUES (?, ?, 'rng_spin', ?)
                        """)) {
                    ins.setInt(1, userId);
                    ins.setInt(2, BONUS);
                    ins.setString(3, todayKey());
                    ins.executeUpdate();
                }

                int balance = 0;
                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT points FROM users WHERE id = ?")) {
                    sel.setInt(1, userId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            balance = rs.getInt(1);
                        }
                    }
                }

                conn.commit();
                return balance;

            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
