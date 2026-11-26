package com.eduvibe.ui;

import com.eduvibe.model.User;
import com.eduvibe.ui.components.FadePanel;
import com.eduvibe.ui.components.SmoothScroll;
import com.eduvibe.ui.components.Avatar;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.FieldBox;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.NavButton;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.util.EntertainmentTimeTracker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * com.eduvibe.ui.DashboardFrame  (REPLACES existing file)
 *
 * Main shell: left navigation, top bar, page area and (on feed pages)
 * a right info sidebar. Constructor DashboardFrame(User) is unchanged.
 */
public class DashboardFrame extends JFrame {

    private final User currentUser;
    private final EntertainmentTimeTracker timeTracker;
    private final java.util.List<com.eduvibe.ui.components.EntertainmentTimerBar> timerBars = new ArrayList<>();
    private javax.swing.Timer timeTicker;
    private java.awt.event.AWTEventListener activityListener;
    private int lastLimit = Integer.MIN_VALUE;
    private int tickCount = 0;

    private CardLayout cardLayout;
    private FadePanel contentPanel;
    private JScrollPane contentScroll;
    private JLabel pageTitle;
    private RightSidebar rightSidebar;
    private JScrollPane rightScroll;
    private NavButton notificationsNav;
    private JLabel bellBadge;
    private Avatar topAvatar;
    private Avatar sidebarAvatar;
    private JLabel sidebarName;
    private JLabel sidebarHandle;
    private FieldBox searchBox;
    private javax.swing.Timer searchDebounce;
    private final Map<String, FeedPanel> feedPanels = new LinkedHashMap<>();
    private HomeFeedPanel homePanel;
    private ReelsHost reelsHost;
    private JScrollPane sidebarScroll;
    private boolean sidebarShown = true;
    private javax.swing.Timer sidebarAnimation;

    private final Map<String, NavButton> navigationButtons = new LinkedHashMap<>();

    private NotificationsPanel notificationsPanel;
    private RewardsPanel rewardsPanel;
    private CustomizePanel customizePanel;
    private LeaderboardPanel leaderboardPanel;
    private FocusTimerPanel focusPanel;
    private PlaytimePanel playtimePanel;
    private ProfilePanel profilePanel;
    private SettingsPanel settingsPanel;
    private AdminPanel adminPanel;
    private SavedPostsPanel savedPostsPanel;
    private UserSearchPanel userSearchPanel;

    private String activePage = "HOME";

