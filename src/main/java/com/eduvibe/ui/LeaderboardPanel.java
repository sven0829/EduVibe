package com.eduvibe.ui;

import com.eduvibe.dao.AchievementDAO;
import com.eduvibe.ui.components.Avatar;
import com.eduvibe.ui.components.EmptyState;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.PageHero;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * com.eduvibe.ui.LeaderboardPanel  (NEW FILE)
 *
 * Left: top 10 users by points. Right: your login streak, rank and badges.
 * Constructor LeaderboardPanel(int userId); call load() when the page opens.
 */
public class LeaderboardPanel extends JPanel {

    private final int userId;
    private final AchievementDAO dao = new AchievementDAO();

    private final JPanel boardList = new JPanel();
    private final JPanel sideColumn = new JPanel();
    private final JPanel podiumHost = new JPanel(new BorderLayout());
    private final PageHero hero = new PageHero(Icons.Type.TROPHY, "Leaderboard",
            "The top learners on EduVibe. Post, comment and log in daily to climb.",
            PageHero.LEADERBOARD[0], PageHero.LEADERBOARD[1]);

    private static final Color GOLD_M = new Color(255, 196, 46);
    private static final Color SILVER_M = new Color(203, 213, 225);
    private static final Color BRONZE_M = new Color(234, 160, 100);

    /** One badge: name, what it needs, and whether it is earned. */
    private record Badge(String name, String rule, Icons.Type icon, boolean earned) {
    }

