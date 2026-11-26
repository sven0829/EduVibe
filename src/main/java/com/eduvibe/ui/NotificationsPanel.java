package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.PageHero;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.ui.components.SegmentedTabs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * com.eduvibe.ui.NotificationsPanel  (REPLACES existing file)
 *
 * Constructor NotificationsPanel(int) and loadNotifications() are unchanged.
 * Tabs: All / Likes / Comments / System (system, reward and reminder types).
 * Loading and read-marking run in SwingWorkers. setOnChanged(Runnable) lets the
 * dashboard refresh its unread badge after notifications are marked read.
 */
public class NotificationsPanel extends JPanel {

    private static final class Row {
        int id;
        String type;
        String message;
        boolean read;
        Timestamp createdAt;
    }

    private final int userId;

    private final JPanel list = new JPanel();
    private final JLabel summary = new JLabel("Loading notifications...");
    private RoundedButton markAll;
    private final PageHero hero = new PageHero(Icons.Type.BELL, "Notifications",
            "Likes, comments, rewards and reminders, all in one place.",
            PageHero.NOTIFICATIONS[0], PageHero.NOTIFICATIONS[1]);
    private SegmentedTabs tabBar;
    private static final String[] TAB_NAMES = {"All", "Likes", "Comments", "System"};

    private List<Row> rows = new ArrayList<>();
    private int activeTab = 0;
    private Runnable onChanged = () -> { };

    public NotificationsPanel(int userId) {

        this.userId = userId;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        summary.setFont(Theme.regular(13));
        summary.setForeground(Theme.MUTED);
        top.add(summary, BorderLayout.CENTER);

        markAll = new RoundedButton("Mark all as read", RoundedButton.Style.OUTLINE)
                .withIcon(Icons.Type.CHECK);
        markAll.addActionListener(e -> markAllAsRead());
        top.add(markAll, BorderLayout.EAST);

        column.add(hero);
        column.add(Box.createVerticalStrut(14));
        column.add(top);
        column.add(Box.createVerticalStrut(12));

        tabBar = new SegmentedTabs(TAB_NAMES, 0, this::selectTab);
        tabBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(tabBar);
        column.add(Box.createVerticalStrut(16));

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(list);

        add(column, BorderLayout.NORTH);

        loadNotifications();
    }

    public void setOnChanged(Runnable callback) {
        this.onChanged = callback == null ? () -> { } : callback;
    }

    private void selectTab(int index) {
        activeTab = index;
        render();
    }

    // ---------------- loading ----------------

