package com.eduvibe.ui;

import com.eduvibe.model.Post;
import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.Avatar;
import com.eduvibe.ui.components.EmptyState;
import com.eduvibe.ui.components.SegmentedTabs;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.ui.components.SkeletonCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.FeedPanel  (REPLACES existing file)
 *
 * Feed for HOME / FOLLOWING / ACADEMIC / ENTERTAINMENT / REELS.
 * Constructor FeedPanel(int, String) and loadPosts() are unchanged.
 * New: setSearchQuery(String) (used by the dashboard's top-bar search).
 * Posts are loaded in a SwingWorker. REELS shows one reel at a time with
 * Previous / Next. The page itself scrolls inside the dashboard.
 */
public class FeedPanel extends JPanel {

    private final int currentUserId;
    private final String feedType;
    private final PostService postService = new PostService();

    private JPanel postsContainer;
    private RoundedButton refreshButton;
    private JComboBox<String> sortBox;
    private int sortMode = 0;                 // 0 newest, 1 most liked, 2 most commented
    private JLabel resultLabel;
    private com.eduvibe.ui.components.PageHero hero;

    private List<Post> loadedPosts = new ArrayList<>();
    private String searchQuery = "";
    private int reelIndex = 0;
    private final List<JComponent> reelRows = new ArrayList<>();
    private com.eduvibe.ui.components.ReelSnapScroller snapper;
    private static final int REEL_BATCH = 6;       // reels built at first; more are added as you scroll
    private int reelsShown = REEL_BATCH;
    private int loadToken = 0;

    private final JPanel headerHost = new JPanel(new BorderLayout());
    private final JPanel timerHost = new JPanel(new BorderLayout());
    private Consumer<String> navigator = page -> { };
    private String fullName = "";
    private String profilePic = null;

    public FeedPanel(int currentUserId, String feedType) {

        this.currentUserId = currentUserId;
        this.feedType = feedType == null ? "HOME" : feedType;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(0, 0, 10, 0));

        headerHost.setOpaque(false);
        headerHost.add(buildHeader(), BorderLayout.CENTER);
        timerHost.setOpaque(false);
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        headerHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        timerHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(headerHost);
        north.add(timerHost);
        add(north, BorderLayout.NORTH);

        postsContainer = new JPanel() {
            /**
             * A comment box inside a post asks the page to scroll to its text cursor when it is laid out,
             * which threw the Reels page to the bottom. Reels move only by the snap scrolling.
             */
            @Override
            public void scrollRectToVisible(Rectangle r) {
                if (!isReels()) {
                    super.scrollRectToVisible(r);
                }
            }
        };
        postsContainer.setLayout(new BoxLayout(postsContainer, BoxLayout.Y_AXIS));
        postsContainer.setBackground(Theme.BACKGROUND);
        add(postsContainer, BorderLayout.CENTER);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & (java.awt.event.HierarchyEvent.SHOWING_CHANGED
                    | java.awt.event.HierarchyEvent.PARENT_CHANGED)) != 0) {
                SwingUtilities.invokeLater(this::updateSnap);
            }
        });

        loadPosts();
    }

    /** Lets the header's Academic/Entertainment tabs and the composer open other pages. */
    public void setHeaderContext(Consumer<String> navigator, String fullName, String profilePic) {
        this.navigator = navigator == null ? page -> { } : navigator;
        this.fullName = fullName == null ? "" : fullName;
        this.profilePic = profilePic;
        headerHost.removeAll();
        headerHost.add(buildHeader(), BorderLayout.CENTER);
        headerHost.revalidate();
        headerHost.repaint();
    }

    /** Shows a compact timer strip under the header (Entertainment / Reels feeds). */
    public void setTimerBar(JComponent bar) {
        timerHost.removeAll();
        if (bar != null) {
            timerHost.setBorder(new EmptyBorder(0, 0, 4, 0));
            timerHost.add(bar, BorderLayout.CENTER);
        }
        timerHost.revalidate();
        timerHost.repaint();
    }

    private com.eduvibe.ui.components.PageHero makeHero() {
        com.eduvibe.ui.components.PageHero h = switch (feedType.toUpperCase()) {
            case "ACADEMIC" -> new com.eduvibe.ui.components.PageHero(Icons.Type.BOOK, "Academic",
                    "Notes, questions and study help.",
                    com.eduvibe.ui.components.PageHero.ACADEMIC[0], com.eduvibe.ui.components.PageHero.ACADEMIC[1]);
            case "ENTERTAINMENT" -> new com.eduvibe.ui.components.PageHero(Icons.Type.SPARKLE, "Entertainment",
                    "Games, memes and everything fun.",
                    com.eduvibe.ui.components.PageHero.ENTERTAINMENT[0], com.eduvibe.ui.components.PageHero.ENTERTAINMENT[1]);
            case "FOLLOWING" -> new com.eduvibe.ui.components.PageHero(Icons.Type.HEART, "Following",
                    "The latest from the people you follow.",
                    com.eduvibe.ui.components.PageHero.FOLLOWING[0], com.eduvibe.ui.components.PageHero.FOLLOWING[1]);
            case "REELS" -> new com.eduvibe.ui.components.PageHero(Icons.Type.PLAY, "Reels",
                    "Short videos from the community.",
                    com.eduvibe.ui.components.PageHero.REELS[0], com.eduvibe.ui.components.PageHero.REELS[1]);
            default -> new com.eduvibe.ui.components.PageHero(Icons.Type.HOME, "Home",
                    "Your campus, today.",
                    com.eduvibe.ui.components.PageHero.HOME[0], com.eduvibe.ui.components.PageHero.HOME[1]);
        };
        return h;
    }

    private boolean isReels() {
        return "REELS".equalsIgnoreCase(feedType);
    }

    // ---------------- header ----------------

    private JPanel buildHeader() {

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        resultLabel = new JLabel(" ");
        resultLabel.setFont(Theme.bold(12));
        resultLabel.setForeground(Theme.BLUE_LIGHT);

        refreshButton = new RoundedButton("Refresh", RoundedButton.Style.SECONDARY);
        refreshButton.addActionListener(e -> loadPosts());

        hero = makeHero();
        header.add(hero);
        header.add(Box.createVerticalStrut(14));

        if (isReels()) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.add(refreshButton, BorderLayout.EAST);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(row);
            header.add(resultLabel);
            return header;
        }

        // ---- Academic | Entertainment tabs + Refresh ----
        int selected = "ACADEMIC".equalsIgnoreCase(feedType) ? 0
                : "ENTERTAINMENT".equalsIgnoreCase(feedType) ? 1 : -1;

        SegmentedTabs tabs = new SegmentedTabs(new String[] {"Academic", "Entertainment"},
                Math.max(0, selected), i -> navigator.accept(i == 0 ? "ACADEMIC" : "ENTERTAINMENT"));
        segTabs = tabs;
        segIndex = Math.max(0, selected);
        if (selected < 0) {
            tabs.putClientProperty("neutral", Boolean.TRUE);
        }

        JPanel tabRow = new JPanel(new BorderLayout(12, 0));
        tabRow.setOpaque(false);
        tabRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tabRow.add(tabs, BorderLayout.CENTER);

        sortBox = new JComboBox<>(new String[] {"Newest", "Most liked", "Most commented"});
        sortBox.setFont(Theme.regular(13));
        sortBox.setFocusable(false);
        sortBox.setToolTipText("Sort the posts below");
        sortBox.setPreferredSize(new Dimension(160, 34));
        sortBox.addActionListener(e -> {
            if (sortBox.getSelectedIndex() != sortMode) {
                sortMode = sortBox.getSelectedIndex();
                reelIndex = 0;
                reelsShown = REEL_BATCH;
                render();
            }
        });

        tabRow.add(refreshButton, BorderLayout.EAST);

        // ---- composer ----
        RoundedPanel composer = new RoundedPanel(16, Theme.CARD, Theme.BORDER);
        composer.setLayout(new BorderLayout(14, 0));
        composer.setBorder(new EmptyBorder(14, 16, 14, 16));
        composer.setAlignmentX(Component.LEFT_ALIGNMENT);
        composer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        composer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Avatar avatar = new Avatar(42);
        avatar.setUser(fullName.isBlank() ? "You" : fullName, profilePic);
        avatar.showFrameOf(currentUserId);

        JLabel prompt = new JLabel("Create a post…");
        prompt.setFont(Theme.regular(14));
        prompt.setForeground(Theme.MUTED);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(chip("Academic", Icons.Type.BOOK));
        chips.add(chip("Entertainment", Icons.Type.SPARKLE));

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);
        prompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(prompt);
        mid.add(Box.createVerticalStrut(10));
        mid.add(chips);

        RoundedButton send = new RoundedButton("", RoundedButton.Style.PRIMARY).withIcon(Icons.Type.SEND);
        send.addActionListener(e -> navigator.accept("CREATE"));
        JPanel sendWrap = new JPanel(new GridBagLayout());
        sendWrap.setOpaque(false);
        sendWrap.add(send);

        composer.add(avatar, BorderLayout.WEST);
        composer.add(mid, BorderLayout.CENTER);
        composer.add(sendWrap, BorderLayout.EAST);

        java.awt.event.MouseAdapter open = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                navigator.accept("CREATE");
            }
        };
        composer.addMouseListener(open);
        prompt.addMouseListener(open);

        JLabel sortCaption = new JLabel("Sort by");
        sortCaption.setFont(Theme.regular(12));
        sortCaption.setForeground(Theme.MUTED);
        JPanel sortHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sortHolder.setOpaque(false);
        sortHolder.add(sortCaption);
        sortHolder.add(sortBox);

        JPanel infoRow = new JPanel(new BorderLayout(12, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        infoRow.add(resultLabel, BorderLayout.CENTER);
        infoRow.add(sortHolder, BorderLayout.EAST);

        header.add(tabRow);
        header.add(Box.createVerticalStrut(14));
        header.add(composer);
        header.add(Box.createVerticalStrut(6));
        header.add(infoRow);
        return header;
    }

    private JComponent chip(String text, Icons.Type icon) {
        JLabel l = new JLabel(text, Icons.icon(icon, 14, Theme.BLUE_LIGHT), SwingConstants.LEFT);
        l.setIconTextGap(6);
        l.setFont(Theme.bold(11));
        l.setForeground(Theme.BLUE_LIGHT);
        l.setOpaque(true);
        l.setBackground(Theme.BLUE_SOFT);
        l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 12));
        return l;
    }

    private String subtitle() {
        return switch (feedType.toUpperCase()) {
            case "FOLLOWING" -> "See the latest posts from people you follow.";
            case "ACADEMIC" -> "Discover ideas, knowledge, and learning content.";
            case "ENTERTAINMENT" -> "Enjoy content shared by the community.";
            case "REELS" -> "Watch short videos from the EduVibe community.";
            default -> "Explore what's happening in the EduVibe community.";
        };
    }

    // ---------------- loading ----------------

    // The Academic | Entertainment tabs only switch pages. Each page keeps its own copy of them, so a copy
    // must always show the page it belongs to (otherwise it still shows the tab last clicked on it).
    private SegmentedTabs segTabs;
    private int segIndex;

    public void loadPosts() {
        if (segTabs != null) {
            segTabs.snapTo(segIndex);
        }

        final int token = ++loadToken;

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        postsContainer.removeAll();
        for (int i = 0; i < 3; i++) {
            postsContainer.add(new SkeletonCard());
            postsContainer.add(Box.createVerticalStrut(16));
        }
        postsContainer.revalidate();
        postsContainer.repaint();

        new SwingWorker<List<Post>, Void>() {
            @Override
            protected List<Post> doInBackground() throws Exception {
                return switch (feedType.toUpperCase()) {
                    case "FOLLOWING" -> postService.getFollowingFeed(currentUserId);
                    case "ACADEMIC" -> postService.getAcademicFeed(currentUserId);
                    case "ENTERTAINMENT" -> postService.getEntertainmentFeed(currentUserId);
                    case "REELS" -> postService.getReels(currentUserId);
                    default -> postService.getHomeFeed(currentUserId);
                };
            }

            @Override
            protected void done() {

                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");

                if (token != loadToken) {
                    return; // a newer load is already running
                }

                try {
                    List<Post> posts = get();
                    loadedPosts = posts == null ? new ArrayList<>() : posts;
                    reelIndex = 0;
                    reelsShown = REEL_BATCH;
                    render();

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    loadedPosts = new ArrayList<>();
                    postsContainer.removeAll();
                    postsContainer.add(message("Unable to load your feed",
                            "Check your database connection, then press Refresh."));
                    postsContainer.revalidate();
                    postsContainer.repaint();
                }
            }
        }.execute();
    }

    // ---------------- search ----------------

    /** Filters the loaded posts by title, description or username. Empty clears the filter. */
    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query.trim().toLowerCase();
        this.reelIndex = 0;
        this.reelsShown = REEL_BATCH;
        render();
    }

    private List<Post> filtered() {

        List<Post> result;

        if (searchQuery.isEmpty()) {
            result = new ArrayList<>(loadedPosts);
        } else {
            result = new ArrayList<>();
            for (Post p : loadedPosts) {
                if (contains(p.getTitle()) || contains(p.getDescription())
                        || contains(p.getUsername())) {
                    result.add(p);
                }
            }
        }

        // Newest is the order the database already returned; the other two re-order what is loaded.
        if (sortMode == 1) {
            result.sort((x, y) -> y.getLikeCount() != x.getLikeCount()
                    ? Integer.compare(y.getLikeCount(), x.getLikeCount())
                    : newerFirst(x, y));
        } else if (sortMode == 2) {
            result.sort((x, y) -> y.getCommentCount() != x.getCommentCount()
                    ? Integer.compare(y.getCommentCount(), x.getCommentCount())
                    : newerFirst(x, y));
        }
        return result;
    }

    /** Newer post first; a post without a date goes last instead of failing. */
    private static int newerFirst(Post x, Post y) {
        if (x.getCreatedAt() == null || y.getCreatedAt() == null) {
            return x.getCreatedAt() == y.getCreatedAt() ? 0 : (x.getCreatedAt() == null ? 1 : -1);
        }
        return y.getCreatedAt().compareTo(x.getCreatedAt());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase().contains(searchQuery);
    }

    // ---------------- rendering ----------------

    private void render() {

        postsContainer.removeAll();
        reelRows.clear();

        List<Post> posts = filtered();
        boolean searching = !searchQuery.isEmpty();

        if (hero != null) {
            int n = loadedPosts.size();
            hero.setChip(isReels() ? n + (n == 1 ? " reel" : " reels") : n + (n == 1 ? " post" : " posts"),
                    isReels() ? Icons.Type.PLAY : Icons.Type.COMMENT);
        }
        resultLabel.setText(searching
                ? posts.size() + " result" + (posts.size() == 1 ? "" : "s")
                  + " for \"" + searchQuery + "\""
                : " ");

        if (posts.isEmpty()) {
            if (searching) {
                postsContainer.add(message("No matching posts found",
                        "Try another title, description, or username."));
            } else if ("FOLLOWING".equalsIgnoreCase(feedType)) {
                postsContainer.add(message("Your following feed is empty",
                        "Follow people to see their posts here."));
            } else if (isReels()) {
                postsContainer.add(message("No reels yet",
                        "Reels are video posts. Upload one from Create Post."));
            } else {
                postsContainer.add(message("It's quiet here!",
                        "No posts available yet. Be the first to share!"));
            }

        } else if (isReels()) {
            renderReel(posts);

        } else {
            for (Post post : posts) {
                postsContainer.add(wrap(new PostCard(post, currentUserId)));
            }
        }

        postsContainer.revalidate();
        postsContainer.repaint();
        updateSnap();
    }

    /** Reels stop at each video (like TikTok) while this page is showing; other pages scroll normally. */
    private void updateSnap() {
        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        boolean want = isReels() && isShowing() && sp != null;
        if (want) {
            if (snapper != null && snapper.scrollPane() != sp) {
                snapper.uninstall();
                snapper = null;
            }
            if (snapper == null) {
                snapper = new com.eduvibe.ui.components.ReelSnapScroller(sp, () -> reelRows);
            }
            snapper.install();
        } else if (snapper != null) {
            snapper.uninstall();
            snapper = null;
        }
    }

    /**
     * Reels scroll one after another like a feed. Only the first few are built; the next ones are added
     * automatically when you reach the end, so a long list never loads every video at once.
     */
    private void renderReel(List<Post> posts) {
        appendReels(posts, 0, Math.min(posts.size(), reelsShown));
    }

    private void appendReels(List<Post> posts, int from, int to) {

        for (int i = from; i < to; i++) {
            JLabel counter = new JLabel("Reel " + (i + 1) + " of " + posts.size(), SwingConstants.CENTER);
            counter.setFont(Theme.bold(13));
            counter.setForeground(Theme.MUTED);
            JPanel nav = new JPanel(new BorderLayout());
            nav.setOpaque(false);
            nav.setAlignmentX(Component.LEFT_ALIGNMENT);
            nav.add(counter, BorderLayout.CENTER);
            nav.setPreferredSize(new Dimension(10, 24));
            JPanel row = reelColumn(nav, new PostCard(posts.get(i), currentUserId));
            reelRows.add(row);
            postsContainer.add(row);
        }

        if (to < posts.size()) {
            postsContainer.add(moreReelsSentinel(posts, to));
        }
    }

    /** An invisible strip at the end of the list: once it scrolls into view, the next reels are added. */
    private JComponent moreReelsSentinel(List<Post> posts, int shown) {
        final JLabel label = new JLabel("Loading more reels...", SwingConstants.CENTER);
        label.setFont(Theme.regular(12));
        label.setForeground(Theme.MUTED);
        final JPanel strip = new JPanel(new BorderLayout());
        strip.setOpaque(false);
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);
        strip.setPreferredSize(new Dimension(10, 60));
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        strip.add(label, BorderLayout.CENTER);

        final javax.swing.Timer timer = new javax.swing.Timer(350, null);
        timer.addActionListener(e -> {
            if (!strip.isShowing()) {
                return;
            }
            if (strip.getVisibleRect().height > 0) {
                timer.stop();
                postsContainer.remove(strip);
                int next = Math.min(posts.size(), shown + REEL_BATCH);
                reelsShown = next;
                appendReels(posts, shown, next);
                postsContainer.revalidate();
                postsContainer.repaint();
            }
        });
        strip.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                    && !strip.isDisplayable()) {
                timer.stop();
            }
        });
        timer.start();
        return strip;
    }

    /** Reels are shown as one centred, phone-shaped column that is never stretched to fill the page. */
    private JPanel reelColumn(JPanel nav, PostCard card) {

        final int width = 560;
        card.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));

        JPanel sized = new JPanel(new BorderLayout(0, 14)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(width, nav.getPreferredSize().height + 14
                        + card.getPreferredSize().height);
            }
        };
        sized.setOpaque(false);
        sized.add(nav, BorderLayout.NORTH);
        sized.add(card, BorderLayout.CENTER);

        JPanel row = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(0, 0, 16, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        gc.weightx = 1;
        row.add(sized, gc);
        return row;
    }

    private JPanel wrap(PostCard card) {

        // never taller than its own content, so a short feed does not stretch the posts
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel message(String title, String body) {

        Icons.Type icon = Icons.Type.SPARKLE;
        String lower = title == null ? "" : title.toLowerCase();

        if (lower.contains("unable")) {
            icon = Icons.Type.ALERT;
        } else if (lower.contains("search") || lower.contains("matching")) {
            icon = Icons.Type.SEARCH;
        } else if (lower.contains("following")) {
            icon = Icons.Type.USERS;
        } else if (lower.contains("reel")) {
            icon = Icons.Type.PLAY;
        }

        EmptyState card = new EmptyState(icon, title, body);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        return card;
    }
}
