
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LikeDAO {

    public boolean hasUserLiked(int postId, int userId)
            throws SQLException {

        String sql = """
                SELECT 1
                FROM post_likes
                WHERE post_id = ?
                  AND user_id = ?
                LIMIT 1
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            statement.setInt(2, userId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public int getLikeCount(int postId) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM post_likes
                WHERE post_id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, postId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        return 0;
    }

    public boolean addLike(int postId, int userId)
            throws SQLException {

        String sql = """
                INSERT INTO post_likes (post_id, user_id)
                VALUES (?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean removeLike(int postId, int userId)
            throws SQLException {

        String sql = """
                DELETE FROM post_likes
                WHERE post_id = ?
                  AND user_id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean toggleLike(int postId, int userId)
            throws SQLException {

        if (postId <= 0 || userId <= 0) {
            throw new IllegalArgumentException(
                    "Post ID and User ID must be valid."
            );
        }

        if (hasUserLiked(postId, userId)) {
            removeLike(postId, userId);
            return false;
        }

        addLike(postId, userId);
        return true;
    }
}
