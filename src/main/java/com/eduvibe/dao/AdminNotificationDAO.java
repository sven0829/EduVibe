
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminNotificationDAO {

    private AdminNotificationDAO() {
    }

    public static int notifyAdmins(String message) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return notifyAdmins(connection, message);
        }
    }

    public static int notifyAdmins(Connection connection, String message)
            throws SQLException {

        if (connection == null || message == null || message.isBlank()) {
            return 0;
        }

        String findAdminsSQL = """
            SELECT id
            FROM users
            WHERE role = 'admin'
              AND status = 'active'
            """;

        String insertSQL = """
            INSERT INTO notifications
                (user_id, type, message, is_read, created_at)
            VALUES (?, 'system', ?, 0, NOW())
            """;

        int notificationCount = 0;

        try (PreparedStatement findStatement =
                     connection.prepareStatement(findAdminsSQL);
             ResultSet result = findStatement.executeQuery();
             PreparedStatement insertStatement =
                     connection.prepareStatement(insertSQL)) {

            while (result.next()) {
                insertStatement.setInt(1, result.getInt("id"));
                insertStatement.setString(2, message);
                insertStatement.executeUpdate();
                notificationCount++;
            }
        }

        return notificationCount;
    }
}
