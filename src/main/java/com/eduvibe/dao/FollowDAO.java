
package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FollowDAO {

    public boolean followUser(int followerId, int followingId) {
        if (followerId <= 0 || followingId <= 0
                || followerId == followingId) {
            return false;
        }

        String sql = """
            INSERT IGNORE INTO follows (follower_id, following_id)
            VALUES (?, ?)
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, followerId);
            ps.setInt(2, followingId);

            int inserted = ps.executeUpdate();

            if (inserted > 0) {
                notifyAdminsAboutFollow(con, followerId, followingId);
                return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void notifyAdminsAboutFollow(
            Connection con, int followerId, int followingId) {

        String sql = """
            SELECT
                follower.username AS follower_username,
                following.username AS following_username
            FROM users follower
            JOIN users following
                ON following.id = ?
            WHERE follower.id = ?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, followingId);
            ps.setInt(2, followerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String follower = rs.getString("follower_username");
                    String following = rs.getString("following_username");

                    String message = "@" + follower
                            + " started following @" + following + ".";

                    try {
                        AdminNotificationDAO.notifyAdmins(con, message);
                    } catch (SQLException e) {
                        // Do not undo a successful follow if notification fails.
                        e.printStackTrace();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean unfollowUser(int followerId, int followingId) {
        String sql = """
            DELETE FROM follows
            WHERE follower_id = ? AND following_id = ?
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, followerId);
            ps.setInt(2, followingId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isFollowing(int followerId, int followingId) {
        String sql = """
            SELECT 1 FROM follows
            WHERE follower_id = ? AND following_id = ?
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, followerId);
            ps.setInt(2, followingId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getFollowersCount(int userId) {
        return getCount(
            "SELECT COUNT(*) FROM follows WHERE following_id = ?", userId
        );
    }

    public int getFollowingCount(int userId) {
        return getCount(
            "SELECT COUNT(*) FROM follows WHERE follower_id = ?", userId
        );
    }

    private int getCount(String sql, int userId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
