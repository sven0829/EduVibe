
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserActivityPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color PANEL = Theme.CARD;
    private static final Color BLUE = Theme.BLUE;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;

    private final int adminId;

    private final JTextField searchField = new JTextField(18);

    private final JComboBox<String> activityFilter = new JComboBox<>(
            new String[]{
                "All Activities",
                "Post Created",
                "Liked Post",
                "Commented",
                "Reported Post",
                "Followed User"
            }
    );

    private final JLabel resultLabel = new JLabel("Loading activities...");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Date & Time", "Username", "Activity", "Details"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(model);

    private final JButton refreshButton = new JButton("Refresh");
    private final JButton viewUserButton = new JButton("View User");

    public UserActivityPanel(int adminId) {
        this.adminId = adminId;

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        styleTable();
        setupFilters();

        viewUserButton.setEnabled(false);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                viewUserButton.setEnabled(table.getSelectedRow() >= 0);
            }
        });

        viewUserButton.addActionListener(e -> viewSelectedUser());
        refreshButton.addActionListener(e -> loadActivities());

        loadActivities();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 15));
        header.setBackground(BACKGROUND);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(BACKGROUND);

        JLabel title = new JLabel("User Activity Monitoring");
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 22));
        title.setForeground(WHITE);

        JLabel subtitle = new JLabel(
                "Monitor posts, likes, comments, reports, and follows."
        );
        subtitle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setBackground(BACKGROUND);

        searchField.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        searchField.setForeground(WHITE);
        searchField.setBackground(PANEL);
        searchField.setCaretColor(WHITE);
        searchField.setBorder(new LineBorder(BORDER));
        searchField.setPreferredSize(new Dimension(220, 34));
        searchField.setToolTipText("Search username or activity details");

        activityFilter.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        activityFilter.setForeground(WHITE);
        activityFilter.setBackground(PANEL);

        styleButton(refreshButton, BLUE);
        styleButton(viewUserButton, new Color(16, 185, 129));

        controls.add(searchField);
        controls.add(activityFilter);
        controls.add(refreshButton);
        controls.add(viewUserButton);

        header.add(titlePanel, BorderLayout.NORTH);
        header.add(controls, BorderLayout.CENTER);

        for (Component c : header.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        return header;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BACKGROUND);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(BACKGROUND);

        JLabel sectionTitle = new JLabel("Platform User Activities");
        sectionTitle.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 15));
        sectionTitle.setForeground(WHITE);

        resultLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        resultLabel.setForeground(MUTED);

        infoPanel.add(sectionTitle, BorderLayout.WEST);
        infoPanel.add(resultLabel, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER));
        scrollPane.getViewport().setBackground(PANEL);

        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        button.setForeground(WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 16, 9, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleTable() {
        table.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        table.setForeground(WHITE);
        table.setBackground(PANEL);
        table.setRowHeight(34);
        table.setGridColor(BORDER);
        table.setSelectionBackground(Theme.BORDER);
        table.setSelectionForeground(WHITE);
        table.setFillsViewportHeight(true);
        table.setRowSorter(sorter);

        table.getTableHeader().setFont(
                com.eduvibe.ui.Theme.font(Font.BOLD, 12)
        );
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setBackground(Theme.INPUT);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void setupFilters() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });

        activityFilter.addActionListener(e -> applyFilters());
    }

    private void applyFilters() {
        List<RowFilter<DefaultTableModel, Integer>> filters =
                new ArrayList<>();

        String search = searchField.getText().trim();

        if (!search.isEmpty()) {
            filters.add(RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(search)
            ));
        }

        String selected = (String) activityFilter.getSelectedItem();

        if (selected != null && !"All Activities".equals(selected)) {
            filters.add(RowFilter.regexFilter(
                    "^" + Pattern.quote(selected) + "$", 2
            ));
        }

        sorter.setRowFilter(filters.isEmpty()
                ? null : RowFilter.andFilter(filters));

        resultLabel.setText(sorter.getViewRowCount() + " activities shown");
    }

    public void loadActivities() {
        refreshButton.setEnabled(false);
        viewUserButton.setEnabled(false);
        resultLabel.setText("Loading activities...");

        new SwingWorker<List<Object[]>, Void>() {

            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> activities = new ArrayList<>();

                String sql =
                        "SELECT activity_date, username, activity, details " +
                        "FROM (" +

                        "SELECT p.created_at AS activity_date, " +
                        "u.username, 'Post Created' AS activity, " +
                        "CONCAT('Post: ', COALESCE(p.title, 'Untitled')) AS details " +
                        "FROM posts p JOIN users u ON p.user_id = u.id " +

                        "UNION ALL " +

                        "SELECT l.created_at, u.username, 'Liked Post', " +
                        "CONCAT('Liked: ', COALESCE(p.title, 'Untitled')) " +
                        "FROM post_likes l " +
                        "JOIN users u ON l.user_id = u.id " +
                        "LEFT JOIN posts p ON l.post_id = p.id " +

                        "UNION ALL " +

                        "SELECT c.created_at, u.username, 'Commented', " +
                        "CONCAT('Comment: ', LEFT(c.content, 180)) " +
                        "FROM comments c JOIN users u ON c.user_id = u.id " +

                        "UNION ALL " +

                        "SELECT r.created_at, u.username, 'Reported Post', " +
                        "CONCAT('Reason: ', r.reason) " +
                        "FROM reports r JOIN users u ON r.reporter_id = u.id " +

                        "UNION ALL " +

                        "SELECT f.created_at, follower.username, 'Followed User', " +
                        "CONCAT('Followed: ', following.username) " +
                        "FROM follows f " +
                        "JOIN users follower ON f.follower_id = follower.id " +
                        "JOIN users following ON f.following_id = following.id " +

                        ") AS user_activities " +
                        "ORDER BY activity_date DESC LIMIT 500";

                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        activities.add(new Object[]{
                            rs.getTimestamp("activity_date"),
                            rs.getString("username"),
                            rs.getString("activity"),
                            rs.getString("details")
                        });
                    }
                }

                return activities;
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);

                try {
                    List<Object[]> activities = get();
                    model.setRowCount(0);

                    for (Object[] activity : activities) {
                        model.addRow(activity);
                    }

                    applyFilters();

                    if (activities.isEmpty()) {
                        resultLabel.setText("No activities found");
                    }

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null
                            ? ex.getCause() : ex;

                    resultLabel.setText("Unable to load activities");

                    JOptionPane.showMessageDialog(
                            UserActivityPanel.this,
                            "Unable to load user activities:\n"
                                    + cause.getMessage(),
                            "Activity Loading Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void viewSelectedUser() {
        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String username = String.valueOf(model.getValueAt(modelRow, 1));

        showUserDetails(username);
    }

    private void showUserDetails(String username) {
        String sql =
                "SELECT u.id, u.full_name, u.username, u.email, " +
                "u.role, u.status, u.points, " +
                "(SELECT COUNT(*) FROM posts p WHERE p.user_id=u.id) AS total_posts, " +
                "(SELECT COUNT(*) FROM comments c WHERE c.user_id=u.id) AS total_comments, " +
                "(SELECT COUNT(*) FROM post_likes l WHERE l.user_id=u.id) AS total_likes, " +
                "(SELECT COUNT(*) FROM follows f WHERE f.following_id=u.id) AS followers, " +
                "(SELECT COUNT(*) FROM follows f WHERE f.follower_id=u.id) AS following " +
                "FROM users u WHERE u.username=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "User not found.");
                    return;
                }

                showUserDialog(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getInt("points"),
                        rs.getInt("total_posts"),
                        rs.getInt("total_comments"),
                        rs.getInt("total_likes"),
                        rs.getInt("followers"),
                        rs.getInt("following")
                );
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load user profile:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showUserDialog(
            int userId, String fullName, String username,
            String email, String role, String status, int points,
            int posts, int comments, int likes, int followers, int following
    ) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "User Profile",
                true
        );

        dialog.setSize(540, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(25, 28, 20, 28));

        JLabel heading = new JLabel("User Profile");
        heading.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 23));
        heading.setForeground(WHITE);

        JLabel name = new JLabel(
                fullName == null || fullName.isBlank() ? username : fullName
        );
        name.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 19));
        name.setForeground(BLUE);

        JLabel handle = new JLabel("@" + username + " • User ID: " + userId);
        handle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        handle.setForeground(MUTED);

        content.add(heading);
        content.add(Box.createVerticalStrut(18));
        content.add(name);
        content.add(Box.createVerticalStrut(5));
        content.add(handle);
        content.add(Box.createVerticalStrut(20));

        JPanel accountCard = createInfoCard("ACCOUNT INFORMATION");
        addInfoRow(accountCard, "Email", email);
        addInfoRow(accountCard, "Role", role);
        addInfoRow(accountCard, "Account Status", status);
        addInfoRow(accountCard, "Points Balance", String.valueOf(points));

        content.add(accountCard);
        content.add(Box.createVerticalStrut(16));

        JPanel statsCard = createInfoCard("ACTIVITY STATISTICS");
        addInfoRow(statsCard, "Posts Created", String.valueOf(posts));
        addInfoRow(statsCard, "Comments", String.valueOf(comments));
        addInfoRow(statsCard, "Likes Given", String.valueOf(likes));
        addInfoRow(statsCard, "Followers", String.valueOf(followers));
        addInfoRow(statsCard, "Following", String.valueOf(following));

        content.add(statsCard);
        content.add(Box.createVerticalStrut(16));

        JPanel recentCard = createInfoCard("RECENT ACTIVITY");
        int count = 0;

        for (int i = 0; i < model.getRowCount() && count < 8; i++) {
            String rowUsername = String.valueOf(model.getValueAt(i, 1));

            if (username.equalsIgnoreCase(rowUsername)) {
                JLabel activityLabel = new JLabel(
                        "• " + model.getValueAt(i, 2)
                                + " — " + model.getValueAt(i, 3)
                );
                activityLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
                activityLabel.setForeground(WHITE);

                recentCard.add(activityLabel);
                recentCard.add(Box.createVerticalStrut(7));
                count++;
            }
        }

        if (count == 0) {
            JLabel empty = new JLabel("No recent activity available.");
            empty.setForeground(MUTED);
            recentCard.add(empty);
        }

        content.add(recentCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND);

        JButton manageButton = new JButton("Manage Account");
        styleButton(manageButton, new Color(217, 119, 6));

        manageButton.addActionListener(e -> {
            UserAccountActionsDialog actionsDialog =
                    new UserAccountActionsDialog(
                            SwingUtilities.getWindowAncestor(dialog),
                            userId,
                            adminId,
                            username,
                            status
                    );

            actionsDialog.setVisible(true);
            dialog.dispose();
            loadActivities();
        });

        JButton closeButton = new JButton("Close");
        styleButton(closeButton, BLUE);
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(BACKGROUND);
        footer.setBorder(new EmptyBorder(8, 15, 12, 15));
        footer.add(manageButton);
        footer.add(closeButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createInfoCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PANEL);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        JLabel heading = new JLabel(title);
        heading.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));
        heading.setForeground(BLUE);

        card.add(heading);
        card.add(Box.createVerticalStrut(10));

        return card;
    }

    private void addInfoRow(JPanel card, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(PANEL);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));

        JLabel left = new JLabel(label);
        left.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        left.setForeground(MUTED);

        JLabel right = new JLabel(
                value == null || value.isBlank() ? "—" : value
        );
        right.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        right.setForeground(WHITE);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);

        card.add(row);
        card.add(Box.createVerticalStrut(5));
    }
}
