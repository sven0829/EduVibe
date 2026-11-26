
package com.eduvibe.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String URL =
            System.getenv().getOrDefault(
                    "EDUVIBE_DB_URL",
                    "jdbc:mysql://localhost:3306/eduvibe?useSSL=false&serverTimezone=Asia/Manila"
            );

    private static final String USER =
            System.getenv().getOrDefault("EDUVIBE_DB_USER", "root");

    private static final String PASSWORD =
            System.getenv("EDUVIBE_DB_PASSWORD");

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("EDUVIBE_DB_PASSWORD is not configured.");
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && connection.isValid(5);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
}
