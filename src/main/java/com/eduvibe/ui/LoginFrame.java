package com.eduvibe.ui;

import com.eduvibe.model.User;
import com.eduvibe.service.AuthService;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.FieldBox;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * com.eduvibe.ui.LoginFrame  (REPLACES existing file)
 *
 * Login screen matching reference screen 1: email-or-username, password
 * with show/hide, Log In button and a link to registration.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private FieldBox identifierField;
    private FieldBox passwordField;
    private RoundedButton loginButton;
    private JLabel errorLabel;

    public LoginFrame() {

        setTitle("EduVibe | Log In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        com.eduvibe.util.WindowFit.sizeAndCenter(this, 980, 720);
        setMinimumSize(new Dimension(Math.min(520, getWidth()), Math.min(560, getHeight())));

        buildUI();
    }

    private void buildUI() {

        RoundedPanel card = AuthUi.card(430);

        card.add(AuthUi.brand());
        card.add(Box.createVerticalStrut(6));
        card.add(AuthUi.centered("Learn  •  Connect  •  Grow",
                Theme.regular(12), Theme.MUTED));
        card.add(Box.createVerticalStrut(34));

        card.add(AuthUi.centered("Welcome back!", Theme.bold(24), Theme.TEXT));
        card.add(Box.createVerticalStrut(6));
        card.add(AuthUi.centered("Log in to continue your journey.",
                Theme.regular(13), Theme.MUTED));
        card.add(Box.createVerticalStrut(28));

        identifierField = FieldBox.text(Icons.Type.USER, "Email or username");
        passwordField = FieldBox.password("Password");

        card.add(AuthUi.fieldLabel("EMAIL OR USERNAME"));
        card.add(Box.createVerticalStrut(7));
        identifierField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(identifierField);
        card.add(Box.createVerticalStrut(18));

        card.add(AuthUi.fieldLabel("PASSWORD"));
        card.add(Box.createVerticalStrut(7));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));
        card.add(buildForgotLink());
        card.add(Box.createVerticalStrut(4));

        errorLabel = AuthUi.errorLabel();
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(12));

        loginButton = new RoundedButton("Log In");
        loginButton.setFont(Theme.bold(15));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        loginButton.setPreferredSize(new Dimension(300, 48));
        loginButton.addActionListener(e -> handleLogin());
        card.add(loginButton);

        card.add(Box.createVerticalStrut(22));
        card.add(buildRegisterLink());
        card.add(Box.createVerticalStrut(26));
        card.add(AuthUi.centered("Better learning. A more balanced you.",
                Theme.regular(11), Theme.alpha(Theme.MUTED, 170)));

        setContentPane(AuthUi.backdrop(card));
        getRootPane().setDefaultButton(loginButton);

        SwingUtilities.invokeLater(() -> identifierField.requestFocusInWindow());
    }

    private JPanel buildForgotLink() {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel link = new JLabel("Forgot password?");
        link.setFont(Theme.regular(12));
        link.setForeground(Theme.BLUE_LIGHT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ForgotPasswordDialog.show(LoginFrame.this);
            }
        });

        row.add(link);
        return row;
    }

    private JPanel buildRegisterLink() {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel prompt = new JLabel("Don't have an account?");
        prompt.setFont(Theme.regular(13));
        prompt.setForeground(Theme.MUTED);

        JLabel link = new JLabel("Register");
        link.setFont(Theme.bold(13));
        link.setForeground(Theme.BLUE_LIGHT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegister();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                link.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                link.setForeground(Theme.BLUE_LIGHT);
            }
        });

        row.add(prompt);
        row.add(link);

        return row;
    }

    private void openRegister() {

        setVisible(false);

        RegisterFrame register = new RegisterFrame(registeredUsername -> {
            setVisible(true);

            if (registeredUsername != null) {
                identifierField.getField().setText(registeredUsername);
                passwordField.clear();
                errorLabel.setText(" ");
                passwordField.requestFocusInWindow();
            }
        });

        register.setLocationRelativeTo(this);
        register.setVisible(true);
    }

    private void showError(String message) {
        errorLabel.setText(AuthUi.html(message));
    }

    private void handleLogin() {

        String identifier = identifierField.getText().trim();
        char[] password = passwordField.getPassword();

        if (identifier.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            showError("Enter your email or username and your password.");
            return;
        }

        errorLabel.setText(" ");
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        new SwingWorker<Optional<User>, Void>() {

            @Override
            protected Optional<User> doInBackground() throws Exception {
                try {
                    return authService.login(identifier, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {

                loginButton.setEnabled(true);
                loginButton.setText("Log In");

                try {
                    Optional<User> result = get();

                    if (result.isEmpty()) {
                        showError("Incorrect email, username or password.");
                        passwordField.clear();
                        passwordField.requestFocusInWindow();
                        return;
                    }

                    DashboardFrame dashboard = new DashboardFrame(result.get());
                    dashboard.setVisible(true);
                    dispose();

                } catch (ExecutionException ex) {

                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();

                    if (cause instanceof IllegalArgumentException
                            || cause instanceof IllegalStateException) {
                        showError(cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        Dialogs.error(LoginFrame.this, "Cannot connect",
                                "EduVibe could not reach the database. "
                                + "Check that MySQL is running and the "
                                + "connection settings are correct.\n\n"
                                + cause.getMessage());
                    }

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