    public LeaderboardPanel(int userId) {

        this.userId = userId;

        setLayout(new BorderLayout(20, 0));
        setBackground(Theme.BACKGROUND);

        boardList.setLayout(new BoxLayout(boardList, BoxLayout.Y_AXIS));
        boardList.setOpaque(false);

        sideColumn.setLayout(new BoxLayout(sideColumn, BoxLayout.Y_AXIS));
        sideColumn.setOpaque(false);

        podiumHost.setOpaque(false);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        podiumHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(podiumHost);
        JLabel title = new JLabel("The chase");
        title.setFont(Theme.bold(16));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(14, 2, 10, 0));
        head.add(title);
        left.add(head, BorderLayout.NORTH);
        left.add(boardList, BorderLayout.CENTER);

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setOpaque(false);
        leftWrap.add(left, BorderLayout.NORTH);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.setPreferredSize(new Dimension(340, 100));
        rightWrap.add(sideColumn, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.add(leftWrap, BorderLayout.CENTER);
        body.add(rightWrap, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        top.add(hero, BorderLayout.CENTER);

        removeAll();
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void load() {

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[]{dao.topUsers(10), dao.loadStats(userId)};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] data = get();
                    render((List<AchievementDAO.RankedUser>) data[0],
                            (AchievementDAO.Stats) data[1]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    boardList.removeAll();
                    boardList.add(new EmptyState(Icons.Type.ALERT,
                            "Unable to load the leaderboard",
                            "Check your database connection and try again."));
                    boardList.revalidate();
                    boardList.repaint();
                }
            }
        }.execute();
    }

    // ---------------- rendering ----------------

    private void render(List<AchievementDAO.RankedUser> users, AchievementDAO.Stats stats) {

        boardList.removeAll();

        if (users.isEmpty()) {
            boardList.add(new EmptyState(Icons.Type.TROPHY, "No learners yet",
                    "Earn points to appear here."));
        }

        hero.setChip("Your rank #" + stats.myRank, Icons.Type.STAR);

        podiumHost.removeAll();
        int max = users.isEmpty() ? 1 : Math.max(1, users.get(0).points);
        if (users.size() >= 3) {
            podiumHost.add(podium(users.get(0), users.get(1), users.get(2)), BorderLayout.CENTER);
        }
        for (AchievementDAO.RankedUser u : users) {
            if (users.size() >= 3 && u.rank <= 3) {
                continue;
            }
            boardList.add(row(u, max));
            boardList.add(Box.createVerticalStrut(8));
        }
        podiumHost.revalidate();

        sideColumn.removeAll();
        sideColumn.add(streakCard(stats));
        sideColumn.add(Box.createVerticalStrut(14));
        sideColumn.add(badgesCard(stats));

        revalidate();
        repaint();
    }

    private JComponent row(AchievementDAO.RankedUser u, int maxPoints) {

        boolean me = u.userId == userId;
        final double share = Math.max(0.04, Math.min(1.0, u.points / (double) maxPoints));

        RoundedPanel row = new RoundedPanel(14,
                me ? Theme.BLUE_SOFT : Theme.CARD,
                me ? Theme.BLUE : Theme.BORDER) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // faint bar: how close this learner is to the leader
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(Theme.brand(0, 0, getWidth(), 0, 0));
                g2.fillRoundRect(14, getHeight() - 5, (int) ((getWidth() - 28) * share), 3, 3, 3);
                g2.dispose();
            }
        };
        row.setLayout(new BorderLayout(14, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 18));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rank = new JLabel("#" + u.rank, SwingConstants.CENTER);
        rank.setPreferredSize(new Dimension(40, 34));
        rank.setFont(Theme.bold(15));
        rank.setForeground(switch (u.rank) {
            case 1 -> Theme.GOLD;
            case 2 -> Theme.isLight() ? new Color(100, 116, 139) : new Color(203, 213, 225);
            case 3 -> new Color(234, 160, 100);
            default -> Theme.MUTED;
        });
        if (u.rank <= 3) {
            rank.setIcon(null);
        }

        Avatar avatar = new Avatar(38);
        avatar.setUser(u.fullName, u.profilePic);
        avatar.showFrameOf(u.userId);

        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setOpaque(false);
        JLabel name = new JLabel(u.fullName == null ? u.username : u.fullName + (me ? "  (you)" : ""));
        name.setFont(Theme.bold(14));
        com.eduvibe.ui.components.RewardArt.attachBadge(name, u.userId, 20);
        name.setForeground(Theme.TEXT);
        JLabel handle = new JLabel("@" + u.username);
        handle.setFont(Theme.regular(12));
        handle.setForeground(Theme.MUTED);
        names.add(name);
        names.add(handle);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(avatar, BorderLayout.WEST);
        center.add(names, BorderLayout.CENTER);

        JLabel pts = new JLabel(u.points + " pts", Icons.icon(Icons.Type.COIN, 18, Theme.GOLD),
                SwingConstants.RIGHT);
        pts.setIconTextGap(8);
        pts.setFont(Theme.bold(14));
        pts.setForeground(Theme.GOLD);

        row.add(rank, BorderLayout.WEST);
        row.add(center, BorderLayout.CENTER);
        row.add(pts, BorderLayout.EAST);
        return row;
    }

    /** Top 3 as a podium: 2nd, 1st, 3rd, with the winner taller and glowing. */
    private JComponent podium(AchievementDAO.RankedUser first, AchievementDAO.RankedUser second,
                              AchievementDAO.RankedUser third) {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        row.add(podiumCard(second, SILVER_M, 164));
        row.add(podiumCard(first, GOLD_M, 200));
        row.add(podiumCard(third, BRONZE_M, 152));
        row.setPreferredSize(new Dimension(100, 206));
        return row;
    }

    private JComponent podiumCard(AchievementDAO.RankedUser u, Color medal, int height) {
        boolean me = u.userId == userId;
        final boolean winner = u.rank == 1;

        RoundedPanel card = new RoundedPanel(18, Theme.CARD, Theme.alpha(medal, 150)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(new GradientPaint(0, 0, Theme.alpha(medal, winner ? 70 : 44),
                        0, getHeight(), Theme.alpha(medal, 4)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
                super.paintComponent(g);
                if (winner) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    Theme.antialias(g3);
                    g3.setStroke(new BasicStroke(2f));
                    g3.setColor(Theme.alpha(medal, 210));
                    g3.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                    g3.dispose();
                }
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        JComponent medalIcon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                int d = 26;
                int x = (getWidth() - d) / 2;
                g2.setPaint(new GradientPaint(x, 0, medal.brighter(), x + d, d, medal.darker()));
                g2.fillOval(x, 0, d, d);
                g2.setColor(new Color(255, 255, 255, 140));
                g2.drawOval(x, 0, d - 1, d - 1);
                g2.setColor(new Color(40, 30, 10));
                g2.setFont(Theme.font(Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = String.valueOf(u.rank);
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, (d - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        medalIcon.setPreferredSize(new Dimension(30, 28));
        medalIcon.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        medalIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        Avatar avatar = new Avatar(winner ? 62 : 50);
        avatar.setUser(u.fullName, u.profilePic);
        avatar.showFrameOf(u.userId);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(u.fullName == null ? u.username : u.fullName, SwingConstants.CENTER);
        name.setFont(Theme.bold(13));
        name.setForeground(me ? Theme.BLUE_LIGHT : Theme.TEXT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setToolTipText("@" + u.username);

        JLabel pts = new JLabel(u.points + " pts", Icons.icon(Icons.Type.COIN, 14, Theme.GOLD), SwingConstants.CENTER);
        pts.setIconTextGap(6);
        pts.setFont(Theme.bold(12));
        pts.setForeground(Theme.GOLD);
        pts.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(medalIcon);
        card.add(Box.createVerticalStrut(6));
        card.add(avatar);
        card.add(Box.createVerticalStrut(8));
        card.add(name);
        card.add(Box.createVerticalStrut(3));
        card.add(pts);

        // sits on the bottom edge so the winner's taller card rises above the others
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(false);
        card.setPreferredSize(new Dimension(100, height));
        cell.add(card, BorderLayout.SOUTH);
        return cell;
    }

    private JComponent streakCard(AchievementDAO.Stats s) {

        RoundedPanel card = new RoundedPanel(16, Theme.CARD, Theme.alpha(new Color(251, 146, 60), 120)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(new GradientPaint(0, 0, new Color(251, 146, 60, 46), getWidth(), getHeight(),
                        new Color(244, 63, 94, 14)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("Login Streak");
        heading.setFont(Theme.bold(14));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(10));

        JLabel days = new JLabel(s.currentStreak + (s.currentStreak == 1 ? " day" : " days"),
                Icons.icon(Icons.Type.FLAME, 30, s.currentStreak > 0 ? new Color(251, 146, 60) : Theme.MUTED),
                SwingConstants.LEFT);
        days.setIconTextGap(10);
        days.setFont(Theme.bold(26));
        days.setForeground(Theme.TEXT);
        days.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(days);
        card.add(Box.createVerticalStrut(8));

        card.add(small("Best streak: " + s.longestStreak + (s.longestStreak == 1 ? " day" : " days")));
        card.add(small("Your rank: #" + s.myRank + "  •  " + s.points + " points"));
        return card;
    }

    private JComponent badgesCard(AchievementDAO.Stats s) {

        List<Badge> badges = List.of(
                new Badge("First Post", "Create 1 post", Icons.Type.PLUS, s.posts >= 1),
                new Badge("Creator", "Create 10 posts", Icons.Type.SPARKLE, s.posts >= 10),
                new Badge("Liked", "Receive 10 likes", Icons.Type.HEART, s.likesReceived >= 10),
                new Badge("Conversation", "Receive 10 comments", Icons.Type.COMMENT, s.commentsReceived >= 10),
                new Badge("Rising Star", "Reach 100 points", Icons.Type.STAR, s.points >= 100),
                new Badge("Scholar", "Reach 500 points", Icons.Type.CAP, s.points >= 500),
                new Badge("On Fire", "3-day login streak", Icons.Type.FLAME, s.longestStreak >= 3),
                new Badge("Dedicated", "7-day login streak", Icons.Type.CLOCK, s.longestStreak >= 7),
                new Badge("Unstoppable", "30-day login streak", Icons.Type.TROPHY, s.longestStreak >= 30));

        long earned = badges.stream().filter(Badge::earned).count();

        RoundedPanel card = new RoundedPanel(16, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("Badges  (" + earned + "/" + badges.size() + ")");
        heading.setFont(Theme.bold(14));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(0, 3, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Badge b : badges) {
            grid.add(badgeTile(b));
        }
        card.add(grid);
        return card;
    }

    private JComponent badgeTile(Badge b) {
        JComponent tile = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                int w = getWidth(), h = getHeight();
                if (b.earned()) {
                    g2.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.GOLD, 60), 0, h, Theme.alpha(Theme.GOLD, 12)));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    g2.setColor(Theme.alpha(Theme.GOLD, 190));
                } else {
                    g2.setColor(Theme.CARD_LIGHT);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    g2.setColor(Theme.BORDER);
                }
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                Icons.paint(g2, b.icon(), (w - 24) / 2f, 10, 24, b.earned() ? Theme.GOLD : new Color(100, 116, 139));
                g2.setFont(Theme.font(Font.BOLD, 10));
                g2.setColor(b.earned() ? Theme.TEXT : Theme.MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(b.name(), (w - fm.stringWidth(b.name())) / 2, 52);
                g2.dispose();
            }
        };
        tile.setPreferredSize(new Dimension(80, 64));
        tile.setToolTipText(b.earned() ? b.name() + " - earned" : b.name() + ": " + b.rule());
        return tile;
    }

    private JLabel small(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.regular(12));
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
