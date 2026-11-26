
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostModerationPanel extends JPanel {

    private final int adminId;

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color CARD = Theme.CARD;
    private static final Color BLUE = Theme.BLUE;
    private static final Color RED = Theme.RED;
    private static final Color GREEN = Theme.GREEN;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;

    private JTable postsTable;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JComboBox<String> categoryFilter;
    private JComboBox<String> formatFilter;

    private JLabel totalLabel;
    private JLabel flaggedLabel;
    private JLabel approvedLabel;
    private JLabel removedLabel;

    private JButton approveButton;
    private JButton removeButton;

    public PostModerationPanel(int adminId) {

        this.adminId = adminId;

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        loadFlaggedPosts();
    }

    // =====================================================
    // HEADER
    // =====================================================

    private JPanel buildHeader() {

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BACKGROUND);

        JLabel title = new JLabel("Post Moderation");
        title.setForeground(WHITE);
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 25));

        JLabel subtitle = new JLabel(
                "Review reported content and manage community posts."
        );
        subtitle.setForeground(MUTED);
        subtitle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setBackground(BACKGROUND);

        totalLabel = new JLabel("0");
        flaggedLabel = new JLabel("0");
        approvedLabel = new JLabel("0");
        removedLabel = new JLabel("0");

        stats.add(createStatCard("Total Posts", totalLabel, BLUE));
        stats.add(createStatCard("Flagged", flaggedLabel, RED));
        stats.add(createStatCard("Approved", approvedLabel, GREEN));
        stats.add(createStatCard("Removed", removedLabel, MUTED));

        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(18));
        header.add(stats);

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
            JLabel value,
            Color accent) {

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel heading = new JLabel(title);
        heading.setForeground(MUTED);
        heading.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));

        value.setForeground(accent);
        value.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 25));

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // =====================================================
    // MAIN CONTENT
    // =====================================================

    private JPanel buildMainContent() {

        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setBackground(BACKGROUND);

        container.add(buildToolbar(), BorderLayout.NORTH);
        container.add(buildTable(), BorderLayout.CENTER);
        container.add(buildActions(), BorderLayout.SOUTH);

        return container;
    }

    // =====================================================
    // SEARCH AND FILTER
    // =====================================================

    private JPanel buildToolbar() {

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(BACKGROUND);

        searchField = new JTextField();
        searchField.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        searchField.setForeground(WHITE);
        searchField.setBackground(CARD);
        searchField.setCaretColor(WHITE);
        searchField.setBorder(new EmptyBorder(10, 12, 10, 12));
        searchField.setToolTipText("Search posts, authors, or descriptions");

        statusFilter = new JComboBox<>(new String[]{
                "All Statuses",
                "Flagged",
                "Clean",
                "Approved",
                "Removed"
        });

        statusFilter.setBackground(CARD);
        statusFilter.setForeground(WHITE);
        statusFilter.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));

        categoryFilter = new JComboBox<>(new String[]{
                "All Categories", "Academic", "Entertainment", "Invalid / Missing"
        });
        formatFilter = new JComboBox<>(new String[]{"All Formats", "Post", "Reel"});
        for (JComboBox<String> box : java.util.List.of(categoryFilter, formatFilter)) {
            box.setBackground(CARD);
            box.setForeground(WHITE);
            box.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
            box.addActionListener(e -> applyFilters());
        }

        JButton refreshButton = createButton(
                "Refresh",
                BLUE
        );

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(BACKGROUND);

        right.add(categoryFilter);
        right.add(formatFilter);
        right.add(statusFilter);
        right.add(refreshButton);

        toolbar.add(searchField, BorderLayout.CENTER);
        toolbar.add(right, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        applyFilters();
                    }

                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        applyFilters();
                    }

                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        applyFilters();
                    }
                }
        );

        statusFilter.addActionListener(e -> applyFilters());
        refreshButton.addActionListener(e -> loadFlaggedPosts());

        return toolbar;
    }

    private void applyFilters() {

        if (sorter == null) {
            return;
        }

        String search = searchField.getText()
                .trim()
                .toLowerCase();

        String selectedStatus =
                statusFilter.getSelectedItem().toString();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {

            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel,
                            ? extends Integer> entry) {

                String status = entry.getStringValue(8)
                        .toLowerCase();

                String rowText = (
                        entry.getStringValue(0) + " "
                        + entry.getStringValue(1) + " "
                        + entry.getStringValue(2) + " "
                        + entry.getStringValue(3) + " "
                        + entry.getStringValue(4) + " "
                        + entry.getStringValue(5)
                ).toLowerCase();

                boolean matchesSearch =
                        search.isEmpty() || rowText.contains(search);

                boolean matchesStatus =
                        selectedStatus.equals("All Statuses")
                        || status.equalsIgnoreCase(selectedStatus);

                String cat = entry.getStringValue(2).trim().toLowerCase();
                String fmt = entry.getStringValue(10);
                String wantCat = String.valueOf(categoryFilter.getSelectedItem());
                String wantFmt = String.valueOf(formatFilter.getSelectedItem());
                boolean validCat = cat.equals("academic") || cat.equals("entertainment");
                boolean matchesCategory = wantCat.equals("All Categories")
                        || (wantCat.equals("Invalid / Missing") ? !entry.getStringValue(2).equals(cat) || !validCat
                                : cat.equalsIgnoreCase(wantCat) && entry.getStringValue(2).equals(cat));
                boolean matchesFormat = wantFmt.equals("All Formats") || fmt.equals(wantFmt);

                return matchesSearch && matchesStatus && matchesCategory && matchesFormat;
            }
        });
    }

    // =====================================================
    // TABLE
    // =====================================================

    private JPanel buildTable() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD);
        panel.setBorder(new LineBorder(BORDER, 1));

        model = new DefaultTableModel(
                new Object[]{
                        "Post ID",
                        "Author ID",
                        "Category",
                        "Title",
                        "Description",
                        "Flag Reason",
                        "Pending Reports",
                        "Media Preview",
                        "Status",
                        "Media Path",
                        "Format"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 6) {
                    return Integer.class;
                }
                return Object.class;
            }
        };

        postsTable = new JTable(model);

        postsTable.setRowHeight(65);
        postsTable.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        postsTable.setForeground(WHITE);
        postsTable.setBackground(CARD);
        postsTable.setGridColor(BORDER);
        postsTable.setSelectionBackground(Theme.BLUE_HOVER);
        postsTable.setSelectionForeground(WHITE);
        postsTable.setShowVerticalLines(false);
        postsTable.setAutoCreateRowSorter(true);
        postsTable.setFillsViewportHeight(true);

        postsTable.getTableHeader().setBackground(
                Theme.SIDEBAR
        );
        postsTable.getTableHeader().setForeground(WHITE);
        postsTable.getTableHeader().setFont(
                com.eduvibe.ui.Theme.font(Font.BOLD, 12)
        );
        postsTable.getTableHeader().setPreferredSize(
                new Dimension(0, 38)
        );

        sorter = new TableRowSorter<>(model);
        postsTable.setRowSorter(sorter);

        postsTable.getColumnModel().getColumn(0).setPreferredWidth(65);
        postsTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        postsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        postsTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        postsTable.getColumnModel().getColumn(4).setPreferredWidth(220);
        postsTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        postsTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        postsTable.getColumnModel().getColumn(7).setPreferredWidth(120);
        postsTable.getColumnModel().getColumn(8).setPreferredWidth(100);

        postsTable.getColumnModel().getColumn(7)
                .setCellRenderer(new MediaRenderer());

        postsTable.getColumnModel().getColumn(8)
                .setCellRenderer(new StatusRenderer());
        postsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String text = v == null ? "" : v.toString();
                boolean ok = text.equals("academic") || text.equals("entertainment");
                setText(ok ? text : (text.isBlank() ? "MISSING" : "INVALID: " + text));
                if (!sel) {
                    c.setForeground(ok ? WHITE : RED);
                }
                return c;
            }
        });

        // Hide the internal media path column.
        postsTable.removeColumn(
                postsTable.getColumnModel().getColumn(9)
        );

        JScrollPane scrollPane = new JScrollPane(postsTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(CARD);

        panel.add(scrollPane, BorderLayout.CENTER);

        postsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelectedPost();
                }
            }
        });

        return panel;
    }

    // =====================================================
    // ACTION BUTTONS
    // =====================================================

    private JPanel buildActions() {

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );
        actions.setBackground(BACKGROUND);

        JButton viewButton = createButton("View Post", CARD);

        approveButton = createButton("Approve Post", GREEN);
        removeButton = createButton("Remove Post", RED);

        viewButton.addActionListener(e -> viewSelectedPost());
        approveButton.addActionListener(
                e -> updatePost("approved")
        );
        removeButton.addActionListener(
                e -> updatePost("removed")
        );

        JButton categoryButton = createButton("Set Category", BLUE);
        categoryButton.addActionListener(e -> changeCategory());
        actions.add(categoryButton);
        actions.add(viewButton);
        actions.add(approveButton);
        actions.add(removeButton);

        return actions;
    }

    private JButton createButton(String text, Color color) {

        JButton button = new JButton(text);

        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        button.setForeground(WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // =====================================================
    // LOAD POSTS
    // =====================================================

    public void loadFlaggedPosts() {

        if (model == null) {
            return;
        }

        model.setRowCount(0);

        String sql = """
            SELECT
                p.id,
                p.user_id,
                p.category,
                p.is_reel,
                p.title,
                p.description,
                p.flagged_reason,
                (
                    SELECT COUNT(*)
                    FROM reports r
                    WHERE r.post_id = p.id
                      AND r.status = 'pending'
                      AND r.source = 'user'
                ) AS pending_reports,
                p.media_type,
                p.media_path,
                p.moderation_status
            FROM posts p
            ORDER BY
                CASE
                    WHEN p.moderation_status = 'flagged' THEN 0
                    ELSE 1
                END,
                p.created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                int postId = rs.getInt("id");
                int authorId = rs.getInt("user_id");

                String category = rs.getString("category");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String reason = rs.getString("flagged_reason");

                int pendingReports =
                        rs.getInt("pending_reports");

                String mediaType = rs.getString("media_type");
                String mediaPath = rs.getString("media_path");
                String status = rs.getString("moderation_status");

                String mediaDisplay = "No Media";

                if ("image".equalsIgnoreCase(mediaType)) {
                    mediaDisplay = "Image";
                } else if ("video".equalsIgnoreCase(mediaType)) {
                    mediaDisplay = "Video";
                }

                model.addRow(new Object[]{
                        postId,
                        authorId,
                        category,
                        title,
                        description,
                        reason == null ? "" : reason,
                        pendingReports,
                        mediaDisplay,
                        status,
                        mediaPath,
                        rs.getBoolean("is_reel") ? "Reel" : "Post"
                });
            }

            loadStatistics(conn);

        } catch (SQLException e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load posts:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        applyFilters();
    }

    // =====================================================
    // STATISTICS
    // =====================================================

    private void loadStatistics(Connection conn) throws SQLException {

        String sql = """
            SELECT
                COUNT(*) AS total,
                SUM(moderation_status = 'flagged') AS flagged,
                SUM(moderation_status = 'approved') AS approved,
                SUM(moderation_status = 'removed') AS removed
            FROM posts
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {

                totalLabel.setText(
                        String.valueOf(rs.getInt("total"))
                );

                flaggedLabel.setText(
                        String.valueOf(rs.getInt("flagged"))
                );

                approvedLabel.setText(
                        String.valueOf(rs.getInt("approved"))
                );

                removedLabel.setText(
                        String.valueOf(rs.getInt("removed"))
                );
            }
        }
    }

    // =====================================================
    // VIEW POST DETAILS
    // =====================================================

    private void viewSelectedPost() {

        int selectedRow = postsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a post first."
            );
            return;
        }

        int row = postsTable.convertRowIndexToModel(selectedRow);

        int postId = Integer.parseInt(
                model.getValueAt(row, 0).toString()
        );

        String author = model.getValueAt(row, 1).toString();
        String category = String.valueOf(model.getValueAt(row, 2));
        String title = String.valueOf(model.getValueAt(row, 3));
        String description = String.valueOf(model.getValueAt(row, 4));
        String reason = String.valueOf(model.getValueAt(row, 5));
        String reports = String.valueOf(model.getValueAt(row, 6));
        String mediaType = String.valueOf(model.getValueAt(row, 7));
        String status = String.valueOf(model.getValueAt(row, 8));
        String mediaPath = String.valueOf(model.getValueAt(row, 9));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        addDetail(content, "Post ID", String.valueOf(postId));
        addDetail(content, "Author ID", author);
        addDetail(content, "Category", category);
        addDetail(content, "Format", String.valueOf(model.getValueAt(row, 10)));
        addDetail(content, "Title", title);
        addDetail(content, "Description", description);
        addDetail(content, "Flag Reason", reason);
        addDetail(content, "Pending Reports", reports);
        addDetail(content, "Media", mediaType);
        addDetail(content, "Status", status);

        if (!mediaPath.isBlank() && !mediaPath.equals("null")) {

            File file = new File(mediaPath);

            if (file.exists()
                    && mediaType.equalsIgnoreCase("Image")) {

                try {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath());

                    Image image = icon.getImage().getScaledInstance(
                            420,
                            260,
                            Image.SCALE_SMOOTH
                    );

                    JLabel imageLabel = new JLabel(new ImageIcon(image));
                    imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    content.add(Box.createVerticalStrut(12));
                    content.add(imageLabel);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (file.exists()
                    && mediaType.equalsIgnoreCase("Video")) {

                JButton openVideo = createButton(
                        "Open Video",
                        BLUE
                );

                openVideo.setAlignmentX(Component.LEFT_ALIGNMENT);

                openVideo.addActionListener(e -> {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Unable to open video: " + ex.getMessage()
                        );
                    }
                });

                content.add(Box.createVerticalStrut(12));
                content.add(openVideo);
            }
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setPreferredSize(new Dimension(520, 520));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Post Details",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private void addDetail(
            JPanel panel,
            String heading,
            String value) {

        JLabel label = new JLabel(
                heading + ": " + value
        );

        label.setForeground(WHITE);
        label.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(9));
    }

    // =====================================================
    // APPROVE / REMOVE WITH ADMIN ACTIVITY LOG
    // =====================================================

    /** Lets an admin correct a post's category. The format (post / reel) is never changed here. */
    private void changeCategory() {
        int selectedRow = postsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a post first.",
                    "No Post Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = postsTable.convertRowIndexToModel(selectedRow);
        int postId = Integer.parseInt(model.getValueAt(row, 0).toString());
        String current = String.valueOf(model.getValueAt(row, 2));

        Object choice = JOptionPane.showInputDialog(this,
                "Current category: " + (current.isBlank() ? "(missing)" : current)
                        + "\nChoose the correct category for post #" + postId + ":",
                "Set Category", JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"academic", "entertainment"}, null);
        if (choice == null) {
            return;
        }
        String category = choice.toString();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.eduvibe.util.AdminGuard.require(conn, adminId);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE posts SET category = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    ps.setString(1, category);
                    ps.setInt(2, postId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO moderation_logs (post_id, admin_id, action, note) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, postId);
                    ps.setInt(2, adminId);
                    ps.setString(3, "category_change");
                    ps.setString(4, "Category changed from '" + current + "' to '" + category + "'");
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not change the category: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadFlaggedPosts();     // the feeds read the category from the database, so the change shows there at once
    }

    private void updatePost(String newStatus) {

        int selectedRow = postsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a post first.",
                    "No Post Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int row = postsTable.convertRowIndexToModel(selectedRow);

        int postId = Integer.parseInt(
                model.getValueAt(row, 0).toString()
        );

        String action = newStatus.equals("approved")
                ? "approve"
                : "remove";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action + " this post?",
                "Confirm Moderation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String updateSql = """
            UPDATE posts
            SET moderation_status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        String logSql = """
            INSERT INTO moderation_logs
                (post_id, admin_id, action, note)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                com.eduvibe.util.AdminGuard.require(conn, adminId);

                // Update the post.
                try (PreparedStatement updateStmt =
                             conn.prepareStatement(updateSql)) {

                    updateStmt.setString(1, newStatus);
                    updateStmt.setInt(2, postId);

                    int affected = updateStmt.executeUpdate();

                    if (affected == 0) {
                        throw new SQLException(
                                "The selected post could not be found."
                        );
                    }
                }

                // Insert the admin activity log.
                try (PreparedStatement logStmt =
                             conn.prepareStatement(logSql)) {

                    logStmt.setInt(1, postId);
                    logStmt.setInt(2, adminId);
                    logStmt.setString(3, newStatus);

                    String note = "Admin ID " + adminId
                            + " " + action + "d post ID " + postId;

                    logStmt.setString(4, note);

                    logStmt.executeUpdate();
                }

                conn.commit();

                JOptionPane.showMessageDialog(
                        this,
                        "Post successfully " + newStatus
                                + ".\nAdmin activity has been recorded.",
                        "Moderation Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (SQLException e) {

                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

            loadFlaggedPosts();

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Moderation failed:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =====================================================
    // MEDIA PREVIEW RENDERER
    // =====================================================

    private class MediaRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setOpaque(true);

            label.setBackground(
                    isSelected
                            ? table.getSelectionBackground()
                            : CARD
            );

            label.setForeground(WHITE);

            int modelRow = table.convertRowIndexToModel(row);

            String mediaType = String.valueOf(
                    model.getValueAt(modelRow, 7)
            );

            String mediaPath = String.valueOf(
                    model.getValueAt(modelRow, 9)
            );

            if (mediaType.equalsIgnoreCase("Image")) {

                File file = new File(mediaPath);

                if (file.exists()) {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath());

                    Image scaled = icon.getImage().getScaledInstance(
                            85,
                            48,
                            Image.SCALE_SMOOTH
                    );

                    label.setIcon(new ImageIcon(scaled));
                    label.setText("");
                } else {
                    label.setText("Image missing");
                }

            } else if (mediaType.equalsIgnoreCase("Video")) {
                label.setText("▶ Video");
            } else {
                label.setText("No Media");
            }

            return label;
        }
    }

    // =====================================================
    // STATUS RENDERER
    // =====================================================

    private class StatusRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );

            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));

            String status = value == null
                    ? ""
                    : value.toString().toLowerCase();

            if (!isSelected) {

                switch (status) {
                    case "flagged":
                        label.setForeground(RED);
                        break;

                    case "approved":
                        label.setForeground(GREEN);
                        break;

                    case "removed":
                        label.setForeground(MUTED);
                        break;

                    default:
                        label.setForeground(BLUE);
                        break;
                }

                label.setBackground(CARD);
            }

            return label;
        }
    }
}
