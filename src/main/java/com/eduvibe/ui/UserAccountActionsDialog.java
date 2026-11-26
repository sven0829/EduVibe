
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.AdminNotificationDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserAccountActionsDialog extends JDialog {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BLUE = Theme.BLUE;
    private static final Color RED = new Color(220, 38, 38);
    private static final Color GREEN = new Color(16, 185, 129);

    private final int targetUserId;
    private final int adminId;
    private final String username;
    private final String currentStatus;

    public UserAccountActionsDialog(
            Window owner,
            int targetUserId,
            int adminId,
            String username,
            String currentStatus
    ) {
        super(owner, "Manage User Account", ModalityType.APPLICATION_MODAL);

        this.targetUserId = targetUserId;
        this.adminId = adminId;
        this.username = username;
        this.currentStatus = currentStatus == null
                ? "active" : currentStatus.toLowerCase();

        setSize(430, 310);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 25, 20, 25));

        JLabel title = new JLabel("Manage Account");
        title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 22));
        title.setForeground(WHITE);

        JLabel userLabel = new JLabel("@" + username);
        userLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 14));
        userLabel.setForeground(MUTED);

        JLabel statusLabel = new JLabel(
                "Current status: " + currentStatus.toUpperCase()
        );
        statusLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        statusLabel.setForeground(
                "active".equals(currentStatus) ? GREEN : RED
        );

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(22));

        if (targetUserId == adminId) {
            JLabel warning = new JLabel(
                    "You cannot change your own account status."
            );
            warning.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
            warning.setForeground(RED);
            panel.add(warning);

        } else if ("active".equals(currentStatus)) {

            panel.add(createActionButton(
                    "Suspend Account",
                    new Color(217, 119, 6),
                    "suspended",
                    "user_suspended"
            ));

            panel.add(Box.createVerticalStrut(10));

            panel.add(createActionButton(
                    "Ban Account",
                    RED,
                    "banned",
                    "user_banned"
            ));

        } else {

            panel.add(createActionButton(
                    "Activate Account",
                    GREEN,
                    "active",
                    "user_activated"
            ));
        }

        panel.add(Box.createVerticalGlue());

        JButton closeButton = new JButton("Close");
        styleButton(closeButton, BLUE);
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(BACKGROUND);
        footer.add(closeButton);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(BACKGROUND);
        container.add(panel, BorderLayout.CENTER);
        container.add(footer, BorderLayout.SOUTH);

        return container;
    }

    private JButton createActionButton(
            String label,
            Color color,
            String newStatus,
            String logAction
    ) {
        JButton button = new JButton(label);
        styleButton(button, color);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        button.addActionListener(
                e -> updateAccountStatus(newStatus, logAction)
        );

        return button;
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        button.setForeground(WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void updateAccountStatus(
            String newStatus,
            String logAction
    ) {

        String actionDescription;

        switch (newStatus) {
            case "suspended":
                actionDescription = "suspend";
                break;
            case "banned":
                actionDescription = "ban";
                break;
            default:
                actionDescription = "activate";
                break;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + actionDescription
                        + " @" + username + "?",
                "Confirm Account Action",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String updateSql =
                "UPDATE users SET status = ? WHERE id = ?";

        String logSql =
                "INSERT INTO moderation_logs "
                + "(post_id, admin_id, action, note) "
                + "VALUES (NULL, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                com.eduvibe.util.AdminGuard.require(conn, adminId);
                int updated;

                try (PreparedStatement update =
                             conn.prepareStatement(updateSql)) {

                    update.setString(1, newStatus);
                    update.setInt(2, targetUserId);
                    updated = update.executeUpdate();
                }

                if (updated == 0) {
                    throw new SQLException(
                            "The user account was not found."
                    );
                }

                String note = "Admin " + adminId
                        + " changed account @" + username
                        + " from " + currentStatus
                        + " to " + newStatus + ".";

                try (PreparedStatement log =
                             conn.prepareStatement(logSql)) {

                    log.setInt(1, adminId);
                    log.setString(2, logAction);
                    log.setString(3, note);
                    log.executeUpdate();
                }

                // Notify active admins within the same transaction.
                String notificationMessage =
                        "Admin changed @" + username
                        + " account status to "
                        + newStatus.toUpperCase() + ".";

                AdminNotificationDAO.notifyAdmins(
                        conn,
                        notificationMessage
                );

                conn.commit();

                JOptionPane.showMessageDialog(
                        this,
                        "Account status updated to "
                                + newStatus.toUpperCase() + ".",
                        "Account Updated",
                        JOptionPane.INFORMATION_MESSAGE
                );

                dispose();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to update account:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
