package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * com.eduvibe.dao.ExtrasSchema  (NEW FILE)
 *
 * Creates three small NEW tables the first time they are needed (CREATE TABLE IF NOT EXISTS):
 *   comment_likes, comment_replies, user_equipped.
 * Existing tables and data are never altered or deleted. If the database user is not allowed
 * to create tables, available() is false and the extra features simply stay hidden.
 * The same SQL is also provided in eduvibe_extras.sql for running by hand.
 */
public final class ExtrasSchema {

    private static volatile Boolean ready;

    private ExtrasSchema() {
    }

    public static synchronized boolean available() {

        if (ready != null) {
            return ready;
        }

        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS comment_likes (
                    comment_id INT NOT NULL,
                    user_id INT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (comment_id, user_id),
                    KEY idx_comment_likes_user (user_id)
                )
                """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS comment_replies (
                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    comment_id INT NOT NULL,
                    user_id INT NOT NULL,
                    content TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_comment_replies_comment (comment_id)
                )
                """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS user_equipped (
                    user_id INT NOT NULL,
                    slot VARCHAR(20) NOT NULL,
                    reward_id INT NOT NULL,
                    PRIMARY KEY (user_id, slot)
                )
                """);

            // posts whose author switched downloads off (no row = downloads allowed)
            st.execute("""
                CREATE TABLE IF NOT EXISTS post_download_blocked (
                    post_id INT NOT NULL PRIMARY KEY,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

            ready = Boolean.TRUE;
        } catch (SQLException ex) {
            System.err.println("[EduVibe] Extra tables unavailable: " + ex.getMessage());
            ready = Boolean.FALSE;
        }
        return ready;
    }
}
