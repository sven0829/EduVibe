package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * com.eduvibe.dao.RewardRollDAO  (NEW FILE)
 *
 * Playtime / RNG reward roll: the user pays ROLL_COST points and receives one random,
 * not-yet-owned digital reward from the existing `rewards` table (rarer rewards are less
 * likely). Everything happens in one transaction. No new tables or columns.
 * Points are in-system only; no real money is involved.
 */
public class RewardRollDAO {

    public static final int ROLL_COST = 100;

    public static final class Result {
        public int rewardId;
        public String name;
        public String description;
        public String rarity;
        public String type;
        public int newBalance;
    }

    /** Relative chance of a reward being picked, by rarity (higher = more likely). */
    public static int weight(String rarity) {
        if (rarity == null) {
            return 20;
        }
        return switch (rarity.trim().toLowerCase()) {
            case "common" -> 60;
            case "uncommon" -> 35;
            case "rare" -> 20;
            case "epic" -> 8;
            case "legendary" -> 3;
            default -> 20;
        };
    }

    /** Picks an index from the weights; package-private for tests. */
    static int pick(int[] weights, int randomValue) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int r = Math.floorMod(randomValue, Math.max(1, total));
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    /** Throws IllegalStateException with a readable message for expected problems. */
    public Result roll(int userId) throws SQLException {

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int balance;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT points FROM users WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("User not found.");
                        }
                        balance = rs.getInt(1);
                    }
                }

                if (balance < ROLL_COST) {
                    throw new IllegalStateException("You need " + ROLL_COST
                            + " points to roll. You have " + balance + ".");
                }

                List<Result> pool = new ArrayList<>();
                List<Integer> weights = new ArrayList<>();

                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT r.id, r.name, r.description, r.rarity, r.reward_type
                        FROM rewards r
                        WHERE r.is_active = 1
                          AND NOT EXISTS (SELECT 1 FROM user_rewards ur
                                          WHERE ur.user_id = ? AND ur.reward_id = r.id)
                        """)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Result r = new Result();
                            r.rewardId = rs.getInt("id");
                            r.name = rs.getString("name");
                            r.description = rs.getString("description");
                            r.rarity = rs.getString("rarity");
                            r.type = rs.getString("reward_type");
                            pool.add(r);
                            weights.add(weight(r.rarity));
                        }
                    }
                }

                if (pool.isEmpty()) {
                    throw new IllegalStateException(
                            "You already own every available reward. Nothing was charged.");
                }

                int[] w = new int[weights.size()];
                for (int i = 0; i < w.length; i++) {
                    w[i] = weights.get(i);
                }
                Result won = pool.get(pick(w, ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET points = points - ? WHERE id = ? AND points >= ?")) {
                    ps.setInt(1, ROLL_COST);
                    ps.setInt(2, userId);
                    ps.setInt(3, ROLL_COST);
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException("Not enough points to roll.");
                    }
                }

                int userRewardId = insertUserReward(conn, userId, won.rewardId, "rng_spin");
                if (userRewardId < 0) {
                    // the column may not accept that label: fall back to the known one
                    userRewardId = insertUserReward(conn, userId, won.rewardId, "purchase");
                }
                if (userRewardId < 0) {
                    throw new SQLException("Could not save your reward.");
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO point_transactions (user_id, points, action_type, ref_key)
                        VALUES (?, ?, 'reward_purchase', ?)
                        """)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, -ROLL_COST);
                    ps.setString(3, "roll:" + userRewardId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO notifications (user_id, type, message, is_read)
                        VALUES (?, 'reward', ?, 0)
                        """)) {
                    ps.setInt(1, userId);
                    ps.setString(2, "You won \"" + won.name + "\" from the Mystery Reward Roll!");
                    ps.executeUpdate();
                }

                conn.commit();
                won.newBalance = balance - ROLL_COST;
                return won;

            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Returns the new row id, or -1 if the label was rejected by the database. */
    private int insertUserReward(Connection conn, int userId, int rewardId, String via)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO user_rewards (user_id, reward_id, obtained_via)
                VALUES (?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, rewardId);
            ps.setString(3, via);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            if ("rng_spin".equals(via)) {
                return -1;   // label rejected (data truncated / constraint): caller retries
            }
            throw ex;
        }
    }
}