    public DashboardFrame(User user) {

        if (user == null) {
            throw new IllegalArgumentException("Logged-in user cannot be null.");
        }

        currentUser = user;
        timeTracker = EntertainmentTimeTracker.forUser(user.getId());
        applyDefaultReminderOnce();

        setTitle("EduVibe | Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        com.eduvibe.util.WindowFit.sizeAndCenter(this, 1500, 900);
        setMinimumSize(new Dimension(
                Math.min(1100, getWidth()), Math.min(700, getHeight())));
        setBackground(Theme.BACKGROUND);

        buildDashboard();
        startTimeReminder();
    }

    /**
     * First run on this computer: reminders default to 30 minutes when the account has no
     * limit saved yet. After that the user's own setting (Settings > Entertainment Reminder) rules.
     */
    private void applyDefaultReminderOnce() {
        try {
            java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot()
                    .node("com/eduvibe/entertainment/" + currentUser.getId());
            if (!p.getBoolean("defaultApplied", false)) {
                p.putBoolean("defaultApplied", true);
                p.flush();
                if (currentUser.getEntertainmentLimitMinutes() <= 0) {
                    final int minutes = 30;
                    currentUser.setEntertainmentLimitMinutes(minutes);
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            new com.eduvibe.dao.ProfileDAO().updateEntertainmentLimit(currentUser.getId(), minutes);
                            return null;
                        }
                    }.execute();
                }
            }
        } catch (Exception ignored) {
            // keep whatever the account has
        }
    }

    /**
     * One 1-second timer drives everything: active-time tracking, the timer bars and the
     * reminder. Window focus and user input are watched so background / idle time is not counted.
     */
    private void startTimeReminder() {

        if (timeTicker != null) {
            return;                                   // never create a second tracking timer
        }

        activityListener = event -> timeTracker.touch();
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
                timeTracker.setWindowActive(true);
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                timeTracker.setWindowActive(false);
            }

            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                timeTracker.setWindowActive(false);
            }

            @Override
            public void windowDeiconified(java.awt.event.WindowEvent e) {
                timeTracker.setWindowActive(isFocused());
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopTimeTracking();
            }
        });

        timeTicker = new javax.swing.Timer(1000, e -> onTimeTick());
        timeTicker.start();
    }

    private void onTimeTick() {

        int limit = currentUser.getEntertainmentLimitMinutes();
        if (limit != lastLimit) {
            if (lastLimit != Integer.MIN_VALUE) {
                timeTracker.limitChanged(limit);
            }
            lastLimit = limit;
        }

        timeTracker.tick();

        if (isEntertainmentPage(activePage)) {
            for (var bar : timerBars) {
                if (bar.isShowing()) {
                    bar.refresh();
                }
            }
        }

        if (++tickCount % 10 == 0 && rightSidebar != null) {
            rightSidebar.updateTime();
        }

        if (!timeTracker.reminderDue(limit) || com.eduvibe.ui.components.ReminderToast.isVisibleNow()) {
            return;
        }

        final String message = com.eduvibe.ui.components.ReminderToast.MESSAGE;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new com.eduvibe.dao.DashboardStatsDAO().addReminder(currentUser.getId(), message);
                return null;
            }

            @Override
            protected void done() {
                if (rightSidebar != null) {
                    rightSidebar.refresh();
                }
            }
        }.execute();

        com.eduvibe.ui.components.ReminderToast.show(this,
                () -> navigate("ACADEMIC"),      // Return to Academic Activities
                () -> { });                      // Continue Entertainment: just dismiss, timer keeps running
    }

    /**
     * Entertainment time counts only while Entertainment content is on screen: the Home feed with Entertainment
     * chosen, or Entertainment Reels. Academic content never counts.
     */
    private boolean isEntertainmentPage(String page) {
        if ("HOME".equals(page)) {
            return homePanel != null && "entertainment".equals(homePanel.getCategory());
        }
        if ("REELS".equals(page)) {
            return reelsHost != null && "entertainment".equals(reelsHost.getCategory());
        }
        return false;
    }

    private void updateEntertainmentTracking() {
        if (isEntertainmentPage(activePage)) {
            timeTracker.enter();
        } else {
            timeTracker.leave();
        }
    }

    /** Stops the timer and listener and saves progress (logout / close). */
    private void stopTimeTracking() {
        if (timeTicker != null) {
            timeTicker.stop();
            timeTicker = null;
        }
        if (activityListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
            activityListener = null;
        }
        com.eduvibe.ui.components.ReminderToast.dismissNow();
        timeTracker.flush();
    }

    private void buildDashboard() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);

        sidebarScroll = buildSidebar();
        root.add(sidebarScroll, BorderLayout.WEST);
        root.add(buildMainArea(), BorderLayout.CENTER);

        setContentPane(root);
        navigate("HOME");
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                pinToTop();                 // the app opens at the top of the page
            }
        });
    }

    // ---------------- LEFT SIDEBAR ----------------

    private JScrollPane buildSidebar() {

        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Color top = Theme.isLight() ? new Color(250, 251, 255)
                        : com.eduvibe.ui.components.Anim.mix(Theme.SIDEBAR, Theme.VIOLET, 0.10f);
                g2.setPaint(new GradientPaint(0, 0, top, 0, Math.max(1, getHeight()), Theme.SIDEBAR));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 16));

        sidebar.add(buildLogo());
        sidebar.add(Box.createVerticalStrut(26));

        addNav(sidebar, Icons.Type.HOME, "Home", "HOME");

        addSection(sidebar, "Feed");
        // Academic / Entertainment are chosen with the floating bar on Home and on Reels, not in this menu.
        addNav(sidebar, Icons.Type.PLAY, "Reels", "REELS");
        addNav(sidebar, Icons.Type.HEART, "Following", "FOLLOWING");

        addSection(sidebar, "Activity");
        addNav(sidebar, Icons.Type.PLUS, "Create Post", "CREATE");
        addNav(sidebar, Icons.Type.COIN, "Points & Rewards", "REWARDS");
        addNav(sidebar, Icons.Type.GIFT, "Playtime Rewards", "PLAYTIME");
        addNav(sidebar, Icons.Type.STAR, "Customize", "CUSTOMIZE");
        notificationsNav = addNav(sidebar, Icons.Type.BELL, "Notifications", "NOTIFICATIONS");

        addSection(sidebar, "Account");
        addNav(sidebar, Icons.Type.USER, "Profile", "PROFILE");
        addNav(sidebar, Icons.Type.GEAR, "Settings", "SETTINGS");

        addSection(sidebar, "Explore");
        addNav(sidebar, Icons.Type.TROPHY, "Leaderboard", "LEADERBOARD");
        addNav(sidebar, Icons.Type.CLOCK, "Focus Timer", "FOCUS");
        addNav(sidebar, Icons.Type.BOOKMARK, "Saved Posts", "SAVED");
        addNav(sidebar, Icons.Type.SEARCH, "Find People", "USER_SEARCH");
        addNav(sidebar, Icons.Type.INFO, "About EduVibe", "ABOUT");

        if (isAdmin()) {
            addNav(sidebar, Icons.Type.SHIELD, "Admin Panel", "ADMIN");
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(buildAccountCard());
        sidebar.add(Box.createVerticalStrut(10));

        NavButton logout = new NavButton(Icons.Type.LOGOUT, "Logout");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        logout.addActionListener(e -> logout());
        sidebar.add(logout);

        JScrollPane scroll = new JScrollPane(sidebar,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.SIDEBAR);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setPreferredSize(new Dimension(252, 100));
        return scroll;
    }

    private JPanel buildLogo() {

        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logo.setOpaque(false);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel icon = new JLabel(Icons.icon(Icons.Type.CAP, 30, Theme.BLUE));
        JLabel text = new com.eduvibe.ui.components.GradientLabel("EduVibe");
        text.setFont(Theme.bold(24));
        text.setForeground(Theme.BLUE);

        logo.add(icon);
        logo.add(text);
        return logo;
    }

    private void addSection(JPanel sidebar, String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(Theme.bold(10));
        label.setForeground(Theme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(12, 14, 4, 0));
        sidebar.add(label);
    }

    private NavButton addNav(JPanel sidebar, Icons.Type icon, String text, String page) {

        NavButton button = new NavButton(icon, text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.addActionListener(e -> navigate(page));

        navigationButtons.put(page, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(2));
        return button;
    }

    /** Reads this account's current details from the database and updates every place that shows them. */
    private void refreshAccountDetails() {
        final int id = currentUser.getId();
        new SwingWorker<java.util.Optional<com.eduvibe.model.User>, Void>() {
            @Override
            protected java.util.Optional<com.eduvibe.model.User> doInBackground() throws Exception {
                return new com.eduvibe.dao.UserDAO().findById(id);
            }

            @Override
            protected void done() {
                try {
                    java.util.Optional<com.eduvibe.model.User> fresh = get();
                    if (fresh.isEmpty()) {
                        return;
                    }
                    currentUser.setFullName(fresh.get().getFullName());
                    currentUser.setProfilePic(fresh.get().getProfilePic());
                    if (sidebarAvatar != null) {
                        sidebarAvatar.setUser(currentUser.getFullName(), currentUser.getProfilePic());
                    }
                    if (topAvatar != null) {
                        topAvatar.setUser(currentUser.getFullName(), currentUser.getProfilePic());
                    }
                    if (sidebarName != null) {
                        sidebarName.setText(safe(currentUser.getFullName()));
                    }
                    if (sidebarHandle != null) {
                        sidebarHandle.setText("@" + safe(currentUser.getUsername()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private JPanel buildAccountCard() {

        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Avatar avatar = new Avatar(38);
        sidebarAvatar = avatar;
        avatar.setUser(currentUser.getFullName(), currentUser.getProfilePic());
        avatar.showFrameOf(currentUser.getId());

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel name = new JLabel(safe(currentUser.getFullName()));
        sidebarName = name;
        name.setFont(Theme.bold(13));
        name.setForeground(Theme.TEXT);

        JLabel handle = new JLabel("@" + safe(currentUser.getUsername()));
        sidebarHandle = handle;
        handle.setFont(Theme.regular(11));
        handle.setForeground(Theme.MUTED);

        text.add(name);
        text.add(handle);

        card.add(avatar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate("PROFILE");
            }
        });
        return card;
    }

    // ---------------- MAIN AREA ----------------

    private JPanel buildMainArea() {

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);

        main.add(buildTopBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new FadePanel(cardLayout);
        contentPanel.setBackground(Theme.BACKGROUND);
        registerPages();

        contentScroll = new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScroll.setBorder(null);
        contentScroll.getViewport().setBackground(Theme.BACKGROUND);
        contentScroll.getVerticalScrollBar().setUnitIncrement(20);
        // animated effects + blit scrolling leave ghost edges; repaint the whole viewport instead
        contentScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        installTopPin(contentScroll);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setBackground(Theme.BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 24));
        body.add(contentScroll, BorderLayout.CENTER);

        rightSidebar = new RightSidebar(currentUser, this::navigate, timeTracker);
        rightSidebar.setUnreadListener(this::applyUnread);
        rightSidebar.setTagListener(this::searchFor);

        rightScroll = new JScrollPane(rightSidebar,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.setBorder(null);
        rightScroll.getViewport().setBackground(Theme.BACKGROUND);
        rightScroll.setPreferredSize(new Dimension(300, 100));
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        installTopPin(rightScroll);
        body.add(rightScroll, BorderLayout.EAST);

        main.add(body, BorderLayout.CENTER);
        SmoothScroll.installAll(main);
        return main;
    }

    private JPanel buildTopBar() {

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BACKGROUND);
        bar.setBorder(BorderFactory.createEmptyBorder(20, 28, 16, 24));

        pageTitle = new JLabel("Home Feed");
        pageTitle.setFont(Theme.bold(26));
        pageTitle.setForeground(Theme.TEXT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        actions.setOpaque(false);

        RoundedButton create = new RoundedButton("Create Post").withIcon(Icons.Type.PLUS);
        create.addActionListener(e -> navigate("CREATE"));

        actions.add(create);
        actions.add(buildBell());

        topAvatar = new Avatar(40);
        topAvatar.setUser(currentUser.getFullName(), currentUser.getProfilePic());
        topAvatar.showFrameOf(currentUser.getId());
        topAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topAvatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate("PROFILE");
            }
        });
        actions.add(topAvatar);

        searchBox = FieldBox.text(Icons.Type.SEARCH, "Search posts or people...");
        searchBox.setPreferredSize(new Dimension(360, 44));
        searchBox.setMaximumSize(new Dimension(360, 44));

        searchDebounce = new javax.swing.Timer(300, e -> applySearch());
        searchDebounce.setRepeats(false);
        searchBox.getField().getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { searchDebounce.restart(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { searchDebounce.restart(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { searchDebounce.restart(); }
                });

        // The search box takes the space left between the title and the buttons (never more than
        // 360 px, never less than 150 px) and is centred, so it can no longer cover the title.
        JPanel centre = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 44);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(150, 44);
            }

            @Override
            public void doLayout() {
                int w = Math.max(150, Math.min(360, getWidth() - 24));
                int x = Math.max(0, (getWidth() - w) / 2);
                searchBox.setBounds(x, (getHeight() - 44) / 2, w, 44);
            }
        };
        centre.setOpaque(false);
        centre.add(searchBox);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleRow.setOpaque(false);
        titleRow.add(buildSidebarToggle());
        titleRow.add(pageTitle);
        bar.add(titleRow, BorderLayout.WEST);
        bar.add(centre, BorderLayout.CENTER);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    /** The menu button: hides or shows the left sidebar, to give the feed the whole width. */
    private JComponent buildSidebarToggle() {
        JComponent b = new JComponent() {
            private boolean hover;

            {
                setPreferredSize(new Dimension(40, 40));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Hide or show the menu");
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        toggleSidebar();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(hover ? Theme.CARD_LIGHT : Theme.CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Theme.TEXT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 3; i++) {
                    g2.drawLine(12, 13 + i * 7, 28, 13 + i * 7);
                }
                g2.dispose();
            }
        };
        return b;
    }

    /** Slides the sidebar away (or back) so the feed can use the whole width. */
    private void toggleSidebar() {
        if (sidebarScroll == null) {
            return;
        }
        sidebarShown = !sidebarShown;
        final int full = 252;
        final int target = sidebarShown ? full : 0;
        if (sidebarAnimation != null) {
            sidebarAnimation.stop();
        }
        if (sidebarShown) {
            sidebarScroll.setVisible(true);
        }
        sidebarAnimation = new javax.swing.Timer(15, null);
        sidebarAnimation.addActionListener(e -> {
            int w = sidebarScroll.getPreferredSize().width;
            int next = w + (int) Math.round((target - w) * 0.3);
            if (Math.abs(target - next) < 4) {
                next = target;
                sidebarAnimation.stop();
                if (!sidebarShown) {
                    sidebarScroll.setVisible(false);
                }
            }
            sidebarScroll.setPreferredSize(new Dimension(next, 100));
            Container host = sidebarScroll.getParent();
            if (host != null) {
                host.revalidate();                    // the feed takes (or gives back) the freed width
                host.repaint();
            }
        });
        sidebarAnimation.start();
    }

    private JComponent buildBell() {

        JPanel wrap = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 40);
            }
        };
        wrap.setOpaque(false);
        wrap.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel bell = new JLabel(Icons.icon(Icons.Type.BELL, 24, Theme.MUTED));
        bell.setBounds(8, 8, 24, 24);
        wrap.add(bell);

        bellBadge = new JLabel("", SwingConstants.CENTER);
        bellBadge.setOpaque(true);
        bellBadge.setBackground(Theme.RED);
        bellBadge.setForeground(Color.WHITE);
        bellBadge.setFont(Theme.bold(10));
        bellBadge.setBounds(22, 2, 18, 16);
        bellBadge.setVisible(false);
        wrap.add(bellBadge, 0);

        wrap.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate("NOTIFICATIONS");
            }
        });
        return wrap;
    }

    private void applyUnread(int count) {
        notificationsNav.setBadge(count);
        bellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        bellBadge.setBounds(count > 9 ? 16 : 22, 2, count > 9 ? 24 : 18, 16);
        bellBadge.setVisible(count > 0);
    }

    // ---------------- PAGES ----------------

    private void registerPages() {

        addPage("HOME", createHomePage());
        addPage("FOLLOWING", createFeedPage("FOLLOWING"));
        addPage("REELS", createReelsPage());

        addPage("CREATE", new CreatePostPanel(currentUser.getId(), () -> navigate("HOME")));

        rewardsPanel = new RewardsPanel(currentUser.getId());
        rewardsPanel.setOnPointsChanged(() -> {
            if (rightSidebar != null) {
                rightSidebar.refresh();
            }
        });
        rewardsPanel.setNavigator(this::navigate);
        addPage("REWARDS", rewardsPanel);

        customizePanel = new CustomizePanel(currentUser.getId(), currentUser.getFullName(),
                currentUser.getUsername(), currentUser.getProfilePic());
        customizePanel.setNavigator(this::navigate);
        customizePanel.setOnChanged(() -> topAvatar.showFrameOf(currentUser.getId()));
        addPage("CUSTOMIZE", customizePanel);

        leaderboardPanel = new LeaderboardPanel(currentUser.getId());
        addPage("LEADERBOARD", leaderboardPanel);

        focusPanel = new FocusTimerPanel(currentUser.getId());
        addPage("FOCUS", focusPanel);
        playtimePanel = new PlaytimePanel(currentUser.getId());
        playtimePanel.setNavigator(this::navigate);
        playtimePanel.setOnPointsChanged(() -> {
            if (rightSidebar != null) {
                rightSidebar.refresh();
            }
        });
        addPage("PLAYTIME", playtimePanel);
        addPage("ABOUT", new AboutPanel());

        notificationsPanel = new NotificationsPanel(currentUser.getId());
        notificationsPanel.setOnChanged(() -> {
            if (rightSidebar != null) {
                rightSidebar.refresh();
            }
        });
        addPage("NOTIFICATIONS", notificationsPanel);

        savedPostsPanel = new SavedPostsPanel(currentUser.getId());
        addPage("SAVED", savedPostsPanel);

        userSearchPanel = new UserSearchPanel(currentUser.getId());
        addPage("USER_SEARCH", userSearchPanel);

        profilePanel = new ProfilePanel(currentUser.getId());
        profilePanel.setOnProfileChanged(this::refreshAccountDetails);
        addPage("PROFILE", profilePanel);

        settingsPanel = new SettingsPanel(currentUser);
        addPage("SETTINGS", settingsPanel);

        if (isAdmin()) {
            adminPanel = new AdminPanel(currentUser.getId());
            addPage("ADMIN", adminPanel);
        }
    }

    private void addPage(String key, JPanel page) {
        contentPanel.add(page, key);
    }

    /** Home: one feed with a floating Academic | Entertainment bar on top. */
    private JPanel createHomePage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        homePanel = new HomeFeedPanel(currentUser.getId());
        homePanel.setNavigator(this::navigate);
        homePanel.setOnCategoryChanged(c -> updateEntertainmentTracking());
        SwingUtilities.invokeLater(homePanel::restoreCategory);
        var bar = new com.eduvibe.ui.components.EntertainmentTimerBar(
                timeTracker, currentUser::getEntertainmentLimitMinutes);
        timerBars.add(bar);
        homePanel.setTimerBar(bar);
        panel.add(homePanel, BorderLayout.CENTER);
        return panel;
    }

    /** The full-screen, TikTok-style Reels page. */
    private JPanel createReelsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND);
        reelsHost = new ReelsHost(currentUser.getId());
        reelsHost.setNavigator(this::navigate);
        reelsHost.setOnCategoryChanged(c -> updateEntertainmentTracking());
        SwingUtilities.invokeLater(reelsHost::restoreCategory);
        // only Entertainment Reels count towards (and show) the entertainment time limit
        var bar = new com.eduvibe.ui.components.EntertainmentTimerBar(
                timeTracker, currentUser::getEntertainmentLimitMinutes);
        timerBars.add(bar);
        reelsHost.setEntertainmentTimerBar(bar);
        panel.add(reelsHost, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFeedPage(String feedType) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND);
        FeedPanel feed = new FeedPanel(currentUser.getId(), feedType);
        feed.setHeaderContext(this::navigate, currentUser.getFullName(), currentUser.getProfilePic());
        feedPanels.put(feedType, feed);
        if ("ENTERTAINMENT".equals(feedType)) {
            var bar = new com.eduvibe.ui.components.EntertainmentTimerBar(
                    timeTracker, currentUser::getEntertainmentLimitMinutes);
            timerBars.add(bar);
            feed.setTimerBar(bar);
        }
        panel.add(feed, BorderLayout.CENTER);
        return panel;
    }

    // ---------------- NAVIGATION ----------------

    /** Opens a page by key (e.g. "SETTINGS"); used when the app reopens itself. */
    public void showPage(String page) {
        navigate(page);
    }

    // ---- every page opens at its top ----
    // A page's content arrives and is laid out a moment after it is shown, and a focused text field
    // can scroll the page to itself. For a short while after a page opens (or the app starts) the
    // scroll is kept at the top, until the person scrolls themselves.
    private final Map<JScrollPane, long[]> topPins = new java.util.HashMap<>();

    private void installTopPin(JScrollPane sp) {
        final long[] state = {System.currentTimeMillis() + 1500, 0};   // {pinned until, user scrolled}
        topPins.put(sp, state);
        sp.getViewport().addChangeListener(e -> {
            if (state[1] == 0 && System.currentTimeMillis() < state[0]) {
                Point pos = sp.getViewport().getViewPosition();
                if (pos.y != 0) {
                    sp.getViewport().setViewPosition(new Point(pos.x, 0));
                }
            }
        });
        sp.addMouseWheelListener(e -> state[1] = 1);
        sp.getVerticalScrollBar().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                state[1] = 1;
            }
        });
    }

    private void pinToTop() {
        long until = System.currentTimeMillis() + 1500;
        for (Map.Entry<JScrollPane, long[]> en : topPins.entrySet()) {
            en.getValue()[0] = until;
            en.getValue()[1] = 0;
            en.getKey().getViewport().setViewPosition(new Point(0, 0));
        }
        SwingUtilities.invokeLater(() -> topPins.keySet().forEach(sp ->
                sp.getViewport().setViewPosition(new Point(0, 0))));
    }

    private void navigate(String page) {

        if (!isValidPage(page)) {
            return;
        }

        // Academic / Entertainment are not pages any more: they are the two choices of the floating bar on Home
        if ("ACADEMIC".equals(page) || "ENTERTAINMENT".equals(page)) {
            if (homePanel != null) {
                homePanel.selectCategory(page.toLowerCase());
            }
            page = "HOME";
        }

        activePage = page;
        pinToTop();

        // Reels fills exactly the visible area (no scrolling); every other page scrolls as before
        contentPanel.setFillViewport("REELS".equals(page) || "HOME".equals(page));

        cardLayout.show(contentPanel, page);
        contentPanel.fadeIn();
        SmoothScroll.installAll(contentPanel);
        pageTitle.setText(getPageTitle(page));

        for (Map.Entry<String, NavButton> e : navigationButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(page));
        }

        updateEntertainmentTracking();

        if (rightScroll != null) {
            rightScroll.setVisible(isFeedPage(page) && !"REELS".equals(page));
        }

        FeedPanel feed = feedPanels.get(page);
        if (feed != null) {
            feed.loadPosts();
            feed.setSearchQuery(searchText());
        }
        if ("HOME".equals(page) && homePanel != null) {
            homePanel.loadPosts();
            homePanel.setSearchQuery(searchText());
        }
        if ("REELS".equals(page) && reelsHost != null) {
            reelsHost.setSearchQuery(searchText());
        }

        if ("NOTIFICATIONS".equals(page) && notificationsPanel != null) {
            notificationsPanel.loadNotifications();
        }
        if ("PLAYTIME".equals(page) && playtimePanel != null) {
            playtimePanel.load();
        }
        if ("FOCUS".equals(page) && focusPanel != null) {
            focusPanel.load();
        }
        if ("LEADERBOARD".equals(page) && leaderboardPanel != null) {
            leaderboardPanel.load();
        }
        if ("REWARDS".equals(page) && rewardsPanel != null) {
            rewardsPanel.loadData();
        }
        if ("CUSTOMIZE".equals(page) && customizePanel != null) {
            customizePanel.load();
        }
        if ("PROFILE".equals(page) && profilePanel != null) {
            profilePanel.loadProfile();
        }
        if ("SETTINGS".equals(page) && settingsPanel != null) {
            settingsPanel.loadSettings();
        }
        if ("ADMIN".equals(page) && adminPanel != null) {
            adminPanel.refreshData();
        }
        if ("SAVED".equals(page) && savedPostsPanel != null) {
            savedPostsPanel.loadSavedPosts();
        }

        if (rightSidebar != null) {
            rightSidebar.updateTime();
            rightSidebar.refresh();
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /** Puts text into the top-bar search box (used by trending hashtags). */
    private void searchFor(String text) {
        if (searchBox != null) {
            searchBox.getField().setText(text);
            applySearch();
        }
    }

    private String searchText() {
        return searchBox == null ? "" : searchBox.getText().trim();
    }

    /** Top-bar search: filters the current feed, or jumps to Home from other pages. */
    private void applySearch() {

        String query = searchText();

        if (!isFeedPage(activePage)) {
            if (query.isEmpty()) {
                return;
            }
            navigate("HOME");
            return;
        }

        FeedPanel feed = feedPanels.get(activePage);
        if (feed != null) {
            feed.setSearchQuery(query);
        } else if ("HOME".equals(activePage) && homePanel != null) {
            homePanel.setSearchQuery(query);
        } else if ("REELS".equals(activePage) && reelsHost != null) {
            reelsHost.setSearchQuery(query);
        }
    }

    private boolean isFeedPage(String page) {
        return switch (page) {
            case "HOME", "FOLLOWING", "REELS" -> true;
            default -> false;
        };
    }

    private boolean isValidPage(String page) {
        return switch (page) {
            case "HOME", "FOLLOWING", "ACADEMIC", "ENTERTAINMENT", "REELS",
                 "CREATE", "REWARDS", "CUSTOMIZE", "LEADERBOARD", "FOCUS", "NOTIFICATIONS", "SAVED",
                 "USER_SEARCH", "PROFILE", "SETTINGS", "ABOUT", "PLAYTIME" -> true;
            case "ADMIN" -> isAdmin();
            default -> false;
        };
    }

    private String getPageTitle(String page) {
        return switch (page) {
            case "HOME" -> "Home Feed";
            case "FOLLOWING" -> "Following Feed";
            case "ACADEMIC" -> "Academic Feed";
            case "ENTERTAINMENT" -> "Entertainment Feed";
            case "REELS" -> "Reels";
            case "CREATE" -> "Create a Post";
            case "REWARDS" -> "Points & Rewards";
            case "LEADERBOARD" -> "Leaderboard";
            case "FOCUS" -> "Focus Timer";
            case "NOTIFICATIONS" -> "Notifications";
            case "SAVED" -> "Saved Posts";
            case "USER_SEARCH" -> "Find People";
            case "PROFILE" -> "My Profile";
            case "SETTINGS" -> "Settings";
            case "ABOUT" -> "About EduVibe";
            case "PLAYTIME" -> "Playtime Rewards";
            case "CUSTOMIZE" -> "Customize Profile";
            case "ADMIN" -> "Admin Panel";
            default -> "EduVibe";
        };
    }

    private void logout() {
        if (Dialogs.confirm(this, "Log out", "Do you want to log out of EduVibe?",
                "Log Out", false)) {
            stopTimeTracking();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private boolean isAdmin() {
        return "admin".equalsIgnoreCase(safe(currentUser.getRole()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
