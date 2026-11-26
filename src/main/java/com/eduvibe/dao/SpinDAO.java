package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * com.eduvibe.dao.SpinDAO  (NEW FILE)
 *
 * Daily reward spin. One free spin per user per day (Asia/Manila date).
 * No schema change: the win is stored in point_transactions with
 * action_type 'rng_spin' (already understood by RewardsPanel) and a
 * ref_key of "spin:YYYY-MM-DD", which also prevents a second spin.
 */
public class SpinDAO {

    /** One segment of the wheel: either points, or a random digital reward of one kind. */
    public static final class Prize {
        public final String label;     // shown on the wheel
        public final int points;       // > 0 for point prizes
        public final String slot;      // "frame" / "banner" / "theme" / "badge" for reward prizes, else null
        public final int weight;

        Prize(String label, int points, String slot, int weight) {
            this.label = label;
            this.points = points;
            this.slot = slot;
            this.weight = weight;
        }

        public boolean isReward() {
            return slot != null;
        }
    }

    /** The wheel, in display order. Odds come from the weights (they add up to 100). */
    public static final Prize[] TABLE = {
        new Prize("5", 5, null, 20),
        new Prize("Frame", 0, "frame", 5),
        new Prize("10", 10, null, 18),
        new Prize("20", 20, null, 10),
        new Prize("Banner", 0, "banner", 5),
        new Prize("15", 15, null, 14),
        new Prize("50", 50, null, 5),
        new Prize("Theme", 0, "theme", 4),
        new Prize("25", 25, null, 8),
        new Prize("75", 75, null, 3),
        new Prize("Badge", 0, "badge", 6),
        new Prize("100", 100, null, 2),
    };

    /** Points given instead when a reward prize is won but the player already owns every one of that kind. */
    public static final int FALLBACK_POINTS = 50;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final class SpinResult {
        public final int prizeIndex;
        public final int points;
        public final int newBalance;
        public final String rewardName;     // null for point prizes
        public final String rewardRarity;
        public final String rewardSlot;
        public final boolean fallback;      // reward prize converted to points

        SpinResult(int prizeIndex, int points, int newBalance,
                   String rewardName, String rewardRarity, String rewardSlot, boolean fallback) {
            this.prizeIndex = prizeIndex;
            this.points = points;
            this.newBalance = newBalance;
            this.rewardName = rewardName;
            this.rewardRarity = rewardRarity;
            this.rewardSlot = rewardSlot;
            this.fallback = fallback;
        }

        public boolean wonReward() {
            return rewardName != null;
        }
    }

    private static String todayKey() {
        return "spin:" + LocalDate.now(ZoneId.of("Asia/Manila"));
    }

    public boolean hasSpunToday(int userId) throws SQLException {

        String sql = """
            SELECT id FROM point_transactions
            WHERE user_id = ? AND action_type = 'rng_spin' AND ref_key = ?
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, todayKey());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Rolls and records the spin atomically. Throws IllegalStateException if already used today. */
    public SpinResult spin(int userId) throws SQLException {

        int index = roll();
        Prize prizeDef = TABLE[index];
        int prize = prizeDef.points;

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

                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT id FROM point_transactions
                        WHERE user_id = ? AND action_type = 'rng_spin' AND ref_key = ?
                        LIMIT 1
                        """)) {
                    ps.setInt(1, userId);
                    ps.setString(2, todayKey());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException(
                                    "You already used today's spin. Come back tomorrow!");
                        }
                    }
                }

                String rewardName = null;
                String rewardRarity = null;
                boolean fallback = false;

                if (prizeDef.isReward()) {
                    int[] picked = new int[1];
                    String[] info = pickReward(conn, userId, prizeDef.slot, picked);
                    if (info == null) {
                        prize = FALLBACK_POINTS;
                        fallback = true;
                    } else {
                        rewardName = info[0];
                        rewardRarity = info[1];
                        grant(conn, userId, picked[0]);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET points = points + ? WHERE id = ?")) {
                    ps.setInt(1, prize);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO point_transactions
                        (user_id, points, action_type, ref_key)
                        VALUES (?, ?, 'rng_spin', ?)
                        """)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, prize);
                    ps.setString(3, todayKey());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO notifications (user_id, type, message, is_read)
                        VALUES (?, 'reward', ?, 0)
                        """)) {
                    ps.setInt(1, userId);
                    ps.setString(2, rewardName != null
                            ? "You won the " + rewardName + " from the daily spin! Find it in Customize."
                            : "You won " + prize + " points from the daily spin!");
                    ps.executeUpdate();
                }

                conn.commit();
                return new SpinResult(index, prize, balance + prize,
                        rewardName, rewardRarity, prizeDef.slot, fallback);

            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static int roll() {
        int total = 0;
        for (Prize p : TABLE) {
            total += p.weight;
        }
        int r = RANDOM.nextInt(total);
        for (int i = 0; i < TABLE.length; i++) {
            r -= TABLE[i].weight;
            if (r < 0) {
                return i;
            }
        }
        return 0;
    }

    private static int rarityWeight(String rarity) {
        String r = rarity == null ? "" : rarity.toLowerCase();
        return switch (r) {
            case "common" -> 50;
            case "uncommon" -> 30;
            case "rare" -> 14;
            case "epic" -> 5;
            case "legendary" -> 1;
            default -> 20;
        };
    }

    /** Random not-yet-owned reward of the given kind (cheaper rarities are likelier), or null. */
    private static String[] pickReward(Connection conn, int userId, String slot, int[] idOut) throws SQLException {

        java.util.List<int[]> ids = new java.util.ArrayList<>();
        java.util.List<String[]> info = new java.util.ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT r.id, r.name, r.reward_type, r.rarity FROM rewards r
                WHERE r.is_active = 1
                  AND NOT EXISTS (SELECT 1 FROM user_rewards ur
                                  WHERE ur.user_id = ? AND ur.reward_id = r.id)
                """)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (slot.equals(EquipDAO.slotFor(rs.getString("reward_type")))) {
                        ids.add(new int[] {rs.getInt("id"), rarityWeight(rs.getString("rarity"))});
                        info.add(new String[] {rs.getString("name"), rs.getString("rarity")});
                    }
                }
            }
        }

        int total = 0;
        for (int[] w : ids) {
            total += w[1];
        }
        if (total <= 0) {
            return null;
        }
        int r = RANDOM.nextInt(total);
        for (int i = 0; i < ids.size(); i++) {
            r -= ids.get(i)[1];
            if (r < 0) {
                idOut[0] = ids.get(i)[0];
                return info.get(i);
            }
        }
        return null;
    }

    /** Adds the reward to the player's collection ('rng', or 'purchase' if the column rejects that value). */
    private static void grant(Connection conn, int userId, int rewardId) throws SQLException {
        String sql = "INSERT INTO user_rewards (user_id, reward_id, obtained_via) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, rewardId);
            ps.setString(3, "rng");
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            // this database may not allow 'rng' here: use the value the shop already uses
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, rewardId);
            ps.setString(3, "purchase");
            ps.executeUpdate();
        }
    }
}
