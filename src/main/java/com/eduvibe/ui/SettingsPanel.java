
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.AdminNotificationDAO;
import com.eduvibe.model.User;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.PageHero;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class SettingsPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color CARD = Theme.CARD;
    private static final Color INPUT = Theme.INPUT;
    private static final Color BLUE = Theme.BLUE;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;
    private static final Color GREEN = Theme.GREEN;

    private final User currentUser;
    private JCheckBox reminderEnabled;
    private JComboBox<Integer> reminderInterval;

    private JTextField fullNameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextArea bioArea;

    private JLabel roleLabel;
    private JLabel statusLabel;
    private JLabel pointsLabel;

    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public SettingsPanel(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }

        currentUser = user;

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        buildUI();
        loadSettings();
    }

    private void buildUI() {

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(8, 0, 20, 0));

        PageHero hero = new PageHero(Icons.Type.GEAR, "Settings",
                "Your profile, password, reminders and look - all in one place.",
                PageHero.SETTINGS[0], PageHero.SETTINGS[1]);
        hero.setChip(safeText(currentUser.getFullName()).isBlank() ? "Your account" : currentUser.getFullName(),
                Icons.Type.USER);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(new EmptyBorder(0, 0, 18, 0));
        heading.add(hero, BorderLayout.CENTER);

        JPanel content = new WidthFollowingPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);

        JPanel leftCol = column();
        leftCol.add(buildProfileSettings());
        leftCol.add(Box.createVerticalStrut(20));
        leftCol.add(buildAccountSettings());
        leftCol.add(Box.createVerticalStrut(20));
        leftCol.add(buildStatusSettings());

        JPanel rightCol = column();
        rightCol.add(buildPasswordSettings());
        rightCol.add(Box.createVerticalStrut(20));
        rightCol.add(buildEntertainmentLimitSettings());
        rightCol.add(Box.createVerticalStrut(20));
        rightCol.add(buildNotificationPreferences());
        rightCol.add(Box.createVerticalStrut(20));
        rightCol.add(buildAppearanceSettings());

        JPanel twoCol = new com.eduvibe.ui.components.TwoColumn(leftCol, rightCol, 20, 760);
        content.add(twoCol);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        page.add(heading, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);

        add(page, BorderLayout.CENTER);
    }

    private static JPanel column() {
        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        return c;
    }

    // PERSONAL INFORMATION

    private JPanel buildProfileSettings() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Personal Information",
                "Update your name and personal introduction."
        ));

        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Full Name"));

        fullNameField = createTextField();
        card.add(fullNameField);

        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Bio"));

        bioArea = new JTextArea(4, 20);
        bioArea.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        bioArea.setForeground(WHITE);
        bioArea.setBackground(INPUT);
        bioArea.setCaretColor(WHITE);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane bioScroll = new JScrollPane(bioArea);
        bioScroll.setBorder(new LineBorder(BORDER, 1, true));
        bioScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        bioScroll.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 110)
        );

        card.add(bioScroll);
        card.add(Box.createVerticalStrut(18));

        JButton saveButton = createButton("Save Profile Changes");
        saveButton.addActionListener(e -> saveProfile());
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(saveButton);

        return card;
    }

    // ACCOUNT INFORMATION

    private JPanel buildAccountSettings() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Account Information",
                "Your registered account details."
        ));

        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Username"));

        usernameField = createTextField();
        usernameField.setEditable(false);
        usernameField.setForeground(MUTED);
        card.add(usernameField);

        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Email Address"));

        emailField = createTextField();
        emailField.setEditable(false);
        emailField.setForeground(MUTED);
        card.add(emailField);

        card.add(Box.createVerticalStrut(10));

        JLabel note = new JLabel(
                "Username and email cannot be changed here."
        );
        note.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        note.setForeground(MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(note);

        return card;
    }

    // PASSWORD SETTINGS

    private JPanel buildPasswordSettings() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Change Password",
                "Protect your account with a new password."
        ));

        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Current Password"));

        currentPasswordField = createPasswordField();
        card.add(currentPasswordField);

        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("New Password"));

        newPasswordField = createPasswordField();
        card.add(newPasswordField);
        com.eduvibe.ui.components.PasswordStrengthBar strength = new com.eduvibe.ui.components.PasswordStrengthBar();
        strength.attach(newPasswordField);
        card.add(strength);

        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Confirm New Password"));

        confirmPasswordField = createPasswordField();
        card.add(confirmPasswordField);

        card.add(Box.createVerticalStrut(18));

        JButton changeButton = createButton("Update Password");
        changeButton.addActionListener(e -> changePassword());
        changeButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(changeButton);

        return card;
    }

    // ACCOUNT STATUS

    private JPanel buildStatusSettings() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Account Status",
                "Your current EduVibe account information."
        ));

        card.add(Box.createVerticalStrut(20));

        roleLabel = createValueLabel("Loading...");
        statusLabel = createValueLabel("Loading...");
        pointsLabel = createValueLabel("Loading...");

        JPanel tiles = new JPanel(new GridLayout(1, 3, 10, 0));
        tiles.setOpaque(false);
        tiles.setAlignmentX(Component.LEFT_ALIGNMENT);
        tiles.add(statusTile("Role", roleLabel));
        tiles.add(statusTile("Status", statusLabel));
        tiles.add(statusTile("Points", pointsLabel));
        card.add(tiles);

        return card;
    }

    // ENTERTAINMENT TIME LIMIT

    // NOTIFICATION PREFERENCES

    private JPanel buildNotificationPreferences() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Notifications",
                "Choose which notifications you want to see. They apply right away."
        ));
        card.add(Box.createVerticalStrut(16));

        card.add(notificationSwitch("Likes on my posts", com.eduvibe.util.NotificationPrefs.LIKES));
        card.add(Box.createVerticalStrut(8));
        card.add(notificationSwitch("Comments on my posts", com.eduvibe.util.NotificationPrefs.COMMENTS));
        card.add(Box.createVerticalStrut(8));
        card.add(notificationSwitch("Rewards and system messages", com.eduvibe.util.NotificationPrefs.SYSTEM));
        card.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("Hidden ones are not deleted, only not shown.");
        hint.setFont(com.eduvibe.ui.Theme.regular(12));
        hint.setForeground(com.eduvibe.ui.Theme.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);

        return card;
    }

    private SwitchBox notificationSwitch(String text, String kind) {
        SwitchBox box = new SwitchBox(text);
        box.setSelected(com.eduvibe.util.NotificationPrefs.isEnabled(currentUser.getId(), kind));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.addActionListener(e ->
                com.eduvibe.util.NotificationPrefs.setEnabled(currentUser.getId(), kind, box.isSelected()));
        return box;
    }

    // APPEARANCE (light / dark)

    private JPanel buildAppearanceSettings() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Appearance",
                "Choose a dark or light look. EduVibe restarts by itself to apply it."
        ));
        card.add(Box.createVerticalStrut(18));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new ThemeTile(false, !Theme.isLight(), () -> chooseTheme(false)));
        row.add(new ThemeTile(true, Theme.isLight(), () -> chooseTheme(true)));
        card.add(row);
        card.add(Box.createVerticalStrut(16));

        if (!Theme.isLight()) {
            JLabel styleTitle = new JLabel("Dark style");
            styleTitle.setFont(Theme.bold(13));
            styleTitle.setForeground(Theme.TEXT);
            styleTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(styleTitle);
            card.add(Box.createVerticalStrut(8));
            JPanel styles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            styles.setOpaque(false);
            styles.setAlignmentX(Component.LEFT_ALIGNMENT);
            styles.add(new StyleSwatch("midnight", "Midnight", new Color(13, 20, 41), new Color(59, 130, 246)));
            styles.add(new StyleSwatch("ocean", "Ocean", new Color(8, 27, 37), new Color(6, 182, 212)));
            styles.add(new StyleSwatch("plum", "Plum", new Color(27, 13, 39), new Color(236, 72, 153)));
            card.add(styles);
            card.add(Box.createVerticalStrut(16));
        }

        SwitchBox reduce = new SwitchBox("Reduce visual effects");
        reduce.setSelected(com.eduvibe.ui.components.Aurora.isReduced());
        reduce.setAlignmentX(Component.LEFT_ALIGNMENT);
        reduce.addActionListener(e -> com.eduvibe.ui.components.Aurora.setReduced(reduce.isSelected()));
        card.add(reduce);
        JLabel reduceHint = new JLabel("Turns off the glow. Helps older laptops.");
        reduceHint.setFont(com.eduvibe.ui.Theme.regular(12));
        reduceHint.setForeground(com.eduvibe.ui.Theme.MUTED);
        reduceHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        reduceHint.setBorder(BorderFactory.createEmptyBorder(2, 52, 0, 0));
        card.add(reduceHint);

        return card;
    }

    /** A round colour sample for a dark style; the one in use has a ring. Picking one restarts EduVibe. */
    private final class StyleSwatch extends JComponent {
        private final String key;
        private final String label;
        private final Color base, accent;
        private boolean hover;

        StyleSwatch(String key, String label, Color base, Color accent) {
            this.key = key;
            this.label = label;
            this.base = base;
            this.accent = accent;
            setPreferredSize(new Dimension(84, 86));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    chooseStyle(key, label);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            boolean on = key.equals(Theme.style());
            int d = 52, x = (getWidth() - d) / 2, y = 4;
            g2.setPaint(new GradientPaint(x, y, base.brighter(), x + d, y + d, base));
            g2.fillOval(x, y, d, d);
            g2.setColor(accent);
            g2.fillArc(x, y, d, d, 40, 100);                  // a sliver of the accent colour
            g2.setStroke(new BasicStroke(on ? 3f : 1.5f));
            g2.setColor(on ? Theme.BLUE_LIGHT : (hover ? Theme.TEXT : Theme.BORDER));
            g2.drawOval(x - 4, y - 4, d + 8, d + 8);
            g2.setFont(Theme.bold(12));
            g2.setColor(on ? Theme.TEXT : Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, y + d + 22);
            g2.dispose();
        }
    }

    private void chooseStyle(String key, String label) {
        if (key.equals(Theme.style())) {
            com.eduvibe.ui.components.Toast.info(this, label + " is already on.");
            return;
        }
        boolean go = com.eduvibe.ui.components.Dialogs.confirm(this,
                "Switch to the " + label + " style?",
                "EduVibe will close and reopen by itself to apply the new colours.\nYou stay signed in.",
                "Switch now", false);
        if (!go) {
            return;
        }
        Theme.saveStyle(key);
        if (!com.eduvibe.util.AppRestart.relaunch(currentUser.getId())) {
            com.eduvibe.ui.components.Toast.info(this, label + " style saved. Reopen EduVibe to see it.");
        }
    }

    /** A small painted preview of the Dark or Light look; the one in use glows. */
    private static final class ThemeTile extends JComponent {
        private final boolean light;
        private final boolean selected;
        private boolean hover;

        ThemeTile(boolean light, boolean selected, Runnable pick) {
            this.light = light;
            this.selected = selected;
            setPreferredSize(new Dimension(150, 112));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(light ? "Light theme" : "Dark theme");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    pick.run();
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            Color bg = light ? new Color(241, 245, 249) : new Color(11, 16, 32);
            Color side = light ? new Color(255, 255, 255) : new Color(17, 24, 45);
            Color card = light ? new Color(255, 255, 255) : new Color(28, 38, 70);
            Color bar = light ? new Color(203, 213, 225) : new Color(51, 65, 100);

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            java.awt.geom.RoundRectangle2D clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, w - 1, h - 1, 16, 16);
            g2.setClip(clip);
            g2.setColor(side);
            g2.fillRect(0, 0, 34, h - 26);
            g2.setPaint(Theme.brand(6, 12, 28, 12, 0));
            g2.fillRoundRect(6, 12, 22, 7, 7, 7);
            g2.setColor(bar);
            g2.fillRoundRect(6, 26, 22, 6, 6, 6);
            g2.fillRoundRect(6, 38, 22, 6, 6, 6);
            g2.setColor(card);
            g2.fillRoundRect(42, 12, w - 54, 22, 8, 8);
            g2.fillRoundRect(42, 40, (w - 54) / 2 - 3, 28, 8, 8);
            g2.fillRoundRect(42 + (w - 54) / 2 + 3, 40, (w - 54) / 2 - 3, 28, 8, 8);
            g2.setPaint(Theme.brand(48, 18, 90, 18, 0));
            g2.fillRoundRect(48, 18, 40, 6, 6, 6);
            g2.setClip(null);

            // caption strip
            g2.setColor(light ? new Color(226, 232, 240) : new Color(20, 28, 52));
            g2.setClip(clip);
            g2.fillRect(0, h - 28, w, 28);
            g2.setClip(null);
            g2.setFont(Theme.font(Font.BOLD, 12));
            g2.setColor(light ? new Color(30, 41, 59) : new Color(226, 232, 240));
            g2.drawString(light ? "Light" : "Dark", 12, h - 9);
            if (selected) {
                g2.setPaint(Theme.brand(w - 30, h - 24, w - 10, h - 24, 0));
                g2.fillOval(w - 28, h - 24, 18, 18);
                Icons.paint(g2, Icons.Type.CHECK, w - 25, h - 21, 12, Color.WHITE);
            }

            if (selected) {
                g2.setStroke(new BasicStroke(2.5f));
                g2.setPaint(Theme.brand(0, 0, w, h, 0));
            } else {
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(hover ? Theme.BLUE_LIGHT : Theme.BORDER);
            }
            g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
            g2.dispose();
        }
    }

    /** Check box drawn as an on/off switch (still a JCheckBox, so isSelected() works as before). */
    private static final class SwitchBox extends JCheckBox {
        SwitchBox(String text) {
            super(text);
            setOpaque(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFont(Theme.font(Font.BOLD, 13));
            setForeground(Theme.TEXT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(52 + fm.stringWidth(getText()), 28);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();      // the default would ignore the extra switch width and clip the text
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int ty = (getHeight() - 22) / 2;
            if (isSelected()) {
                g2.setPaint(Theme.brand(0, ty, 40, ty, 0));
            } else {
                g2.setColor(Theme.CARD_LIGHT);
            }
            g2.fillRoundRect(0, ty, 40, 22, 22, 22);
            if (!isSelected()) {
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, ty, 39, 21, 22, 22);
            }
            g2.setColor(Color.WHITE);
            g2.fillOval(isSelected() ? 20 : 3, ty + 3, 16, 16);
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 52, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private void chooseTheme(boolean light) {
        if (light == Theme.isLight()) {
            com.eduvibe.ui.components.Toast.info(this,
                    (light ? "Light" : "Dark") + " theme is already on.");
            return;
        }
        boolean go = com.eduvibe.ui.components.Dialogs.confirm(this,
                "Switch to " + (light ? "light" : "dark") + " theme?",
                "EduVibe will close and reopen by itself to apply the new look.\n"
                        + "You stay signed in.",
                "Switch now", false);
        if (!go) {
            return;
        }
        Theme.saveLight(light);
        if (!com.eduvibe.util.AppRestart.relaunch(currentUser.getId())) {
            com.eduvibe.ui.components.Toast.info(this,
                    (light ? "Light" : "Dark") + " theme saved. Reopen EduVibe to see it.");
        }
    }

    private static final int[] INTERVALS = {15, 30, 45, 60};

    private java.util.prefs.Preferences reminderPrefs() {
        return java.util.prefs.Preferences.userRoot()
                .node("com/eduvibe/entertainment/" + currentUser.getId());
    }

    private JPanel buildEntertainmentLimitSettings() {

        JPanel card = createCard();

        card.add(sectionTitle(
                "Entertainment Reminder",
                "A friendly reminder after you've actively browsed Entertainment and Reels for a while. "
                        + "It never blocks or hides anything."
        ));
        card.add(Box.createVerticalStrut(18));

        int saved = Math.max(0, currentUser.getEntertainmentLimitMinutes());
        int remembered = reminderPrefs().getInt("interval", 30);
        int interval = saved > 0 ? saved : remembered;

        reminderEnabled = new SwitchBox("Remind me while I browse");
        reminderEnabled.setSelected(saved > 0);
        reminderEnabled.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(reminderEnabled);
        card.add(Box.createVerticalStrut(14));

        card.add(fieldLabel("Remind me after"));

        reminderInterval = new JComboBox<>();
        for (int m : INTERVALS) {
            reminderInterval.addItem(m);
        }
        if (interval > 0 && java.util.Arrays.stream(INTERVALS).noneMatch(m -> m == interval)) {
            reminderInterval.addItem(interval);          // keep a custom value saved earlier
        }
        reminderInterval.setSelectedItem(interval > 0 ? interval : 30);
        reminderInterval.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean sel, boolean focus) {
                return super.getListCellRendererComponent(list, value + " minutes", index, sel, focus);
            }
        });
        reminderInterval.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 14));
        reminderInterval.setMaximumSize(new Dimension(200, 40));
        reminderInterval.setAlignmentX(Component.LEFT_ALIGNMENT);
        reminderInterval.setEnabled(reminderEnabled.isSelected());
        reminderEnabled.addActionListener(e -> reminderInterval.setEnabled(reminderEnabled.isSelected()));
        card.add(reminderInterval);

        JLabel hint = new JLabel("After a reminder, you'll get another one after 15 more minutes of browsing.");
        hint.setForeground(Theme.MUTED);
        hint.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(8));
        card.add(hint);

        card.add(Box.createVerticalStrut(16));

        com.eduvibe.ui.components.RoundedButton save =
                new com.eduvibe.ui.components.RoundedButton("Save reminder");
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> saveEntertainmentLimit(save));
        card.add(save);

        return card;
    }

    private void saveEntertainmentLimit(com.eduvibe.ui.components.RoundedButton button) {

        final int interval = reminderInterval.getSelectedItem() == null
                ? 30 : (Integer) reminderInterval.getSelectedItem();
        final boolean enabled = reminderEnabled.isSelected();
        final int minutes = enabled ? interval : 0;
        button.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return new com.eduvibe.dao.ProfileDAO()
                        .updateEntertainmentLimit(currentUser.getId(), minutes);
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    get();
                    currentUser.setEntertainmentLimitMinutes(minutes);
                    try {
                        reminderPrefs().putInt("interval", interval);
                        reminderPrefs().flush();
                    } catch (Exception ignored) {
                        // remembered interval is a convenience only
                    }
                    com.eduvibe.ui.components.Dialogs.success(SettingsPanel.this,
                            enabled ? "Reminder saved" : "Reminders off",
                            enabled ? "You'll get a friendly reminder after " + interval
                                    + " minutes of active browsing."
                                    : "Entertainment reminders are turned off. The feed is unaffected.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException ex) {
                    ex.printStackTrace();
                    com.eduvibe.ui.components.Dialogs.error(SettingsPanel.this,
                            "Could not save reminder", String.valueOf(ex.getCause().getMessage()));
                }
            }
        }.execute();
    }

    // LOAD SETTINGS

    public void loadSettings() {

        String sql = """
            SELECT full_name, username, email, bio,
                   role, status, points
            FROM users
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, currentUser.getId());

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    String fullName = rs.getString("full_name");
                    String username = rs.getString("username");
                    String email = rs.getString("email");
                    String bio = rs.getString("bio");
                    String role = rs.getString("role");
                    String status = rs.getString("status");
                    int points = rs.getInt("points");

                    fullNameField.setText(safeText(fullName));
                    usernameField.setText(safeText(username));
                    emailField.setText(safeText(email));
                    bioArea.setText(bio == null ? "" : bio);

                    roleLabel.setText(safeText(role).toUpperCase());
                    statusLabel.setText(safeText(status).toUpperCase());
                    pointsLabel.setText(points + " points");

                    roleLabel.setForeground(BLUE);
                    statusLabel.setForeground(getStatusColor(status));

                    currentUser.setFullName(fullName);
                    currentUser.setUsername(username);
                    currentUser.setEmail(email);
                    currentUser.setBio(bio);
                    currentUser.setRole(role);
                    currentUser.setStatus(status);
                    currentUser.setPoints(points);

                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "User account could not be found.",
                            "Account Not Found",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load settings:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // SAVE PROFILE WITH ADMIN NOTIFICATION

    private void saveProfile() {

        String fullName = fullNameField.getText().trim();
        String bio = bioArea.getText().trim();

        if (fullName.isBlank()) {
            showWarning("Full name cannot be empty.");
            return;
        }

        if (fullName.length() > 100) {
            showWarning("Full name must not exceed 100 characters.");
            return;
        }

        String sql = """
            UPDATE users
            SET full_name = ?, bio = ?
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, bio);
            statement.setInt(3, currentUser.getId());

            int updated = statement.executeUpdate();

            if (updated > 0) {

                currentUser.setFullName(fullName);
                currentUser.setBio(bio);

                try {
                    AdminNotificationDAO.notifyAdmins(
                            connection,
                            "@" + currentUser.getUsername()
                            + " updated their profile information."
                    );
                } catch (SQLException notificationError) {
                    notificationError.printStackTrace();
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Your profile has been updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                showWarning("No account was updated.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Could not save profile:\n" + ex.getMessage());
        }
    }

    // CHANGE PASSWORD WITH ADMIN NOTIFICATION

    private void changePassword() {

        char[] currentPassword = currentPasswordField.getPassword();
        char[] newPassword = newPasswordField.getPassword();
        char[] confirmPassword = confirmPasswordField.getPassword();

        try {

            if (currentPassword.length == 0
                    || newPassword.length == 0
                    || confirmPassword.length == 0) {

                showWarning("Please complete all password fields.");
                return;
            }

            if (newPassword.length < 8) {
                showWarning(
                        "New password must contain at least 8 characters."
                );
                return;
            }

            if (!Arrays.equals(newPassword, confirmPassword)) {
                showWarning("New passwords do not match.");
                return;
            }

            String storedHash = getCurrentPasswordHash();

            if (storedHash == null || storedHash.isBlank()) {
                showError("Unable to verify your current password.");
                return;
            }

            if (!BCrypt.checkpw(new String(currentPassword), storedHash)) {
                showWarning("Your current password is incorrect.");
                return;
            }

            String newHash = BCrypt.hashpw(
                    new String(newPassword),
                    BCrypt.gensalt(12)
            );

            String sql = """
                UPDATE users
                SET password_hash = ?
                WHERE id = ?
                """;

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, newHash);
                statement.setInt(2, currentUser.getId());

                int updated = statement.executeUpdate();

                if (updated > 0) {

                    try {
                        AdminNotificationDAO.notifyAdmins(
                                connection,
                                "@" + currentUser.getUsername()
                                + " changed their account password."
                        );
                    } catch (SQLException notificationError) {
                        notificationError.printStackTrace();
                    }

                    JOptionPane.showMessageDialog(
                            this,
                            "Your password has been changed successfully!",
                            "Password Updated",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    currentPasswordField.setText("");
                    newPasswordField.setText("");
                    confirmPasswordField.setText("");

                } else {
                    showError("Password update failed.");
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Could not update password:\n" + ex.getMessage());

        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
            Arrays.fill(confirmPassword, '\0');
        }
    }

    private String getCurrentPasswordHash() throws SQLException {

        String sql = """
            SELECT password_hash
            FROM users
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, currentUser.getId());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        }

        return null;
    }

    // UI HELPERS

    /** Page body that always matches the scroll area's width, so cards never run off the right edge. */
    private static final class WidthFollowingPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(60, visible.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel createCard() {

        RoundedPanel card = new RoundedPanel(18, CARD, BORDER) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 22, 22));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private static Icons.Type iconForSection(String title) {
        String t = title.toLowerCase();
        if (t.contains("password")) {
            return Icons.Type.LOCK;
        }
        if (t.contains("status")) {
            return Icons.Type.SHIELD;
        }
        if (t.contains("account")) {
            return Icons.Type.MAIL;
        }
        if (t.contains("reminder")) {
            return Icons.Type.CLOCK;
        }
        if (t.contains("notification")) {
            return Icons.Type.BELL;
        }
        if (t.contains("appearance")) {
            return Icons.Type.SPARKLE;
        }
        return Icons.Type.USER;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private JComponent sectionTitle(String title, String subtitle) {

        JPanel p = new JPanel(new BorderLayout(14, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        final Icons.Type icon = iconForSection(title);
        JComponent bubble = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(Theme.brand(0, 0, getWidth(), getHeight(), 0));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                Icons.paint(g2, icon, (getWidth() - 20) / 2f, (getHeight() - 20) / 2f, 20, Color.WHITE);
                g2.dispose();
            }
        };
        bubble.setPreferredSize(new Dimension(40, 40));
        JPanel bubbleWrap = new JPanel(new BorderLayout());
        bubbleWrap.setOpaque(false);
        bubbleWrap.add(bubble, BorderLayout.NORTH);

        JLabel text = new JLabel(
                "<html><div style='width:215px'><b style='font-size:13px;color:" + hex(Theme.TEXT) + "'>"
                + title + "</b><br><span style='font-size:11px;color:" + hex(Theme.MUTED) + "'>"
                + subtitle + "</span></div></html>");
        text.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        text.setForeground(MUTED);

        p.add(bubbleWrap, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        return p;
    }

    private JLabel fieldLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JTextField createTextField() {

        JTextField field = new JTextField();
        field.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        field.setForeground(WHITE);
        field.setBackground(INPUT);
        field.setCaretColor(WHITE);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(11, 12, 11, 12)
        ));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        glowOnFocus(field);

        return field;
    }

    /** Border turns blue while the field is being edited. */
    private static void glowOnFocus(JTextComponent field) {
        final javax.swing.border.Border idle = field.getBorder();
        final javax.swing.border.Border active = BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BLUE_LIGHT, 2, true), new EmptyBorder(10, 11, 10, 11));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(active);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(idle);
            }
        });
    }

    private JPasswordField createPasswordField() {

        JPasswordField field = new JPasswordField();
        field.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        field.setForeground(WHITE);
        field.setBackground(INPUT);
        field.setCaretColor(WHITE);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(11, 12, 11, 12)
        ));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        glowOnFocus(field);

        return field;
    }

    private JButton createButton(String text) {
        RoundedButton button = new RoundedButton(text, RoundedButton.Style.PRIMARY);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private JLabel createValueLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 14));
        label.setForeground(WHITE);

        return label;
    }

    private JPanel statusTile(String title, JLabel value) {
        RoundedPanel t = new RoundedPanel(12, Theme.CARD_LIGHT, BORDER);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel cap = fieldLabel(title);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.add(cap);
        t.add(Box.createVerticalStrut(4));
        t.add(value);
        return t;
    }

    private JPanel buildInfoRow(String title, JLabel value) {

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(CARD);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(fieldLabel(title));
        row.add(Box.createVerticalStrut(6));
        row.add(value);

        return row;
    }

    private Color getStatusColor(String status) {

        if (status == null) {
            return MUTED;
        }

        return switch (status.toLowerCase()) {
            case "active" -> GREEN;
            case "suspended" -> Theme.YELLOW;
            case "banned" -> Theme.RED;
            default -> MUTED;
        };
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void showWarning(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Validation",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "EduVibe Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
