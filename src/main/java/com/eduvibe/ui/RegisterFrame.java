package com.eduvibe.ui;

import com.eduvibe.service.AuthService;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.FieldBox;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;

import com.eduvibe.ui.components.PasswordStrengthBar;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.RegisterFrame  (REPLACES existing file)
 *
 * Registration screen matching reference screen 1 (right side).
 * Validation, duplicate checks and BCrypt hashing stay in AuthService.
 */
public class RegisterFrame extends JFrame {

    private final AuthService authService = new AuthService();

    /** Receives the new username after a successful sign-up, or null if closed. */
    private final Consumer<String> onFinished;

    private FieldBox fullNameField;
    private FieldBox emailField;
    private FieldBox usernameField;
    private FieldBox passwordField;
    private FieldBox confirmField;
    private RoundedButton registerButton;
    private JLabel errorLabel;

    private String registeredUsername;

    /** Compatibility constructor: behaves like the old no-argument one. */
    public RegisterFrame() {
        this(null);
    }

    public RegisterFrame(Consumer<String> onFinished) {

        this.onFinished = onFinished;

        setTitle("EduVibe | Create Account");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 820);
        setMinimumSize(new Dimension(520, 780));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (RegisterFrame.this.onFinished != null) {
                    RegisterFrame.this.onFinished.accept(registeredUsername);
                }
            }
        });

        buildUI();
    }

    private void buildUI() {

        RoundedPanel card = AuthUi.card(430);
        card.setBorder(BorderFactory.createEmptyBorder(26, 38, 26, 38));

        card.add(buildBackLink());
        card.add(Box.createVerticalStrut(16));

        JLabel heading = new JLabel("Create your account");
        heading.setFont(Theme.bold(24));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);

        card.add(Box.createVerticalStrut(5));

        JLabel subtitle = new JLabel("Join EduVibe and be part of our community.");
        subtitle.setFont(Theme.regular(13));
        subtitle.setForeground(Theme.MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);

        card.add(Box.createVerticalStrut(20));

        fullNameField = FieldBox.text(Icons.Type.USER, "Full name");
        emailField = FieldBox.text(Icons.Type.MAIL, "Email");
        usernameField = FieldBox.text(Icons.Type.AT, "Username");
        passwordField = FieldBox.password("Password (min. 8 characters)");
        confirmField = FieldBox.password("Confirm password");

        addField(card, "FULL NAME", fullNameField);
        addField(card, "EMAIL", emailField);
        addField(card, "USERNAME", usernameField);
        addField(card, "PASSWORD", passwordField);
        PasswordStrengthBar strength = new PasswordStrengthBar();
        strength.attach(passwordField.getField());
        card.add(strength);
        card.add(Box.createVerticalStrut(10));
        addField(card, "CONFIRM PASSWORD", confirmField);

        errorLabel = AuthUi.errorLabel();
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        registerButton = new RoundedButton("Sign Up");
        registerButton.setFont(Theme.bold(15));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        registerButton.setPreferredSize(new Dimension(300, 48));
        registerButton.addActionListener(e -> handleRegistration());
        card.add(registerButton);

        card.add(Box.createVerticalStrut(18));
        card.add(buildLoginLink());

        setContentPane(AuthUi.backdrop(card));
        getRootPane().setDefaultButton(registerButton);

        SwingUtilities.invokeLater(() -> fullNameField.requestFocusInWindow());
    }

    private void addField(JPanel card, String label, FieldBox field) {
        card.add(AuthUi.fieldLabel(label));
        card.add(Box.createVerticalStrut(6));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(field);
        card.add(Box.createVerticalStrut(12));
    }

    private JPanel buildBackLink() {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel back = new JLabel("Back to log in",
                Icons.icon(Icons.Type.ARROW_LEFT, 16, Theme.MUTED), SwingConstants.LEFT);
        back.setFont(Theme.regular(13));
        back.setForeground(Theme.MUTED);
        back.setIconTextGap(8);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                back.setForeground(Theme.TEXT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                back.setForeground(Theme.MUTED);
            }
        });

        row.add(back);
        return row;
    }

    private JPanel buildLoginLink() {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel prompt = new JLabel("Already have an account?");
        prompt.setFont(Theme.regular(13));
        prompt.setForeground(Theme.MUTED);

        JLabel link = new JLabel("Log In");
        link.setFont(Theme.bold(13));
        link.setForeground(Theme.BLUE_LIGHT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });

        row.add(prompt);
        row.add(link);

        return row;
    }

    private void showError(String message) {
        errorLabel.setText(AuthUi.html(message));
    }

    private void handleRegistration() {

        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();

        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();

        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty()
                || password.length == 0 || confirm.length == 0) {
            wipe(password, confirm);
            showError("Please complete all fields.");
            return;
        }

        if (!Arrays.equals(password, confirm)) {
            wipe(password, confirm);
            showError("Passwords do not match.");
            confirmField.clear();
            confirmField.requestFocusInWindow();
            return;
        }

        errorLabel.setText(" ");
        registerButton.setEnabled(false);
        registerButton.setText("Creating account...");

        new SwingWorker<Integer, Void>() {

            @Override
            protected Integer doInBackground() throws Exception {
                try {
                    return authService.register(fullName, username, email, password);
                } finally {
                    wipe(password, confirm);
                }
            }

            @Override
            protected void done() {

                registerButton.setEnabled(true);
                registerButton.setText("Sign Up");

                try {
                    get();

                    registeredUsername = username;

                    Dialogs.success(RegisterFrame.this, "Account created",
                            "Welcome to EduVibe, " + fullName
                            + "! You can now log in.");

                    dispose();

                } catch (ExecutionException ex) {

                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();

                    if (cause instanceof IllegalArgumentException) {
                        showError(cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        Dialogs.error(RegisterFrame.this, "Registration failed",
                                "The account could not be created. Check your "
                                + "database connection and try again.\n\n"
                                + cause.getMessage());
                    }

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private static void wipe(char[]... arrays) {
        for (char[] array : arrays) {
            Arrays.fill(array, '\0');
        }
    }
}
