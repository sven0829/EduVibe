package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * com.eduvibe.dao.AchievementDAO  (NEW FILE)
 *
 * Read-only data for the Leaderboard, login streak and badges.
 * No schema change: everything is derived from users, posts, post_likes,
 * comments and point_transactions (daily_login rows, ref_key "login:YYYY-MM-DD").
 */
public class AchievementDAO {

    public static final class RankedUser {
        public final int rank;
        public final int userId;
        public final String fullName;
        public final String username;
        public final String profilePic;
        public final int points;

        RankedUser(int rank, int userId, String fullName, String username,
                   String profilePic, int points) {
            this.rank = rank;
            this.userId = userId;
            this.fullName = fullName;
            this.username = username;
            this.profilePic = profilePic;
            this.points = points;
        }
    }

    public static final class Stats {
        public int points;
        public int posts;
        public int likesReceived;
        public int commentsReceived;
        public int currentStreak;
        public int longestStreak;
        public int myRank;
    }

    public List<RankedUser> topUsers(int limit) throws SQLException {

        String sql = """
            SELECT id, full_name, username, profile_pic, points
            FROM users
            WHERE status = 'active'
            ORDER BY points DESC, id ASC
            LIMIT ?
            """;

        List<RankedUser> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                int previousPoints = Integer.MIN_VALUE;
                int shownRank = 1;

                while (rs.next()) {
                    int pts = rs.getInt("points");
                    // equal points share a rank
                    shownRank = (pts == previousPoints) ? shownRank : rank;
                    previousPoints = pts;

                    list.add(new RankedUser(shownRank,
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("profile_pic"),
                            pts));
                    rank++;
                }
            }
        }
        return list;
    }

    /** Login streak details for the sidebar and profile. */
    public static final class StreakInfo {
        public int current;
        public int longest;
        public boolean claimedToday;
        /** Oldest first; last entry is today. */
        public final boolean[] last7 = new boolean[7];
        public final LocalDate[] dates = new LocalDate[7];
    }

    public StreakInfo loadStreak(int userId) throws SQLException {

        Set<LocalDate> days = loginDays(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Manila"));

        StreakInfo info = new StreakInfo();
        info.current = currentStreak(days);
        info.longest = longestStreak(days);
        info.claimedToday = days.contains(today);
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.minusDays(6 - i);
            info.dates[i] = d;
            info.last7[i] = days.contains(d);
        }
        return info;
    }

    public Stats loadStats(int userId) throws SQLException {

        Stats s = new Stats();

        String sql = """
            SELECT
              (SELECT points FROM users WHERE id = ?) AS points,
              (SELECT COUNT(*) FROM posts WHERE user_id = ?) AS posts,
              (SELECT COUNT(*) FROM post_likes pl
                 JOIN posts p ON p.id = pl.post_id
                 WHERE p.user_id = ?) AS likes_received,
              (SELECT COUNT(*) FROM comments c
                 JOIN posts p ON p.id = c.post_id
                 WHERE p.user_id = ?) AS comments_received,
              (SELECT COUNT(*) + 1 FROM users
                 WHERE status = 'active'
                   AND points > (SELECT points FROM users WHERE id = ?)) AS my_rank
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 5; i++) {
                ps.setInt(i, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.points = rs.getInt("points");
                    s.posts = rs.getInt("posts");
                    s.likesReceived = rs.getInt("likes_received");
                    s.commentsReceived = rs.getInt("comments_received");
                    s.myRank = rs.getInt("my_rank");
                }
            }
        }

        Set<LocalDate> days = loginDays(userId);
        s.currentStreak = currentStreak(days);
        s.longestStreak = longestStreak(days);
        return s;
    }

    private Set<LocalDate> loginDays(int userId) throws SQLException {

        String sql = """
            SELECT ref_key
            FROM point_transactions
            WHERE user_id = ? AND action_type = 'daily_login'
            ORDER BY id DESC
            LIMIT 800
            """;

        Set<LocalDate> days = new HashSet<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("ref_key");
                    if (key != null && key.startsWith("login:")) {
                        try {
                            days.add(LocalDate.parse(key.substring(6)));
                        } catch (Exception ignored) {
                            // skip malformed keys
                        }
                    }
                }
            }
        }
        return days;
    }

    /** Consecutive days ending today (or yesterday, if today is not claimed yet). */
    static int currentStreak(Set<LocalDate> days) {

        LocalDate day = LocalDate.now(ZoneId.of("Asia/Manila"));

        if (!days.contains(day)) {
            day = day.minusDays(1);
        }

        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    static int longestStreak(Set<LocalDate> days) {

        int best = 0;

        for (LocalDate d : days) {
            if (days.contains(d.minusDays(1))) {
                continue; // not the start of a run
            }
            int length = 0;
            LocalDate cursor = d;
            while (days.contains(cursor)) {
                length++;
                cursor = cursor.plusDays(1);
            }
            best = Math.max(best, length);
        }
        return best;
    }
}
