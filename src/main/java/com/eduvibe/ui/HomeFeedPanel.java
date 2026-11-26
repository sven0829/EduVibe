package com.eduvibe.ui;

import com.eduvibe.model.Post;
import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.EmptyState;
import com.eduvibe.ui.components.FloatingCategoryNav;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.SkeletonCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.HomeFeedPanel  (NEW FILE; replaces HomeColumnsPanel)
 *
 * The Home feed. A floating pill at the top, ACADEMIC | ENTERTAINMENT, chooses which category's posts are
 * shown. The pill floats above the feed (it is not inside the scrolling content): scrolling down glides it
 * away, scrolling up brings it back, and it returns by itself when scrolling stops.
 *
 * Both categories are loaded from the database by their stored category (Reels excluded: they have their
 * own page), kept side by side, and switching only shows the other list. Each keeps its own scroll position.
 * The choice is remembered while you stay in the app and is separate from the choice made in Reels.
 */
public final class HomeFeedPanel extends JPanel {

    private static final int IDLE_MS = 700;                 // how long scrolling must stop before the pill returns
    private static final int NAV_TOP = 12 + FloatingCategoryNav.HEIGHT + 16;   // room under the pill at the top
    private static final int SHOWN = 40;                    // newest posts per category

    private final int userId;
    private final PostService postService = new PostService();
    private Consumer<String> navigator = key -> { };
    private Consumer<String> onCategoryChanged = c -> { };
    private String query = "";

    private final CategoryList academic;
    private final CategoryList entertainment;
    private final JPanel cards = new JPanel(new CardLayout());
    private final FloatingCategoryNav nav;
    private final JLayeredPane stage;
    private CategoryList active;

    public HomeFeedPanel(int userId) {
        super(new BorderLayout());
        this.userId = userId;
        setOpaque(false);

        academic = new CategoryList("academic", "Academic");
        entertainment = new CategoryList("entertainment", "Entertainment");
        active = academic;

        cards.setOpaque(false);
        cards.add(academic.scroll, "academic");
        cards.add(entertainment.scroll, "entertainment");

        nav = new FloatingCategoryNav(
                new String[] {"ACADEMIC", "ENTERTAINMENT"},
                new Icons.Type[] {Icons.Type.BOOK, Icons.Type.SPARKLE},
                new Color[][] {
                    {new Color(59, 130, 246), new Color(6, 182, 212)},
                    {new Color(139, 92, 246), new Color(236, 72, 153)}},
                0, this::onPick);
        nav.setIdleDelay(IDLE_MS);
        nav.setVisibilityListener(v -> placeNav());
        nav.addMouseWheelListener(this::forwardWheel);

        stage = new JLayeredPane() {
            @Override
            public void doLayout() {
                cards.setBounds(0, 0, getWidth(), getHeight());
                placeNav();
            }
        };
        stage.setOpaque(false);
        stage.add(cards, JLayeredPane.DEFAULT_LAYER);
        stage.add(nav, JLayeredPane.PALETTE_LAYER);
        timerHost.setOpaque(false);
        add(timerHost, BorderLayout.NORTH);
        add(stage, BorderLayout.CENTER);
    }

    private final JPanel timerHost = new JPanel(new BorderLayout());
    private JComponent timerBar;

    /** The slim entertainment-time strip: shown only while Entertainment is the chosen category. */
    public void setTimerBar(JComponent bar) {
        timerHost.removeAll();
        timerBar = bar;
        if (bar != null) {
            timerHost.setBorder(BorderFactory.createEmptyBorder(0, 18, 4, 18));
            timerHost.add(bar, BorderLayout.CENTER);
        }
        timerHost.setVisible(false);
        timerHost.revalidate();
    }

