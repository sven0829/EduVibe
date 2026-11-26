
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminActivityLogDAO {

    public static class ActivityLog {

        private final int id;
        private final Integer postId;
        private final int adminId;
        private final String adminUsername;
        private final String action;
        private final String note;
        private final String createdAt;

        public ActivityLog(
                int id,
                Integer postId,
                int adminId,
                String adminUsername,
                String action,
                String note,
                String createdAt) {

            this.id = id;
            this.postId = postId;
            this.adminId = adminId;
            this.adminUsername = adminUsername;
            this.action = action;
            this.note = note;
            this.createdAt = createdAt;
        }

        public int getId() {
            return id;
        }

        public Integer getPostId() {
            return postId;
        }

        public int getAdminId() {
            return adminId;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public String getAction() {
            return action;
        }

        public String getNote() {
            return note;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
            SELECT
                ml.id,
                ml.post_id,
                ml.admin_id,
                u.username AS admin_username,
                ml.action,
                ml.note,
                DATE_FORMAT(
                    ml.created_at,
                    '%Y-%m-%d %H:%i:%s'
                ) AS created_at
            FROM moderation_logs ml
            LEFT JOIN users u
                ON ml.admin_id = u.id
            ORDER BY ml.created_at DESC, ml.id DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                int postValue = rs.getInt("post_id");
                Integer postId = rs.wasNull() ? null : postValue;

                logs.add(new ActivityLog(
                        rs.getInt("id"),
                        postId,
                        rs.getInt("admin_id"),
                        rs.getString("admin_username"),
                        rs.getString("action"),
                        rs.getString("note"),
                        rs.getString("created_at")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }
}