    public void loadNotifications() {

        summary.setText("Loading notifications...");
        markAll.setEnabled(false);

        new SwingWorker<List<Row>, Void>() {
            @Override
            protected List<Row> doInBackground() throws Exception {

                String sql = """
                    SELECT id, type, message, is_read, created_at
                    FROM notifications
                    WHERE user_id = ?
                    ORDER BY created_at DESC
                    """;

                List<Row> result = new ArrayList<>();

                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Row r = new Row();
                            r.id = rs.getInt("id");
                            r.type = rs.getString("type");
                            r.message = rs.getString("message");
                            r.read = rs.getBoolean("is_read");
                            r.createdAt = rs.getTimestamp("created_at");
                            result.add(r);
                        }
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<Row> all = get();
                    rows = new ArrayList<>();
                    for (Row r : all) {
                        if (com.eduvibe.util.NotificationPrefs.shows(userId, r.type)) {
                            rows.add(r);
                        }
                    }
                    render();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    summary.setText("Unable to load notifications.");
                    list.removeAll();
                    list.add(empty("Unable to load notifications",
                            "Please refresh or check your database connection."));
                    list.revalidate();
                    list.repaint();
                }
            }
        }.execute();
    }

    private boolean inActiveTab(Row r) {
        String type = r.type == null ? "" : r.type.toLowerCase();
        return switch (activeTab) {
            case 1 -> type.equals("like");
            case 2 -> type.equals("comment");
            case 3 -> !type.equals("like") && !type.equals("comment");
            default -> true;
        };
    }

    private void render() {

        list.removeAll();

        int unread = 0;
        int shown = 0;
        String group = null;

        for (Row r : rows) {
            if (!r.read) {
                unread++;
            }
            if (inActiveTab(r)) {
                String g = groupFor(r.createdAt);
                if (!g.equals(group)) {
                    group = g;
                    list.add(groupHeader(g));
                }
                list.add(card(r));
                list.add(Box.createVerticalStrut(10));
                shown++;
            }
        }
        hero.setChip(unread == 0 ? "All read" : unread + " unread",
                unread == 0 ? Icons.Type.CHECK : Icons.Type.BELL);

        summary.setText(rows.size() + " notification(s) • " + unread + " unread");
        markAll.setEnabled(unread > 0);

        if (shown == 0) {
            list.add(empty("You're all caught up",
                    rows.isEmpty() ? "New notifications will appear here."
                                   : "Nothing in this tab yet."));
        }

        list.revalidate();
        list.repaint();
    }

    // ---------------- grouping ----------------

    private static String groupFor(Timestamp ts) {
        if (ts == null) {
            return "Earlier";
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                ts.toLocalDateTime().toLocalDate(), java.time.LocalDate.now());
        if (days <= 0) {
            return "Today";
        }
        if (days == 1) {
            return "Yesterday";
        }
        return days < 7 ? "This week" : "Earlier";
    }

    private JComponent groupHeader(String text) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(4, 2, 8, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(Theme.bold(11));
        l.setForeground(Theme.MUTED);
        JComponent line = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.alpha(Theme.BORDER, 180));
                g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        p.add(l, BorderLayout.WEST);
        p.add(line, BorderLayout.CENTER);
        return p;
    }

    // ---------------- card ----------------

    private RoundedPanel card(Row r) {

        Color accent = colorFor(r.type);

        RoundedPanel card = new RoundedPanel(16,
                Theme.CARD,
                r.read ? Theme.BORDER : Theme.alpha(accent, 150)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!r.read) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.antialias(g2);
                    g2.setPaint(new GradientPaint(0, 0, Theme.alpha(accent, 40), getWidth(), 0, Theme.alpha(accent, 0)));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                    g2.setColor(accent);
                    g2.fillRoundRect(0, 16, 4, getHeight() - 32, 4, 4);
                    g2.dispose();
                }
            }
        };
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        // round coloured badge with the notification icon
        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(new GradientPaint(0, 0, Theme.alpha(accent, 85), getWidth(), getHeight(), Theme.alpha(accent, 25)));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(Theme.alpha(accent, 120));
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                Icons.paint(g2, iconFor(r.type), (getWidth() - 20) / 2f, (getHeight() - 20) / 2f, 20, accent);
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(44, 44));

        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(icon, BorderLayout.NORTH);

        JTextArea message = new JTextArea(safe(r.message, "No message available."));
        message.setFont(r.read ? Theme.regular(13) : Theme.bold(13));
        message.setForeground(r.read ? Theme.MUTED : Theme.TEXT);
        message.setOpaque(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setEditable(false);
        message.setFocusable(false);
        message.setBorder(null);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel tag = new JLabel(tagFor(r.type));
        tag.setFont(Theme.bold(10));
        tag.setForeground(accent);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(tag);
        text.add(Box.createVerticalStrut(3));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(message);

        JLabel when = new JLabel(relativeTime(r.createdAt));
        when.setFont(Theme.regular(11));
        when.setForeground(Theme.MUTED);
        when.setToolTipText(r.createdAt == null ? null : r.createdAt.toLocalDateTime().format(
                DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")));

        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
        when.setAlignmentX(Component.RIGHT_ALIGNMENT);
        east.add(when);
        if (!r.read) {
            RoundedButton read = new RoundedButton("Mark read", RoundedButton.Style.GHOST);
            read.setFont(Theme.bold(11));
            read.setAlignmentX(Component.RIGHT_ALIGNMENT);
            read.addActionListener(e -> markAsRead(r.id));
            east.add(Box.createVerticalStrut(2));
            east.add(read);
        }

        card.add(iconWrap, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(east, BorderLayout.EAST);

        return card;
    }

    /** "Just now", "2m ago", "3h ago", "2d ago" or a date. */
    static String relativeTime(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        long secs = Math.max(0, (System.currentTimeMillis() - ts.getTime()) / 1000);
        if (secs < 60) {
            return "Just now";
        }
        if (secs < 3600) {
            return (secs / 60) + "m ago";
        }
        if (secs < 86400) {
            return (secs / 3600) + "h ago";
        }
        if (secs < 7 * 86400) {
            return (secs / 86400) + "d ago";
        }
        return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    private RoundedPanel empty(String title, String body) {

        RoundedPanel panel = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(36, 20, 36, 20));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel icon = new JLabel(Icons.icon(Icons.Type.BELL, 34, Theme.MUTED));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(Theme.bold(16));
        t.setForeground(Theme.TEXT);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel b = new JLabel(body);
        b.setFont(Theme.regular(12));
        b.setForeground(Theme.MUTED);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(8));
        panel.add(t);
        panel.add(Box.createVerticalStrut(6));
        panel.add(b);
        return panel;
    }

    // ---------------- actions ----------------

    private void markAsRead(int id) {
        runUpdate("UPDATE notifications SET is_read = 1 "
                + "WHERE id = ? AND user_id = ? AND is_read = 0", id, null);
    }

    private void markAllAsRead() {
        runUpdate("UPDATE notifications SET is_read = 1 "
                + "WHERE user_id = ? AND is_read = 0", null,
                "All notifications marked as read.");
    }

    /** Runs an UPDATE in the background. singleId == null means "all for this user". */
    private void runUpdate(String sql, Integer singleId, String successMessage) {

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                try (Connection c = DatabaseConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    if (singleId != null) {
                        ps.setInt(1, singleId);
                        ps.setInt(2, userId);
                    } else {
                        ps.setInt(1, userId);
                    }
                    return ps.executeUpdate();
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (successMessage != null) {
                        Dialogs.success(NotificationsPanel.this, "Notifications", successMessage);
                    }
                    loadNotifications();
                    onChanged.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    Dialogs.error(NotificationsPanel.this, "Could not update notifications",
                            String.valueOf(ex.getCause().getMessage()));
                }
            }
        }.execute();
    }

    // ---------------- helpers ----------------

    private Color colorFor(String type) {
        if (type == null) {
            return Theme.MUTED;
        }
        return switch (type.toLowerCase()) {
            case "like" -> Theme.RED;
            case "comment" -> Theme.BLUE_LIGHT;
            case "reward" -> Theme.GOLD;
            case "reminder" -> Theme.YELLOW;
            case "system" -> Theme.PURPLE;
            default -> Theme.MUTED;
        };
    }

    private static String tagFor(String type) {
        if (type == null || type.isBlank()) {
            return "UPDATE";
        }
        return type.toUpperCase();
    }

    private Icons.Type iconFor(String type) {
        if (type == null) {
            return Icons.Type.BELL;
        }
        return switch (type.toLowerCase()) {
            case "like" -> Icons.Type.HEART_FILLED;
            case "comment" -> Icons.Type.COMMENT;
            case "reward" -> Icons.Type.COIN;
            case "reminder" -> Icons.Type.CLOCK;
            case "system" -> Icons.Type.INFO;
            default -> Icons.Type.BELL;
        };
    }

    private static String safe(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }
}
