
package com.eduvibe.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color PANEL = Theme.CARD;
    private static final Color BLUE = Theme.BLUE;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;

    private final AdminDashboardPanel dashboardPanel;
    private final UserManagementPanel userManagementPanel;
    private final PostModerationPanel postModerationPanel;
    private final ReportManagementPanel reportManagementPanel;
    private final AdminActivityLogPanel activityLogPanel;
    private final UserActivityPanel userActivityPanel;

    private final com.eduvibe.ui.components.SegmentedTabs tabs;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private int selectedIndex = 0;

    public AdminPanel(int adminId) {

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(0, 0, 10, 0));

        dashboardPanel = new AdminDashboardPanel();
        userManagementPanel = new UserManagementPanel(adminId);
        postModerationPanel = new PostModerationPanel(adminId);
        reportManagementPanel = new ReportManagementPanel(adminId);
        activityLogPanel = new AdminActivityLogPanel();
        userActivityPanel = new UserActivityPanel(adminId);

        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(BACKGROUND);

        page.add(buildHeader(), BorderLayout.NORTH);

        cardHost.setOpaque(false);
        cardHost.add(dashboardPanel, "0");
        cardHost.add(userManagementPanel, "1");
        cardHost.add(postModerationPanel, "2");
        cardHost.add(reportManagementPanel, "3");
        cardHost.add(activityLogPanel, "4");
        cardHost.add(userActivityPanel, "5");

        tabs = new com.eduvibe.ui.components.SegmentedTabs(
                new String[] {"Overview", "Users", "Content", "Reports", "Activity Log", "User Activities"},
                0, i -> {
                    selectedIndex = i;
                    cards.show(cardHost, String.valueOf(i));
                    refreshSelectedTab();
                });
        tabs.setPreferredSize(new Dimension(820, 44));
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel tabRow = new JPanel(new BorderLayout());
        tabRow.setOpaque(false);
        tabRow.setBorder(new EmptyBorder(0, 20, 0, 20));   // lines up with the cards below
        tabRow.add(tabs, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(tabRow, BorderLayout.NORTH);
        center.add(cardHost, BorderLayout.CENTER);

        page.add(center, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);

    }

    private JPanel buildHeader() {

        com.eduvibe.ui.components.PageHero hero = new com.eduvibe.ui.components.PageHero(
                com.eduvibe.ui.components.Icons.Type.SHIELD, "Admin Dashboard",
                "Manage users, moderate posts, review reports and track activity.",
                com.eduvibe.ui.components.PageHero.ADMIN[0], com.eduvibe.ui.components.PageHero.ADMIN[1]);
        hero.setChip("Control center", com.eduvibe.ui.components.Icons.Type.SHIELD);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(0, 20, 0, 20));    // lines up with the tabs and cards below
        header.add(hero, BorderLayout.CENTER);
        return header;
    }

    /** Switches to a tab (0 Overview, 1 Users, 2 Content, 3 Reports, 4 Activity Log, 5 User Activities). */
    public void showTab(int index) {
        if (index < 0 || index > 5) {
            return;
        }
        tabs.select(index, true);
    }

    public void refreshData() {
        refreshSelectedTab();
    }

    private void refreshSelectedTab() {

        int selected = selectedIndex;

        switch (selected) {

            case 0:
                dashboardPanel.refreshDashboard();
                break;

            case 1:
                userManagementPanel.loadUsers();
                break;

            case 2:
                postModerationPanel.loadFlaggedPosts();
                break;

            case 3:
                reportManagementPanel.loadReports();
                break;

            case 4:
                activityLogPanel.loadLogs();
                break;

            case 5:
                userActivityPanel.loadActivities();
                break;

            default:
                break;
        }
    }
}
