package com.eduvibe.ui;

import com.eduvibe.service.AuthService;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.FieldBox;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * com.eduvibe.ui.ForgotPasswordDialog  (NEW FILE)
 *
 * Reset a forgotten password. The username and the email must both match an
 * active account. Runs in a SwingWorker; the new password is hashed with BCrypt
 * in AuthService.
 */
public class ForgotPasswordDialog extends JDialog {

    private final AuthService authService = new AuthService();

    private FieldBox usernameField;
    private FieldBox emailField;
    private FieldBox passwordField;
    private FieldBox confirmField;
    private RoundedButton resetButton;
    private JLabel errorLabel;

    public static void show(Window owner) {
        new ForgotPasswordDialog(owner).setVisible(true);
    }

    private ForgotPasswordDialog(Window owner) {

        super(owner, "Reset password", Dialog.ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        // Same fixed-width stacking card as Login/Register (see AuthUi).
        RoundedPanel card = AuthUi.card(430);
        card.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));

        JLabel title = new JLabel("Reset your password");
        title.setFont(Theme.bold(20));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("<html><div style='width:290px'>Enter the username and email "
                + "of your account, then choose a new password.</div></html>");
        sub.setFont(Theme.regular(13));
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(18));

        usernameField = FieldBox.text(Icons.Type.AT, "Username");
        emailField = FieldBox.text(Icons.Type.MAIL, "Email");
        passwordField = FieldBox.password("New password (min. 8 characters)");
        confirmField = FieldBox.password("Confirm new password");

        add(card, "USERNAME", usernameField);
        add(card, "EMAIL", emailField);
        add(card, "NEW PASSWORD", passwordField);
        add(card, "CONFIRM PASSWORD", confirmField);

        errorLabel = AuthUi.errorLabel();
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        resetButton = new RoundedButton("Reset Password");
        resetButton.addActionListener(e -> reset());

        RoundedButton cancel = new RoundedButton("Cancel", RoundedButton.Style.OUTLINE);
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        buttons.add(cancel);
        buttons.add(resetButton);
        card.add(buttons);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.BACKGROUND);
        wrap.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        wrap.add(card, BorderLayout.CENTER);
        setContentPane(wrap);

        getRootPane().setDefaultButton(resetButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(owner);
    }

    private void add(JPanel card, String label, FieldBox field) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.bold(11));
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(l);
        card.add(Box.createVerticalStrut(6));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(field);
        card.add(Box.createVerticalStrut(12));
    }

    private void reset() {

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();

        if (username.isEmpty() || email.isEmpty() || password.length == 0) {
            wipe(password, confirm);
            error("Please complete all fields.");
            return;
        }

        if (!Arrays.equals(password, confirm)) {
            wipe(password, confirm);
            error("Passwords do not match.");
            return;
        }

        error(" ");
        resetButton.setEnabled(false);
        resetButton.setText("Resetting...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    return authService.resetPassword(username, email, password);
                } finally {
                    wipe(password, confirm);
                }
            }

            @Override
            protected void done() {
                resetButton.setEnabled(true);
                resetButton.setText("Reset Password");

                try {
                    if (get()) {
                        Dialogs.success(ForgotPasswordDialog.this, "Password reset",
                                "Your password was updated. You can now log in.");
                        dispose();
                    } else {
                        error("We could not verify those details.");
                    }
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof IllegalArgumentException) {
                        error(cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        Dialogs.error(ForgotPasswordDialog.this, "Reset failed",
                                "Could not reset the password.\n" + cause.getMessage());
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void error(String text) {
        errorLabel.setText(text.isBlank() ? " " : AuthUi.html(text));
    }

    private static void wipe(char[]... arrays) {
        for (char[] a : arrays) {
            Arrays.fill(a, '\0');
        }
    }
}
