
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminActivityLogPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color PANEL = Theme.CARD;
    private static final Color BLUE = Theme.BLUE;
    private static final Color GREEN = new Color(16, 185, 129);
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;
    private static final Color RED = new Color(248, 113, 113);
    private static final Color ORANGE = new Color(251, 146, 60);

    private final JTextField searchField = new JTextField(16);

    private final JComboBox<String> actionFilter =
            new JComboBox<>(new String[]{
                "All Actions",
                "flagged",
                "approved",
                "removed",
                "edited_by_owner",
                "deleted_by_owner",
                "user_suspended",
                "user_banned",
                "user_activated",
                "role_promoted",
                "role_demoted",
                "report_reviewed",
                "report_dismissed",
                "points_adjusted",
                "user_registered",
                "daily_login_reward"
            });

    private final JComboBox<String> dateFilter =
            new JComboBox<>(new String[]{
                "All Time",
                "Today",
                "Last 7 Days",
                "Last 30 Days"
            });

    private final JComboBox<String> actorFilter =
            new JComboBox<>();

    private final Map<String, Integer> actorIdsByLabel =
            new LinkedHashMap<>();

    private final DefaultTableModel model =
            new DefaultTableModel(
                    new String[]{
                        "Log ID",
                        "Actor ID",
                        "Actor Username",
                        "Post ID",
                        "Action",
                        "Note",
                        "Date & Time"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable table = new JTable(model);

    private final TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(model);

    private final JLabel resultLabel =
            new JLabel("Loading logs...");

    private final JButton refreshButton =
            new JButton("Refresh");

    private final JButton exportButton =
            new JButton("Export CSV");

    private final JButton clearButton =
            new JButton("Clear Filters");

    public AdminActivityLogPanel() {

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        styleTable();
        setupFilters();
        loadLogs();
    }

    private JPanel buildHeader() {

        JPanel header = new JPanel(new BorderLayout(0, 15));
        header.setBackground(BACKGROUND);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(
                titlePanel, BoxLayout.Y_AXIS
        ));
        titlePanel.setBackground(BACKGROUND);

        JLabel title = new JLabel("Admin Activity Log");
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 22));
        title.setForeground(WHITE);

        JLabel subtitle = new JLabel(
                "Monitor registrations, login rewards, account changes, "
                + "and moderation history."
        );
        subtitle.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);

        JPanel controls = new JPanel(new FlowLayout(
                FlowLayout.LEFT, 8, 0
        ));
        controls.setBackground(BACKGROUND);

        searchField.setPreferredSize(new Dimension(175, 34));
        searchField.setToolTipText(
                "Search log ID, actor, action, post ID, or note"
        );

        actorFilter.addItem("All Actors");

        styleField(searchField);
        styleCombo(actionFilter);
        styleCombo(dateFilter);
        styleCombo(actorFilter);

        styleButton(refreshButton, BLUE);
        styleButton(exportButton, GREEN);
        styleButton(clearButton, new Color(71, 85, 105));

        refreshButton.addActionListener(e -> loadLogs());
        exportButton.addActionListener(e -> exportCSV());
        clearButton.addActionListener(e -> clearFilters());

        controls.add(searchField);
        controls.add(actorFilter);
        controls.add(actionFilter);
        controls.add(dateFilter);
        controls.add(clearButton);
        controls.add(refreshButton);
        controls.add(exportButton);

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

        JPanel info = new JPanel(new BorderLayout());
        info.setBackground(BACKGROUND);

        JLabel sectionTitle =
                new JLabel("Platform Activity History");

        sectionTitle.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 15));
        sectionTitle.setForeground(WHITE);

        resultLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        resultLabel.setForeground(MUTED);

        info.add(sectionTitle, BorderLayout.WEST);
        info.add(resultLabel, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER));
        scrollPane.getViewport().setBackground(PANEL);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(info, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void styleField(JTextField field) {

        field.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        field.setForeground(WHITE);
        field.setBackground(PANEL);
        field.setCaretColor(WHITE);
        field.setBorder(new LineBorder(BORDER));
    }

    private void styleCombo(JComboBox<String> combo) {

        combo.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        combo.setForeground(WHITE);
        combo.setBackground(PANEL);
        combo.setFocusable(false);
    }

    private void styleButton(JButton button, Color color) {

        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));
        button.setForeground(WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleTable() {

        table.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        table.setForeground(WHITE);
        table.setBackground(PANEL);
        table.setRowHeight(37);
        table.setGridColor(BORDER);
        table.setSelectionBackground(Theme.BORDER);
        table.setSelectionForeground(WHITE);
        table.setFillsViewportHeight(true);
        table.setRowSorter(sorter);
        table.setAutoCreateRowSorter(false);

        table.getTableHeader().setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setBackground(
                Theme.INPUT
        );
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(145);
        table.getColumnModel().getColumn(5).setPreferredWidth(300);
        table.getColumnModel().getColumn(6).setPreferredWidth(155);

        table.getColumnModel().getColumn(4).setCellRenderer(
                new ActionCellRenderer()
        );

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedLogDetails();
                }
            }
        });
    }

    private class ActionCellRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {

            Component component =
                    super.getTableCellRendererComponent(
                            table, value, selected,
                            focused, row, column
                    );

            String action = value == null
                    ? "" : value.toString();

            if (!selected) {

                setBackground(PANEL);

                if (action.contains("removed")
                        || action.contains("banned")
                        || action.contains("suspended")
                        || action.contains("deleted")) {

                    setForeground(RED);

                } else if (action.contains("approved")
                        || action.contains("activated")
                        || action.contains("promoted")
                        || action.contains("reviewed")
                        || action.equals("daily_login_reward")
                        || action.equals("user_registered")) {

                    setForeground(GREEN);

                } else if (action.contains("flagged")
                        || action.contains("dismissed")
                        || action.contains("demoted")) {

                    setForeground(ORANGE);

                } else {
                    setForeground(BLUE);
                }

            } else {
                setBackground(table.getSelectionBackground());
                setForeground(WHITE);
            }

            setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));

            return component;
        }
    }

    private void setupFilters() {

        searchField.getDocument().addDocumentListener(
                new DocumentListener() {

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

        actorFilter.addActionListener(e -> applyFilters());
        actionFilter.addActionListener(e -> applyFilters());
        dateFilter.addActionListener(e -> applyFilters());
    }

    public void loadLogs() {

        refreshButton.setEnabled(false);
        exportButton.setEnabled(false);
        resultLabel.setText("Loading logs...");

        new SwingWorker<List<Object[]>, Void>() {

            @Override
            protected List<Object[]> doInBackground()
                    throws Exception {

                List<Object[]> logs = new ArrayList<>();

                String sql =
                        "SELECT m.id, m.admin_id, "
                        + "COALESCE(u.username, 'Unknown') "
                        + "AS actor_username, "
                        + "m.post_id, m.action, m.note, "
                        + "m.created_at "
                        + "FROM moderation_logs m "
                        + "LEFT JOIN users u ON m.admin_id = u.id "
                        + "ORDER BY m.created_at DESC";

                try (Connection conn =
                             DatabaseConnection.getConnection();
                     PreparedStatement stmt =
                             conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    SimpleDateFormat format =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss"
                            );

                    while (rs.next()) {

                        java.sql.Timestamp timestamp =
                                rs.getTimestamp("created_at");

                        logs.add(new Object[]{
                            rs.getInt("id"),
                            rs.getObject("admin_id"),
                            rs.getString("actor_username"),
                            rs.getObject("post_id"),
                            rs.getString("action"),
                            rs.getString("note"),
                            timestamp == null
                                    ? ""
                                    : format.format(timestamp)
                        });
                    }
                }

                return logs;
            }

            @Override
            protected void done() {

                refreshButton.setEnabled(true);
                exportButton.setEnabled(true);

                try {

                    List<Object[]> logs = get();

                    model.setRowCount(0);
                    actorIdsByLabel.clear();

                    for (Object[] log : logs) {

                        model.addRow(log);

                        Object actorId = log[1];
                        String username = String.valueOf(log[2]);

                        if (actorId != null) {

                            String label = "@"
                                    + username
                                    + " (ID: " + actorId + ")";

                            actorIdsByLabel.put(
                                    label,
                                    Integer.parseInt(
                                            actorId.toString()
                                    )
                            );
                        }
                    }

                    updateActorFilter();
                    applyFilters();

                    if (logs.isEmpty()) {
                        resultLabel.setText(
                                "No activity logs found"
                        );
                    }

                } catch (Exception ex) {

                    Throwable cause = ex.getCause() == null
                            ? ex : ex.getCause();

                    resultLabel.setText("Unable to load logs");

                    JOptionPane.showMessageDialog(
                            AdminActivityLogPanel.this,
                            "Unable to load activity logs:\n"
                            + cause.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void updateActorFilter() {

        String previousSelection =
                (String) actorFilter.getSelectedItem();

        actorFilter.removeAllItems();
        actorFilter.addItem("All Actors");

        for (String label : actorIdsByLabel.keySet()) {
            actorFilter.addItem(label);
        }

        if (previousSelection != null) {
            actorFilter.setSelectedItem(previousSelection);
        }

        if (actorFilter.getSelectedItem() == null) {
            actorFilter.setSelectedIndex(0);
        }
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

        String selectedAction =
                (String) actionFilter.getSelectedItem();

        if (selectedAction != null
                && !"All Actions".equals(selectedAction)) {

            filters.add(RowFilter.regexFilter(
                    "^" + Pattern.quote(selectedAction) + "$",
                    4
            ));
        }

        String selectedActor =
                (String) actorFilter.getSelectedItem();

        if (selectedActor != null
                && !"All Actors".equals(selectedActor)) {

            Integer actorId =
                    actorIdsByLabel.get(selectedActor);

            if (actorId != null) {
                filters.add(RowFilter.regexFilter(
                        "^" + actorId + "$",
                        1
                ));
            }
        }

        String selectedDate =
                (String) dateFilter.getSelectedItem();

        if (selectedDate != null
                && !"All Time".equals(selectedDate)) {

            long now = System.currentTimeMillis();
            long start;

            switch (selectedDate) {

                case "Today":

                    Calendar calendar = Calendar.getInstance();

                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    start = calendar.getTimeInMillis();
                    break;

                case "Last 7 Days":
                    start = now - 7L * 24 * 60 * 60 * 1000;
                    break;

                case "Last 30 Days":
                    start = now - 30L * 24 * 60 * 60 * 1000;
                    break;

                default:
                    start = 0;
            }

            final long dateStart = start;

            filters.add(
                    new RowFilter<DefaultTableModel, Integer>() {

                @Override
                public boolean include(
                        Entry<? extends DefaultTableModel,
                        ? extends Integer> entry) {

                    Object value = entry.getValue(6);

                    if (value == null
                            || value.toString().isEmpty()) {
                        return false;
                    }

                    try {

                        Date date = new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss"
                        ).parse(value.toString());

                        return date.getTime() >= dateStart;

                    } catch (Exception ex) {
                        return false;
                    }
                }
            });
        }

        sorter.setRowFilter(
                filters.isEmpty()
                        ? null
                        : RowFilter.andFilter(filters)
        );

        resultLabel.setText(
                sorter.getViewRowCount()
                + " of " + model.getRowCount()
                + " logs shown"
        );
    }

    private void clearFilters() {

        searchField.setText("");
        actionFilter.setSelectedIndex(0);
        dateFilter.setSelectedIndex(0);
        actorFilter.setSelectedIndex(0);

        applyFilters();
    }

    private void showSelectedLogDetails() {

        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        int row = table.convertRowIndexToModel(selectedRow);

        StringBuilder details = new StringBuilder();

        details.append("Log ID: ")
                .append(model.getValueAt(row, 0))
                .append("\n\n");

        details.append("Actor ID: ")
                .append(model.getValueAt(row, 1))
                .append("\n");

        details.append("Actor Username: ")
                .append(model.getValueAt(row, 2))
                .append("\n\n");

        details.append("Post ID: ")
                .append(model.getValueAt(row, 3))
                .append("\n");

        details.append("Action: ")
                .append(model.getValueAt(row, 4))
                .append("\n\n");

        details.append("Note:\n")
                .append(model.getValueAt(row, 5))
                .append("\n\n");

        details.append("Date & Time: ")
                .append(model.getValueAt(row, 6));

        JTextArea textArea = new JTextArea(
                details.toString()
        );

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        textArea.setBackground(PANEL);
        textArea.setForeground(WHITE);
        textArea.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(430, 300));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Activity Log Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void exportCSV() {

        if (table.getRowCount() == 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "There are no visible logs to export."
            );

            return;
        }

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle(
                "Export Platform Activity Logs"
        );

        chooser.setSelectedFile(
                new File("EduVibe_Admin_Activity_Logs.csv")
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "CSV Files (*.csv)", "csv"
                )
        );

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(
                    file.getAbsolutePath() + ".csv"
            );
        }

        try (FileWriter writer = new FileWriter(file)) {

            int columns = table.getColumnCount();

            for (int col = 0; col < columns; col++) {

                writer.append(
                        csvEscape(table.getColumnName(col))
                );

                if (col < columns - 1) {
                    writer.append(",");
                }
            }

            writer.append("\n");

            for (int row = 0; row < table.getRowCount(); row++) {

                for (int col = 0; col < columns; col++) {

                    Object value = table.getValueAt(row, col);

                    writer.append(
                            csvEscape(
                                    value == null
                                            ? ""
                                            : value.toString()
                            )
                    );

                    if (col < columns - 1) {
                        writer.append(",");
                    }
                }

                writer.append("\n");
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Activity logs exported successfully:\n"
                    + file.getAbsolutePath(),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to export CSV:\n"
                    + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String csvEscape(String value) {

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }
}
