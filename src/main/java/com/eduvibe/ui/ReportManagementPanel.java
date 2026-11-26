
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportManagementPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color PANEL = Theme.CARD;
    private static final Color BLUE = Theme.BLUE;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color RED = Theme.RED;
    private static final Color GREEN = Theme.GREEN;

    private final Integer adminId;

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JLabel pendingLabel;

    // Compatibility constructor
    public ReportManagementPanel() {
        this(null);
    }

    public ReportManagementPanel(int adminId) {
        this(Integer.valueOf(adminId));
    }

    private ReportManagementPanel(Integer adminId) {
        this.adminId = adminId;

        setLayout(new BorderLayout(0, 15));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        loadReports();
    }

    private JPanel buildHeader() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(BACKGROUND);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(BACKGROUND);

        JLabel title = new JLabel("Community Reports");
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 21));
        title.setForeground(WHITE);

        JLabel subtitle = new JLabel(
                "Search, review, and manage reports submitted by EduVibe users."
        );
        subtitle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitle);

        JButton refreshButton = createButton("Refresh", BLUE);
        refreshButton.addActionListener(e -> loadReports());

        topPanel.add(textPanel, BorderLayout.WEST);
        topPanel.add(refreshButton, BorderLayout.EAST);

        JPanel filterPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8)
        );
        filterPanel.setBackground(BACKGROUND);

        searchField = new JTextField(22);
        searchField.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        searchField.setForeground(WHITE);
        searchField.setBackground(PANEL);
        searchField.setCaretColor(WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        searchField.setToolTipText("Search post ID, reporter, or reason");

        statusFilter = new JComboBox<>(new String[]{
                "All Statuses", "Pending", "Reviewed", "Dismissed"
        });
        statusFilter.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        statusFilter.setBackground(PANEL);
        statusFilter.setForeground(WHITE);

        pendingLabel = new JLabel("Pending reports: 0");
        pendingLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        pendingLabel.setForeground(new Color(251, 191, 36));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadReports();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadReports();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadReports();
            }
        });

        statusFilter.addActionListener(e -> loadReports());

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(MUTED);
        filterPanel.add(searchLabel);
        filterPanel.add(searchField);

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setForeground(MUTED);
        filterPanel.add(statusLabel);
        filterPanel.add(statusFilter);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(pendingLabel);

        container.add(topPanel);
        container.add(Box.createVerticalStrut(12));
        container.add(filterPanel);

        return container;
    }

    private JScrollPane buildTable() {
        model = new DefaultTableModel(
                new String[]{
                        "Report ID", "Post ID", "Reporter",
                        "Reason", "Status", "Date Reported"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        table.setForeground(WHITE);
        table.setBackground(PANEL);
        table.setRowHeight(36);
        table.setSelectionBackground(Theme.BORDER);
        table.setSelectionForeground(WHITE);
        table.setGridColor(Theme.BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setFont(
                com.eduvibe.ui.Theme.font(Font.BOLD, 12)
        );
        table.getTableHeader().setBackground(Theme.SIDEBAR);
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PANEL);

        return scrollPane;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 5)
        );
        panel.setBackground(BACKGROUND);

        JButton viewButton = createButton("View Details", BLUE);
        JButton reviewButton = createButton("Mark Reviewed", GREEN);
        JButton dismissButton = createButton("Dismiss Report", RED);

        viewButton.addActionListener(e -> viewSelectedReport());
        reviewButton.addActionListener(e -> updateSelectedReport("reviewed"));
        dismissButton.addActionListener(e -> updateSelectedReport("dismissed"));

        panel.add(viewButton);
        panel.add(reviewButton);
        panel.add(dismissButton);

        return panel;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        button.setForeground(WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 15, 9, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // LOAD REPORTS

    public void loadReports() {
        if (model == null) {
            return;
        }

        model.setRowCount(0);

        String search = searchField == null
                ? ""
                : searchField.getText().trim();

        String selectedStatus = statusFilter == null
                ? "All Statuses"
                : String.valueOf(statusFilter.getSelectedItem());

        String sql = """
            SELECT
                r.id,
                r.post_id,
                COALESCE(u.username, 'Unknown') AS reporter,
                r.reason,
                r.status,
                r.created_at
            FROM reports r
            LEFT JOIN users u ON r.reporter_id = u.id
            WHERE
                (? = 'All Statuses' OR r.status = ?)
                AND (
                    ? = ''
                    OR CAST(r.post_id AS CHAR) LIKE ?
                    OR COALESCE(u.username, 'Unknown') LIKE ?
                    OR r.reason LIKE ?
                )
            ORDER BY
                CASE WHEN r.status = 'pending' THEN 0 ELSE 1 END,
                r.created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, selectedStatus);
                stmt.setString(2, selectedStatus.toLowerCase());
                stmt.setString(3, search);

                String keyword = "%" + search + "%";

                stmt.setString(4, keyword);
                stmt.setString(5, keyword);
                stmt.setString(6, keyword);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                                rs.getInt("id"),
                                rs.getInt("post_id"),
                                rs.getString("reporter"),
                                rs.getString("reason"),
                                rs.getString("status"),
                                rs.getTimestamp("created_at")
                        });
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reports WHERE status = 'pending'"
            );
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    pendingLabel.setText(
                            "Pending reports: " + rs.getInt(1)
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load reports:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // VIEW REPORT DETAILS

    private void viewSelectedReport() {
        int row = table.getSelectedRow();

        if (row < 0) {
            showSelectMessage();
            return;
        }

        row = table.convertRowIndexToModel(row);

        int reportId = (int) model.getValueAt(row, 0);
        int postId = (int) model.getValueAt(row, 1);

        String sql = """
            SELECT
                r.reason,
                r.status,
                r.source,
                r.created_at,
                p.title,
                p.description,
                p.moderation_status,
                owner.username AS post_owner,
                reporter.username AS reporter
            FROM reports r
            LEFT JOIN posts p ON r.post_id = p.id
            LEFT JOIN users owner ON p.user_id = owner.id
            LEFT JOIN users reporter ON r.reporter_id = reporter.id
            WHERE r.id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reportId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String details =
                            "Report ID: " + reportId
                            + "\nPost ID: " + postId
                            + "\nReporter: " + rs.getString("reporter")
                            + "\nPost Owner: " + rs.getString("post_owner")
                            + "\nReason: " + rs.getString("reason")
                            + "\nSource: " + rs.getString("source")
                            + "\nReport Status: " + rs.getString("status")
                            + "\nPost Moderation: " + rs.getString("moderation_status")
                            + "\nDate Reported: " + rs.getTimestamp("created_at")
                            + "\n\nPost Title:\n" + rs.getString("title")
                            + "\n\nPost Description:\n" + rs.getString("description");

                    JTextArea area = new JTextArea(details, 18, 55);
                    area.setEditable(false);
                    area.setLineWrap(true);
                    area.setWrapStyleWord(true);
                    area.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
                    area.setBackground(PANEL);
                    area.setForeground(WHITE);
                    area.setCaretPosition(0);
                    area.setBorder(new EmptyBorder(12, 12, 12, 12));

                    JOptionPane.showMessageDialog(
                            this,
                            new JScrollPane(area),
                            "Report Details",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } else {
                    JOptionPane.showMessageDialog(this, "Report not found.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not open report details:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // UPDATE REPORT AND RECORD ACTIVITY

    private void updateSelectedReport(String newStatus) {
        int row = table.getSelectedRow();

        if (row < 0) {
            showSelectMessage();
            return;
        }

        row = table.convertRowIndexToModel(row);

        int reportId = (int) model.getValueAt(row, 0);
        int postId = (int) model.getValueAt(row, 1);
        String currentStatus = String.valueOf(model.getValueAt(row, 4));

        if (!"pending".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "This report has already been processed."
            );
            return;
        }

        String action = newStatus.equals("reviewed")
                ? "mark this report as reviewed"
                : "dismiss this report";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action + "?",
                "Confirm Action",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String updateSql = """
            UPDATE reports
            SET status = ?
            WHERE id = ? AND status = 'pending'
        """;

        String logSql = """
            INSERT INTO moderation_logs
                (post_id, admin_id, action, note)
            VALUES (?, ?, ?, ?)
        """;

        String logAction = newStatus.equals("reviewed")
                ? "report_reviewed"
                : "report_dismissed";

        String note = "Admin "
                + (adminId == null ? "unknown" : adminId)
                + " "
                + newStatus
                + " report ID "
                + reportId;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                com.eduvibe.util.AdminGuard.require(conn, adminId);
                int updatedRows;

                try (PreparedStatement stmt =
                             conn.prepareStatement(updateSql)) {

                    stmt.setString(1, newStatus);
                    stmt.setInt(2, reportId);
                    updatedRows = stmt.executeUpdate();
                }

                if (updatedRows == 0) {
                    conn.rollback();

                    JOptionPane.showMessageDialog(
                            this,
                            "The report was already processed or could not be found."
                    );
                    return;
                }

                try (PreparedStatement stmt =
                             conn.prepareStatement(logSql)) {

                    stmt.setInt(1, postId);

                    if (adminId == null) {
                        stmt.setNull(2, java.sql.Types.INTEGER);
                    } else {
                        stmt.setInt(2, adminId);
                    }

                    stmt.setString(3, logAction);
                    stmt.setString(4, note);

                    stmt.executeUpdate();
                }

                conn.commit();

                JOptionPane.showMessageDialog(
                        this,
                        "Report updated and activity recorded successfully."
                );

            } catch (SQLException e) {
                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

            loadReports();

        } catch (SQLException e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not update report or record activity:\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showSelectMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Please select a report first."
        );
    }
}
