package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.eduvibe.dao.EquipDAO  (NEW FILE)
 * Which owned reward the user wears in each profile slot (frame / banner / theme / badge).
 * Stored in the new user_equipped table; a user can only equip rewards they own.
 */
public class EquipDAO {

    /** userId -> {frameName, rarity, timestamp}; a missing frame is cached as an empty array. */
    private static final Map<String, Object[]> FRAME_CACHE = new ConcurrentHashMap<>();
    private static final long FRAME_TTL_MS = 60_000L;

    /** Equipped frame of any user as {name, rarity}, or null if none. Cached briefly; call from a worker thread. */
    public static String[] frameOf(int userId) {
        return slotOf(userId, "frame");
    }

    /** Equipped badge of any user as {name, rarity}, or null if none. */
    public static String[] badgeOf(int userId) {
        return slotOf(userId, "badge");
    }

    /** Equipped reward of any user in a slot as {name, rarity}, or null. Cached; call off the EDT. */
    public static String[] slotOf(int userId, String slot) {
        String key = userId + ":" + slot;
        Object[] hit = FRAME_CACHE.get(key);
        if (hit != null && System.currentTimeMillis() - (Long) hit[1] < FRAME_TTL_MS) {
            return (String[]) hit[0];
        }
        String[] result = null;
        if (ExtrasSchema.available()) {
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT r.name, r.rarity FROM user_equipped e JOIN rewards r ON r.id = e.reward_id "
                                 + "WHERE e.user_id = ? AND e.slot = ?")) {
                ps.setInt(1, userId);
                ps.setString(2, slot);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result = new String[]{rs.getString(1), rs.getString(2)};
                    }
                }
            } catch (SQLException ignored) {
                return null;
            }
        }
        FRAME_CACHE.put(key, new Object[]{result, System.currentTimeMillis()});
        return result;
    }

    public static final String FRAME = "frame";
    public static final String BANNER = "banner";
    public static final String THEME = "theme";
    public static final String BADGE = "badge";

    /** Maps a reward_type text (any wording) to a profile slot. */
    public static String slotFor(String rewardType) {
        String t = rewardType == null ? "" : rewardType.toLowerCase();
        if (t.contains("frame") || t.contains("avatar")) {
            return FRAME;
        }
        if (t.contains("banner")) {
            return BANNER;
        }
        if (t.contains("theme")) {
            return THEME;
        }
        return BADGE;
    }

    /** slot -> reward id. Empty if the extra tables are unavailable. */
    public Map<String, Integer> equipped(int userId) throws SQLException {

        Map<String, Integer> map = new HashMap<>();
        if (!ExtrasSchema.available()) {
            return map;
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT slot, reward_id FROM user_equipped WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return map;
    }

    /** Returns false if the user does not own that reward. */
    public boolean equip(int userId, String slot, int rewardId) throws SQLException {

        if (!ExtrasSchema.available()) {
            return false;
        }

        try (Connection c = DatabaseConnection.getConnection()) {

            try (PreparedStatement own = c.prepareStatement(
                    "SELECT 1 FROM user_rewards WHERE user_id = ? AND reward_id = ? LIMIT 1")) {
                own.setInt(1, userId);
                own.setInt(2, rewardId);
                try (ResultSet rs = own.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO user_equipped (user_id, slot, reward_id) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE reward_id = VALUES(reward_id)
                    """)) {
                ps.setInt(1, userId);
                ps.setString(2, slot);
                ps.setInt(3, rewardId);
                ps.executeUpdate();
            }
            FRAME_CACHE.remove(userId + ":frame");
            FRAME_CACHE.remove(userId + ":badge");
            return true;
        }
    }

    public void unequip(int userId, String slot) throws SQLException {
        if (!ExtrasSchema.available()) {
            return;
        }
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM user_equipped WHERE user_id = ? AND slot = ?")) {
            ps.setInt(1, userId);
            ps.setString(2, slot);
            ps.executeUpdate();
            FRAME_CACHE.remove(userId + ":frame");
            FRAME_CACHE.remove(userId + ":badge");
        }
    }
}
