
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.FollowDAO;
import com.eduvibe.dao.PostDAO;
import com.eduvibe.dao.ProfileDAO;
import com.eduvibe.model.Post;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.ui.components.Anim;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfilePanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color CARD = Theme.CARD;
    private static final Color CARD_DARK = Theme.INPUT;
    private static final Color BLUE = Theme.BLUE;
    private static final Color BLUE_LIGHT = Theme.BLUE_LIGHT;
    private static final Color WHITE = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BORDER = Theme.BORDER;
    private static final Color GREEN = Theme.GREEN;

    private final int userId;
    private final FollowDAO followDAO = new FollowDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final PostDAO postDAO = new PostDAO();

    private JLabel postsCountLabel;
    private JLabel pointsCountLabel;
    private JPanel myPostsContainer;
    private String currentFullName = "";
    private String currentBio = "";

    private AvatarView avatarView;
    private JPanel headerCard;
    private BannerStrip bannerStrip;
    private final JPanel slotsRow = new JPanel(new GridLayout(1, 3, 12, 0));

    private JLabel nameLabel;
    private JLabel usernameLabel;
    private JLabel roleLabel;
    private JLabel statusLabel;

    private JTextArea bioArea;
    private final JPanel rewardsRow = new JPanel(new WrapFlow(FlowLayout.LEFT, 8, 8)) {
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    };

    /** FlowLayout that wraps onto new lines and reports the wrapped height (plain FlowLayout runs off the edge in a BoxLayout). */
    private static final class WrapFlow extends FlowLayout {
        WrapFlow(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return measure(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = measure(target, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension measure(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                Container c = target;
                while (c.getSize().width == 0 && c.getParent() != null) {
                    c = c.getParent();
                }
                targetWidth = c.getSize().width;
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }
                int hgap = getHgap(), vgap = getVgap();
                Insets in = target.getInsets();
                int padW = in.left + in.right + hgap * 2;
                int maxWidth = targetWidth - padW;
                Dimension dim = new Dimension(0, 0);
                int rowW = 0, rowH = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + d.width > maxWidth && rowW > 0) {
                        dim.width = Math.max(dim.width, rowW);
                        dim.height += (dim.height > 0 ? vgap : 0) + rowH;
                        rowW = 0;
                        rowH = 0;
                    }
                    if (rowW != 0) {
                        rowW += hgap;
                    }
                    rowW += d.width;
                    rowH = Math.max(rowH, d.height);
                }
                dim.width = Math.max(dim.width, rowW);
                dim.height += (dim.height > 0 ? vgap : 0) + rowH;
                dim.width += padW;
                dim.height += in.top + in.bottom + vgap * 2;
                return dim;
            }
        }
    }

    private JLabel emailValue;
    private JLabel accountIdValue;

    private JLabel followersCountLabel;
    private JLabel fullNameValue;
    private JLabel usernameValue;
    private JLabel levelLabel;
    private JLabel streakBadge;
    private BadgeChip badgeChip;
    private javax.swing.Timer shimmer;
    private JLabel followingCountLabel;

    private Runnable onProfileChanged = () -> { };

    /** Called after the name / bio or the photo was saved, so the sidebar can show the new details at once. */
    public void setOnProfileChanged(Runnable r) {
        this.onProfileChanged = r == null ? () -> { } : r;
    }

    public ProfilePanel(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        this.userId = userId;

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        buildUI();
        loadProfile();
    }

    // ==========================================
    // MAIN UI
    // ==========================================

    private void buildUI() {

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);

        content.add(buildProfileHeader());
        content.add(Box.createVerticalStrut(16));

        // ---- tabs: About | My Posts ----
        JPanel aboutTab = new JPanel();
        aboutTab.setLayout(new BoxLayout(aboutTab, BoxLayout.Y_AXIS));
        aboutTab.setOpaque(false);
        aboutTab.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Personal information and Activity sit side by side; on a narrow window they stack.
        JPanel infoBlock = block(buildSectionHeading("Personal Information", "Your registered account details."),
                buildAboutCard());
        JPanel activityBlock = block(buildSectionHeading("Activity", "Posts, comments and logins, last 12 weeks."),
                buildActivityCard());
        JPanel twoCol = new com.eduvibe.ui.components.TwoColumn(infoBlock, activityBlock, 20, 740);
        aboutTab.add(twoCol);
        aboutTab.add(Box.createVerticalStrut(22));
        aboutTab.add(buildSectionHeading("Profile Customization", "Rewards you have collected."));
        aboutTab.add(Box.createVerticalStrut(12));
        aboutTab.add(buildCustomizationCard());

        JPanel postsTab = new JPanel();
        postsTab.setLayout(new BoxLayout(postsTab, BoxLayout.Y_AXIS));
        postsTab.setOpaque(false);
        postsTab.setAlignmentX(Component.LEFT_ALIGNMENT);
        postsTab.setVisible(false);

        myPostsContainer = new JPanel();
        myPostsContainer.setLayout(new BoxLayout(myPostsContainer, BoxLayout.Y_AXIS));
        myPostsContainer.setOpaque(false);
        myPostsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        postsTab.add(myPostsContainer);

        com.eduvibe.ui.components.SegmentedTabs tabs = new com.eduvibe.ui.components.SegmentedTabs(
                new String[] {"About", "My Posts"}, 0, i -> {
                    aboutTab.setVisible(i == 0);
                    postsTab.setVisible(i == 1);
                    content.revalidate();
                    content.repaint();
                });
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setPreferredSize(new Dimension(320, 44));
        tabs.setMaximumSize(new Dimension(320, 44));

        content.add(tabs);
        content.add(Box.createVerticalStrut(16));
        content.add(aboutTab);
        content.add(postsTab);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        page.add(scrollPane, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    /** A heading with its card underneath, as one column. */
    private JPanel block(JPanel heading, JPanel card) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.add(heading);
        b.add(Box.createVerticalStrut(12));
        b.add(card);
        return b;
    }

    // ==========================================
    // PROFILE HEADER
    // ==========================================

    private JPanel buildProfileHeader() {

        JPanel card = new JPanel(new BorderLayout(0, 18)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        headerCard = card;
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(24, 26, 20, 26)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        bannerStrip = new BannerStrip();

        avatarView = new AvatarView();
        avatarView.setPreferredSize(new Dimension(104, 104));
        avatarView.setMinimumSize(new Dimension(104, 104));
        avatarView.setMaximumSize(new Dimension(104, 104));

        nameLabel = new JLabel("Loading profile...");
        nameLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 24));
        nameLabel.setForeground(WHITE);

        usernameLabel = new JLabel("@username");
        usernameLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 14));
        usernameLabel.setForeground(MUTED);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setBackground(CARD);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);

        roleLabel = createBadge("USER", BLUE);
        statusLabel = createBadge("ACTIVE", GREEN);
        levelLabel = createBadge("LEVEL 1", Theme.GOLD);

        badges.add(roleLabel);
        badges.add(statusLabel);
        badges.add(levelLabel);
        streakBadge = createBadge("0 DAY STREAK", new Color(251, 146, 60));
        badges.add(streakBadge);
        badgeChip = new BadgeChip();
        badges.add(badgeChip);

        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setBackground(CARD);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        identity.add(Box.createVerticalGlue());
        identity.add(nameLabel);
        identity.add(Box.createVerticalStrut(4));
        identity.add(usernameLabel);
        identity.add(Box.createVerticalStrut(12));
        identity.add(badges);
        identity.add(Box.createVerticalGlue());

        RoundedButton editButton = new RoundedButton("Edit Profile", RoundedButton.Style.SECONDARY);
        editButton.addActionListener(e -> showEditProfileDialog());

        RoundedButton photoButton = new RoundedButton("Change Photo", RoundedButton.Style.OUTLINE);
        photoButton.addActionListener(e -> changeProfilePhoto());

        editButton.setPreferredSize(new Dimension(150, 40));
        photoButton.setPreferredSize(new Dimension(150, 40));
        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 8));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(photoButton);
        JPanel actionsWrap = new JPanel(new GridBagLayout());
        actionsWrap.setOpaque(false);
        actionsWrap.add(actions);

        JPanel top = new JPanel(new BorderLayout(20, 0));
        top.setOpaque(false);
        top.add(avatarView, BorderLayout.WEST);
        top.add(identity, BorderLayout.CENTER);
        top.add(actionsWrap, BorderLayout.EAST);

        card.add(top, BorderLayout.CENTER);
        card.add(buildStatsRow(), BorderLayout.SOUTH);
        card.add(bannerStrip, BorderLayout.NORTH);
        return card;
    }

    /** Big number above a small caption, painted by hand so it always lines up. Clickable ones get a hover colour. */
    private static final class StatLabel extends JLabel {
        private final Color numberColor;
        private String number = "0";
        private String caption = "";
        private boolean hover;

        StatLabel(String initial, Color numberColor) {
            super("", SwingConstants.CENTER);
            this.numberColor = numberColor;
            setText(initial);
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
            });
        }

        @Override
        public void setText(String text) {
            if (numberColor == null || text == null) {
                super.setText(text);
                return;
            }
            String[] parts = text.split(" ", 2);
            number = parts[0];
            caption = parts.length > 1 ? parts[1] : "";
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 60);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (numberColor == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            if (hover && getCursor().getType() == Cursor.HAND_CURSOR) {
                g2.setColor(Theme.alpha(Theme.BLUE, 28));
                g2.fillRoundRect(4, 2, w - 8, h - 4, 14, 14);
            }
            g2.setFont(Theme.font(Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int nw = fm.stringWidth(number);
            int ny = h / 2 - 2;
            if (numberColor.equals(Theme.TEXT)) {
                g2.setPaint(Theme.brand((w - nw) / 2f, 0, (w + nw) / 2f + 1, 0, 0.1f));
            } else {
                g2.setColor(numberColor);
            }
            g2.drawString(number, (w - nw) / 2, ny);
            g2.setFont(Theme.font(Font.PLAIN, 12));
            fm = g2.getFontMetrics();
            g2.setColor(hover && getCursor().getType() == Cursor.HAND_CURSOR ? Theme.BLUE_LIGHT : Theme.MUTED);
            g2.drawString(caption, (w - fm.stringWidth(caption)) / 2, ny + 19);
            g2.dispose();
        }
    }

    private JPanel buildStatsRow() {

        JPanel row = new JPanel(new GridLayout(1, 4, 0, 0)) {
            @Override
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                g.setColor(Theme.alpha(BORDER, 170));
                for (int i = 1; i < 4; i++) {
                    int x = i * getWidth() / 4;
                    g.drawLine(x, 22, x, getHeight() - 6);
                }
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(12, 0, 0, 0)));

        postsCountLabel = new StatLabel("0 Posts", WHITE);
        followersCountLabel = new StatLabel("0 Followers", WHITE);
        followingCountLabel = new StatLabel("0 Following", WHITE);
        pointsCountLabel = new StatLabel("0 Points", Theme.GOLD);

        for (JLabel l : new JLabel[] {postsCountLabel, followersCountLabel, followingCountLabel, pointsCountLabel}) {
            l.setForeground(MUTED);
            l.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        }

        followersCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        followersCountLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showConnectionsDialog("followers");
            }
        });
        followingCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        followingCountLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showConnectionsDialog("following");
            }
        });

        row.add(postsCountLabel);
        row.add(followersCountLabel);
        row.add(followingCountLabel);
        row.add(pointsCountLabel);
        return row;
    }

    private JLabel createBadge(String text, Color color) {

        JLabel badge = new JLabel(text);
        badge.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 10));
        badge.setForeground(color);
        badge.setOpaque(true);
        badge.setBackground(new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                25
        ));
        badge.setBorder(new EmptyBorder(6, 11, 6, 11));

        return badge;
    }

    // ==========================================
    // SOCIAL COUNTS
    // ==========================================

    private void refreshSocialCounts() {

        followersCountLabel.setText(
                followDAO.getFollowersCount(userId) + " Followers"
        );

        followingCountLabel.setText(
                followDAO.getFollowingCount(userId) + " Following"
        );

        revalidate();
        repaint();
    }

    private void showConnectionsDialog(String type) {

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                type.equals("followers")
                        ? "Followers" : "Following",
                true
        );

        dialog.setSize(440, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(BACKGROUND);
        main.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel heading = new JLabel(
                type.equals("followers")
                        ? "People following you"
                        : "People you follow"
        );
        heading.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 17));
        heading.setForeground(WHITE);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(
                listPanel, BoxLayout.Y_AXIS
        ));
        listPanel.setBackground(BACKGROUND);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        main.add(heading, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);

        dialog.add(main, BorderLayout.CENTER);

        Runnable[] reload = new Runnable[1];

        reload[0] = () -> {
            listPanel.removeAll();

            List<ConnectionUser> users = getConnections(type);

            if (users.isEmpty()) {
                JLabel empty = new JLabel(
                        type.equals("followers")
                                ? "No followers yet."
                                : "Not following anyone yet."
                );
                empty.setForeground(MUTED);
                empty.setBorder(new EmptyBorder(20, 5, 20, 5));
                listPanel.add(empty);
            } else {
                for (ConnectionUser user : users) {
                    listPanel.add(buildConnectionRow(
                            user, reload[0]
                    ));
                    listPanel.add(Box.createVerticalStrut(10));
                }
            }

            listPanel.revalidate();
            listPanel.repaint();
        };

        reload[0].run();
        dialog.setVisible(true);
    }

    private List<ConnectionUser> getConnections(String type) {

        List<ConnectionUser> users = new ArrayList<>();

        String sql;

        if ("followers".equals(type)) {
            sql = """
                SELECT u.id, u.full_name, u.username
                FROM follows f
                JOIN users u ON u.id = f.follower_id
                WHERE f.following_id = ?
                  AND u.status = 'active'
                ORDER BY f.created_at DESC
                """;
        } else {
            sql = """
                SELECT u.id, u.full_name, u.username
                FROM follows f
                JOIN users u ON u.id = f.following_id
                WHERE f.follower_id = ?
                  AND u.status = 'active'
                ORDER BY f.created_at DESC
                """;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new ConnectionUser(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("username")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load connections:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        return users;
    }

    private JPanel buildConnectionRow(
            ConnectionUser user,
            Runnable reload
    ) {

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(CARD);
        row.setBorder(new EmptyBorder(12, 12, 12, 12));
        row.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 75
        ));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(
                details, BoxLayout.Y_AXIS
        ));

        JLabel name = new JLabel(
                safeText(user.fullName, "Unnamed User")
        );
        name.setForeground(WHITE);
        name.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 14));

        JLabel username = new JLabel(
                "@" + safeText(user.username, "username")
        );
        username.setForeground(MUTED);
        username.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));

        details.add(name);
        details.add(Box.createVerticalStrut(4));
        details.add(username);

        JButton action = new JButton(
                followDAO.isFollowing(userId, user.id)
                        ? "Following" : "Follow"
        );

        action.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        action.setFocusPainted(false);
        action.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));

        updateConnectionButton(action);

        action.addActionListener(e -> {

            action.setEnabled(false);

            boolean following = followDAO.isFollowing(
                    userId, user.id
            );

            boolean success = following
                    ? followDAO.unfollowUser(userId, user.id)
                    : followDAO.followUser(userId, user.id);

            if (success) {
                refreshSocialCounts();
                reload.run();
            } else {
                action.setEnabled(true);

                JOptionPane.showMessageDialog(
                        this,
                        "Could not update follow status.",
                        "Follow Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        row.add(details, BorderLayout.CENTER);
        row.add(action, BorderLayout.EAST);

        return row;
    }

    private void updateConnectionButton(JButton button) {

        if ("Following".equals(button.getText())) {
            button.setForeground(WHITE);
            button.setBackground(CARD_DARK);
        } else {
            button.setForeground(Color.WHITE);
            button.setBackground(BLUE);
        }
    }

    private static class ConnectionUser {
        int id;
        String fullName;
        String username;

        ConnectionUser(int id, String fullName, String username) {
            this.id = id;
            this.fullName = fullName;
            this.username = username;
        }
    }

    // ==========================================
    // ABOUT ME
    // ==========================================

    private JPanel buildAboutCard() {

        JPanel card = createCard();

        fullNameValue = createValueLabel("Loading...");
        emailValue = createValueLabel("Loading...");
        usernameValue = createValueLabel("Loading...");
        accountIdValue = createValueLabel("Loading...");

        bioArea = new JTextArea("Loading bio...");
        bioArea.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 14));
        bioArea.setForeground(MUTED);
        bioArea.setBackground(CARD);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setEditable(false);
        bioArea.setFocusable(false);
        bioArea.setOpaque(false);
        bioArea.setBorder(null);

        card.add(infoLine(com.eduvibe.ui.components.Icons.Type.USER, "Full name", fullNameValue, false));
        card.add(createDivider());
        card.add(infoLine(com.eduvibe.ui.components.Icons.Type.MAIL, "Email", emailValue, false));
        card.add(createDivider());
        card.add(infoLine(com.eduvibe.ui.components.Icons.Type.AT, "Username", usernameValue, false));
        card.add(createDivider());
        card.add(infoLine(com.eduvibe.ui.components.Icons.Type.KEY, "Account ID", accountIdValue, false));
        card.add(createDivider());
        card.add(infoLine(com.eduvibe.ui.components.Icons.Type.EDIT, "Bio", bioArea, true));

        return card;
    }

    /** One aligned row: icon + caption on the left, value on the right, both on the same centre line. */
    private JPanel infoLine(com.eduvibe.ui.components.Icons.Type icon, String caption, JComponent value, boolean top) {

        JPanel line = new JPanel(new BorderLayout(12, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel label = new JLabel(caption);
        label.setIcon(com.eduvibe.ui.components.Icons.icon(icon, 16, Theme.BLUE_LIGHT));
        label.setIconTextGap(10);
        label.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        label.setForeground(MUTED);
        label.setPreferredSize(new Dimension(130, 24));
        label.setVerticalAlignment(top ? SwingConstants.TOP : SwingConstants.CENTER);
        label.setBorder(new EmptyBorder(top ? 1 : 0, 0, 0, 0));

        if (value instanceof JLabel vl) {
            vl.setVerticalAlignment(SwingConstants.CENTER);
            vl.setPreferredSize(new Dimension(10, 24));
        }

        line.add(label, BorderLayout.WEST);
        line.add(value, BorderLayout.CENTER);
        return line;
    }

    private final com.eduvibe.ui.components.ActivityHeatmap heatmap =
            new com.eduvibe.ui.components.ActivityHeatmap();
    private final JLabel activitySummary = new JLabel(" ");
    private final JLabel actTotal = new JLabel("0");
    private final JLabel actDays = new JLabel("0");
    private final JLabel actBest = new JLabel("0");

    private JPanel statPill(JLabel value, String caption) {
        RoundedPanel pill = new RoundedPanel(12, Theme.CARD_LIGHT, BORDER);
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setBorder(new EmptyBorder(8, 12, 8, 12));
        value.setFont(Theme.font(Font.BOLD, 18));
        value.setForeground(WHITE);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cap = new JLabel(caption);
        cap.setFont(Theme.font(Font.PLAIN, 11));
        cap.setForeground(MUTED);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        pill.add(value);
        pill.add(cap);
        return pill;
    }

    private JPanel buildActivityCard() {

        JPanel card = createCard();

        heatmap.setAlignmentX(Component.LEFT_ALIGNMENT);
        activitySummary.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        activitySummary.setForeground(MUTED);
        activitySummary.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pills = new JPanel(new GridLayout(1, 3, 10, 0));
        pills.setOpaque(false);
        pills.setAlignmentX(Component.LEFT_ALIGNMENT);
        pills.add(statPill(actTotal, "Activities"));
        pills.add(statPill(actDays, "Active days"));
        pills.add(statPill(actBest, "Best day"));
        pills.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        card.add(heatmap);
        card.add(Box.createVerticalStrut(10));
        card.add(pills);
        return card;
    }

    private void loadStreakBadge() {
        new SwingWorker<com.eduvibe.dao.AchievementDAO.StreakInfo, Void>() {
            @Override
            protected com.eduvibe.dao.AchievementDAO.StreakInfo doInBackground() throws Exception {
                return new com.eduvibe.dao.AchievementDAO().loadStreak(userId);
            }

            @Override
            protected void done() {
                try {
                    int n = get().current;
                    streakBadge.setText(n + (n == 1 ? " DAY STREAK" : " DAY STREAK"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void loadActivity() {
        loadStreakBadge();
        new SwingWorker<java.util.Map<java.time.LocalDate, Integer>, Void>() {
            @Override
            protected java.util.Map<java.time.LocalDate, Integer> doInBackground() throws Exception {
                return new com.eduvibe.dao.ActivityDAO().countsSince(
                        userId, com.eduvibe.ui.components.ActivityHeatmap.startDate());
            }

            @Override
            protected void done() {
                try {
                    java.util.Map<java.time.LocalDate, Integer> m = get();
                    heatmap.setCounts(m);
                    int total = m.values().stream().mapToInt(Integer::intValue).sum();
                    int days = m.size();
                    actTotal.setText(String.valueOf(total));
                    actDays.setText(String.valueOf(days));
                    actBest.setText(String.valueOf(m.values().stream().mapToInt(Integer::intValue).max().orElse(0)));
                    activitySummary.setText(total + " activities on " + days + " active day"
                            + (days == 1 ? "" : "s") + " in the last 12 weeks");
                } catch (Exception ex) {
                    ex.printStackTrace();   // not critical for the profile
                }
            }
        }.execute();
    }

    private JPanel buildCustomizationCard() {

        JPanel card = createCard();

        JLabel caption = new JLabel("Equip a frame, banner or theme from Points & Rewards \u2192 My Rewards. Earn more in Playtime Rewards.");
        caption.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        caption.setForeground(MUTED);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        rewardsRow.setOpaque(false);
        rewardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rewardsRow.addComponentListener(new java.awt.event.ComponentAdapter() {
            private int lastW = -1;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (rewardsRow.getWidth() != lastW) {
                    lastW = rewardsRow.getWidth();
                    rewardsRow.revalidate();
                }
            }
        });

        slotsRow.setOpaque(false);
        slotsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        slotsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel ownedTitle = new JLabel("Rewards collected");
        ownedTitle.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        ownedTitle.setForeground(MUTED);
        ownedTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(caption);
        card.add(Box.createVerticalStrut(12));
        card.add(slotsRow);
        card.add(Box.createVerticalStrut(14));
        card.add(ownedTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(rewardsRow);
        return card;
    }

    // ==========================================
    // ACCOUNT INFORMATION
    // ==========================================

    private JPanel buildAccountCard() {

        JPanel card = createCard();

        emailValue = createValueLabel("Loading...");
        accountIdValue = createValueLabel("Loading...");

        card.add(buildInfoRow(
                "Email Address",
                emailValue,
                "Registered email used for your account."
        ));

        card.add(createDivider());

        card.add(buildInfoRow(
                "Account ID",
                accountIdValue,
                "Your unique EduVibe user identification."
        ));

        return card;
    }

    private JPanel buildInfoRow(
            String title,
            JLabel value,
            String description
    ) {

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(CARD);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        titleLabel.setForeground(MUTED);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        descriptionLabel.setForeground(MUTED);

        row.add(titleLabel);
        row.add(Box.createVerticalStrut(7));
        row.add(value);
        row.add(Box.createVerticalStrut(4));
        row.add(descriptionLabel);

        return row;
    }

    // ==========================================
    // LOAD PROFILE FROM MYSQL
    // ==========================================

    /** Fills the "Rewards collected" chips from user_rewards. */
    private void loadOwnedRewards() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                java.util.List<com.eduvibe.dao.RewardDAO.OwnedReward> owned =
                        new com.eduvibe.dao.RewardDAO().loadOwned(userId);
                java.util.Map<String, Integer> eq;
                try {
                    eq = new com.eduvibe.dao.EquipDAO().equipped(userId);
                } catch (Exception ex) {
                    eq = new java.util.HashMap<>();
                }
                return new Object[] { owned, eq };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    rewardsRow.removeAll();
                    Object[] data = get();
                    java.util.List<com.eduvibe.dao.RewardDAO.OwnedReward> list =
                            (java.util.List<com.eduvibe.dao.RewardDAO.OwnedReward>) data[0];
                    applyEquipped(list, (java.util.Map<String, Integer>) data[1]);
                    if (list.isEmpty()) {
                        JLabel none = new JLabel("No rewards yet. Earn points and get some!");
                        none.setForeground(MUTED);
                        none.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
                        rewardsRow.add(none);
                    }
                    for (com.eduvibe.dao.RewardDAO.OwnedReward o : list) {
                        JLabel chip = new JLabel(o.name == null ? "Reward" : o.name);
                        chip.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
                        chip.setForeground(WHITE);
                        chip.setOpaque(true);
                        Color rc = com.eduvibe.util.RewardStyle.primary(o.name, o.rarity);
                        chip.setBackground(Anim_mix(com.eduvibe.ui.Theme.CARD_LIGHT, rc, 0.14f));
                        chip.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(Theme.alpha(rc, 170), 1, true), new EmptyBorder(5, 11, 5, 11)));
                        chip.setToolTipText(o.rarity == null ? null : o.rarity);
                        rewardsRow.add(chip);
                    }
                    rewardsRow.revalidate();
                    rewardsRow.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();   // not critical for the profile
                }
            }
        }.execute();
    }

    public void loadProfile() {

        loadOwnedRewards();
        loadActivity();

        String sql = """
            SELECT id, full_name, username, email,
                   bio, profile_pic, role, status
            FROM users
            WHERE id = ?
            """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    String fullName = rs.getString("full_name");
                    String username = rs.getString("username");
                    String email = rs.getString("email");
                    String bio = rs.getString("bio");
                    String profilePic = rs.getString("profile_pic");
                    String role = rs.getString("role");
                    String status = rs.getString("status");

                    nameLabel.setText(
                            safeText(fullName, "Unnamed User")
                    );

                    usernameLabel.setText(
                            "@" + safeText(username, "username")
                    );
                    fullNameValue.setText(safeText(fullName, "Unnamed User"));
                    usernameValue.setText(safeText(username, "username"));

                    roleLabel.setText(
                            safeText(role, "user").toUpperCase()
                    );

                    statusLabel.setText(
                            safeText(status, "unknown").toUpperCase()
                    );

                    roleLabel.setForeground(getRoleColor(role));
                    statusLabel.setForeground(getStatusColor(status));

                    emailValue.setText(
                            safeText(email, "No email available")
                    );

                    accountIdValue.setText("#" + rs.getLong("id"));

                    bioArea.setText(
                            bio == null || bio.isBlank()
                                    ? "No bio added yet."
                                    : bio.trim()
                    );

                    bioArea.setForeground(
                            bio == null || bio.isBlank()
                                    ? MUTED : WHITE
                    );

                    currentFullName = fullName == null ? "" : fullName;
                    currentBio = bio == null ? "" : bio;

                    loadProfilePicture(profilePic, fullName);

                } else {
                    showUnavailableProfile();
                }
            }

        } catch (SQLException ex) {

            nameLabel.setText("Unable to load profile");
            usernameLabel.setText("");
            bioArea.setText(
                    "There was a problem retrieving your profile."
            );
            emailValue.setText("Unavailable");
            accountIdValue.setText("Unavailable");

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not load profile:\n" + ex.getMessage(),
                    "EduVibe Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        refreshSocialCounts();
        loadCountersAndPosts();
        revalidate();
        repaint();
    }

    // ==========================================
    // COUNTERS + MY POSTS (background thread)
    // ==========================================

    /** Fills the My Posts tab: a heading, then the posts one under another, or a friendly empty state. */
    private void showMyPosts(List<Post> posts) {

        myPostsContainer.add(buildSectionHeading("My Posts",
                posts.isEmpty() ? "Nothing shared yet." : "Everything you have shared, newest first."));
        myPostsContainer.add(Box.createVerticalStrut(12));

        if (posts.isEmpty()) {
            RoundedPanel empty = new RoundedPanel(16, Theme.CARD, Theme.BORDER);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setBorder(new EmptyBorder(28, 24, 28, 24));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel title = new JLabel("You haven't posted anything yet");
            title.setFont(Theme.bold(15));
            title.setForeground(Theme.TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel hint = new JLabel("Press Create Post at the top to share your first post and earn points.");
            hint.setFont(Theme.regular(13));
            hint.setForeground(MUTED);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);

            empty.add(title);
            empty.add(Box.createVerticalStrut(6));
            empty.add(hint);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, empty.getPreferredSize().height));
            myPostsContainer.add(empty);
            return;
        }

        for (Post post : posts) {
            // never taller than its own content, so a short list does not stretch the cards
            JPanel wrapper = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                }
            };
            wrapper.setOpaque(false);
            wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
            wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(new PostCard(post, userId), BorderLayout.CENTER);
            myPostsContainer.add(wrapper);
        }
    }

    private void loadCountersAndPosts() {

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {
                    profileDAO.loadCounters(userId),
                    postDAO.findByUser(userId, userId)
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                myPostsContainer.removeAll();
                try {
                    Object[] data = get();
                    ProfileDAO.Counters counters = (ProfileDAO.Counters) data[0];
                    List<Post> posts = (List<Post>) data[1];

                    postsCountLabel.setText(counters.posts + " Posts");
                    pointsCountLabel.setText(String.format("%,d Points", counters.points));
                    levelLabel.setText("LEVEL " + (counters.points / 100 + 1));

                    showMyPosts(posts);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    JLabel error = new JLabel("Unable to load your posts.");
                    error.setForeground(Theme.RED);
                    myPostsContainer.add(error);
                }

                myPostsContainer.revalidate();
                myPostsContainer.repaint();
            }
        }.execute();
    }

    // ==========================================
    // EDIT PROFILE + CHANGE PHOTO
    // ==========================================

    private void showEditProfileDialog() {

        JTextField nameField = new JTextField(currentFullName, 28);
        nameField.setBackground(Theme.INPUT);
        nameField.setForeground(WHITE);
        nameField.setCaretColor(WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true), new EmptyBorder(9, 10, 9, 10)));

        JTextArea bioField = new JTextArea(currentBio, 5, 28);
        bioField.setLineWrap(true);
        bioField.setWrapStyleWord(true);
        bioField.setBackground(Theme.INPUT);
        bioField.setForeground(WHITE);
        bioField.setCaretColor(WHITE);
        bioField.setBorder(new EmptyBorder(9, 10, 9, 10));

        JScrollPane bioScroll = new JScrollPane(bioField);
        bioScroll.setBorder(new LineBorder(BORDER, 1, true));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JLabel l1 = new JLabel("Full name");
        l1.setForeground(MUTED);
        JLabel l2 = new JLabel("Bio (max 500 characters)");
        l2.setForeground(MUTED);

        form.add(l1);
        form.add(Box.createVerticalStrut(5));
        form.add(nameField);
        form.add(Box.createVerticalStrut(12));
        form.add(l2);
        form.add(Box.createVerticalStrut(5));
        form.add(bioScroll);

        int choice = JOptionPane.showConfirmDialog(this, form, "Edit Profile",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        final String newName = nameField.getText().trim();
        final String newBio = bioField.getText().trim();

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return profileDAO.updateProfile(userId, newName, newBio);
            }

            @Override
            protected void done() {
                try {
                    get();
                    Dialogs.success(ProfilePanel.this, "Profile updated", "Your profile was saved.");
                    loadProfile();
                    onProfileChanged.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof IllegalArgumentException) {
                        Dialogs.warning(ProfilePanel.this, "Check your details", cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        Dialogs.error(ProfilePanel.this, "Could not save profile",
                                String.valueOf(cause.getMessage()));
                    }
                }
            }
        }.execute();
    }

    private void changeProfilePhoto() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a profile picture");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final File source = chooser.getSelectedFile();

        if (source == null || !source.isFile()
                || new ImageIcon(source.getAbsolutePath()).getIconWidth() <= 0) {
            Dialogs.error(this, "Invalid image", "Please choose a valid image file.");
            return;
        }

        if (source.length() > 5L * 1024 * 1024) {
            Dialogs.warning(this, "Image too large", "Please choose an image under 5 MB.");
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {

                String name = source.getName();
                int dot = name.lastIndexOf('.');
                String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "png";

                Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
                Files.createDirectories(dir);

                String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
                Path target = dir.resolve(fileName);
                Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

                String relative = "uploads/avatars/" + fileName;

                try {
                    profileDAO.updateProfilePic(userId, relative);
                } catch (Exception ex) {
                    Files.deleteIfExists(target);
                    throw ex;
                }
                return relative;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Dialogs.success(ProfilePanel.this, "Photo updated",
                            "Your profile photo was changed.");
                    loadProfile();
                    onProfileChanged.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    Dialogs.error(ProfilePanel.this, "Could not change photo",
                            String.valueOf(ex.getCause().getMessage()));
                }
            }
        }.execute();
    }

    private void showUnavailableProfile() {

        nameLabel.setText("User not found");
        usernameLabel.setText("");
        roleLabel.setText("UNKNOWN");
        statusLabel.setText("UNKNOWN");

        emailValue.setText("Unavailable");
        accountIdValue.setText("Unavailable");
        bioArea.setText("This profile could not be found.");

        avatarView.setInitials("?");
        avatarView.setProfileImage(null);
    }

    // ==========================================
    // PROFILE PICTURE
    // ==========================================

    private void loadProfilePicture(
            String path,
            String fullName
    ) {

        avatarView.setInitials(getInitials(fullName));
        avatarView.setProfileImage(null);

        if (path == null || path.isBlank()) {
            return;
        }

        File imageFile = new File(path);

        if (!imageFile.isAbsolute() && !imageFile.exists()) {
            imageFile = new File(
                    System.getProperty("user.dir"),
                    path
            );
        }

        if (!imageFile.exists() || !imageFile.isFile()) {
            return;
        }

        ImageIcon original = new ImageIcon(
                imageFile.getAbsolutePath()
        );

        if (original.getIconWidth() <= 0) {
            return;
        }

        avatarView.setProfileImage(original.getImage());
    }

    private String getInitials(String fullName) {

        if (fullName == null || fullName.isBlank()) {
            return "EV";
        }

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(
                    0,
                    Math.min(2, parts[0].length())
            ).toUpperCase();
        }

        return (
                parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1)
        ).toUpperCase();
    }

    // ==========================================
    // EQUIPPED REWARDS (frame / banner / theme)
    // ==========================================

    private void applyEquipped(java.util.List<com.eduvibe.dao.RewardDAO.OwnedReward> owned,
                               java.util.Map<String, Integer> eq) {

        com.eduvibe.dao.RewardDAO.OwnedReward frame = find(owned, eq, com.eduvibe.dao.EquipDAO.FRAME);
        com.eduvibe.dao.RewardDAO.OwnedReward banner = find(owned, eq, com.eduvibe.dao.EquipDAO.BANNER);
        com.eduvibe.dao.RewardDAO.OwnedReward theme = find(owned, eq, com.eduvibe.dao.EquipDAO.THEME);
        com.eduvibe.dao.RewardDAO.OwnedReward badge = find(owned, eq, com.eduvibe.dao.EquipDAO.BADGE);

        avatarView.setFrame(frame);
        badgeChip.setReward(badge);
        bannerStrip.setReward(banner);

        Color base = CARD;
        if (theme != null) {
            Color t = com.eduvibe.util.RewardStyle.primary(theme.name, theme.rarity);
            base = Anim_mix(CARD, t, 0.16f);
        }
        headerCard.setBackground(base);
        for (Component c : headerCard.getComponents()) {
            tint(c, base);
        }
        headerCard.repaint();

        boolean anyFancy = false;
        for (com.eduvibe.dao.RewardDAO.OwnedReward r : new com.eduvibe.dao.RewardDAO.OwnedReward[]{frame, banner, theme, badge}) {
            anyFancy |= r != null;
        }
        if (anyFancy && shimmer == null) {
            shimmer = new javax.swing.Timer(50, e -> {
                if (isShowing()) {
                    avatarView.repaint();
                    bannerStrip.repaint();
                    badgeChip.repaint();
                }
            });
            shimmer.start();
        } else if (!anyFancy && shimmer != null) {
            shimmer.stop();
            shimmer = null;
        }

        slotsRow.removeAll();
        slotsRow.add(slotTile("Avatar frame", frame));
        slotsRow.add(slotTile("Banner", banner));
        slotsRow.add(slotTile("Theme", theme));
        slotsRow.add(slotTile("Badge", badge));
        slotsRow.setLayout(new GridLayout(1, 4, 12, 0));
        slotsRow.revalidate();
        slotsRow.repaint();
    }

    private static com.eduvibe.dao.RewardDAO.OwnedReward find(
            java.util.List<com.eduvibe.dao.RewardDAO.OwnedReward> owned,
            java.util.Map<String, Integer> eq, String slot) {
        Integer id = eq.get(slot);
        if (id == null) {
            return null;
        }
        for (com.eduvibe.dao.RewardDAO.OwnedReward o : owned) {
            if (o.rewardId == id) {
                return o;
            }
        }
        return null;
    }

    private static Color Anim_mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Re-colours the plain (non-card) panels inside the header so the theme tint is seamless. */
    private void tint(Component c, Color bg) {
        if (c instanceof JPanel p && c != bannerStrip && p.isOpaque()) {
            p.setBackground(bg);
        }
        if (c instanceof Container ct) {
            for (Component child : ct.getComponents()) {
                if (child instanceof JPanel jp && jp.isOpaque()
                        && jp.getBackground().equals(CARD)) {
                    jp.setBackground(bg);
                }
                tint(child, bg);
            }
        }
    }

    private JPanel slotTile(String title, com.eduvibe.dao.RewardDAO.OwnedReward r) {

        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(com.eduvibe.ui.Theme.CARD_LIGHT);
        Color edge = r == null ? BORDER : com.eduvibe.util.RewardStyle.primary(r.name, r.rarity);
        tile.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(edge, 1, true), new EmptyBorder(8, 12, 8, 12)));

        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 10));
        t.setForeground(MUTED);

        JLabel v = new JLabel(r == null ? "Not equipped" : r.name);
        v.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
        v.setForeground(r == null ? MUTED : WHITE);

        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(t);
        tile.add(Box.createVerticalStrut(3));
        tile.add(v);
        return tile;
    }

    /** Equipped badge shown next to the level / streak badges; invisible when none is equipped. */
    private static final class BadgeChip extends JComponent {

        private com.eduvibe.dao.RewardDAO.OwnedReward reward;

        BadgeChip() {
            setVisible(false);
        }

        void setReward(com.eduvibe.dao.RewardDAO.OwnedReward r) {
            this.reward = r;
            setVisible(r != null);
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (reward == null) {
                return new Dimension(0, 28);
            }
            FontMetrics fm = getFontMetrics(com.eduvibe.ui.Theme.font(Font.BOLD, 11));
            return new Dimension(fm.stringWidth(reward.name == null ? "" : reward.name) + 44, 28);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (reward == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = com.eduvibe.util.RewardStyle.primary(reward.name, reward.rarity);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 160));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            com.eduvibe.ui.components.RewardArt.paintBadge(g2, 17, getHeight() / 2.0, 20,
                    reward.name, reward.rarity);
            g2.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString(reward.name == null ? "" : reward.name, 32, getHeight() / 2 + 4);
            g2.dispose();
        }
    }

    /** Thin gradient strip across the top of the header; hidden until a banner is equipped. */
    private static final class BannerStrip extends JComponent {

        private com.eduvibe.dao.RewardDAO.OwnedReward reward;

        BannerStrip() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 84));
        }

        void setReward(com.eduvibe.dao.RewardDAO.OwnedReward r) {
            this.reward = r;
            setPreferredSize(new Dimension(10, 84));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (reward == null) {
                // no banner equipped: a soft brand cover instead of an empty gap
                Graphics2D c2 = (Graphics2D) g.create();
                Theme.antialias(c2);
                int w = getWidth(), h = getHeight();
                java.awt.geom.RoundRectangle2D shape = new java.awt.geom.RoundRectangle2D.Float(0, 0, w - 1, h - 1, 16, 16);
                c2.setPaint(Theme.brand(0, 0, w, h, 0));
                c2.fill(shape);
                c2.setClip(shape);
                c2.setColor(new Color(255, 255, 255, 30));
                c2.fillOval(w - 170, -90, 260, 260);
                c2.setColor(new Color(255, 255, 255, 20));
                c2.fillOval(w - 330, 10, 170, 170);
                c2.setColor(new Color(255, 255, 255, 190));
                c2.setFont(Theme.font(Font.BOLD, 12));
                c2.drawString("Equip a banner in Customize to make this yours", 16, h - 14);
                c2.dispose();
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            com.eduvibe.ui.components.RewardArt.paintBanner(g2, 0, 0, getWidth(), getHeight(),
                    reward.name, reward.rarity, 16);
            g2.setColor(new Color(255, 255, 255, 230));
            g2.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 12));
            g2.drawString(reward.name == null ? "" : reward.name, 16, getHeight() - 14);
            g2.dispose();
        }
    }

    // ==========================================
    // AVATAR COMPONENT
    // ==========================================

    private class AvatarView extends JPanel {

        private String initials = "EV";
        private Image profileImage;
        private com.eduvibe.dao.RewardDAO.OwnedReward frame;

        void setFrame(com.eduvibe.dao.RewardDAO.OwnedReward f) {
            this.frame = f;
            repaint();
        }

        AvatarView() {
            setOpaque(false);
            setPreferredSize(new Dimension(140, 140));
        }

        void setInitials(String initials) {
            this.initials = initials;
            repaint();
        }

        void setProfileImage(Image image) {
            this.profileImage = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            int diameter = Math.min(getWidth(), getHeight()) - 24;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            if (frame != null) {
                // same artwork as the Customize page, so the profile matches the preview exactly
                com.eduvibe.ui.components.RewardArt.paintFrame(g2, x + diameter / 2.0, y + diameter / 2.0,
                        diameter, frame.name, frame.rarity);
                g2.setColor(CARD);
                g2.fillOval(x - 0, y - 0, diameter, diameter);
            } else {
                g2.setColor(new Color(59, 130, 246, 65));
                g2.fillOval(x - 4, y - 4, diameter + 8, diameter + 8);
            }

            g2.setColor(BLUE);
            g2.fillOval(x, y, diameter, diameter);

            if (profileImage != null) {

                Shape oldClip = g2.getClip();

                g2.setClip(new Ellipse2D.Float(
                        x, y, diameter, diameter
                ));

                int imageWidth = profileImage.getWidth(this);
                int imageHeight = profileImage.getHeight(this);

                if (imageWidth > 0 && imageHeight > 0) {

                    double scale = Math.max(
                            (double) diameter / imageWidth,
                            (double) diameter / imageHeight
                    );

                    int drawWidth = (int) (imageWidth * scale);
                    int drawHeight = (int) (imageHeight * scale);

                    int drawX = x + (diameter - drawWidth) / 2;
                    int drawY = y + (diameter - drawHeight) / 2;

                    g2.drawImage(
                            profileImage,
                            drawX,
                            drawY,
                            drawWidth,
                            drawHeight,
                            this
                    );
                }

                g2.setClip(oldClip);

            } else {

                g2.setColor(WHITE);
                g2.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 30));

                FontMetrics fm = g2.getFontMetrics();

                int textX = x + (diameter - fm.stringWidth(initials)) / 2;
                int textY = y + (diameter - fm.getHeight()) / 2
                        + fm.getAscent();

                g2.drawString(initials, textX, textY);
            }

            g2.dispose();
        }
    }

    // ==========================================
    // UI HELPERS
    // ==========================================

    private JPanel createCard() {

        RoundedPanel card = new RoundedPanel(16, CARD, BORDER) {
            @Override
            public Dimension getMaximumSize() {
                // never taller than its content, so no empty space appears inside or below it
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 22, 18, 22));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel buildSectionHeading(
            String title,
            String subtitle
    ) {

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(Theme.brand(0, 0, 0, 38, 0));
                g2.fillRoundRect(0, 2, 5, 38, 5, 5);
                g2.dispose();
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(0, 16, 0, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 19));
        titleLabel.setForeground(WHITE);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        subtitleLabel.setForeground(MUTED);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(subtitleLabel);

        return panel;
    }

    private JSeparator createDivider() {

        JSeparator separator = new JSeparator();
        separator.setForeground(Theme.alpha(BORDER, 150));
        separator.setBackground(new Color(0, 0, 0, 0));
        separator.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 1
        ));

        return separator;
    }

    private JLabel createValueLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 14));
        label.setForeground(WHITE);

        return label;
    }

    private Color getRoleColor(String role) {

        if (role == null) {
            return BLUE;
        }

        return role.equalsIgnoreCase("admin")
                ? new Color(192, 132, 252)
                : BLUE;
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

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value;
    }
}
