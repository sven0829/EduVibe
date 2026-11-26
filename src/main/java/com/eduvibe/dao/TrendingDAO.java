package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * com.eduvibe.dao.TrendingDAO  (NEW FILE)
 *
 * Finds the most used #hashtags in recent visible posts (title + description).
 * Read-only, no schema change.
 */
public class TrendingDAO {

    public static final class Tag {
        public final String tag;   // includes the leading '#', lower case
        public final int count;

        Tag(String tag, int count) {
            this.tag = tag;
            this.count = count;
        }
    }

    private static final Pattern HASHTAG = Pattern.compile("#([\\p{L}\\p{N}_]{2,30})");

    public List<Tag> topTags(int limit) throws SQLException {

        String sql = """
            SELECT p.title, p.description
            FROM posts p
            JOIN users u ON u.id = p.user_id
            WHERE p.moderation_status IN ('clean', 'approved')
              AND u.status = 'active'
            ORDER BY p.created_at DESC
            LIMIT 300
            """;

        List<String> texts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                texts.add(rs.getString("title"));
                texts.add(rs.getString("description"));
            }
        }

        return countTags(texts, limit);
    }

    /** Pure counting logic (also used by the unit tests). */
    public static List<Tag> countTags(List<String> texts, int limit) {

        Map<String, Integer> counts = new HashMap<>();

        for (String text : texts) {
            if (text == null) {
                continue;
            }
            Matcher m = HASHTAG.matcher(text);
            while (m.find()) {
                String tag = "#" + m.group(1).toLowerCase(Locale.ROOT);
                counts.merge(tag, 1, Integer::sum);
            }
        }

        List<Tag> result = new ArrayList<>();
        counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() != a.getValue().intValue()
                        ? b.getValue() - a.getValue()
                        : a.getKey().compareTo(b.getKey()))
                .limit(limit)
                .forEach(e -> result.add(new Tag(e.getKey(), e.getValue())));
        return result;
    }
}
