package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * com.eduvibe.dao.RewardDAO  (NEW FILE)
 *
 * Reward catalogue, purchase transaction and points history.
 * SQL is taken from the original RewardsPanel (verified columns only).
 */
public class RewardDAO {

    public static final class Reward {
        public int id;
        public String name;
        public String description;
        public String type;
        public String rarity;
        public int cost;
        public boolean owned;
    }

    public static final class HistoryEntry {
        public int points;
        public String action;
        public String refKey;
        public Timestamp createdAt;
    }

    public static final class OwnedReward {
        public int rewardId;
        public String name;
        public String description;
        public String type;
        public String rarity;
        public String obtainedVia;
    }

    /** Rewards the user owns, newest first. */
    public List<OwnedReward> loadOwned(int userId) throws SQLException {

        String sql = """
            SELECT r.id AS reward_id, r.name, r.description, r.reward_type, r.rarity, ur.obtained_via
            FROM user_rewards ur
            JOIN rewards r ON r.id = ur.reward_id
            WHERE ur.user_id = ?
            ORDER BY ur.id DESC
            """;

        List<OwnedReward> list = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OwnedReward o = new OwnedReward();
                    o.rewardId = rs.getInt("reward_id");
                    o.name = rs.getString("name");
                    o.description = rs.getString("description");
                    o.type = rs.getString("reward_type");
                    o.rarity = rs.getString("rarity");
                    o.obtainedVia = rs.getString("obtained_via");
                    list.add(o);
                }
            }
        }
        return list;
    }

    public int loadBalance(int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT points FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Reward> loadRewards(int userId) throws SQLException {

        RewardCatalogSeeder.seedIfSparse();

        String sql = """
            SELECT r.id, r.name, r.description,
                   r.reward_type, r.rarity, r.point_cost,
                   EXISTS (SELECT 1 FROM user_rewards ur
                           WHERE ur.user_id = ? AND ur.reward_id = r.id) AS owned
            FROM rewards r
            WHERE r.is_active = 1
            ORDER BY r.point_cost ASC
            """;

        List<Reward> list = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reward r = new Reward();
                    r.id = rs.getInt("id");
                    r.name = rs.getString("name");
                    r.description = rs.getString("description");
                    r.type = rs.getString("reward_type");
                    r.rarity = rs.getString("rarity");
                    r.cost = rs.getInt("point_cost");
                    r.owned = rs.getBoolean("owned");
                    list.add(r);
                }
            }
        }
        return list;
    }

    public List<HistoryEntry> loadHistory(int userId, int limit) throws SQLException {

        String sql = """
            SELECT points, action_type, ref_key, created_at
            FROM point_transactions
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

        List<HistoryEntry> list = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistoryEntry h = new HistoryEntry();
                    h.points = rs.getInt("points");
                    h.action = rs.getString("action_type");
                    h.refKey = rs.getString("ref_key");
                    h.createdAt = rs.getTimestamp("created_at");
                    list.add(h);
                }
            }
        }
        return list;
    }

    /**
     * Buys a reward in one transaction. Throws SQLException with a readable
     * message when the reward is gone, already owned, or unaffordable.
     * Returns the buyer's username (for the admin notification).
     */
    public String purchase(int userId, int rewardId) throws SQLException {

        String username = "User";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int cost;

                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT point_cost FROM rewards
                        WHERE id = ? AND is_active = 1 FOR UPDATE
                        """)) {
                    ps.setInt(1, rewardId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("This reward is no longer available.");
                        }
                        cost = rs.getInt("point_cost");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM user_rewards WHERE user_id = ? AND reward_id = ?")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, rewardId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new SQLException("You already own this reward.");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET points = points - ? WHERE id = ? AND points >= ?")) {
                    ps.setInt(1, cost);
                    ps.setInt(2, userId);
                    ps.setInt(3, cost);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Not enough points to purchase this reward.");
                    }
                }

                int userRewardId;

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO user_rewards (user_id, reward_id, obtained_via)
                        VALUES (?, ?, 'purchase')
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, rewardId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not save your reward.");
                        }
                        userRewardId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO point_transactions (user_id, points, action_type, ref_key)
                        VALUES (?, ?, 'reward_purchase', ?)
                        """)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, -cost);
                    ps.setString(3, "reward:" + userRewardId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT username FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            username = rs.getString("username");
                        }
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return username;
    }
}
