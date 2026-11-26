
package com.eduvibe.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class DatabaseTest {

    public static void main(String[] args) {

        System.out.println("Testing EduVibe database connection...");

        try (Connection connection =
                DatabaseConnection.getConnection()) {

            DatabaseMetaData metadata = connection.getMetaData();

            System.out.println("================================");
            System.out.println("DATABASE CONNECTION SUCCESSFUL!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("MySQL: " + metadata.getDatabaseProductVersion());
            System.out.println("================================");

        } catch (Exception e) {

            System.out.println("DATABASE CONNECTION FAILED!");
            e.printStackTrace();

        }
    }
}