    // ------------------------------------------------------------------ outside API

    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator == null ? key -> { } : navigator;
    }

    /** Called when the chosen category changes ("academic" or "entertainment"). */
    public void setOnCategoryChanged(Consumer<String> listener) {
        this.onCategoryChanged = listener == null ? c -> { } : listener;
    }

    public String getCategory() {
        return active.category;
    }

    /** Chooses the category from outside (for example "Return to Academic Activities"). */
    public void selectCategory(String category) {
        nav.select("entertainment".equalsIgnoreCase(category) ? 1 : 0, true);
    }

    /** Filters the shown posts by title, description or username. Empty clears the filter. */
    public void setSearchQuery(String q) {
        this.query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        academic.render();
        entertainment.render();
    }


    private static final String PREF_KEY = "homeCategory";

    private static String savedCategory() {
        try {
            return java.util.prefs.Preferences.userRoot().node("eduvibe/ui").get(PREF_KEY, "academic");
        } catch (Exception e) {
            return "academic";
        }
    }

    private static void saveCategory(String category) {
        try {
            java.util.prefs.Preferences.userRoot().node("eduvibe/ui").put(PREF_KEY, category);
        } catch (Exception ignored) {
            // a remembered tab is only a convenience
        }
    }

    /** Opens on the category used last time (the owner has attached its listeners by then). */
    public void restoreCategory() {
        if ("entertainment".equals(savedCategory())) {
            selectCategory("entertainment");
        }
    }

    /** Loads the chosen category first, then the other one, each on its own. */
    public void loadPosts() {
        active.load();
        (active == academic ? entertainment : academic).load();
    }

    // ------------------------------------------------------------------ the floating pill

    private void onPick(int index) {
        active = index == 0 ? academic : entertainment;
        saveCategory(active.category);
        ((CardLayout) cards.getLayout()).show(cards, active.category);
        nav.setAtTop(active.scroll.getVerticalScrollBar().getValue() <= 2);
        nav.reveal();
        if (!active.loaded) {
            active.load();
        }
        timerHost.setVisible(timerBar != null && active == entertainment);
        timerHost.revalidate();
        onCategoryChanged.accept(active.category);
    }

    /** Centres the pill at the top and slides it up out of view by its visibility. */
    private void placeNav() {
        Dimension d = nav.getPreferredSize();
        int shown = 10 - FloatingCategoryNav.shadow();
        int hidden = -(d.height + 6);
        int y = (int) Math.round(hidden + (shown - hidden) * nav.visibility());
        nav.setBounds((stage.getWidth() - d.width) / 2, y, d.width, d.height);
    }

    private void forwardWheel(MouseWheelEvent e) {
        JScrollBar bar = active.scroll.getVerticalScrollBar();
        bar.setValue(bar.getValue() + e.getUnitsToScroll() * 24);
    }

    // ================================================================== one category's list

    /** A scrolling list that always matches the width of its viewport, so cards never overflow. */
    private static final class Body extends JPanel implements Scrollable {
        Body() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(NAV_TOP, 18, 24, 18));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return Math.max(40, r.height - 60);
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

    private final class CategoryList {

        final String category;                  // "academic" or "entertainment"
        final String name;
        final Body body = new Body();
        final JScrollPane scroll;
        List<Post> posts = new ArrayList<>();
        boolean loaded;
        private int token;
        private String error;
        private int lastValue;

        CategoryList(String category, String name) {
            this.category = category;
            this.name = name;

            scroll = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(24);
            scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

            // scroll direction -> the floating pill
            scroll.getVerticalScrollBar().addAdjustmentListener(e -> {
                int v = e.getValue();
                int dy = v - lastValue;
                lastValue = v;
                if (CategoryList.this != active) {
                    return;
                }
                nav.setAtTop(v <= 2);
                nav.onScroll(dy);
            });
            showSkeleton();
        }

        private void showSkeleton() {
            body.removeAll();
            for (int i = 0; i < 3; i++) {
                SkeletonCard card = new SkeletonCard();
                card.setPreferredSize(new Dimension(100, 170));
                card.setMaximumSize(new Dimension(780, 170));
                card.setAlignmentX(Component.CENTER_ALIGNMENT);
                body.add(card);
                body.add(Box.createVerticalStrut(16));
            }
            body.revalidate();
            body.repaint();
        }

        void load() {
            final int mine = ++token;
            error = null;
            if (!loaded) {
                showSkeleton();
            }
            new SwingWorker<List<Post>, Void>() {
                @Override
                protected List<Post> doInBackground() throws Exception {
                    return "academic".equals(category)
                            ? postService.getAcademicFeed(userId)
                            : postService.getEntertainmentFeed(userId);
                }

                @Override
                protected void done() {
                    if (mine != token) {
                        return;                       // a newer load is running
                    }
                    try {
                        List<Post> got = get();
                        posts = got == null ? new ArrayList<>() : got;
                        loaded = true;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        error = "Check your database connection, then try again.";
                    }
                    render();
                }
            }.execute();
        }

        void render() {
            int keep = scroll.getVerticalScrollBar().getValue();
            body.removeAll();

            List<Post> shown = new ArrayList<>();
            for (Post p : posts) {
                if (query.isEmpty() || has(p.getTitle()) || has(p.getDescription()) || has(p.getUsername())) {
                    shown.add(p);
                }
            }

            if (error != null && !loaded) {
                body.add(card(new EmptyState(Icons.Type.ALERT, "Unable to load " + name, error), 260));
                RoundedButton retry = new RoundedButton("Try again", RoundedButton.Style.OUTLINE);
                retry.addActionListener(e -> load());
                body.add(centered(retry));
            } else if (!loaded) {
                showSkeleton();
                return;
            } else if (shown.isEmpty()) {
                body.add(card(query.isEmpty()
                        ? new EmptyState(Icons.Type.SPARKLE, "Nothing here yet",
                                "No " + name + " posts yet. Be the first to share!")
                        : new EmptyState(Icons.Type.SEARCH, "No matching posts",
                                "Nothing in " + name + " matches your search."), 260));
                RoundedButton create = new RoundedButton("Create a post", RoundedButton.Style.PRIMARY);
                create.addActionListener(e -> navigator.accept("CREATE"));
                body.add(centered(create));
            } else {
                int n = Math.min(SHOWN, shown.size());
                boolean animate = !revealed;                    // only the first time this feed appears
                for (int i = 0; i < n; i++) {
                    JPanel wrapped = card(new PostCard(shown.get(i), userId), -1);
                    if (animate && i < 6) {
                        reveal(wrapped, i * 70);
                    }
                    body.add(wrapped);
                }
                revealed = true;
            }
            body.add(Box.createVerticalGlue());
            body.revalidate();
            body.repaint();
            // keep the place in the list when it was only refreshed
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(keep));
        }

        private boolean revealed;

        /** The first cards fade in and rise a little, one after another. */
        private void reveal(JPanel wrapper, int delayMs) {
            final long start = System.nanoTime() + delayMs * 1_000_000L;
            final double[] p = {0};
            wrapper.putClientProperty("reveal", p);
            javax.swing.Timer t = new javax.swing.Timer(16, null);
            t.addActionListener(e -> {
                double x = (System.nanoTime() - start) / 420e6;
                p[0] = Math.max(0, Math.min(1, x));
                if (p[0] >= 1) {
                    t.stop();
                    wrapper.putClientProperty("reveal", null);
                }
                wrapper.repaint();
            });
            t.start();
        }

        private boolean has(String v) {
            return v != null && v.toLowerCase(Locale.ROOT).contains(query);
        }

        private JPanel centered(JComponent c) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            row.setMaximumSize(new Dimension(780, 48));
            row.add(c);
            return row;
        }

        /** Same width limit and margin under every card, and never taller than its own content. */
        private JPanel card(JComponent c, int fixedHeight) {
            JPanel wrapper = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(780, fixedHeight > 0 ? fixedHeight : getPreferredSize().height);
                }

                @Override
                public void paint(Graphics g) {
                    Object r = getClientProperty("reveal");
                    if (!(r instanceof double[] p) || p[0] >= 1) {
                        super.paint(g);
                        return;
                    }
                    double e = 1 - Math.pow(1 - p[0], 3);          // ease out
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.translate(0, (1 - e) * 22);
                    g2.setComposite(AlphaComposite.SrcOver.derive((float) e));
                    super.paint(g2);
                    g2.dispose();
                }
            };
            wrapper.setOpaque(false);
            wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
            wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrapper.add(c, BorderLayout.CENTER);
            return wrapper;
        }
    }
}
