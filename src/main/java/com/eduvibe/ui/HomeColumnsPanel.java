package com.eduvibe.ui;

import com.eduvibe.model.Post;
import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.EmptyState;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.ui.components.SkeletonCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.HomeColumnsPanel  (NEW FILE)
 *
 * The Home page: two independent columns side by side, Academic on the left and Entertainment on the
 * right. Each column loads its own posts from the database (by the category stored with every post),
 * scrolls on its own, and has its own loading, empty and error state, so one can never disturb the other.
 * Reels have their own page and never appear here. Every card has the same width, margin and spacing.
 *
 * Search filters both columns. "See all" opens the full Academic / Entertainment page.
 */
public final class HomeColumnsPanel extends JPanel {

    private static final int SHOWN = 20;                 // newest posts per column; "See all" has the rest

    private final int userId;
    private final String fullName;
    private final PostService postService = new PostService();
    private Consumer<String> navigator = key -> { };
    private String query = "";

    private final Column academic;
    private final Column entertainment;
    private final RoundedButton refresh = new RoundedButton("Refresh", RoundedButton.Style.SECONDARY);

    public HomeColumnsPanel(int userId, String fullName) {
        super(new BorderLayout(0, 16));
        this.userId = userId;
        this.fullName = fullName == null ? "" : fullName;
        setOpaque(false);

        academic = new Column("Academic", "Learning, research and school life",
                Icons.Type.BOOK, new Color(59, 130, 246), new Color(6, 182, 212), "ACADEMIC");
        entertainment = new Column("Entertainment", "Memes, games, music and fun",
                Icons.Type.SPARKLE, new Color(139, 92, 246), new Color(236, 72, 153), "ENTERTAINMENT");

        add(buildTop(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.add(academic);
        columns.add(entertainment);
        add(columns, BorderLayout.CENTER);

        refresh.addActionListener(e -> loadPosts());
    }

    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator == null ? key -> { } : navigator;
    }

    /** Filters both columns by title, description or username. Empty clears the filter. */
    public void setSearchQuery(String q) {
        this.query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        academic.render();
        entertainment.render();
    }

    /** Loads both columns again, each on its own. */
    public void loadPosts() {
        refresh.setEnabled(false);
        academic.load();
        entertainment.load();
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(new GradientPaint(0, 0, new Color(79, 70, 229), getWidth(), getHeight(),
                        new Color(6, 182, 212)));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
                g2.dispose();
            }
        };
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(14, 22, 14, 18));
        top.setPreferredSize(new Dimension(100, 76));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Welcome back" + (fullName.isBlank() ? "" : ", " + firstName(fullName)));
        title.setFont(Theme.bold(20));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Your Academic and Entertainment feeds, side by side. Reels have their own page.");
        sub.setFont(Theme.regular(12.5f));
        sub.setForeground(new Color(255, 255, 255, 215));
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);

        RoundedButton create = new RoundedButton("Create post", RoundedButton.Style.SECONDARY);
        create.addActionListener(e -> navigator.accept("CREATE"));
        RoundedButton reels = new RoundedButton("Watch Reels", RoundedButton.Style.SECONDARY);
        reels.addActionListener(e -> navigator.accept("REELS"));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(reels);
        actions.add(create);
        actions.add(refresh);

        JPanel actionsWrap = new JPanel(new GridBagLayout());
        actionsWrap.setOpaque(false);
        actionsWrap.add(actions);

        top.add(text, BorderLayout.CENTER);
        top.add(actionsWrap, BorderLayout.EAST);
        return top;
    }

    private static String firstName(String full) {
        String t = full.trim();
        int space = t.indexOf(' ');
        return space > 0 ? t.substring(0, space) : t;
    }

    // ================================================================== one column

    /** A scrolling list that always matches the width of its viewport, so cards never overflow. */
    private static final class Body extends JPanel implements Scrollable {
        Body() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(0, 0, 0, 12));       // room for the scroll bar
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

    private final class Column extends JPanel {

        private final String category;          // "ACADEMIC" or "ENTERTAINMENT": the page "See all" opens
        private final Body body = new Body();
        private final JScrollPane scroll;
        private final JLabel count = new JLabel(" ");
        private List<Post> posts = new ArrayList<>();
        private int token;
        private String error;
        private boolean loaded;

        Column(String title, String subtitle, Icons.Type icon, Color a, Color b, String category) {
            super(new BorderLayout(0, 10));
            this.category = category;
            setOpaque(false);

            JPanel head = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.antialias(g2);
                    RoundRectangle2D.Float r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16);
                    g2.setColor(Theme.CARD);
                    g2.fill(r);
                    g2.setPaint(new GradientPaint(0, 0, a, 6, 0, a));
                    g2.fill(new RoundRectangle2D.Float(0, 10, 4, getHeight() - 20, 4, 4));
                    g2.setColor(Theme.BORDER);
                    g2.draw(r);
                    g2.dispose();
                }
            };
            head.setOpaque(false);
            head.setBorder(new EmptyBorder(10, 18, 10, 12));
            head.setPreferredSize(new Dimension(100, 62));

            JLabel iconLabel = new JLabel(Icons.icon(icon, 26, a));
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel t = new JLabel(title);
            t.setFont(Theme.bold(16));
            t.setForeground(Theme.TEXT);
            JLabel s = new JLabel(subtitle);
            s.setFont(Theme.regular(11.5f));
            s.setForeground(Theme.MUTED);
            text.add(t);
            text.add(s);

            count.setFont(Theme.bold(11.5f));
            count.setForeground(Theme.BLUE_LIGHT);

            RoundedButton all = new RoundedButton("See all", RoundedButton.Style.GHOST);
            all.addActionListener(e -> navigator.accept(category));

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            right.add(count);
            right.add(all);
            JPanel rightWrap = new JPanel(new GridBagLayout());
            rightWrap.setOpaque(false);
            rightWrap.add(right);

            JPanel left = new JPanel(new BorderLayout(10, 0));
            left.setOpaque(false);
            left.add(iconLabel, BorderLayout.WEST);
            left.add(text, BorderLayout.CENTER);

            head.add(left, BorderLayout.CENTER);
            head.add(rightWrap, BorderLayout.EAST);

            scroll = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(24);
            scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

            add(head, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            showSkeleton();
        }

        private void showSkeleton() {
            body.removeAll();
            for (int i = 0; i < 3; i++) {
                SkeletonCard card = new SkeletonCard();
                card.setPreferredSize(new Dimension(100, 170));
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
                    return "ACADEMIC".equals(category)
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
                        error = "Check your database connection, then press Refresh.";
                    }
                    finished();
                    render();
                }
            }.execute();
        }

        private void finished() {
            // the Refresh button comes back once both columns have answered
            if (!academic.isBusy() && !entertainment.isBusy()) {
                refresh.setEnabled(true);
            }
        }

        private boolean isBusy() {
            return !loaded && error == null;
        }

        void render() {
            body.removeAll();
            List<Post> shown = new ArrayList<>();
            for (Post p : posts) {
                if (query.isEmpty() || has(p.getTitle()) || has(p.getDescription()) || has(p.getUsername())) {
                    shown.add(p);
                }
            }
            count.setText(!loaded ? " " : shown.size() + (shown.size() == 1 ? " post" : " posts"));

            if (error != null && !loaded) {
                body.add(card(new EmptyState(Icons.Type.ALERT, "Unable to load " + categoryName(), error)));
            } else if (!loaded) {
                showSkeleton();
                return;
            } else if (shown.isEmpty()) {
                body.add(card(query.isEmpty()
                        ? new EmptyState(Icons.Type.SPARKLE, "Nothing here yet",
                                "No " + categoryName() + " posts yet. Be the first to share!")
                        : new EmptyState(Icons.Type.SEARCH, "No matching posts",
                                "Nothing in " + categoryName() + " matches your search.")));
            } else {
                int n = Math.min(SHOWN, shown.size());
                for (int i = 0; i < n; i++) {
                    body.add(card(new PostCard(shown.get(i), userId)));
                }
                if (shown.size() > n) {
                    RoundedButton more = new RoundedButton("See all " + shown.size() + " in " + categoryName(),
                            RoundedButton.Style.OUTLINE);
                    more.addActionListener(e -> navigator.accept(category));
                    JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                    row.setOpaque(false);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                    row.add(more);
                    body.add(row);
                }
            }
            body.add(Box.createVerticalGlue());
            body.revalidate();
            body.repaint();
            SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
        }

        private String categoryName() {
            return "ACADEMIC".equals(category) ? "Academic" : "Entertainment";
        }

        private boolean has(String v) {
            return v != null && v.toLowerCase(Locale.ROOT).contains(query);
        }

        /** Same margin under every card, whatever it holds, and never taller than its own content. */
        private JPanel card(JComponent c) {
            JPanel wrapper = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                }
            };
            wrapper.setOpaque(false);
            wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
            wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(c, BorderLayout.CENTER);
            return wrapper;
        }
    }
}
