
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color PANEL = Theme.CARD;
    private static final Color BLUE = Theme.BLUE;
    private static final Color GREEN = Theme.GREEN;
    private static final Color ORANGE = new Color(249, 115, 22);
    private static final Color RED = Theme.RED;
    private static final Color PURPLE = new Color(139, 92, 246);
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;

    // Existing statistics
    private final JLabel usersValue = new JLabel("...");
    private final JLabel postsValue = new JLabel("...");
    private final JLabel reportsValue = new JLabel("...");
    private final JLabel restrictedValue = new JLabel("...");
    private final JLabel academicValue = new JLabel("...");
    private final JLabel entertainmentValue = new JLabel("...");

    // New statistics
    private final JLabel registrationsValue = new JLabel("...");
    private final JLabel loginRewardsValue = new JLabel("...");
    private final JLabel pointsDistributedValue = new JLabel("...");
    private final JLabel activityLogsValue = new JLabel("...");

    private final JComboBox<String> dateFilter = new JComboBox<>(
            new String[]{
                "Today",
                "Last 7 Days",
                "Last 30 Days",
                "All Time"
            }
    );

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                        "Date & Time",
                        "Actor",
                        "Action",
                        "Details"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable activityTable = new JTable(tableModel);
    private final JButton refreshButton = new JButton("Refresh");

    private final com.eduvibe.ui.components.WeeklyPostsChart weeklyChart =
            new com.eduvibe.ui.components.WeeklyPostsChart("Posts created, last 7 days");

    private final DistributionChart postChart =
            new DistributionChart(
                    "Post Categories",
                    new String[]{
                        "Academic",
                        "Entertainment"
                    },
                    new Color[]{
                        BLUE,
                        ORANGE
                    }
            );

    private final DistributionChart userChart =
            new DistributionChart(
                    "User Account Status",
                    new String[]{
                        "Active",
                        "Suspended",
                        "Banned"
                    },
                    new Color[]{
                        GREEN,
                        ORANGE,
                        RED
                    }
            );

    private final DistributionChart moderationChart =
            new DistributionChart(
                    "Post Moderation Status",
                    new String[]{
                        "Clean",
                        "Flagged",
                        "Approved",
                        "Removed"
                    },
                    new Color[]{
                        GREEN,
                        ORANGE,
                        BLUE,
                        RED
                    }
            );

    private SwingWorker<DashboardData, Void> dashboardWorker;

    public AdminDashboardPanel() {

        setLayout(new BorderLayout(0, 20));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        styleActivityTable();

        dateFilter.addActionListener(e -> {
            if (dateFilter.isEnabled()) {
                loadActivitiesInBackground();
            }
        });

        refreshDashboard();
    }

    // =====================================================
    // DASHBOARD DATA
    // =====================================================

    private static class DashboardData {

        int totalUsers;
        int totalPosts;
        int pendingReports;

        int suspended;
        int banned;
        int active;

        int academic;
        int entertainment;

        int clean;
        int flagged;
        int approved;
        int removed;

        int newRegistrations;
        int dailyLoginRewards;

        long totalPointsDistributed;
        long totalActivityLogs;

        List<Object[]> activities = new ArrayList<>();

        String[] dayLabels = new String[0];
        int[] postsPerDay = new int[0];
    }

    // =====================================================
    // TOP BAR
    // =====================================================

    private JPanel buildTopBar() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(
                heading,
                BoxLayout.Y_AXIS
        ));
        heading.setBackground(BACKGROUND);

        JLabel title = new JLabel("Platform Overview");
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 22));
        title.setForeground(WHITE);

        JLabel subtitle = new JLabel(
                "Live statistics and recent administrative activities."
        );
        subtitle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);

        styleButton(refreshButton, BLUE);
        refreshButton.addActionListener(e -> refreshDashboard());

        panel.add(heading, BorderLayout.WEST);
        panel.add(refreshButton, BorderLayout.EAST);

        return panel;
    }

    // =====================================================
    // MAIN CONTENT
    // =====================================================

    private JPanel buildContent() {

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(BACKGROUND);

        // Ten statistic cards: five columns and two rows.
        JPanel cards = new JPanel(
                new GridLayout(2, 5, 12, 12)
        );
        cards.setBackground(BACKGROUND);

        cards.add(createStatCard("Total Users", usersValue));
        cards.add(createStatCard("Total Posts", postsValue));
        cards.add(createStatCard("Pending Reports", reportsValue));
        cards.add(createStatCard("Restricted", restrictedValue));
        cards.add(createStatCard("Academic Posts", academicValue));

        cards.add(createStatCard("Entertainment Posts", entertainmentValue));
        cards.add(createStatCard("New Registrations", registrationsValue));
        cards.add(createStatCard("Daily Login Rewards", loginRewardsValue));
        cards.add(createStatCard("Points Distributed", pointsDistributedValue));
        cards.add(createStatCard("Total Activity Logs", activityLogsValue));

        JPanel quickActions = buildQuickActions();

        JPanel charts = new JPanel(
                new GridLayout(1, 3, 15, 15)
        );
        charts.setBackground(BACKGROUND);

        charts.add(postChart);
        charts.add(userChart);
        charts.add(moderationChart);

        JPanel activityPanel = buildActivityPanel();

        JPanel upperContent = new JPanel(
                new BorderLayout(0, 18)
        );
        upperContent.setBackground(BACKGROUND);

        upperContent.add(quickActions, BorderLayout.NORTH);
        JPanel chartsBlock = new JPanel(new BorderLayout(0, 15));
        chartsBlock.setBackground(BACKGROUND);
        chartsBlock.add(charts, BorderLayout.NORTH);
        chartsBlock.add(weeklyChart, BorderLayout.CENTER);
        upperContent.add(chartsBlock, BorderLayout.CENTER);

        JPanel lowerContent = new JPanel(
                new BorderLayout(0, 18)
        );
        lowerContent.setBackground(BACKGROUND);

        lowerContent.add(upperContent, BorderLayout.NORTH);
        lowerContent.add(activityPanel, BorderLayout.CENTER);

        content.add(cards, BorderLayout.NORTH);
        content.add(lowerContent, BorderLayout.CENTER);

        return content;
    }

    // =====================================================
    // QUICK ACTIONS
    // =====================================================

    private JPanel buildQuickActions() {

        JPanel container = new JPanel(
                new BorderLayout(0, 10)
        );
        container.setBackground(BACKGROUND);

        JLabel title = new JLabel("Quick Actions");
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 16));
        title.setForeground(WHITE);

        JPanel buttons = new JPanel(
                new GridLayout(1, 5, 12, 0)
        );
        buttons.setBackground(BACKGROUND);

        buttons.add(createQuickAction(
                "User Management",
                "Manage accounts and roles",
                BLUE,
                1
        ));

        buttons.add(createQuickAction(
                "Post Moderation",
                "Review and moderate posts",
                ORANGE,
                2
        ));

        buttons.add(createQuickAction(
                "Reports",
                "Review user reports",
                RED,
                3
        ));

        buttons.add(createQuickAction(
                "Activity Logs",
                "View admin actions",
                GREEN,
                4
        ));

        buttons.add(createQuickAction(
                "User Activities",
                "Monitor user activities",
                PURPLE,
                5
        ));

        container.add(title, BorderLayout.NORTH);
        container.add(buttons, BorderLayout.CENTER);

        return container;
    }

    private JButton createQuickAction(
            String title,
            String description,
            Color accent,
            int tabIndex
    ) {

        JButton button = new JButton();

        button.setLayout(new BorderLayout(0, 7));
        button.setBackground(PANEL);
        button.setForeground(WHITE);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER),
                        new EmptyBorder(12, 12, 12, 12)
                )
        );

        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        titleLabel.setForeground(accent);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 10));
        descriptionLabel.setForeground(MUTED);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(
                textPanel,
                BoxLayout.Y_AXIS
        ));
        textPanel.setBackground(PANEL);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(descriptionLabel);

        JLabel arrow = new JLabel("→");
        arrow.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 18));
        arrow.setForeground(WHITE);

        button.add(textPanel, BorderLayout.CENTER);
        button.add(arrow, BorderLayout.EAST);

        button.addActionListener(e -> navigateToTab(tabIndex));

        return button;
    }

    private void navigateToTab(int tabIndex) {

        AdminPanel host = (AdminPanel) SwingUtilities.getAncestorOfClass(AdminPanel.class, this);

        if (host != null) {
            host.showTab(tabIndex);
            return;
        }

        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof JTabbedPane tabs) {
                if (tabIndex >= 0 && tabIndex < tabs.getTabCount()) {
                    tabs.setSelectedIndex(tabIndex);
                }
                return;
            }
            parent = parent.getParent();
        }
    }

    // =====================================================
    // ACTIVITY PANEL
    // =====================================================

    private JPanel buildActivityPanel() {

        JPanel activityPanel = new JPanel(
                new BorderLayout(0, 12)
        );

        activityPanel.setBackground(PANEL);
        activityPanel.setBorder(
                new EmptyBorder(18, 18, 18, 18)
        );

        JPanel activityHeader = new JPanel(
                new BorderLayout()
        );
        activityHeader.setBackground(PANEL);

        JLabel activityTitle = new JLabel(
                "Recent Admin Activities"
        );

        activityTitle.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 16));
        activityTitle.setForeground(WHITE);

        dateFilter.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        dateFilter.setForeground(WHITE);
        dateFilter.setBackground(PANEL);
        dateFilter.setFocusable(false);

        activityHeader.add(activityTitle, BorderLayout.WEST);
        activityHeader.add(dateFilter, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(activityTable);

        scrollPane.setBorder(new LineBorder(BORDER));
        scrollPane.getViewport().setBackground(PANEL);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        activityPanel.add(activityHeader, BorderLayout.NORTH);
        activityPanel.add(scrollPane, BorderLayout.CENTER);

        return activityPanel;
    }

    // =====================================================
    // STATISTIC CARDS
    // =====================================================

    private JPanel createStatCard(
            String title,
            JLabel value
    ) {

        JPanel card = new JPanel();

        card.setLayout(new BoxLayout(
                card,
                BoxLayout.Y_AXIS
        ));

        card.setBackground(PANEL);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER),
                        new EmptyBorder(15, 14, 15, 14)
                )
        );

        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        titleLabel.setForeground(MUTED);

        value.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 23));
        value.setForeground(WHITE);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(value);

        return card;
    }

    private void styleButton(
            JButton button,
            Color color
    ) {

        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));

        button.setForeground(WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // =====================================================
    // TABLE DESIGN
    // =====================================================

    private void styleActivityTable() {

        activityTable.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));

        activityTable.setForeground(WHITE);
        activityTable.setBackground(PANEL);
        activityTable.setRowHeight(32);
        activityTable.setGridColor(BORDER);

        activityTable.setSelectionBackground(
                Theme.BORDER
        );

        activityTable.setSelectionForeground(WHITE);
        activityTable.setFillsViewportHeight(true);
        activityTable.setAutoCreateRowSorter(true);

        activityTable.getTableHeader().setFont(
                com.eduvibe.ui.Theme.font(Font.BOLD, 12)
        );

        activityTable.getTableHeader().setForeground(WHITE);

        activityTable.getTableHeader().setBackground(
                Theme.INPUT
        );

        activityTable.getTableHeader().setReorderingAllowed(false);
    }

    // =====================================================
    // REFRESH DASHBOARD
    // =====================================================

    public void refreshDashboard() {

        if (dashboardWorker != null
                && !dashboardWorker.isDone()) {
            dashboardWorker.cancel(true);
        }

        refreshButton.setEnabled(false);
        dateFilter.setEnabled(false);
        refreshButton.setText("Loading...");

        dashboardWorker = new SwingWorker<DashboardData, Void>() {

            @Override
            protected DashboardData doInBackground()
                    throws Exception {

                DashboardData data = new DashboardData();

                try (Connection conn =
                             DatabaseConnection.getConnection()) {

                    loadStatistics(conn, data);
                    loadWeeklyPosts(conn, data);
                    loadRecentActivities(conn, data);
                }

                return data;
            }

            @Override
            protected void done() {

                refreshButton.setEnabled(true);
                dateFilter.setEnabled(true);
                refreshButton.setText("Refresh");

                try {

                    if (isCancelled()) {
                        return;
                    }

                    DashboardData data = get();

                    displayStatistics(data);
                    displayActivities(data.activities);

                } catch (Exception ex) {
                    showError(ex);
                }
            }
        };

        dashboardWorker.execute();
    }

    private void displayStatistics(DashboardData data) {

        usersValue.setText(
                String.valueOf(data.totalUsers)
        );

        postsValue.setText(
                String.valueOf(data.totalPosts)
        );

        reportsValue.setText(
                String.valueOf(data.pendingReports)
        );

        restrictedValue.setText(
                String.valueOf(data.suspended + data.banned)
        );

        academicValue.setText(
                String.valueOf(data.academic)
        );

        entertainmentValue.setText(
                String.valueOf(data.entertainment)
        );

        // New dashboard metrics
        registrationsValue.setText(
                String.valueOf(data.newRegistrations)
        );

        loginRewardsValue.setText(
                String.valueOf(data.dailyLoginRewards)
        );

        pointsDistributedValue.setText(
                String.format("%,d", data.totalPointsDistributed)
        );

        activityLogsValue.setText(
                String.format("%,d", data.totalActivityLogs)
        );

        weeklyChart.setData(data.dayLabels, data.postsPerDay);

        postChart.setValues(new int[]{
            data.academic,
            data.entertainment
        });

        userChart.setValues(new int[]{
            data.active,
            data.suspended,
            data.banned
        });

        moderationChart.setValues(new int[]{
            data.clean,
            data.flagged,
            data.approved,
            data.removed
        });
    }

    private void loadActivitiesInBackground() {

        if (dashboardWorker != null
                && !dashboardWorker.isDone()) {
            return;
        }

        dateFilter.setEnabled(false);

        new SwingWorker<List<Object[]>, Void>() {

            @Override
            protected List<Object[]> doInBackground()
                    throws Exception {

                DashboardData data = new DashboardData();

                try (Connection conn =
                             DatabaseConnection.getConnection()) {

                    loadRecentActivities(conn, data);
                }

                return data.activities;
            }

            @Override
            protected void done() {

                dateFilter.setEnabled(true);

                try {
                    displayActivities(get());
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void displayActivities(List<Object[]> activities) {

        tableModel.setRowCount(0);

        for (Object[] activity : activities) {
            tableModel.addRow(activity);
        }
    }

    private void showError(Exception ex) {

        Throwable cause = ex.getCause() != null
                ? ex.getCause()
                : ex;

        JOptionPane.showMessageDialog(
                AdminDashboardPanel.this,
                "Unable to load dashboard data:\n"
                + cause.getMessage(),
                "Dashboard Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // DATABASE STATISTICS
    // =====================================================

    /** Posts created on each of the last 7 days (today included), counted from the posts table. */
    private void loadWeeklyPosts(Connection conn, DashboardData data) throws Exception {

        java.time.LocalDate today;
        try (PreparedStatement ps = conn.prepareStatement("SELECT CURDATE()");
             ResultSet rs = ps.executeQuery()) {
            today = rs.next() ? rs.getDate(1).toLocalDate() : java.time.LocalDate.now();
        }

        int[] counts = new int[7];
        String sql = "SELECT DATE(created_at) AS d, COUNT(*) AS c FROM posts "
                + "WHERE created_at >= CURDATE() - INTERVAL 6 DAY GROUP BY DATE(created_at)";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long ago = java.time.temporal.ChronoUnit.DAYS.between(
                        rs.getDate("d").toLocalDate(), today);
                if (ago >= 0 && ago < 7) {
                    counts[6 - (int) ago] = rs.getInt("c");
                }
            }
        }

        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate day = today.minusDays(6 - i);
            labels[i] = i == 6 ? "Today" : day.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
        }
        data.dayLabels = labels;
        data.postsPerDay = counts;
    }

    private void loadStatistics(
            Connection conn,
            DashboardData data
    ) throws Exception {

        // Existing statistics
        data.totalUsers = getCount(
                conn,
                "SELECT COUNT(*) FROM users"
        );

        data.totalPosts = getCount(
                conn,
                "SELECT COUNT(*) FROM posts"
        );

        data.pendingReports = getCount(
                conn,
                "SELECT COUNT(*) FROM reports WHERE status = 'pending'"
        );

        data.active = getCount(
                conn,
                "SELECT COUNT(*) FROM users WHERE status = 'active'"
        );

        data.suspended = getCount(
                conn,
                "SELECT COUNT(*) FROM users WHERE status = 'suspended'"
        );

        data.banned = getCount(
                conn,
                "SELECT COUNT(*) FROM users WHERE status = 'banned'"
        );

        data.academic = getCount(
                conn,
                "SELECT COUNT(*) FROM posts WHERE category = 'academic'"
        );

        data.entertainment = getCount(
                conn,
                "SELECT COUNT(*) FROM posts WHERE category = 'entertainment'"
        );

        data.clean = getCount(
                conn,
                "SELECT COUNT(*) FROM posts WHERE moderation_status = 'clean'"
        );

        data.flagged = getCount(
                conn,
                "SELECT COUNT(*) FROM posts WHERE moderation_status = 'flagged'"
        );

        data.approved = getCount(
                conn,
                "SELECT COUNT(*) FROM posts WHERE moderation_status = 'approved'"
        );

        data.removed = getCount(
                conn,
                "SELECT COUNT(*) FROM posts WHERE moderation_status = 'removed'"
        );

        // =================================================
        // NEW REGISTRATIONS
        // =================================================

        data.newRegistrations = getCount(
                conn,
                """
                SELECT COUNT(*)
                FROM moderation_logs
                WHERE action = 'user_registered'
                """
        );

        // =================================================
        // DAILY LOGIN REWARDS
        // =================================================

        data.dailyLoginRewards = getCount(
                conn,
                """
                SELECT COUNT(*)
                FROM moderation_logs
                WHERE action = 'daily_login_reward'
                """
        );

        // =================================================
        // TOTAL POINTS DISTRIBUTED
        // =================================================

        data.totalPointsDistributed = getLongValue(
                conn,
                """
                SELECT COALESCE(
                    SUM(
                        CASE
                            WHEN points > 0 THEN points
                            ELSE 0
                        END
                    ), 0
                )
                FROM point_transactions
                """
        );

        // =================================================
        // TOTAL ACTIVITY LOGS
        // =================================================

        data.totalActivityLogs = getLongValue(
                conn,
                """
                SELECT COUNT(*)
                FROM moderation_logs
                """
        );
    }

    private int getCount(
            Connection conn,
            String sql
    ) throws Exception {

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    private long getLongValue(
            Connection conn,
            String sql
    ) throws Exception {

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0L;
    }

    // =====================================================
    // RECENT ACTIVITIES
    // =====================================================

    private void loadRecentActivities(
            Connection conn,
            DashboardData data
    ) throws Exception {

        String selected =
                (String) dateFilter.getSelectedItem();

        String sql =
                "SELECT m.created_at, "
                + "COALESCE(u.username, 'Unknown') AS actor_username, "
                + "m.action, m.note "
                + "FROM moderation_logs m "
                + "LEFT JOIN users u ON m.admin_id = u.id ";

        if ("Today".equals(selected)) {

            sql += "WHERE m.created_at >= CURDATE() ";

        } else if ("Last 7 Days".equals(selected)) {

            sql += "WHERE m.created_at >= "
                    + "DATE_SUB(NOW(), INTERVAL 7 DAY) ";

        } else if ("Last 30 Days".equals(selected)) {

            sql += "WHERE m.created_at >= "
                    + "DATE_SUB(NOW(), INTERVAL 30 DAY) ";
        }

        sql += "ORDER BY m.created_at DESC LIMIT 100";

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Timestamp timestamp =
                        rs.getTimestamp("created_at");

                data.activities.add(new Object[]{
                    timestamp,
                    rs.getString("actor_username"),
                    rs.getString("action"),
                    rs.getString("note") == null
                            ? ""
                            : rs.getString("note")
                });
            }
        }
    }

    // =====================================================
    // CUSTOM DISTRIBUTION CHART
    // =====================================================

    private class DistributionChart extends JPanel {

        private final String chartTitle;
        private final String[] labels;
        private final Color[] colors;

        private int[] values;

        DistributionChart(
                String chartTitle,
                String[] labels,
                Color[] colors
        ) {

            this.chartTitle = chartTitle;
            this.labels = labels;
            this.colors = colors;
            this.values = new int[labels.length];

            setLayout(new BorderLayout());
            setBackground(PANEL);

            setBorder(
                    BorderFactory.createCompoundBorder(
                            new LineBorder(BORDER),
                            new EmptyBorder(15, 15, 15, 15)
                    )
            );

            setPreferredSize(new Dimension(250, 220));
        }

        void setValues(int[] values) {

            this.values = values.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(WHITE);

            g.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 14));

            g.drawString(chartTitle, 15, 25);

            int maxValue = 0;

            for (int value : values) {
                maxValue = Math.max(maxValue, value);
            }

            int startY = 55;
            int barHeight = 15;
            int gap = 37;
            int labelWidth = 95;
            int rightMargin = 42;

            int availableWidth = Math.max(
                    20,
                    getWidth() - labelWidth - rightMargin - 35
            );

            for (int i = 0; i < labels.length; i++) {

                int y = startY + (i * gap);

                g.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 10));

                g.setColor(MUTED);

                g.drawString(
                        labels[i],
                        15,
                        y + 12
                );

                int barX = 15 + labelWidth;
                int barWidth = availableWidth;

                g.setColor(BORDER);

                g.fillRoundRect(
                        barX,
                        y,
                        barWidth,
                        barHeight,
                        8,
                        8
                );

                int filledWidth = maxValue == 0
                        ? 0
                        : (int) (
                                (values[i] / (double) maxValue)
                                * barWidth
                        );

                if (filledWidth > 0) {

                    g.setColor(colors[i]);

                    g.fillRoundRect(
                            barX,
                            y,
                            filledWidth,
                            barHeight,
                            8,
                            8
                    );
                }

                g.setColor(WHITE);

                g.drawString(
                        String.valueOf(values[i]),
                        barX + barWidth + 7,
                        y + 12
                );
            }

            g.dispose();
        }
    }
}
