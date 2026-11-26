
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.AdminNotificationDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.regex.Pattern;

public class UserManagementPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color PANEL = Theme.CARD;
    private static final Color PANEL_LIGHT = Theme.CARD_LIGHT;
    private static final Color BLUE = Theme.BLUE;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;
    private static final Color GREEN = new Color(52, 211, 153);
    private static final Color RED = new Color(248, 113, 113);
    private static final Color YELLOW = Theme.YELLOW;

    private final int adminId;

    private JTable usersTable;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField searchField;

    private JLabel totalUsersLabel;
    private JLabel adminCountLabel;
    private JLabel suspendedCountLabel;

    public UserManagementPanel(int adminId) {
        this.adminId = adminId;

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        buildUI();
        loadUsers();
    }

    // ==========================================
    // MAIN UI
    // ==========================================

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableSection(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BACKGROUND);

        JLabel eyebrow = new JLabel("ACCOUNT CONTROL");
        eyebrow.setForeground(BLUE);
        eyebrow.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 10));

        JLabel title = new JLabel("User Management");
        title.setForeground(WHITE);
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 23));

        JLabel subtitle = new JLabel(
                "View registered accounts and manage their permissions."
        );
        subtitle.setForeground(MUTED);
        subtitle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));

        header.add(eyebrow);
        header.add(Box.createVerticalStrut(5));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(18));

        JPanel statistics = new JPanel(new GridLayout(1, 3, 12, 0));
        statistics.setBackground(BACKGROUND);
        statistics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        totalUsersLabel = new JLabel("0");
        adminCountLabel = new JLabel("0");
        suspendedCountLabel = new JLabel("0");

        statistics.add(createStatCard(
                "TOTAL USERS", totalUsersLabel, BLUE
        ));

        statistics.add(createStatCard(
                "ADMIN ACCOUNTS", adminCountLabel, GREEN
        ));

        statistics.add(createStatCard(
                "SUSPENDED / BANNED", suspendedCountLabel, RED
        ));

        header.add(statistics);

        for (Component c : header.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        return header;
    }

    private JPanel createStatCard(
            String title,
            JLabel valueLabel,
            Color accent
    ) {

        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 15, 12, 15)
        ));

        valueLabel.setForeground(accent);
        valueLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 23));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 9));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // ==========================================
    // TABLE SECTION
    // ==========================================

    private JPanel buildTableSection() {

        JPanel section = new JPanel(new BorderLayout(0, 12));
        section.setBackground(BACKGROUND);

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(BACKGROUND);

        JLabel sectionTitle = new JLabel("Registered Accounts");
        sectionTitle.setForeground(WHITE);
        sectionTitle.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 15));

        searchField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(MUTED);
                    g2.setFont(getFont());
                    Insets in = getInsets();
                    g2.drawString("Search users...", in.left,
                            (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        searchField.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        searchField.setForeground(WHITE);
        searchField.setBackground(PANEL);
        searchField.setCaretColor(WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        searchField.setPreferredSize(new Dimension(250, 37));
        searchField.setToolTipText(
                "Search by name, username, email, role, or status"
        );

        searchField.getDocument().addDocumentListener(
                new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterUsers();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterUsers();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterUsers();
            }
        });

        toolbar.add(sectionTitle, BorderLayout.WEST);
        toolbar.add(searchField, BorderLayout.EAST);

        model = new DefaultTableModel(
                new String[]{
                    "ID", "Full Name", "Username",
                    "Email", "Role", "Status", "Points"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 6) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        usersTable = new JTable(model);

        usersTable.setBackground(PANEL);
        usersTable.setForeground(WHITE);
        usersTable.setGridColor(BORDER);
        usersTable.setSelectionBackground(new Color(45, 75, 120));
        usersTable.setSelectionForeground(WHITE);
        usersTable.setRowHeight(43);
        usersTable.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        usersTable.setShowVerticalLines(false);
        usersTable.setShowHorizontalLines(true);
        usersTable.setIntercellSpacing(new Dimension(0, 1));
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        usersTable.getTableHeader().setBackground(PANEL_LIGHT);
        usersTable.getTableHeader().setForeground(MUTED);
        usersTable.getTableHeader().setFont(
                com.eduvibe.ui.Theme.font(Font.BOLD, 11)
        );
        usersTable.getTableHeader().setPreferredSize(
                new Dimension(0, 40)
        );
        usersTable.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(model);
        usersTable.setRowSorter(sorter);

        DefaultTableCellRenderer centered =
                new DefaultTableCellRenderer();

        centered.setHorizontalAlignment(SwingConstants.CENTER);
        centered.setForeground(WHITE);
        centered.setBackground(PANEL);

        usersTable.getColumnModel().getColumn(0)
                .setCellRenderer(centered);

        usersTable.getColumnModel().getColumn(6)
                .setCellRenderer(centered);

        usersTable.getColumnModel().getColumn(4)
                .setCellRenderer(new BadgeRenderer(true));

        usersTable.getColumnModel().getColumn(5)
                .setCellRenderer(new BadgeRenderer(false));

        usersTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        usersTable.getColumnModel().getColumn(6).setPreferredWidth(65);

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        section.add(toolbar, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    // ==========================================
    // ROLE AND STATUS BADGES
    // ==========================================

    private class BadgeRenderer extends DefaultTableCellRenderer {

        private final boolean roleColumn;

        BadgeRenderer(boolean roleColumn) {
            this.roleColumn = roleColumn;
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            String text = value == null
                    ? ""
                    : value.toString().toLowerCase();

            setText(text.toUpperCase());
            setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 10));

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(WHITE);
                return this;
            }

            if (roleColumn) {
                setForeground(text.equals("admin") ? GREEN : BLUE);
            } else {
                switch (text) {
                    case "active" -> setForeground(GREEN);
                    case "suspended" -> setForeground(YELLOW);
                    case "banned" -> setForeground(RED);
                    default -> setForeground(MUTED);
                }
            }

            setBackground(PANEL);

            return this;
        }
    }

    // ==========================================
    // ACTION BUTTONS
    // ==========================================

    private JPanel buildActions() {

        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(BACKGROUND);

        JLabel label = new JLabel("ACCOUNT ACTIONS");
        label.setForeground(MUTED);
        label.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 10));

        JPanel buttons = new JPanel(new FlowLayout(
                FlowLayout.LEFT, 8, 0
        ));
        buttons.setBackground(BACKGROUND);

        JButton adminButton = createButton("Make Admin", BLUE);
        JButton userButton = createButton("Make User", PANEL_LIGHT);
        JButton activateButton = createButton("Activate", GREEN);
        JButton suspendButton = createButton(
                "Suspend", new Color(180, 130, 35)
        );
        JButton banButton = createButton("Ban", RED);
        JButton pointsButton = createButton("Adjust Points", BLUE);
        JButton refreshButton = createButton("Refresh", PANEL_LIGHT);

        adminButton.addActionListener(e ->
                updateSelectedUser("role", "admin")
        );

        userButton.addActionListener(e ->
                updateSelectedUser("role", "user")
        );

        activateButton.addActionListener(e ->
                updateSelectedUser("status", "active")
        );

        suspendButton.addActionListener(e ->
                updateSelectedUser("status", "suspended")
        );

        banButton.addActionListener(e ->
                updateSelectedUser("status", "banned")
        );

        pointsButton.addActionListener(e -> adjustSelectedUserPoints());

        refreshButton.addActionListener(e -> loadUsers());

        buttons.add(adminButton);
        buttons.add(userButton);
        buttons.add(activateButton);
        buttons.add(suspendButton);
        buttons.add(banButton);
        buttons.add(pointsButton);
        buttons.add(refreshButton);

        section.add(label, BorderLayout.NORTH);
        section.add(buttons, BorderLayout.CENTER);

        return section;
    }

    private JButton createButton(String text, Color background) {

        JButton button = new JButton(text);

        button.setBackground(background);
        button.setForeground(WHITE);
        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 13, 10, 13));
        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));
        button.setOpaque(true);
        button.setBorderPainted(false);

        return button;
    }

    // ==========================================
    // SEARCH
    // ==========================================

    private void filterUsers() {

        if (sorter == null || searchField == null) {
            return;
        }

        String search = searchField.getText().trim();

        if (search.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(search)
            ));
        }
    }

    // ==========================================
    // LOAD USERS
    // ==========================================

    public void loadUsers() {

        if (model == null) {
            return;
        }

        model.setRowCount(0);

        String sql = """
            SELECT id, full_name, username, email,
                   role, status, points
            FROM users
            ORDER BY id ASC
            """;

        int total = 0;
        int admins = 0;
        int restricted = 0;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                String role = rs.getString("role");
                String status = rs.getString("status");

                total++;

                if ("admin".equalsIgnoreCase(role)) {
                    admins++;
                }

                if ("suspended".equalsIgnoreCase(status)
                        || "banned".equalsIgnoreCase(status)) {
                    restricted++;
                }

                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getString("email"),
                    role,
                    status,
                    rs.getInt("points")
                });
            }

            totalUsersLabel.setText(String.valueOf(total));
            adminCountLabel.setText(String.valueOf(admins));
            suspendedCountLabel.setText(String.valueOf(restricted));

            filterUsers();

        } catch (SQLException ex) {
            showError("Unable to load users:", ex);
        }
    }

    // ==========================================
    // UPDATE USER AND RECORD ACTIVITY
    // ==========================================

    private void updateSelectedUser(String column, String value) {

        int selectedRow = usersTable.getSelectedRow();

        if (selectedRow == -1) {
            showWarning("Please select a user from the table first.");
            return;
        }

        int modelRow = usersTable.convertRowIndexToModel(selectedRow);

        int selectedUserId = (Integer) model.getValueAt(modelRow, 0);
        String username = String.valueOf(model.getValueAt(modelRow, 2));

        String currentValue = String.valueOf(
                model.getValueAt(modelRow, column.equals("role") ? 4 : 5)
        );

        if (!column.equals("role") && !column.equals("status")) {
            return;
        }

        if (selectedUserId == adminId
                && column.equals("role")
                && value.equals("user")) {
            showWarning("You cannot remove your own admin role.");
            return;
        }

        if (selectedUserId == adminId
                && column.equals("status")
                && !value.equals("active")) {
            showWarning("You cannot suspend or ban your own account.");
            return;
        }

        if (currentValue.equalsIgnoreCase(value)) {
            showWarning("This account already has the selected " + column + ".");
            return;
        }

        String action;
        String logAction;

        if (column.equals("role")) {
            if (value.equals("admin")) {
                action = "promote to ADMIN";
                logAction = "role_promoted";
            } else {
                action = "change to USER";
                logAction = "role_demoted";
            }
        } else {
            action = "change status to " + value.toUpperCase();

            if (value.equals("suspended")) {
                logAction = "user_suspended";
            } else if (value.equals("banned")) {
                logAction = "user_banned";
            } else {
                logAction = null;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action
                + " for @" + username + "?",
                "Confirm Account Update",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String updateSQL =
                "UPDATE users SET " + column + " = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                com.eduvibe.util.AdminGuard.require(connection, adminId);
                try (PreparedStatement statement =
                        connection.prepareStatement(updateSQL)) {

                    statement.setString(1, value);
                    statement.setInt(2, selectedUserId);

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Selected user was not found.");
                    }
                }

                if (logAction != null) {

                    String logSQL = """
                        INSERT INTO moderation_logs
                        (admin_id, action, note)
                        VALUES (?, ?, ?)
                        """;

                    try (PreparedStatement logStatement =
                            connection.prepareStatement(logSQL)) {

                        logStatement.setInt(1, adminId);
                        logStatement.setString(2, logAction);
                        logStatement.setString(
                                3,
                                "Admin changed @" + username
                                + " (User ID: " + selectedUserId
                                + ") " + column + " to " + value.toUpperCase()
                        );

                        logStatement.executeUpdate();
                    }
                }

                // Notify active admins about role and status changes.
                String notificationMessage;

                if (column.equals("role")) {
                    notificationMessage =
                            "Admin changed @" + username
                            + "'s role to " + value.toUpperCase()
                            + ". User ID: " + selectedUserId;
                } else {
                    notificationMessage =
                            "Admin changed @" + username
                            + "'s account status to " + value.toUpperCase()
                            + ". User ID: " + selectedUserId;
                }

                AdminNotificationDAO.notifyAdmins(
                        connection,
                        notificationMessage
                );

                connection.commit();

                JOptionPane.showMessageDialog(
                        this,
                        "@" + username + "'s account has been updated.",
                        "Update Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (SQLException ex) {
                connection.rollback();
                throw ex;

            } finally {
                connection.setAutoCommit(true);
            }

            loadUsers();

        } catch (SQLException ex) {
            showError("Unable to update user:", ex);
        }
    }

    // ==========================================
    // ADJUST USER POINTS
    // ==========================================

    private void adjustSelectedUserPoints() {

        int selectedRow = usersTable.getSelectedRow();

        if (selectedRow == -1) {
            showWarning("Please select a user from the table first.");
            return;
        }

        int modelRow = usersTable.convertRowIndexToModel(selectedRow);
        int selectedUserId = (Integer) model.getValueAt(modelRow, 0);
        String username = String.valueOf(model.getValueAt(modelRow, 2));
        int displayedPoints = (Integer) model.getValueAt(modelRow, 6);

        String[] choices = {"Add Points", "Deduct Points", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose a points adjustment for @" + username
                + "\nCurrent balance: " + displayedPoints + " points",
                "Adjust User Points",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        boolean adding = choice == 0;

        String amountInput = JOptionPane.showInputDialog(
                this,
                "Enter the number of points to "
                + (adding ? "ADD:" : "DEDUCT:"),
                "Points Amount",
                JOptionPane.QUESTION_MESSAGE
        );

        if (amountInput == null) {
            return;
        }

        final int amount;

        try {
            amount = Integer.parseInt(amountInput.trim());

            if (amount <= 0) {
                showWarning("Points amount must be greater than zero.");
                return;
            }

        } catch (NumberFormatException ex) {
            showWarning("Please enter a valid whole number.");
            return;
        }

        String reasonInput = JOptionPane.showInputDialog(
                this,
                "Enter the reason for this adjustment:",
                "Adjustment Reason",
                JOptionPane.QUESTION_MESSAGE
        );

        if (reasonInput == null) {
            return;
        }

        String reason = reasonInput.trim();

        if (reason.isEmpty()) {
            showWarning("Please provide a reason.");
            return;
        }

        if (reason.length() > 200) {
            showWarning("Reason must not exceed 200 characters.");
            return;
        }

        int adjustment = adding ? amount : -amount;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "User: @" + username
                + "\nAdjustment: " + (adding ? "+" : "-") + amount
                + " points\nReason: " + reason
                + "\n\nContinue?",
                "Confirm Points Adjustment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String updateSQL = """
            UPDATE users
            SET points = points + ?
            WHERE id = ? AND points + ? >= 0
            """;

        String logSQL = """
            INSERT INTO moderation_logs (admin_id, action, note)
            VALUES (?, 'points_adjusted', ?)
            """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                com.eduvibe.util.AdminGuard.require(connection, adminId);
                int newBalance;

                try (PreparedStatement updateStatement =
                        connection.prepareStatement(updateSQL)) {

                    updateStatement.setInt(1, adjustment);
                    updateStatement.setInt(2, selectedUserId);
                    updateStatement.setInt(3, adjustment);

                    if (updateStatement.executeUpdate() == 0) {
                        throw new SQLException(
                                "Points could not be updated. The user may "
                                + "have an insufficient balance or no longer exist."
                        );
                    }
                }

                try (PreparedStatement balanceStatement =
                        connection.prepareStatement(
                                "SELECT points FROM users WHERE id = ? FOR UPDATE"
                        )) {

                    balanceStatement.setInt(1, selectedUserId);

                    try (ResultSet rs = balanceStatement.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Selected user was not found.");
                        }

                        newBalance = rs.getInt("points");
                    }
                }

                String logNote = "Admin adjusted @" + username
                        + " (User ID: " + selectedUserId + ") by "
                        + (adding ? "+" : "-") + amount
                        + " points. New balance: " + newBalance
                        + ". Reason: " + reason;

                try (PreparedStatement logStatement =
                        connection.prepareStatement(logSQL)) {

                    logStatement.setInt(1, adminId);
                    logStatement.setString(2, logNote);
                    logStatement.executeUpdate();
                }

                AdminNotificationDAO.notifyAdmins(
                        connection,
                        "Admin adjusted @" + username + "'s points by "
                        + (adding ? "+" : "-") + amount
                        + ". New balance: " + newBalance
                        + ". Reason: " + reason
                );

                connection.commit();

                JOptionPane.showMessageDialog(
                        this,
                        "Points successfully adjusted!\n@" + username
                        + " now has " + newBalance + " points.",
                        "Points Updated",
                        JOptionPane.INFORMATION_MESSAGE
                );

                loadUsers();

            } catch (SQLException ex) {
                connection.rollback();
                throw ex;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            showError("Unable to adjust user points:", ex);
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void showWarning(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "User Management",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(String message, SQLException ex) {

        JOptionPane.showMessageDialog(
                this,
                message + "\n" + ex.getMessage(),
                "EduVibe Error",
                JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }
}
