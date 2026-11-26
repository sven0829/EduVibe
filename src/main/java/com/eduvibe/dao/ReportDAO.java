
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportDAO {

    private static final int AUTO_FLAG_THRESHOLD = 3;

    public enum Result { SUBMITTED, DUPLICATE, POST_NOT_FOUND }

    /** Old signature kept for compatibility: true only when the report was stored. */
    public boolean reportPost(int postId, int reporterId, String reason) {
        try {
            return submitReport(postId, reporterId, reason) == Result.SUBMITTED;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Distinguishes a new report, a duplicate pending report and a missing post; database errors are thrown. */
    public Result submitReport(int postId, int reporterId, String reason) throws SQLException {

        String lockPostSql = """
            SELECT id
            FROM posts
            WHERE id = ?
            FOR UPDATE
        """;

        String checkSql = """
            SELECT id
            FROM reports
            WHERE post_id = ?
              AND reporter_id = ?
              AND status = 'pending'
              AND source = 'user'
        """;

        String insertSql = """
            INSERT INTO reports
                (post_id, reporter_id, source, reason, status)
            VALUES (?, ?, 'user', ?, 'pending')
        """;

        String countSql = """
            SELECT COUNT(*)
            FROM reports
            WHERE post_id = ?
              AND status = 'pending'
              AND source = 'user'
        """;

        String flagSql = """
            UPDATE posts
            SET moderation_status = 'flagged',
                flagged_reason = ?
            WHERE id = ?
              AND moderation_status IN ('clean', 'approved')
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                // Lock the post while processing its report.
                try (PreparedStatement stmt =
                             conn.prepareStatement(lockPostSql)) {

                    stmt.setInt(1, postId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Result.POST_NOT_FOUND;
                        }
                    }
                }

                // Prevent duplicate pending reports.
                try (PreparedStatement stmt =
                             conn.prepareStatement(checkSql)) {

                    stmt.setInt(1, postId);
                    stmt.setInt(2, reporterId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return Result.DUPLICATE;
                        }
                    }
                }

                // Insert the user report.
                try (PreparedStatement stmt =
                             conn.prepareStatement(insertSql)) {

                    stmt.setInt(1, postId);
                    stmt.setInt(2, reporterId);
                    stmt.setString(3, reason);

                    if (stmt.executeUpdate() == 0) {
                        conn.rollback();
                        throw new SQLException("The report could not be saved.");
                    }
                }

                // Notify active admins about the new report.
                AdminNotificationDAO.notifyAdmins(
                        conn,
                        "A post has received a new user report. Post ID: "
                        + postId
                );

                // Count pending user reports.
                int totalReports = 0;

                try (PreparedStatement stmt =
                             conn.prepareStatement(countSql)) {

                    stmt.setInt(1, postId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            totalReports = rs.getInt(1);
                        }
                    }
                }

                // Automatically flag after 3 pending reports.
                if (totalReports >= AUTO_FLAG_THRESHOLD) {

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(flagSql)) {

                        stmt.setString(
                                1,
                                "Automatically flagged: "
                                + totalReports
                                + " pending user reports."
                        );

                        stmt.setInt(2, postId);

                        int updated = stmt.executeUpdate();

                        // Notify admins only when the post is newly flagged.
                        if (updated > 0) {
                            AdminNotificationDAO.notifyAdmins(
                                    conn,
                                    "A post was automatically flagged after "
                                    + "multiple reports. Post ID: "
                                    + postId
                            );
                        }
                    }
                }

                conn.commit();
                return Result.SUBMITTED;

            } catch (SQLException e) {
                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

        }
    }
}
