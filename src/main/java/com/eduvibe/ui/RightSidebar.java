package com.eduvibe.ui;

import com.eduvibe.dao.DashboardStatsDAO;
import com.eduvibe.dao.TrendingDAO;
import com.eduvibe.model.User;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.util.EntertainmentTimeTracker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.RightSidebar  (NEW FILE)
 *
 * Right-hand info column of the dashboard. Every number comes from the database
 * (DashboardStatsDAO) or the session timer; nothing is hard-coded.
 */
public class RightSidebar extends JPanel {

    private final User user;
    private final Consumer<String> navigator;
    private final EntertainmentTimeTracker tracker;
    private final DashboardStatsDAO statsDAO = new DashboardStatsDAO();
    private final TrendingDAO trendingDAO = new TrendingDAO();
    private final JPanel tagsBody = new JPanel();
    private Consumer<String> tagListener = t -> { };

    private final JLabel pointsValue = valueLabel("…", 30, Theme.GOLD);
    private final JLabel postsValue = valueLabel("…", 18, Theme.TEXT);
    private final JLabel likesValue = valueLabel("…", 18, Theme.TEXT);
    private final JLabel commentsValue = valueLabel("…", 18, Theme.TEXT);
    private final JLabel pointsTodayValue = valueLabel("…", 18, Theme.GREEN);
    private final JLabel timeLabel = new JLabel();
    private final JProgressBar timeBar = new JProgressBar(0, 100);

    private Consumer<Integer> unreadListener = n -> { };

    public RightSidebar(User user, Consumer<String> navigator,
                        EntertainmentTimeTracker tracker) {
        this.user = user;
        this.navigator = navigator;
        this.tracker = tracker;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        add(pointsCard());
        add(Box.createVerticalStrut(14));
        add(streakCard());
        add(Box.createVerticalStrut(14));
        add(goalsCard());
        add(Box.createVerticalStrut(14));
        add(quickAccessCard());
        add(Box.createVerticalStrut(14));
        add(todayCard());
        add(Box.createVerticalStrut(14));
        add(trendingCard());
        add(Box.createVerticalStrut(14));
        add(timeCard());
        add(Box.createVerticalGlue());

        updateTime();
        new Timer(30_000, e -> updateTime()).start();
    }

    /** Called with the unread notification count after each refresh. */
    public void setUnreadListener(Consumer<Integer> listener) {
        this.unreadListener = listener == null ? n -> { } : listener;
    }

    /** Called with a hashtag (e.g. "#exam") when the user clicks it. */
    public void setTagListener(Consumer<String> listener) {
        this.tagListener = listener == null ? t -> { } : listener;
    }

    private RoundedPanel trendingCard() {
        RoundedPanel card = newCard();
        card.add(title("Trending Tags"));
        card.add(Box.createVerticalStrut(8));
        tagsBody.setLayout(new BoxLayout(tagsBody, BoxLayout.Y_AXIS));
        tagsBody.setOpaque(false);
        tagsBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel none = new JLabel("No hashtags yet");
        none.setFont(Theme.regular(12));
        none.setForeground(Theme.MUTED);
        tagsBody.add(none);
        card.add(tagsBody);
        return card;
    }

