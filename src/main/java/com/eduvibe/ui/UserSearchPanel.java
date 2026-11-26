
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.FollowDAO;
import com.eduvibe.ui.components.Avatar;
import com.eduvibe.ui.components.FieldBox;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.PageHero;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserSearchPanel extends JPanel {

    private static final Color BACKGROUND = Theme.BACKGROUND;
    private static final Color CARD = Theme.CARD;
    private static final Color HOVER = Theme.BORDER;
    private static final Color TEXT = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BLUE = Theme.BLUE;
    private static final Color BLUE_LIGHT = Theme.BLUE_LIGHT;
    private static final Color BORDER = Theme.BORDER;

    private final int currentUserId;
    private final FollowDAO followDAO = new FollowDAO();

    private JTextField searchField;
    private JPanel resultsPanel;
    private JLabel statusLabel;
    private JButton searchButton;
    private PageHero hero;

    public UserSearchPanel(int currentUserId) {
        if (currentUserId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        this.currentUserId = currentUserId;

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(8, 0, 25, 0));

        buildInterface();
        searchUsers("");
    }

    private void buildInterface() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        hero = new PageHero(Icons.Type.USERS, "Find People",
                "Discover people and connect with the EduVibe community.",
                PageHero.PEOPLE[0], PageHero.PEOPLE[1]);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(hero);
        header.add(Box.createVerticalStrut(16));

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);

        FieldBox box = FieldBox.text(Icons.Type.SEARCH, "Search by name or username...");
        searchField = (JTextField) box.getField();
        searchField.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 15));

        RoundedButton go = new RoundedButton("Search", RoundedButton.Style.PRIMARY).withIcon(Icons.Type.SEARCH);
        go.setPreferredSize(new Dimension(130, 44));
        searchButton = go;
        searchButton.addActionListener(e -> searchUsers(searchField.getText().trim()));
        searchField.addActionListener(e -> searchUsers(searchField.getText().trim()));

        searchPanel.add(box, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        header.add(searchPanel);
        add(header, BorderLayout.NORTH);

        // people cards: two columns on a wide window, one on a narrow one
        resultsPanel = new JPanel(new GridLayout(0, 2, 14, 14));
        resultsPanel.setOpaque(false);
        resultsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int cols = resultsPanel.getWidth() >= 640 ? 2 : 1;
                if (((GridLayout) resultsPanel.getLayout()).getColumns() != cols) {
                    resultsPanel.setLayout(new GridLayout(0, cols, 14, 14));
                    resultsPanel.revalidate();
                }
            }
        });
        JPanel holder = new WidthFollowing(new BorderLayout());
        holder.setBackground(BACKGROUND);
        holder.add(resultsPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("Loading people...");
        statusLabel.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 13));
        statusLabel.setForeground(MUTED);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(statusLabel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        content.add(scroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    /** Scroll content that always takes the viewport's width, so the cards never run off the right edge. */
    private static final class WidthFollowing extends JPanel implements Scrollable {
        WidthFollowing(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(60, r.height - 40);
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

    private void searchUsers(String keyword) {
        searchButton.setEnabled(false);
        statusLabel.setText("Searching...");

        resultsPanel.removeAll();
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingWorker<List<UserResult>, Void> worker =
            new SwingWorker<>() {

            @Override
            protected List<UserResult> doInBackground()
                    throws Exception {
                return findUsers(keyword);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);

                try {
                    List<UserResult> users = get();

                    resultsPanel.removeAll();
                    hero.setChip(users.size() + (users.size() == 1 ? " person" : " people"), Icons.Type.USERS);

                    if (users.isEmpty()) {
                        statusLabel.setText(" ");
                        resultsPanel.add(new com.eduvibe.ui.components.EmptyState(Icons.Type.USERS,
                                "No one found",
                                "Try another name or username, or clear the box to see everyone."));
                    } else {
                        statusLabel.setText(
                            users.size() + " people found"
                        );

                        for (UserResult user : users) {
                            resultsPanel.add(buildUserCard(user));
                        }
                    }

                } catch (Exception e) {
                    statusLabel.setText(
                        "Unable to load users. Check your database."
                    );
                    e.printStackTrace();
                }

                resultsPanel.revalidate();
                resultsPanel.repaint();
            }
        };

        worker.execute();
    }

    private List<UserResult> findUsers(String keyword)
            throws SQLException {

        List<UserResult> users = new ArrayList<>();

        String sql = """
            SELECT id, full_name, username, bio, profile_pic
            FROM users
            WHERE status = 'active'
              AND id <> ?
              AND (
                  full_name LIKE ?
                  OR username LIKE ?
              )
            ORDER BY full_name ASC
            LIMIT 100
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, currentUserId);
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new UserResult(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("bio"),
                        rs.getString("profile_pic")
                    ));
                }
            }
        }

        return users;
    }

    private JPanel buildUserCard(UserResult user) {

        RoundedPanel card = new RoundedPanel(18, CARD, BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 18, 14, 18));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBorderColor(BLUE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBorderColor(BORDER);
            }
        });

        Avatar avatar = new Avatar(54);
        avatar.setUser(user.fullName, user.profilePic);
        avatar.showFrameOf(user.id);

        JLabel name = new JLabel(safeText(user.fullName));
        name.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 16));
        name.setForeground(TEXT);
        com.eduvibe.ui.components.RewardArt.attachBadge(name, user.id, 20);

        JLabel username = new JLabel("@" + safeText(user.username));
        username.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        username.setForeground(BLUE_LIGHT);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Box.createVerticalGlue());
        names.add(name);
        names.add(Box.createVerticalStrut(2));
        names.add(username);
        names.add(Box.createVerticalGlue());

        JPanel top = new JPanel(new BorderLayout(14, 0));
        top.setOpaque(false);
        top.add(avatar, BorderLayout.WEST);
        top.add(names, BorderLayout.CENTER);

        boolean hasBio = user.bio != null && !user.bio.isBlank();
        JTextArea bio = new JTextArea(hasBio ? user.bio.trim() : "No bio yet.");
        bio.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 12));
        bio.setForeground(MUTED);
        bio.setOpaque(false);
        bio.setEditable(false);
        bio.setFocusable(false);
        bio.setLineWrap(true);
        bio.setWrapStyleWord(true);
        bio.setRows(2);
        bio.setBorder(null);

        JLabel counts = new JLabel(
            followDAO.getFollowersCount(user.id) + " followers  \u00b7  "
            + followDAO.getFollowingCount(user.id) + " following");
        counts.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 11));
        counts.setForeground(MUTED);

        boolean followingNow = followDAO.isFollowing(currentUserId, user.id);
        RoundedButton followButton = new RoundedButton(followingNow ? "Following" : "Follow",
                followingNow ? RoundedButton.Style.SECONDARY : RoundedButton.Style.PRIMARY);
        followButton.setPreferredSize(new Dimension(98, 34));

        followButton.addActionListener(e -> {
            followButton.setEnabled(false);

            boolean following = followDAO.isFollowing(currentUserId, user.id);
            boolean success = following
                ? followDAO.unfollowUser(currentUserId, user.id)
                : followDAO.followUser(currentUserId, user.id);

            if (success) {
                followButton.setText(following ? "Follow" : "Following");
                followButton.setStyle(following ? RoundedButton.Style.PRIMARY : RoundedButton.Style.SECONDARY);
                counts.setText(
                    followDAO.getFollowersCount(user.id) + " followers  \u00b7  "
                    + followDAO.getFollowingCount(user.id) + " following");
            } else {
                JOptionPane.showMessageDialog(this, "Unable to update follow status.",
                    "Follow Error", JOptionPane.ERROR_MESSAGE);
            }

            followButton.setEnabled(true);
        });

        RoundedButton viewButton = new RoundedButton("Profile", RoundedButton.Style.OUTLINE);
        viewButton.setPreferredSize(new Dimension(78, 34));
        viewButton.addActionListener(e -> showUserProfile(user));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(viewButton);
        buttons.add(followButton);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(counts, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.setOpaque(false);
        middle.add(bio, BorderLayout.NORTH);

        card.add(top, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void showUserProfile(UserResult user) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "User Profile",
            true
        );

        dialog.setSize(640, 640);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        panel.setLayout(new BoxLayout(
            panel, BoxLayout.Y_AXIS
        ));

        com.eduvibe.ui.components.ProfilePreview preview = new com.eduvibe.ui.components.ProfilePreview();
        preview.setUser(safeText(user.fullName), safeText(user.username), null);
        preview.setCaption("", Color.WHITE);
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        new SwingWorker<com.eduvibe.ui.components.ProfilePreview.Look, Void>() {
            @Override
            protected com.eduvibe.ui.components.ProfilePreview.Look doInBackground() {
                return new com.eduvibe.ui.components.ProfilePreview.Look(
                    piece(user.id, "frame"), piece(user.id, "banner"),
                    piece(user.id, "theme"), piece(user.id, "badge"));
            }

            @Override
            protected void done() {
                try {
                    preview.setLook(get());
                } catch (Exception ignored) {
                    // plain profile
                }
            }
        }.execute();

        JLabel followers = new JLabel(
            "Followers: " + followDAO.getFollowersCount(user.id)
        );
        followers.setForeground(MUTED);

        JLabel following = new JLabel(
            "Following: " + followDAO.getFollowingCount(user.id)
        );
        following.setForeground(MUTED);

        JTextArea bio = new JTextArea(
            safeText(user.bio).equals("Not provided")
                ? "No bio available."
                : user.bio
        );
        bio.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 14));
        bio.setForeground(TEXT);
        bio.setBackground(CARD);
        bio.setLineWrap(true);
        bio.setWrapStyleWord(true);
        bio.setEditable(false);
        bio.setBorder(new EmptyBorder(12, 12, 12, 12));

        panel.add(preview);
        panel.add(Box.createVerticalStrut(16));
        panel.add(followers);
        panel.add(Box.createVerticalStrut(5));
        panel.add(following);
        panel.add(Box.createVerticalStrut(18));
        panel.add(new JLabel("About"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(bio);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private static com.eduvibe.ui.components.ProfilePreview.Piece piece(int userId, String slot) {
        String[] r = com.eduvibe.dao.EquipDAO.slotOf(userId, slot);
        return r == null ? null : new com.eduvibe.ui.components.ProfilePreview.Piece(0, r[0], r[1]);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (
            parts[0].substring(0, 1)
            + parts[parts.length - 1].substring(0, 1)
        ).toUpperCase();
    }

    private String safeText(String value) {
        return value == null || value.isBlank()
            ? "Not provided" : value;
    }

    private static class UserResult {
        int id;
        String fullName;
        String username;
        String bio;
        String profilePic;

        UserResult(int id, String fullName,
                   String username, String bio, String profilePic) {
            this.profilePic = profilePic;
            this.id = id;
            this.fullName = fullName;
            this.username = username;
            this.bio = bio;
        }
    }
}