    private void loadTags() {
        new SwingWorker<java.util.List<TrendingDAO.Tag>, Void>() {
            @Override
            protected java.util.List<TrendingDAO.Tag> doInBackground() throws Exception {
                return trendingDAO.topTags(6);
            }

            @Override
            protected void done() {
                try {
                    java.util.List<TrendingDAO.Tag> tags = get();
                    tagsBody.removeAll();
                    if (tags.isEmpty()) {
                        JLabel none = new JLabel("Add #hashtags to your posts!");
                        none.setFont(Theme.regular(12));
                        none.setForeground(Theme.MUTED);
                        tagsBody.add(none);
                    }
                    for (TrendingDAO.Tag t : tags) {
                        tagsBody.add(tagRow(t));
                    }
                    tagsBody.revalidate();
                    tagsBody.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private JPanel tagRow(TrendingDAO.Tag t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel name = new JLabel(t.tag);
        name.setFont(Theme.bold(13));
        name.setForeground(Theme.BLUE_LIGHT);
        JLabel count = new JLabel(t.count + (t.count == 1 ? " post" : " posts"));
        count.setFont(Theme.regular(12));
        count.setForeground(Theme.MUTED);
        p.add(name, BorderLayout.WEST);
        p.add(count, BorderLayout.EAST);
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tagListener.accept(t.tag);
            }
        });
        return p;
    }

    /** Reloads statistics from the database in the background. */
    public void refresh() {
        loadTags();
        loadStreak();
        loadGoals();
        new SwingWorker<DashboardStatsDAO.UserStats, Void>() {
            @Override
            protected DashboardStatsDAO.UserStats doInBackground() throws Exception {
                return statsDAO.loadStats(user.getId());
            }

            @Override
            protected void done() {
                try {
                    DashboardStatsDAO.UserStats s = get();
                    pointsValue.setText(String.valueOf(s.points));
                    postsValue.setText(String.valueOf(s.postsToday));
                    likesValue.setText(String.valueOf(s.likesToday));
                    commentsValue.setText(String.valueOf(s.commentsToday));
                    pointsTodayValue.setText((s.pointsToday >= 0 ? "+" : "") + s.pointsToday);
                    user.setPoints(s.points);
                    unreadListener.accept(s.unreadNotifications);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    public void updateTime() {
        int used = tracker.minutesUsed();
        int limit = user.getEntertainmentLimitMinutes();

        if (limit > 0) {
            timeLabel.setText(used + " / " + limit + " min used");
            timeBar.setValue(Math.min(100, used * 100 / limit));
            timeBar.setForeground(used >= limit ? Theme.RED : Theme.BLUE);
        } else {
            timeLabel.setText(used + " min used (no limit set)");
            timeBar.setValue(0);
        }
    }

    // ---- cards ----

    private RoundedPanel pointsCard() {
        RoundedPanel card = newCard();
        card.add(title("Your Points"));
        card.add(Box.createVerticalStrut(8));

        JPanel row = row();
        row.add(Box.createHorizontalStrut(0));
        JLabel coin = new JLabel(Icons.icon(Icons.Type.COIN, 28, Theme.GOLD));
        row.add(coin);
        row.add(Box.createHorizontalStrut(8));
        row.add(pointsValue);
        card.add(row);
        card.add(Box.createVerticalStrut(12));

        RoundedButton view = new RoundedButton("View Rewards", RoundedButton.Style.OUTLINE);
        view.setAlignmentX(Component.LEFT_ALIGNMENT);
        view.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        view.addActionListener(e -> navigator.accept("REWARDS"));
        card.add(view);
        return card;
    }

    private final JLabel streakValue = valueLabel("…", 22, new Color(251, 146, 60));
    private final JLabel streakHint = new JLabel(" ");
    private final StreakDots streakDots = new StreakDots();

    private RoundedPanel streakCard() {
        RoundedPanel card = newCard();
        card.add(title("Login Streak"));
        card.add(Box.createVerticalStrut(8));

        JPanel row = row();
        row.add(new JLabel(Icons.icon(Icons.Type.FLAME, 28, new Color(251, 146, 60))));
        row.add(Box.createHorizontalStrut(8));
        row.add(streakValue);
        card.add(row);
        card.add(Box.createVerticalStrut(10));

        streakDots.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(streakDots);
        card.add(Box.createVerticalStrut(8));

        streakHint.setFont(Theme.regular(11));
        streakHint.setForeground(Theme.MUTED);
        streakHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(streakHint);
        return card;
    }

    private void loadStreak() {
        new SwingWorker<com.eduvibe.dao.AchievementDAO.StreakInfo, Void>() {
            @Override
            protected com.eduvibe.dao.AchievementDAO.StreakInfo doInBackground() throws Exception {
                return new com.eduvibe.dao.AchievementDAO().loadStreak(user.getId());
            }

            @Override
            protected void done() {
                try {
                    com.eduvibe.dao.AchievementDAO.StreakInfo i = get();
                    streakValue.setText(i.current + (i.current == 1 ? " day" : " days"));
                    streakDots.set(i);
                    int[] goals = {3, 7, 14, 30, 60, 100};
                    int next = -1;
                    for (int g : goals) {
                        if (i.current < g) {
                            next = g;
                            break;
                        }
                    }
                    String msg = next < 0 ? "Amazing! Best streak: " + i.longest + " days"
                            : (next - i.current) + " more day" + (next - i.current == 1 ? "" : "s")
                                    + " to reach " + next + "  \u2022  Best: " + i.longest;
                    if (!i.claimedToday) {
                        msg = "Log in daily to keep it going. " + msg;
                    }
                    streakHint.setText("<html><body style='width:190px'>" + msg + "</body></html>");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    /** Seven dots, one per day, oldest first; filled = logged in that day. */
    private static final class StreakDots extends JComponent {

        private com.eduvibe.dao.AchievementDAO.StreakInfo info;

        StreakDots() {
            setPreferredSize(new Dimension(200, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        }

        void set(com.eduvibe.dao.AchievementDAO.StreakInfo i) {
            this.info = i;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int step = Math.max(24, Math.min(34, getWidth() / 7));
            int d = 20;
            for (int k = 0; k < 7; k++) {
                int x = k * step;
                boolean on = info != null && info.last7[k];
                g2.setColor(on ? new Color(251, 146, 60) : Theme.CARD_LIGHT);
                g2.fillOval(x, 0, d, d);
                if (on) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 6, 10, x + 9, 14);
                    g2.drawLine(x + 9, 14, x + 15, 6);
                }
                if (info != null && info.dates[k] != null) {
                    String t = info.dates[k].getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.NARROW, java.util.Locale.ENGLISH);
                    g2.setFont(Theme.regular(10));
                    g2.setColor(k == 6 ? Theme.TEXT : Theme.MUTED);
                    g2.drawString(t, x + (d - g2.getFontMetrics().stringWidth(t)) / 2, 34);
                }
            }
            g2.dispose();
        }
    }

    private final JPanel goalsBody = new JPanel();
    private final JLabel goalsCount = new JLabel("0/4");
    private final JProgressBar goalsBar = new JProgressBar(0, 4);

    private final RoundedButton claimButton = new RoundedButton("Claim +30 bonus points", RoundedButton.Style.PRIMARY);
    private final JLabel claimedLabel = new JLabel("Bonus claimed today");

    private RoundedPanel goalsCard() {
        RoundedPanel card = newCard();

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel t = title("Daily Goals");
        goalsCount.setFont(Theme.bold(12));
        goalsCount.setForeground(Theme.GREEN);
        head.add(t, BorderLayout.WEST);
        head.add(goalsCount, BorderLayout.EAST);
        card.add(head);
        card.add(Box.createVerticalStrut(8));

        goalsBar.setBorderPainted(false);
        goalsBar.setBackground(Theme.INPUT);
        goalsBar.setForeground(Theme.GREEN);
        goalsBar.setPreferredSize(new Dimension(100, 6));
        goalsBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        goalsBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(goalsBar);
        card.add(Box.createVerticalStrut(10));

        goalsBody.setLayout(new BoxLayout(goalsBody, BoxLayout.Y_AXIS));
        goalsBody.setOpaque(false);
        goalsBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(goalsBody);
        card.add(Box.createVerticalStrut(8));

        claimButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        claimButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        claimButton.setVisible(false);
        claimButton.addActionListener(e -> {
            claimButton.setEnabled(false);
            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return new com.eduvibe.dao.GoalsDAO().claim(user.getId());
                }

                @Override
                protected void done() {
                    try {
                        get();
                        com.eduvibe.ui.components.Toast.success(RightSidebar.this,
                                "+" + com.eduvibe.dao.GoalsDAO.BONUS + " bonus points. Nice work!");
                        refresh();
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() == null ? ex : ex.getCause();
                        com.eduvibe.ui.components.Toast.error(RightSidebar.this,
                                c.getMessage() == null ? "Could not claim the bonus." : c.getMessage());
                        claimButton.setEnabled(true);
                    }
                }
            }.execute();
        });
        card.add(claimButton);

        claimedLabel.setFont(Theme.bold(11));
        claimedLabel.setForeground(Theme.GREEN);
        claimedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        claimedLabel.setVisible(false);
        card.add(claimedLabel);
        return card;
    }

    private void loadGoals() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                boolean login = new com.eduvibe.dao.AchievementDAO().loadStreak(user.getId()).claimedToday;
                DashboardStatsDAO.UserStats st = statsDAO.loadStats(user.getId());
                int commented = new com.eduvibe.dao.ActivityDAO().commentsWrittenToday(user.getId());
                boolean claimed = new com.eduvibe.dao.GoalsDAO().claimedToday(user.getId());
                return new Object[] { login, st, commented, claimed };
            }

            @Override
            protected void done() {
                try {
                    Object[] d = get();
                    DashboardStatsDAO.UserStats st = (DashboardStatsDAO.UserStats) d[1];
                    boolean[] done = {
                        (Boolean) d[0],
                        st.postsToday >= 1,
                        (Integer) d[2] >= 1,
                        st.pointsToday >= com.eduvibe.dao.GoalsDAO.POINTS_GOAL
                    };
                    String[] names = { "Log in today", "Create a post", "Comment on a post", "Earn 50 points" };

                    goalsBody.removeAll();
                    int count = 0;
                    for (int i = 0; i < 4; i++) {
                        if (done[i]) {
                            count++;
                        }
                        goalsBody.add(goalRow(names[i], done[i]));
                    }
                    boolean claimed = (Boolean) d[3];
                    claimButton.setVisible(count == 4 && !claimed);
                    claimButton.setEnabled(true);
                    claimedLabel.setVisible(claimed);
                    goalsCount.setText(count + "/4");
                    goalsBar.setValue(count);
                    goalsBody.revalidate();
                    goalsBody.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private JPanel goalRow(String text, boolean done) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JLabel icon = new JLabel(done
                ? Icons.icon(Icons.Type.CHECK, 16, Theme.GREEN)
                : Icons.icon(Icons.Type.CLOCK, 16, Theme.MUTED));
        JLabel label = new JLabel(text);
        label.setFont(Theme.regular(12));
        label.setForeground(done ? Theme.TEXT : Theme.MUTED);

        row.add(icon, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private RoundedPanel quickAccessCard() {
        RoundedPanel card = newCard();
        card.add(title("Quick Access"));
        card.add(Box.createVerticalStrut(6));
        card.add(link(Icons.Type.GIFT, "Playtime Rewards", "PLAYTIME"));
        card.add(link(Icons.Type.TROPHY, "Leaderboard", "LEADERBOARD"));
        card.add(link(Icons.Type.BOOKMARK, "Saved Posts", "SAVED"));
        card.add(link(Icons.Type.USERS, "Find People", "USER_SEARCH"));
        return card;
    }

    private RoundedPanel todayCard() {
        RoundedPanel card = newCard();
        card.add(title("Today's Activity"));
        card.add(Box.createVerticalStrut(10));
        card.add(stat("Posts", postsValue));
        card.add(stat("Likes received", likesValue));
        card.add(stat("Comments received", commentsValue));
        card.add(stat("Points earned", pointsTodayValue));
        return card;
    }

    private RoundedPanel earnCard() {
        RoundedPanel card = newCard();
        card.add(title("How to Earn Points"));
        card.add(Box.createVerticalStrut(8));
        card.add(earnRow("Create a post", "+15"));
        card.add(earnRow("Like on your post", "+5"));
        card.add(earnRow("Comment on your post", "+10"));
        card.add(earnRow("Daily login", "+20"));
        return card;
    }

    private RoundedPanel timeCard() {
        RoundedPanel card = newCard();
        card.add(title("Entertainment Time"));
        card.add(Box.createVerticalStrut(8));
        timeLabel.setFont(Theme.regular(12));
        timeLabel.setForeground(Theme.MUTED);
        timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(timeLabel);
        card.add(Box.createVerticalStrut(8));
        timeBar.setBorderPainted(false);
        timeBar.setBackground(Theme.INPUT);
        timeBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        timeBar.setPreferredSize(new Dimension(100, 8));
        card.add(timeBar);
        return card;
    }

    // ---- helpers ----

    private RoundedPanel newCard() {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(14));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel valueLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(size));
        l.setForeground(color);
        return l;
    }

    private JPanel row() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JPanel stat(String label, JLabel value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel l = new JLabel(label);
        l.setFont(Theme.regular(13));
        l.setForeground(Theme.MUTED);
        p.add(l, BorderLayout.WEST);
        p.add(value, BorderLayout.EAST);
        return p;
    }

    private JPanel earnRow(String label, String pts) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel l = new JLabel(label);
        l.setFont(Theme.regular(13));
        l.setForeground(Theme.MUTED);
        JLabel v = new JLabel(pts);
        v.setFont(Theme.bold(13));
        v.setForeground(Theme.GREEN);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel link(Icons.Type icon, String text, String page) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel l = new JLabel(text, Icons.icon(icon, 18, Theme.BLUE_LIGHT), SwingConstants.LEFT);
        l.setIconTextGap(10);
        l.setFont(Theme.regular(13));
        l.setForeground(Theme.TEXT);
        p.add(l);
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigator.accept(page);
            }
        });
        return p;
    }
}
