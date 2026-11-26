package com.eduvibe.ui;

import com.eduvibe.dao.FollowDAO;
import com.eduvibe.model.Post;
import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.Avatar;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.ReelPlayer;
import com.eduvibe.ui.components.ReelPlayers;
import com.eduvibe.ui.components.ReelVideoView;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.Toast;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.ReelsPage  (NEW FILE)
 *
 * The TikTok-style Reels page. One reel fills the screen at a time; scroll (mouse wheel, touchpad,
 * arrow keys or the up / down buttons) to glide to the next or previous reel. The reel that arrives
 * plays automatically, the one you leave stops, and only one video ever plays. Tap the video to
 * pause / play; it loops. Like, comment, save, share, report and follow sit on the right, the caption
 * at the bottom, For You / Following tabs on top and a navigation bar below.
 *
 * Only the current reel plays and only the next one is preloaded; the rest are released, so scrolling
 * stays light on a small laptop. More reels are fetched as you near the end and are added without
 * replacing the ones already seen. Your place is kept when you leave the page and come back.
 *
 * Like / save / comment / share / report reuse the logic of PostCard (through a hidden card per reel),
 * so notifications, points and duplicate checks behave exactly as everywhere else.
 */
public final class ReelsPage extends JPanel {

    private static final int NAV_H = 54;
    private static final int TABS_H = 52;
    private static final int ANIM_MS = 280;
    private static final int MORE_COOLDOWN_MS = 15000;

    private enum Tab { FOR_YOU, FOLLOWING }

    private final int userId;
    private final PostService postService = new PostService();
    private final FollowDAO followDAO = new FollowDAO();
    private Consumer<String> navigator = key -> { };

    private final List<Post> pool = new ArrayList<>();          // everything loaded for this tab, in order
    private final Set<Integer> known = new HashSet<>();         // ids already in the pool (no duplicates)
    private final Map<Integer, Slide> slides = new HashMap<>();

    private Tab tab = Tab.FOR_YOU;
    private String query = "";
    private int index;
    private Slide current;

    private boolean loading;
    private boolean loadedOnce;
    private boolean fetchingMore;
    private long lastFetch;
    private String loadError;
    private int loadToken;

    // transition
    private boolean animating;
    private Slide outgoing;
    private Slide incoming;
    private int direction;
    private long animStart;
    private int bounceOffset;
    private final javax.swing.Timer animTimer = new javax.swing.Timer(15, e -> tick());

    // gesture handling for the wheel / touchpad
    private long lastWheelEvent;
    private long lastWheelAccepted;

    private final JPanel timerHost = new JPanel(new BorderLayout());
    private final Stage stage = new Stage();
    private final TabsBar tabsBar = new TabsBar();
    private final NavBar navBar = new NavBar();
    private final RoundButton upButton = new RoundButton(true);
    private final RoundButton downButton = new RoundButton(false);
    private final MessageBox messageBox = new MessageBox();
    private final Pill pill = new Pill();
    private final SidePanel sidePanel = new SidePanel();
    private final InfoCard infoCard = new InfoCard();
    private boolean commentsOpen;

    // ambient colours that follow the reel in focus
    private java.awt.Color accentA = Theme.BLUE, accentB = Theme.VIOLET;
    private java.awt.Color fromA = Theme.BLUE, fromB = Theme.VIOLET;
    private java.awt.Color toA = Theme.BLUE, toB = Theme.VIOLET;
    private long accentStart;
    private final javax.swing.Timer ambientTimer = new javax.swing.Timer(60, e -> {
        stage.repaintCentre();
        if (System.currentTimeMillis() - accentStart > 760) {
            ((javax.swing.Timer) e.getSource()).stop();      // still background: no repaints at all
        }
    });

    private final String category;                // "academic" or "entertainment": this page shows only that

    public ReelsPage(int userId, String category) {
        super(new BorderLayout());
        if (!"academic".equals(category) && !"entertainment".equals(category)) {
            throw new IllegalArgumentException("Reels need a category: academic or entertainment.");
        }
        this.category = category;
        this.userId = userId;
        setBackground(Theme.BACKGROUND);

        timerHost.setOpaque(false);
        add(timerHost, BorderLayout.NORTH);
        add(stage, BorderLayout.CENTER);

        stage.add(tabsBar, JLayeredPane.PALETTE_LAYER);
        stage.add(navBar, JLayeredPane.PALETTE_LAYER);
        stage.add(upButton, JLayeredPane.PALETTE_LAYER);
        stage.add(downButton, JLayeredPane.PALETTE_LAYER);
        stage.add(pill, JLayeredPane.PALETTE_LAYER);
        stage.add(messageBox, JLayeredPane.PALETTE_LAYER);
        stage.add(sidePanel, JLayeredPane.MODAL_LAYER);
        stage.add(infoCard, JLayeredPane.PALETTE_LAYER);

        upButton.setAction(() -> goTo(index - 1));
        downButton.setAction(() -> goTo(index + 1));

        MouseWheelListener wheel = this::onWheel;
        stage.addMouseWheelListener(wheel);

        installKeys();
        Perf.onSlow = () -> {
            if (!perfMode && isShowing()) {
                pill.show("Reels running slowly?  Press P for light mode", 5000);
            }
        };

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                announceEngine(false);                     // log only: no message over the video
                onShown();
            } else {
                onHidden();
            }
        });
        updateMessage();
    }

    private static boolean engineLogged;

    /** Says which video engine is in use (on screen for a moment, and once per run in eduvibe-video.log). */
    private void announceEngine(boolean showPill) {
        boolean ff = ReelPlayers.usesFfmpeg();
        String name = ff ? "light (ffmpeg)" : "JavaFX";
        String why = ff ? "" : (com.eduvibe.util.VideoConverter.isAvailable() ? " (switched off with V)" : " (ffmpeg not found)");
        if (showPill) {
            pill.show("Video engine: " + name + why, 3500);
        }
        if (!engineLogged) {
            engineLogged = true;
            try {
                java.nio.file.Files.writeString(java.nio.file.Paths.get(System.getProperty("user.dir"), "eduvibe-video.log"),
                        java.time.LocalDateTime.now() + "  reel-engine  " + name + why + "  ffmpeg="
                                + com.eduvibe.util.VideoConverter.command()
                                + "  cpus=" + Runtime.getRuntime().availableProcessors()
                                + "  maxHeapMB=" + Runtime.getRuntime().maxMemory() / 1048576
                                + "  java=" + System.getProperty("java.version")
                                + "  os=" + System.getProperty("os.name")
                                + "  scale=" + java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                                        .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX()
                                + "  d3d=" + System.getProperty("sun.java2d.d3d")
                                + "  opengl=" + System.getProperty("sun.java2d.opengl") + "\n",
                        java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {
                // the log is a bonus
            }
        }
    }

    // ------------------------------------------------------------------ outside API

    /** Lets the bottom bar and the empty states open other pages. */
    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator == null ? key -> { } : navigator;
    }

    /** The slim entertainment timer strip (same one the old Reels feed had). */
    // ---- hooks for the floating category bar that sits on top of this page (ReelsHost) ----
    private int tabsDrop;                                  // how far the Following | For You tabs are pushed down
    private java.util.function.IntConsumer scrollListener = d -> { };
    private Runnable layoutListener = () -> { };

    /** Pushes the Following | For You tabs down so the floating bar can sit above them. */
    public void setTabsDrop(int px) {
        if (px != tabsDrop) {
            tabsDrop = px;
            stage.relayoutTabs();
        }
    }

    /** Told (+1 / -1) each time the viewer moves to the next / previous reel. */
    public void setScrollListener(java.util.function.IntConsumer l) {
        this.scrollListener = l == null ? d -> { } : l;
    }

    /** Told whenever the reel's frame is placed, so the floating bar can centre itself over it. */
    public void setLayoutListener(Runnable l) {
        this.layoutListener = l == null ? () -> { } : l;
    }

    /** Horizontal centre of the reel on screen, in this page's own coordinates. */
    public int frameCenterX() {
        return stage.frameCenterX();
    }

    public void setTimerBar(JComponent bar) {
        timerHost.removeAll();
        if (bar != null) {
            timerHost.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            timerHost.add(bar, BorderLayout.CENTER);
        }
        timerHost.revalidate();
    }

    /** Top-bar search: shows only reels that match. */
    public void setSearchQuery(String text) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (q.equals(query)) {
            return;
        }
        query = q;
        resetToStart();
    }

    // ------------------------------------------------------------------ page lifecycle

    private void onShown() {
        if (!loadedOnce) {
            loadFeed();
            return;
        }
        // back on the page: carry on where the viewer left off
        if (current != null) {
            stage.addSlide(current);
            stage.placeSlides();
            playSoon(current);
            preloadNext();
            focusChanged();
        } else if (!view().isEmpty()) {
            showImmediate(Math.min(index, view().size() - 1));
        }
        updateMessage();
        fetchMoreInBackground(true);
    }

    private void onHidden() {
        playTimer.stop();
        preloadTimer.stop();
        ambientTimer.stop();
        animTimer.stop();
        if (animating) {
            finishAnimation();
        }
        for (Slide s : slides.values()) {
            s.pause();
            s.release();
        }
        pill.hideNow();
    }

    // ------------------------------------------------------------------ data

    private List<Post> view() {
        if (query.isEmpty()) {
            return pool;
        }
        List<Post> out = new ArrayList<>();
        for (Post p : pool) {
            if (contains(p.getTitle()) || contains(p.getDescription()) || contains(p.getUsername())) {
                out.add(p);
            }
        }
        return out;
    }

    private boolean contains(String text) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<Post> fetch(Tab forTab) throws Exception {
        boolean academic = "academic".equals(category);
        return forTab == Tab.FOR_YOU
                ? (academic ? postService.getAcademicReels(userId) : postService.getEntertainmentReels(userId))
                : postService.getFollowingReels(userId, category);
    }

    /** Fresh start for the current tab. */
    private void loadFeed() {
        final int token = ++loadToken;
        final Tab forTab = tab;
        loading = true;
        loadError = null;
        updateMessage();

        new SwingWorker<List<Post>, Void>() {
            @Override
            protected List<Post> doInBackground() throws Exception {
                return fetch(forTab);
            }

            @Override
            protected void done() {
                if (token != loadToken) {
                    return;
                }
                loading = false;
                try {
                    List<Post> got = get();
                    dropAllSlides();
                    pool.clear();
                    known.clear();
                    for (Post p : got) {
                        if (known.add(p.getId())) {
                            pool.add(p);
                        }
                    }
                    loadedOnce = true;
                    lastFetch = System.currentTimeMillis();
                    index = 0;
                    if (!view().isEmpty() && isShowing()) {
                        showImmediate(0);
                    }
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    c.printStackTrace();
                    loadError = "Check that the database is running, then try again.";
                }
                updateMessage();
            }
        }.execute();
    }

    /**
     * Fetches again in the background. Reels never seen before are added: newer ones before what is
     * already loaded (your position is kept), the rest at the end. Nothing is replaced.
     */
    private void fetchMoreInBackground(boolean force) {
        long now = System.currentTimeMillis();
        if (fetchingMore || loading || (!force && now - lastFetch < MORE_COOLDOWN_MS)) {
            return;
        }
        fetchingMore = true;
        lastFetch = now;
        final Tab forTab = tab;
        final int token = loadToken;
        if (!force) {
            pill.show("Loading more reels...", 0);
        }
        new SwingWorker<List<Post>, Void>() {
            @Override
            protected List<Post> doInBackground() throws Exception {
                return fetch(forTab);
            }

            @Override
            protected void done() {
                fetchingMore = false;
                if (token != loadToken || forTab != tab) {
                    return;
                }
                try {
                    int added = merge(get());
                    if (!force) {
                        if (added > 0) {
                            pill.hideNow();
                        } else {
                            pill.show("You're all caught up", 1800);
                        }
                    }
                } catch (Exception ignored) {
                    pill.hideNow();
                }
            }
        }.execute();
    }

    private int merge(List<Post> fetched) {
        Post keep = currentPost();
        List<Post> front = new ArrayList<>();
        List<Post> back = new ArrayList<>();
        boolean reachedKnown = false;
        for (Post p : fetched) {
            if (known.contains(p.getId())) {
                reachedKnown = true;
                continue;
            }
            (reachedKnown ? back : front).add(p);
        }
        for (Post p : front) {
            known.add(p.getId());
        }
        for (Post p : back) {
            known.add(p.getId());
        }
        pool.addAll(0, front);
        pool.addAll(back);

        if (keep != null) {
            int at = indexOfId(view(), keep.getId());
            index = Math.max(0, at);
        } else if (!view().isEmpty() && isShowing()) {
            index = 0;
            showImmediate(0);
        }
        updateMessage();
        return front.size() + back.size();
    }

    private static int indexOfId(List<Post> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private Post currentPost() {
        List<Post> v = view();
        return current != null && index >= 0 && index < v.size() ? v.get(index) : null;
    }

    private void switchTab(Tab t) {
        if (t == tab) {
            return;
        }
        tab = t;
        loadToken++;
        animTimer.stop();
        animating = false;
        dropAllSlides();
        pool.clear();
        known.clear();
        index = 0;
        loadedOnce = false;
        tabsBar.repaint();
        loadFeed();
    }

    private void resetToStart() {
        animTimer.stop();
        animating = false;
        dropAllSlides();
        index = 0;
        if (loadedOnce && !view().isEmpty() && isShowing()) {
            showImmediate(0);
        }
        updateMessage();
    }

    // ------------------------------------------------------------------ pager

    private Slide slideFor(Post p) {
        Slide s = slides.get(p.getId());
        if (s == null) {
            s = new Slide(p);
            slides.put(p.getId(), s);
        }
        return s;
    }

    private void showImmediate(int target) {
        List<Post> v = view();
        if (v.isEmpty()) {
            return;
        }
        index = Math.max(0, Math.min(v.size() - 1, target));
        if (current != null) {
            current.pause();
            stage.removeSlide(current);
        }
        current = slideFor(v.get(index));
        stage.addSlide(current);
        stage.placeSlides();
        playSoon(current);
        preloadNext();
        prune();
        focusChanged();
        updateMessage();
        if (index >= v.size() - 2) {
            fetchMoreInBackground(false);
        }
    }

    private void goTo(int target) {
        List<Post> v = view();
        if (v.isEmpty() || animating || current == null) {
            return;
        }
        if (target < 0 || target >= v.size()) {
            bounce(target < 0 ? 1 : -1);
            if (target >= v.size()) {
                fetchMoreInBackground(false);
                pill.show("That's the last reel for now", 1600);
            }
            return;
        }
        if (target == index) {
            return;
        }
        direction = target > index ? 1 : -1;
        scrollListener.accept(direction);
        outgoing = current;
        incoming = slideFor(v.get(target));
        index = target;

        outgoing.pause();                 // the one you leave stops at once
        stage.addSlide(incoming);
        incoming.setOffset(direction * Math.max(1, stage.slideHeight()));
        stage.placeSlides();
        playSoon(incoming);               // the one that arrives starts playing

        animating = true;
        animStart = System.currentTimeMillis();
        animTimer.start();
    }

    private void tick() {
        if (bounceOffset != 0 && !animating) {
            return;
        }
        double t = Math.min(1, (System.currentTimeMillis() - animStart) / (double) ANIM_MS);
        double eased = 1 - Math.pow(1 - t, 3);
        int h = Math.max(1, stage.slideHeight());
        if (animating) {
            outgoing.setOffset((int) Math.round(-direction * h * eased));
            incoming.setOffset((int) Math.round(direction * h * (1 - eased)));
            stage.placeSlides();
            if (t >= 1) {
                finishAnimation();
            }
        }
    }

    private void finishAnimation() {
        animTimer.stop();
        if (!animating) {
            return;
        }
        animating = false;
        if (outgoing != null) {
            stage.removeSlide(outgoing);
            outgoing.setOffset(0);
            outgoing.release();           // frees its player (preloaded neighbours are made again on demand)
        }
        current = incoming;
        current.setOffset(0);
        outgoing = null;
        incoming = null;
        stage.placeSlides();
        preloadNext();
        prune();
        focusChanged();
        List<Post> v = view();
        if (index >= v.size() - 2) {
            fetchMoreInBackground(false);
        }
        updateMessage();
    }

    /** The reel in focus changed: ambient colours, the creator card and the side panel follow it. */
    private void focusChanged() {
        Slide s = current;
        if (s != null) {
            long now = System.currentTimeMillis();
            double t = Math.min(1, (now - accentStart) / 700.0);
            fromA = mix(fromA, toA, t);
            fromB = mix(fromB, toB, t);
            java.awt.Color[] c = accentFor(s.post);
            toA = c[0];
            toB = c[1];
            accentStart = now;
        }
        infoCard.bind(s);
        sidePanel.refresh();
        stage.placeSlides();
        if (!ambientTimer.isRunning()) {
            ambientTimer.start();
        }
    }

    private static java.awt.Color[] accentFor(Post p) {
        return "academic".equalsIgnoreCase(p.getCategory())
                ? new java.awt.Color[] {new java.awt.Color(59, 130, 246), new java.awt.Color(34, 211, 238)}
                : new java.awt.Color[] {new java.awt.Color(139, 92, 246), new java.awt.Color(236, 72, 153)};
    }

    private static java.awt.Color mix(java.awt.Color a, java.awt.Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new java.awt.Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Jumps to any reel (the next / previous one glide, the rest cut straight to it). */
    private void jumpTo(int target) {
        List<Post> v = view();
        if (animating || target < 0 || target >= v.size() || target == index) {
            return;
        }
        if (Math.abs(target - index) == 1) {
            goTo(target);
        } else {
            showImmediate(target);
        }
    }

    private void showComments() {
        commentsOpen = true;
        sidePanel.showComments();
        stage.placeSlides();
    }

    private void hideComments() {
        commentsOpen = false;
        sidePanel.showNext();
        stage.placeSlides();
    }

    /** A small nudge when you try to go past the first or last reel. */
    private void bounce(int dir) {
        if (current == null || animating) {
            return;
        }
        final long start = System.currentTimeMillis();
        final javax.swing.Timer t = new javax.swing.Timer(15, null);
        t.addActionListener(e -> {
            double p = Math.min(1, (System.currentTimeMillis() - start) / 260.0);
            double wave = Math.sin(Math.PI * p);
            if (current != null) {
                current.setOffset((int) Math.round(dir * -26 * wave));
                stage.placeSlides();
            }
            if (p >= 1) {
                t.stop();
                if (current != null) {
                    current.setOffset(0);
                    stage.placeSlides();
                }
            }
        });
        t.start();
    }

    /** Only the next reel is prepared ahead of time. */
    // Fast scrolling: a reel that is only passed through is never started or prepared, so nothing is
    // built, decoded or converted for it. Work starts once the scrolling settles.
    private Slide pendingPlay;
    private final javax.swing.Timer playTimer = new javax.swing.Timer(220, e -> {
        Slide s = pendingPlay;
        pendingPlay = null;
        if (s != null && (s == current || s == incoming) && isShowing()) {
            s.play();
        }
    });
    private final javax.swing.Timer preloadTimer = new javax.swing.Timer(450, e -> {
        List<Post> v = view();
        int next = index + 1;
        if (next < v.size() && isShowing() && !animating) {
            slideFor(v.get(next)).prepare();
        }
    });

    {
        playTimer.setRepeats(false);
        preloadTimer.setRepeats(false);
    }

    private void playSoon(Slide s) {
        if (s == null) {
            return;
        }
        if (s.isReadyToPlay()) {
            pendingPlay = null;
            playTimer.stop();
            s.play();                      // already prepared: starts at once
            return;
        }
        pendingPlay = s;
        playTimer.restart();
    }

    private void preloadNext() {
        preloadTimer.restart();
    }

    /** Keeps the current reel and its neighbours; everything else is released and forgotten. */
    private void prune() {
        List<Post> v = view();
        Set<Integer> keep = new HashSet<>();
        for (int i = index - 1; i <= index + 1; i++) {
            if (i >= 0 && i < v.size()) {
                keep.add(v.get(i).getId());
            }
        }
        List<Integer> drop = new ArrayList<>();
        for (Map.Entry<Integer, Slide> e : slides.entrySet()) {
            if (!keep.contains(e.getKey()) && e.getValue() != current) {
                drop.add(e.getKey());
            }
        }
        for (Integer id : drop) {
            Slide s = slides.remove(id);
            stage.removeSlide(s);
            s.release();
        }
        // the previous reel does not need a live player while you are on the current one
        List<Post> vv = view();
        if (index - 1 >= 0 && index - 1 < vv.size()) {
            Slide prev = slides.get(vv.get(index - 1).getId());
            if (prev != null && prev != current) {
                prev.release();
            }
        }
    }

    private void dropAllSlides() {
        for (Slide s : slides.values()) {
            stage.removeSlide(s);
            s.release();
        }
        slides.clear();
        current = null;
        outgoing = null;
        incoming = null;
        stage.repaint();
    }

    // ------------------------------------------------------------------ input

    private void onWheel(MouseWheelEvent e) {
        double precise = e.getPreciseWheelRotation();
        if (precise == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long gap = now - lastWheelEvent;
        lastWheelEvent = now;
        // a touchpad swipe sends a stream of events; only the start of a swipe moves one reel
        if (animating || gap <= 120 || now - lastWheelAccepted < 250) {
            return;
        }
        lastWheelAccepted = now;
        goTo(index + (precise > 0 ? 1 : -1));
    }

    private void installKeys() {
        bind("UP", "reelPrev", () -> goTo(index - 1));
        bind("K", "reelPrev2", () -> goTo(index - 1));
        bind("PAGE_UP", "reelPrev3", () -> goTo(index - 1));
        bind("DOWN", "reelNext", () -> goTo(index + 1));
        bind("J", "reelNext2", () -> goTo(index + 1));
        bind("PAGE_DOWN", "reelNext3", () -> goTo(index + 1));
        bind("SPACE", "reelToggle", () -> {
            if (current != null) {
                current.toggle();
            }
        });
        bind("M", "reelMute", this::toggleMute);
        bind("P", "reelPerf", this::togglePerformance);
        bind("V", "reelEngine", this::toggleVideoEngine);
        bind("D", "reelDebug", this::toggleDebug);
        bind("L", "reelLike", () -> {
            if (current != null) {
                current.like();
            }
        });
    }

    private void bind(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) {
                    return;
                }
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus instanceof JTextComponent) {
                    return;               // typing in the search box must not move the reels
                }
                action.run();
            }
        });
    }

    /** Performance mode: only the reel and the bottom bar, flat background. The lightest the page can be. */
    private boolean perfMode = loadPerf();

    private static boolean loadPerf() {
        try {
            return java.util.prefs.Preferences.userRoot().node("eduvibe/ui").getBoolean("reelsPerformance", false);
        } catch (Exception e) {
            return false;
        }
    }

    private void togglePerformance() {
        perfMode = !perfMode;
        try {
            java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("eduvibe/ui");
            p.putBoolean("reelsPerformance", perfMode);
            p.flush();
        } catch (Exception ignored) {
            // the switch still works for this session
        }
        stage.placeSlides();
        pill.show(perfMode ? "Performance mode on  -  press P to turn it off"
                : "Performance mode off", 2200);
    }

    /** V: switches between the light ffmpeg video engine and the JavaFX one. Applies to the next reels opened. */
    private void toggleVideoEngine() {
        if (!com.eduvibe.util.VideoConverter.isAvailable()) {
            pill.show("ffmpeg was not found, so the JavaFX video engine is used", 4000);
            return;
        }
        boolean ff = ReelPlayers.toggle();
        pill.show(ff ? "Video engine: light (ffmpeg). Close and reopen Reels to apply."
                : "Video engine: JavaFX. Close and reopen Reels to apply.", 5000);
    }

    private javax.swing.Timer debugTimer;

    /** D: shows playback numbers on screen (frames per second, skipped frames, decode time). Send me a photo. */
    private void toggleDebug() {
        if (debugTimer != null) {
            debugTimer.stop();
            debugTimer = null;
            pill.show("Diagnostics off", 1200);
            return;
        }
        debugTimer = new javax.swing.Timer(1000, e -> {
            if (!isShowing()) {
                return;
            }
            String info = current != null && current.video != null
                    ? current.video.debugInfo() : "no video on this reel";
            pill.show(info + " | scale " + java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX(), 1100);
        });
        debugTimer.setInitialDelay(0);
        debugTimer.start();
    }

    private void toggleMute() {
        boolean now = !ReelVideoView.isMuted();
        for (Slide s : slides.values()) {
            s.setMuted(now);
        }
        tabsBar.repaint();
    }

    // ------------------------------------------------------------------ empty / loading / error

    private void updateMessage() {
        List<Post> v = view();
        boolean showMessage = v.isEmpty() || (loading && current == null);
        if (!showMessage) {
            messageBox.setVisible(false);
            upButton.setVisible(true);
            downButton.setVisible(true);
            upButton.setEnabled(index > 0);
            downButton.setEnabled(index < v.size() - 1);
            stage.revalidate();
            return;
        }
        upButton.setVisible(false);
        downButton.setVisible(false);
        if (loading) {
            messageBox.set("Loading reels...", "Getting the latest videos.", null, null);
        } else if (loadError != null) {
            messageBox.set("Couldn't load reels", loadError, "Try again", this::loadFeed);
        } else if (!query.isEmpty()) {
            messageBox.set("No reels match your search", "Try another word.", null, null);
        } else if (tab == Tab.FOLLOWING) {
            messageBox.set("Nothing here yet", "Follow people to see their reels here.",
                    "Find people", () -> navigator.accept("USER_SEARCH"));
        } else {
            messageBox.set("No " + ("academic".equals(category) ? "Academic" : "Entertainment") + " reels yet", "Be the first to share one.",
                    "Create a reel", () -> navigator.accept("CREATE"));
        }
        messageBox.setVisible(true);
        stage.revalidate();
        stage.repaint();
    }

    // ================================================================== stage

    /** The area that holds the reel column (centred, phone-shaped), the tabs, the bar and the buttons. */
    private final class Stage extends JLayeredPane {

        private Rectangle frame = new Rectangle();      // where the reel in focus sits
        private Rectangle leftRect = new Rectangle();
        private Rectangle rightRect = new Rectangle();
        private int areaX0, areaX1, navX, navW;
        private boolean leftVisible, sideVisible;

        Stage() {
            setOpaque(true);
            setBackground(Theme.BACKGROUND);
        }

        int slideHeight() {
            return Math.max(1, getHeight() - NAV_H);
        }

        void addSlide(Slide s) {
            if (s.getParent() != this) {
                add(s, JLayeredPane.DEFAULT_LAYER);
            }
        }

        void removeSlide(Slide s) {
            if (s != null && s.getParent() == this) {
                remove(s);
                repaint();
            }
        }

        /** Splits the width: creator card | reel | side panel. Small windows drop the card, then dock less. */
        private void computeAreas() {
            int w = getWidth(), h = getHeight(), pad = 14;
            leftVisible = w >= 1040;
            sideVisible = w >= 700;
            int leftW = leftVisible ? 236 : 0;
            int rightW = w >= 1040 ? 360 : Math.max(290, Math.min(340, w / 3 + 20));
            leftRect = leftVisible ? new Rectangle(pad, 12, leftW, h - 24) : new Rectangle();
            if (sideVisible) {
                rightRect = new Rectangle(w - pad - rightW, 12, rightW, h - 24);
            } else if (commentsOpen) {
                int ow = Math.min(330, Math.max(240, w - 2 * pad));
                rightRect = new Rectangle(w - pad - ow, 12, ow, h - 24);     // a drawer over the reel
            } else {
                rightRect = new Rectangle();
            }
            areaX0 = leftVisible && !perfMode ? leftRect.x + leftRect.width + 18 : pad;
            areaX1 = sideVisible && (!perfMode || commentsOpen) ? rightRect.x - 18 : w - pad;
        }

        /**
         * The reel's frame hugs the video's own proportions, so the whole picture is visible with no
         * cropping and no empty bars. It is centred between the creator card and the side panel.
         */
        Rectangle frameFor(double ratio) {
            int maxW = Math.max(260, areaX1 - areaX0 - 62);        // 62: room for the up / down buttons
            int maxH = Math.max(240, getHeight() - NAV_H - 12);
            double r = ratio > 0 ? Math.max(0.35, Math.min(3.0, ratio)) : 9.0 / 16;
            int fw, fh;
            if (maxW / (double) maxH > r) {
                fh = maxH;
                fw = (int) Math.round(fh * r);
            } else {
                fw = maxW;
                fh = (int) Math.round(fw / r);
            }
            return new Rectangle(areaX0 + (maxW - fw) / 2, 6 + (maxH - fh) / 2, Math.max(1, fw), Math.max(1, fh));
        }

        void relayoutTabs() {
            tabsBar.setBounds(frame.x, frame.y + tabsDrop, frame.width, TABS_H);
            tabsBar.repaint();
        }

        int frameCenterX() {
            return frame.width > 1 ? frame.x + frame.width / 2 : getWidth() / 2;
        }

        void placeSlides() {
            computeAreas();
            Slide focus = animating && incoming != null ? incoming : current;
            frame = frameFor(focus == null ? 0 : focus.ratio());
            for (Component c : getComponents()) {
                if (c instanceof Slide s) {
                    Rectangle f = frameFor(s.ratio());
                    s.setBounds(f.x, f.y + s.offset, f.width, f.height);
                    s.setVisible(true);
                }
            }
            layoutOverlays();
            repaint();
        }

        private void layoutOverlays() {
            int h = getHeight();
            boolean hasReel = current != null && !messageBox.isVisible();
            navW = Math.max(380, Math.min(520, frame.width));
            navX = Math.max(areaX0 - 10, frame.x + frame.width / 2 - navW / 2);
            tabsBar.setBounds(frame.x, frame.y + tabsDrop, frame.width, TABS_H);
            navBar.setBounds(navX, h - NAV_H, navW, NAV_H);

            int ax = frame.x + frame.width + 16;
            boolean room = hasReel && ax + 46 <= areaX1 + 62 && !(commentsOpen && !sideVisible);
            int cy = frame.y + frame.height / 2;
            upButton.setBounds(ax, cy - 54, 46, 46);
            downButton.setBounds(ax, cy + 8, 46, 46);
            upButton.setVisible(room);
            downButton.setVisible(room);

            pill.setBounds(navX, h - NAV_H - 44, navW, 30);
            messageBox.setBounds(areaX0, TABS_H, Math.max(1, areaX1 - areaX0), Math.max(1, h - TABS_H - NAV_H));

            boolean showSide = hasReel && rightRect.width > 0 && (sideVisible || commentsOpen) && (!perfMode || commentsOpen);
            sidePanel.setVisible(showSide);
            if (showSide) {
                sidePanel.setBounds(rightRect);
            }
            boolean showLeft = hasReel && leftVisible && !perfMode;
            infoCard.setVisible(showLeft);
            if (showLeft) {
                infoCard.setBounds(leftRect);
            }
            layoutListener.run();
        }

        @Override
        public void doLayout() {
            placeSlides();
        }

        /** Repaints only the middle (the glow behind the reel); the side cards do not need it. */
        void repaintCentre() {
            if (isShowing()) {
                repaint(Math.max(0, areaX0 - 18), 0, Math.max(1, areaX1 - areaX0 + 36), getHeight());
            }
        }

        private java.awt.image.BufferedImage bg;
        private int bgW, bgH, bgA, bgB;
        private java.awt.image.BufferedImage full;
        private String fullKey = "";

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();

            // colours slide from the previous reel's to the current reel's
            double t = Math.min(1, (System.currentTimeMillis() - accentStart) / 700.0);
            java.awt.Color a = mix(fromA, toA, t);
            java.awt.Color b = mix(fromB, toB, t);

            // The glow is drawn small and stretched: smooth, and cheap enough for a small laptop.
            int sw = Math.max(1, w / 4), sh = Math.max(1, h / 4);
            if (bg == null || bgW != sw || bgH != sh || bgA != a.getRGB() || bgB != b.getRGB()) {
                if (bg == null || bgW != sw || bgH != sh) {
                    bg = new java.awt.image.BufferedImage(sw, sh, java.awt.image.BufferedImage.TYPE_INT_RGB);
                }
                bgW = sw;
                bgH = sh;
                bgA = a.getRGB();
                bgB = b.getRGB();
                Graphics2D bgG = bg.createGraphics();
                bgG.setColor(Theme.BACKGROUND);
                bgG.fillRect(0, 0, sw, sh);
                boolean light = Theme.BACKGROUND.getRed() > 128;
                glow(bgG, (int) (sw * 0.28), (int) (sh * 0.28), (int) (Math.max(sw, sh) * 0.62), a, light ? 70 : 95);
                glow(bgG, (int) (sw * 0.80), (int) (sh * 0.82), (int) (Math.max(sw, sh) * 0.58), b, light ? 60 : 85);
                bgG.dispose();
            }
            if (perfMode || com.eduvibe.ui.components.Aurora.isReduced()) {      // "reduce effects" is on: a flat background
                g2.setColor(Theme.BACKGROUND);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
                return;
            }
            // background + halo are put together once at full size, so each repaint is a plain copy
            boolean hasHalo = current != null && frame.width > 1;
            String key = w + "x" + h + "/" + bgA + "/" + bgB + "/" + (hasHalo ? frame.x + "," + frame.y + "," + frame.width + "," + frame.height : "-");
            if (full == null || full.getWidth() != w || full.getHeight() != h || !key.equals(fullKey)) {
                if (full == null || full.getWidth() != w || full.getHeight() != h) {
                    full = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
                }
                fullKey = key;
                Graphics2D fg = full.createGraphics();
                fg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                fg.drawImage(bg, 0, 0, w, h, null);
                if (hasHalo) {
                    Theme.antialias(fg);
                    for (int i = 6; i >= 1; i--) {
                        fg.setColor(Theme.alpha(a, 6 + (6 - i) * 5));
                        fg.fill(new RoundRectangle2D.Float(frame.x - i * 4, frame.y - i * 4,
                                frame.width + i * 8, frame.height + i * 8, 24 + i * 6, 24 + i * 6));
                    }
                }
                fg.dispose();
            }
            g2.drawImage(full, 0, 0, null);
            g2.dispose();
        }

        private void glow(Graphics2D g2, int cx, int cy, int radius, java.awt.Color c, int alpha) {
            RadialGradientPaint paint = new RadialGradientPaint(new java.awt.geom.Point2D.Float(cx, cy), Math.max(1, radius),
                    new float[] {0f, 1f}, new java.awt.Color[] {Theme.alpha(c, alpha), Theme.alpha(c, 0)});
            g2.setPaint(paint);
            g2.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);
        }
    }

    // ================================================================== one reel

    private final class Slide extends JPanel {

        final Post post;
        final PostCard card;
        final ReelPlayer video;        // null for a post that has no video
        final JComponent media;           // the video view, or a still picture
        final RailButton likeB, commentB, saveB, shareB, reportB, downloadB, moveB;
        final JPanel author = new JPanel(null);
        final FollowBadge badge;
        final Avatar avatar = new Avatar(46);
        final Caption caption;
        final JComponent shade = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new java.awt.Color(0, 0, 0, 130), 0, 90,
                        new java.awt.Color(0, 0, 0, 0)));
                g2.fillRect(0, 0, w, 90);
                int from = (int) (h * 0.50);
                g2.setPaint(new GradientPaint(0, from, new java.awt.Color(0, 0, 0, 0), 0, h,
                        new java.awt.Color(0, 0, 0, 215)));
                g2.fillRect(0, from, w, h - from);
                // the progress bar goes on top of the shading, so it stays clean white
                ReelPlayer v = video;
                double pr = v == null ? -1 : v.progressNow();
                if (pr >= 0) {
                    int bh = v.scrubbingNow() ? 6 : 3;
                    g2.setColor(new java.awt.Color(255, 255, 255, 80));
                    g2.fillRect(0, h - bh, w, bh);
                    g2.setColor(java.awt.Color.WHITE);
                    g2.fillRect(0, h - bh, (int) Math.round(w * pr), bh);
                }
                g2.dispose();
            }

            @Override
            public boolean contains(int x, int y) {
                return false;                       // clicks go through to the video
            }
        };
        int offset;
        boolean followed;
        private long burstStart;
        private Point burstAt = new Point();
        private final javax.swing.Timer burstTimer = new javax.swing.Timer(16, e -> {
            if (System.currentTimeMillis() - burstStart > 650) {
                ((javax.swing.Timer) e.getSource()).stop();
            }
            Slide.this.burst.repaint();
        });
        final JComponent burst = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                double t = (System.currentTimeMillis() - burstStart) / 650.0;
                if (t < 0 || t > 1) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                double pop = t < 0.3 ? 0.4 + 0.9 * (t / 0.3) : 1.3 - 0.3 * ((t - 0.3) / 0.7);
                int size = (int) (96 * pop);
                int rise = (int) (60 * t);
                float alpha = (float) (t < 0.6 ? 1 : 1 - (t - 0.6) / 0.4);
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
                Icons.paint(g2, Icons.Type.HEART_FILLED, burstAt.x - size / 2, burstAt.y - size / 2 - rise, size, Theme.RED);
                g2.dispose();
            }

            @Override
            public boolean contains(int x, int y) {
                return false;
            }
        };

        /** Double tap: like (never un-likes by accident) with a heart that pops where you tapped. */
        void doubleTapLike(Point at) {
            burstAt = at == null ? new Point(getWidth() / 2, getHeight() / 2) : at;
            burstStart = System.currentTimeMillis();
            burstTimer.restart();
            if (!card.isLikedNow()) {
                like();
            }
        }

        Slide(Post p) {
            super(null);
            this.post = p;
            setOpaque(true);
            setBackground(java.awt.Color.BLACK);

            card = new PostCard(p, userId, true);
            card.setAnchor(ReelsPage.this);
            card.setStateListener(this::sync);

            File file = resolve(p.getMediaPath());
            boolean isVideo = "video".equalsIgnoreCase(p.getMediaType()) && file != null;
            if (isVideo) {
                video = ReelPlayers.create(file);
                video.setWheelListener(ReelsPage.this::onWheel);
                video.setOnRatio(() -> {
                    stage.placeSlides();
                    stage.revalidate();
                });
                video.setOnDoubleClick(this::doubleTapLike);
                media = (JComponent) video;
            } else {
                video = null;
                StillView still = new StillView(p, file);
                still.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            doubleTapLike(e.getPoint());
                        }
                    }
                });
                media = still;
            }

            likeB = new RailButton(Icons.Type.HEART, this::like);
            commentB = new RailButton(Icons.Type.COMMENT, this::comments);
            saveB = new RailButton(Icons.Type.BOOKMARK, () -> card.actionToggleSave());
            shareB = new RailButton(Icons.Type.SHARE, () -> card.actionShare());
            reportB = new RailButton(Icons.Type.FLAG, this::report);
            downloadB = new RailButton(Icons.Type.DOWNLOAD, () -> card.actionDownload());
            downloadB.setVisible(false);                  // shown once we know downloads are allowed
            moveB = new RailButton(Icons.Type.EDIT, () -> card.actionMoveCategory(() -> {
                pill.show("Moved. It now appears in the other category's Reels.", 3500);
                sync();
            }));

            avatar.setUser(p.getUsername(), p.getAuthorProfilePic());
            avatar.showFrameOf(p.getUserId());
            badge = new FollowBadge(this::follow);
            author.setOpaque(false);
            author.setSize(58, 72);
            avatar.setBounds(6, 0, 46, 46);
            badge.setBounds(21, 36, 20, 20);
            author.add(badge);
            author.add(avatar);
            badge.setVisible(false);

            caption = new Caption(p);

            add(likeB);
            add(commentB);
            add(saveB);
            add(shareB);
            add(downloadB);
            if (p.getUserId() != userId) {
                add(reportB);
            } else {
                add(moveB);                               // the author can fix a wrong category right here
            }
            add(author);
            add(burst);                 // first = on top of everything
            add(caption);
            add(shade);
            add(media);                 // last = bottom of the stack

            sync();
            checkFollow();
            card.loadDownloadState(() -> {
                downloadB.setVisible(card.canDownloadNow());
                revalidate();
                repaint();
            });
        }

        void setOffset(int y) {
            offset = y;
        }

        /** Video first, then a fine light edge on top (no clipping: keeps playback smooth). */
        @Override
        public void paint(Graphics g) {
            long t0 = System.nanoTime();
            super.paint(g);
            Perf.record(System.nanoTime() - t0, post.getId());
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new java.awt.Color(255, 255, 255, 46));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }

        /** Width / height of the picture, or 0 while it is not known yet. */
        double ratio() {
            if (video != null) {
                return video.ratio();
            }
            return media instanceof StillView sv ? sv.ratio() : 0;
        }

        boolean isReadyToPlay() {
            return video == null || video.isReady();
        }

        void play() {
            if (video != null) {
                video.setMuted(ReelVideoView.isMuted());
                video.play();
            }
        }

        void pause() {
            if (video != null) {
                video.pause();
            }
        }

        void prepare() {
            if (video != null) {
                video.prepare();
            }
        }

        void release() {
            if (video != null) {
                video.release();
            }
        }

        void toggle() {
            if (video != null) {
                video.toggle();
            }
        }

        void setMuted(boolean m) {
            if (video != null) {
                video.setMuted(m);
            }
        }

        void like() {
            card.actionToggleLike();
            likeB.bump();
        }

        /** Comments open in the panel beside the reel (no floating window); the reel keeps playing. */
        private void comments() {
            showComments();
        }

        private void report() {
            boolean was = video != null && video.isPlaying();
            pause();
            card.actionReport();
            if (was && current == this && isShowing()) {
                play();
            }
        }

        /** Updates the counts and colours from the card's current state. */
        void sync() {
            boolean liked = card.isLikedNow();
            likeB.set(liked ? Icons.Type.HEART_FILLED : Icons.Type.HEART, compact(card.likeCountNow()),
                    liked ? Theme.RED : java.awt.Color.WHITE);
            commentB.set(Icons.Type.COMMENT, compact(card.commentCountNow()), java.awt.Color.WHITE);
            boolean saved = card.isSavedNow();
            saveB.set(Icons.Type.BOOKMARK, saved ? "Saved" : "Save",
                    saved ? Theme.YELLOW : java.awt.Color.WHITE);
            shareB.set(Icons.Type.SHARE, "Share", java.awt.Color.WHITE);
            reportB.set(Icons.Type.FLAG, "Report", new java.awt.Color(255, 255, 255, 200));
            downloadB.set(Icons.Type.DOWNLOAD, "Download", java.awt.Color.WHITE);
            moveB.set(Icons.Type.EDIT, "Move", java.awt.Color.WHITE);
            // keep the loaded post in step, so a reel opened again shows the same numbers
            post.setLikeCount(card.likeCountNow());
            post.setLikedByCurrentUser(liked);
            post.setCommentCount(card.commentCountNow());
            if (current == this) {
                sidePanel.tabs.repaint();
                infoCard.repaint();
            }
        }

        private void checkFollow() {
            if (post.getUserId() == userId || post.getUserId() <= 0) {
                return;
            }
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return followDAO.isFollowing(userId, post.getUserId());
                }

                @Override
                protected void done() {
                    try {
                        followed = get();
                        badge.setVisible(!followed);
                        if (current == Slide.this) {
                            infoCard.bind(Slide.this);
                        }
                    } catch (Exception ignored) {
                        // no badge
                    }
                }
            }.execute();
        }

        void follow() {
            badge.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return followDAO.followUser(userId, post.getUserId());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            followed = true;
                            badge.setVisible(false);
                            if (current == Slide.this) {
                                infoCard.bind(Slide.this);
                            }
                            Toast.success(ReelsPage.this, "You are now following @" + post.getUsername());
                        } else {
                            badge.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        badge.setEnabled(true);
                    }
                }
            }.execute();
        }

        @Override
        public void doLayout() {
            int w = getWidth(), h = getHeight();
            media.setBounds(0, 0, w, h);
            shade.setBounds(0, 0, w, h);
            burst.setBounds(0, 0, w, h);

            int railX = w - 58 - 8;
            int y = h - 14;
            java.util.List<RailButton> stack = new java.util.ArrayList<>();
            if (post.getUserId() != userId) {
                stack.add(reportB);
            } else {
                stack.add(moveB);
            }
            if (downloadB.isVisible()) {
                stack.add(downloadB);
            }
            stack.addAll(java.util.List.of(shareB, saveB, commentB, likeB));
            // the "Following / For You" bar and the speaker sit over the top TABS_H pixels: keep the rail below them
            int top = TABS_H + 6;
            int authorH = 72;
            int avail = Math.max(120, h - 14 - top);
            int bh = Math.max(34, Math.min(66, (avail - authorH - 4) / stack.size()));
            for (RailButton b : stack) {
                y -= bh;
                b.setBounds(railX, y, 58, bh);
            }
            y -= authorH + 4;
            author.setBounds(railX, Math.max(top, y), 58, authorH);

            int capW = Math.max(120, w - 58 - 8 - 18 - 14);
            caption.setBounds(14, h - 14 - 170, capW, 170);
        }
    }

    private static File resolve(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(System.getProperty("user.dir"), path);
        }
        return f;
    }

    private static String compact(int n) {
        if (n >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0).replace(".0M", "M");
        }
        if (n >= 1_000) {
            return String.format(Locale.ROOT, "%.1fK", n / 1_000.0).replace(".0K", "K");
        }
        return String.valueOf(n);
    }

    // ================================================================== small components

    /** A reel that is a picture (or has no usable video) is shown filling the frame instead. */
    private static final class StillView extends JComponent {
        private final Post post;
        private Image image;

        StillView(Post post, File file) {
            this.post = post;
            if (file != null && file.isFile() && "image".equalsIgnoreCase(post.getMediaType())) {
                image = new ImageIcon(file.getAbsolutePath()).getImage();
            }
        }

        double ratio() {
            return image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0
                    ? image.getWidth(null) / (double) image.getHeight(null) : 0;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(java.awt.Color.BLACK);
            g2.fillRect(0, 0, w, h);
            if (image != null && image.getWidth(null) > 0) {
                double s = Math.min(w / (double) image.getWidth(null), h / (double) image.getHeight(null));
                int iw = (int) (image.getWidth(null) * s), ih = (int) (image.getHeight(null) * s);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new java.awt.Color(24, 40, 96), w, h,
                        new java.awt.Color(74, 40, 140)));
                g2.fillRect(0, 0, w, h);
                g2.setColor(java.awt.Color.WHITE);
                g2.setFont(Theme.bold(18));
                String t = post.getTitle() == null ? "" : post.getTitle();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t, Math.max(16, (w - fm.stringWidth(t)) / 2), h / 2);
            }
            g2.dispose();
        }
    }

    /** Creator name, category, title and description with highlighted #hashtags. */
    private static final class Caption extends CachedView {
        private final Post post;
        private boolean expanded;
        private Rectangle moreRect;

        Caption(Post post) {
            this.post = post;
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    expanded = !expanded;
                    dirty();
                }
            });
        }

        /** Only the "more / less" link takes clicks; the rest lets taps reach the video. */
        @Override
        public boolean contains(int x, int y) {
            return moreRect != null && moreRect.contains(x, y);
        }

        @Override
        protected void render(Graphics2D g2, int w, int h) {
            java.util.List<Object[]> rows = new ArrayList<>();      // {text, font, color-kind}
            Font nameF = Theme.bold(16), titleF = Theme.bold(14), bodyF = Theme.regular(13);

            String title = post.getTitle() == null ? "" : post.getTitle().trim();
            String body = post.getDescription() == null ? "" : post.getDescription().trim();
            if (!body.isEmpty() && body.equalsIgnoreCase(title)) {
                body = "";
            }

            java.util.List<java.util.List<String[]>> titleLines = wrap(g2, title, titleF, w, expanded ? 3 : 2, true);
            java.util.List<java.util.List<String[]>> bodyLines = wrap(g2, body, bodyF, w, expanded ? 6 : 2, false);
            boolean more = !expanded && (wrapCount(g2, title, titleF, w) > 2 || wrapCount(g2, body, bodyF, w) > 2);

            int lineT = 19, lineB = 18;
            int total = 26 + titleLines.size() * lineT + bodyLines.size() * lineB + (more || expanded ? 20 : 0);
            int y = h - total;

            // @name + category
            g2.setFont(nameF);
            g2.setColor(java.awt.Color.WHITE);
            String name = "@" + (post.getUsername() == null ? "user" : post.getUsername());
            drawShadow(g2, name, 0, y + 16);
            int nameW = g2.getFontMetrics().stringWidth(name);
            String cat = "academic".equalsIgnoreCase(post.getCategory()) ? "Academic" : "Entertainment";
            Font pillF = Theme.bold(11);
            g2.setFont(pillF);
            int pw = g2.getFontMetrics().stringWidth(cat) + 14;
            java.awt.Color pc = "Academic".equals(cat) ? Theme.BLUE : Theme.VIOLET;
            g2.setColor(Theme.alpha(pc, 210));
            g2.fill(new RoundRectangle2D.Float(nameW + 10, y + 1, pw, 20, 20, 20));
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(cat, nameW + 17, y + 15);
            y += 26;

            y = drawLines(g2, titleLines, titleF, y, lineT);
            y = drawLines(g2, bodyLines, bodyF, y, lineB);

            moreRect = null;
            if (more || expanded) {
                g2.setFont(Theme.bold(12));
                g2.setColor(new java.awt.Color(255, 255, 255, 190));
                String label = expanded ? "less" : "more";
                drawShadow(g2, label, 0, y + 14);
                moreRect = new Rectangle(0, y, g2.getFontMetrics().stringWidth(label) + 14, 20);
            }
        }

        private int drawLines(Graphics2D g2, java.util.List<java.util.List<String[]>> lines, Font font, int y, int lh) {
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            for (java.util.List<String[]> line : lines) {
                int x = 0;
                for (String[] tok : line) {
                    boolean tag = "1".equals(tok[1]);
                    g2.setColor(tag ? Theme.BLUE_LIGHT : java.awt.Color.WHITE);
                    drawShadow(g2, tok[0], x, y + 14);
                    x += fm.stringWidth(tok[0]);
                }
                y += lh;
            }
            return y;
        }

        private void drawShadow(Graphics2D g2, String s, int x, int y) {
            java.awt.Color c = g2.getColor();
            g2.setColor(new java.awt.Color(0, 0, 0, 150));
            g2.drawString(s, x + 1, y + 1);
            g2.setColor(c);
            g2.drawString(s, x, y);
        }

        /** Words (each with its trailing space) packed into lines; the last allowed line gets "...". */
        private static java.util.List<java.util.List<String[]>> wrap(Graphics2D g2, String text, Font font, int width,
                                                               int maxLines, boolean unusedTagColour) {
            java.util.List<java.util.List<String[]>> lines = new ArrayList<>();
            if (text.isEmpty()) {
                return lines;
            }
            FontMetrics fm = g2.getFontMetrics(font);
            java.util.List<String[]> line = new ArrayList<>();
            int x = 0;
            String[] words = text.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String word = words[i] + " ";
                int ww = fm.stringWidth(word);
                if (x + ww > width && !line.isEmpty()) {
                    lines.add(line);
                    if (lines.size() == maxLines) {
                        ellipsize(lines.get(lines.size() - 1), fm, width);
                        return lines;
                    }
                    line = new ArrayList<>();
                    x = 0;
                }
                line.add(new String[] {word, words[i].startsWith("#") ? "1" : "0"});
                x += ww;
            }
            if (!line.isEmpty()) {
                lines.add(line);
            }
            return lines;
        }

        private static int wrapCount(Graphics2D g2, String text, Font font, int width) {
            return wrap(g2, text, font, width, 1000, false).size();
        }

        private static void ellipsize(java.util.List<String[]> line, FontMetrics fm, int width) {
            if (line.isEmpty()) {
                return;
            }
            String[] last = line.get(line.size() - 1);
            String t = last[0].trim();
            int used = 0;
            for (int i = 0; i < line.size() - 1; i++) {
                used += fm.stringWidth(line.get(i)[0]);
            }
            while (t.length() > 1 && used + fm.stringWidth(t + "...") > width) {
                t = t.substring(0, t.length() - 1);
            }
            last[0] = t + "...";
        }
    }

    /** A round icon with a count underneath, used for like / comment / save / share / report. */
    private static final class RailButton extends CachedView {
        private Icons.Type icon;
        private String label = "";
        private java.awt.Color tint = java.awt.Color.WHITE;
        private boolean hover;
        private double scale = 1;
        private final Runnable action;

        RailButton(Icons.Type icon, Runnable action) {
            this.icon = icon;
            this.action = action;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    dirty();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    dirty();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        void set(Icons.Type icon, String label, java.awt.Color tint) {
            this.icon = icon;
            this.label = label;
            this.tint = tint;
            dirty();
        }

        /** A quick pop when pressed. */
        void bump() {
            final long start = System.currentTimeMillis();
            final javax.swing.Timer t = new javax.swing.Timer(15, null);
            t.addActionListener(e -> {
                double p = Math.min(1, (System.currentTimeMillis() - start) / 260.0);
                scale = 1 + 0.28 * Math.sin(Math.PI * p);
                dirty();
                if (p >= 1) {
                    scale = 1;
                    t.stop();
                }
            });
            t.start();
        }

        @Override
        protected void render(Graphics2D g2, int w, int h) {
            int d = (int) Math.round(44 * scale);
            int x = (w - d) / 2, y = (int) Math.round((44 - d) / 2.0) + 2;
            g2.setColor(new java.awt.Color(0, 0, 0, hover ? 140 : 100));
            g2.fillOval(x, y, d, d);
            float isz = (float) (24 * scale);
            Icons.paint(g2, icon, (w - isz) / 2f, y + (d - isz) / 2f, isz, tint);
            g2.setFont(Theme.bold(12));
            FontMetrics fm = g2.getFontMetrics();
            int lx = (w - fm.stringWidth(label)) / 2;
            g2.setColor(new java.awt.Color(0, 0, 0, 160));
            g2.drawString(label, lx + 1, 61);
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(label, lx, 60);
        }
    }

    /** The little red + on the creator's picture; click to follow. */
    private static final class FollowBadge extends JComponent {
        FollowBadge(Runnable action) {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isEnabled()) {
                        action.run();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(isEnabled() ? Theme.RED : new java.awt.Color(150, 150, 150));
            g2.fillOval(0, 0, 20, 20);
            g2.setColor(java.awt.Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(10, 5, 10, 15);
            g2.drawLine(5, 10, 15, 10);
            g2.dispose();
        }
    }

    /** "Following | For You" on top of the reel, plus the sound switch. */
    private final class TabsBar extends JComponent {
        private Rectangle followingRect = new Rectangle(), forYouRect = new Rectangle(), muteRect = new Rectangle();

        TabsBar() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (followingRect.contains(e.getPoint())) {
                        switchTab(Tab.FOLLOWING);
                    } else if (forYouRect.contains(e.getPoint())) {
                        switchTab(Tab.FOR_YOU);
                    } else if (muteRect.contains(e.getPoint())) {
                        toggleMute();
                    }
                }
            });
        }

        @Override
        public boolean contains(int x, int y) {
            return followingRect.contains(x, y) || forYouRect.contains(x, y) || muteRect.contains(x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            g2.setFont(Theme.bold(17));
            FontMetrics fm = g2.getFontMetrics();
            String a = "Following", b = "For You";
            int aw = fm.stringWidth(a), bw = fm.stringWidth(b), gap = 26;
            int x = (w - (aw + gap + bw)) / 2;
            int base = 31;
            followingRect = new Rectangle(x - 8, 6, aw + 16, 38);
            forYouRect = new Rectangle(x + aw + gap - 8, 6, bw + 16, 38);
            muteRect = new Rectangle(w - 48, 8, 36, 34);

            drawTab(g2, a, x, base, tab == Tab.FOLLOWING, aw);
            drawTab(g2, b, x + aw + gap, base, tab == Tab.FOR_YOU, bw);

            // speaker icon
            int sx = w - 40, sy = 15;
            g2.setColor(java.awt.Color.WHITE);
            Polygon cone = new Polygon(new int[] {sx, sx + 6, sx + 12, sx + 12, sx + 6, sx},
                    new int[] {sy + 6, sy + 6, sy, sy + 20, sy + 14, sy + 14}, 6);
            g2.fillPolygon(cone);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (ReelVideoView.isMuted()) {
                g2.setColor(Theme.RED);
                g2.drawLine(sx + 16, sy + 5, sx + 25, sy + 15);
                g2.drawLine(sx + 25, sy + 5, sx + 16, sy + 15);
            } else {
                g2.drawArc(sx + 9, sy + 4, 10, 12, -50, 100);
                g2.drawArc(sx + 12, sy + 1, 14, 18, -50, 100);
            }
            g2.dispose();
        }

        private void drawTab(Graphics2D g2, String text, int x, int base, boolean selected, int width) {
            g2.setColor(new java.awt.Color(0, 0, 0, 140));
            g2.drawString(text, x + 1, base + 1);
            g2.setColor(selected ? java.awt.Color.WHITE : new java.awt.Color(255, 255, 255, 160));
            g2.drawString(text, x, base);
            if (selected) {
                g2.fillRoundRect(x + width / 2 - 14, base + 8, 28, 3, 3, 3);
            }
        }
    }

    /** Home / Following / + / Alerts / Profile, like the bar on a phone. */
    private final class NavBar extends JComponent {
        private final String[] keys = {"HOME", "FOLLOWING", "CREATE", "NOTIFICATIONS", "PROFILE"};
        private final String[] labels = {"Home", "Following", "", "Alerts", "Profile"};
        private final Icons.Type[] icons = {Icons.Type.HOME, Icons.Type.USERS, Icons.Type.PLUS,
                Icons.Type.BELL, Icons.Type.USER};
        private int hover = -1;

        NavBar() {
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int i = e.getX() * keys.length / Math.max(1, getWidth());
                    if (i >= 0 && i < keys.length) {
                        navigator.accept(keys[i]);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int i = e.getX() * keys.length / Math.max(1, getWidth());
                    if (i != hover) {
                        hover = i;
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new java.awt.Color(8, 11, 22));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new java.awt.Color(255, 255, 255, 28));
            g2.drawLine(0, 0, w, 0);
            int cell = w / keys.length;
            for (int i = 0; i < keys.length; i++) {
                int cx = i * cell + cell / 2;
                boolean hot = i == hover;
                if (i == 2) {
                    int bw = 44, bh = 30;
                    g2.setPaint(new GradientPaint(cx - bw / 2f, 0, Theme.BLUE, cx + bw / 2f, 0, Theme.VIOLET));
                    g2.fill(new RoundRectangle2D.Float(cx - bw / 2f, (h - bh) / 2f, bw, bh, 12, 12));
                    Icons.paint(g2, icons[i], cx - 10, (h - 20) / 2f, 20, java.awt.Color.WHITE);
                } else {
                    java.awt.Color c = hot ? java.awt.Color.WHITE : new java.awt.Color(255, 255, 255, 170);
                    Icons.paint(g2, icons[i], cx - 11, 8, 22, c);
                    g2.setFont(Theme.regular(10));
                    g2.setColor(c);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(labels[i], cx - fm.stringWidth(labels[i]) / 2, 43);
                }
            }
            g2.dispose();
        }
    }

    /** The round up / down buttons beside the reel. */
    private static final class RoundButton extends JComponent {
        private final boolean up;
        private Runnable action = () -> { };
        private boolean hover;

        RoundButton(boolean up) {
            this.up = up;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                    if (isEnabled()) {
                        action.run();
                    }
                }
            });
        }

        void setAction(Runnable a) {
            this.action = a;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            g2.setColor(Theme.alpha(Theme.CARD_LIGHT, on ? (hover ? 255 : 210) : 90));
            g2.fillOval(0, 0, w, h);
            g2.setColor(on ? Theme.TEXT : Theme.alpha(Theme.MUTED, 140));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = w / 2, cy = h / 2;
            if (up) {
                g2.drawLine(cx - 8, cy + 3, cx, cy - 5);
                g2.drawLine(cx, cy - 5, cx + 8, cy + 3);
            } else {
                g2.drawLine(cx - 8, cy - 3, cx, cy + 5);
                g2.drawLine(cx, cy + 5, cx + 8, cy - 3);
            }
            g2.dispose();
        }
    }

    /** The "Loading more reels..." / "You're all caught up" pill. */
    private static final class Pill extends JComponent {
        private String text = "";
        private final javax.swing.Timer hide = new javax.swing.Timer(1800, null);

        Pill() {
            setOpaque(false);
            setVisible(false);
            hide.setRepeats(false);
            hide.addActionListener(e -> setVisible(false));
        }

        void show(String message, int millis) {
            text = message;
            setVisible(true);
            hide.stop();
            if (millis > 0) {
                hide.setInitialDelay(millis);
                hide.restart();
            }
            repaint();
        }

        void hideNow() {
            hide.stop();
            setVisible(false);
        }

        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setFont(Theme.bold(12));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 28, h = 28;
            int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
            g2.setColor(new java.awt.Color(0, 0, 0, 180));
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(text, x + 14, y + 18);
            g2.dispose();
        }
    }

    /** Centred text (and an optional button) for loading, errors and empty lists. */
    private static final class MessageBox extends JPanel {
        private final JLabel title = new JLabel("", SwingConstants.CENTER);
        private final JLabel body = new JLabel("", SwingConstants.CENTER);
        private final RoundedButton button = new RoundedButton("", RoundedButton.Style.PRIMARY);
        private Runnable action;

        MessageBox() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            title.setFont(Theme.bold(20));
            title.setForeground(java.awt.Color.WHITE);
            body.setFont(Theme.regular(13));
            body.setForeground(new java.awt.Color(190, 200, 228));
            button.addActionListener(e -> {
                if (action != null) {
                    action.run();
                }
            });
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(title, c);
            c.gridy = 1;
            c.insets = new Insets(8, 0, 0, 0);
            add(body, c);
            c.gridy = 2;
            c.insets = new Insets(18, 0, 0, 0);
            add(button, c);
        }

        void set(String t, String b, String buttonText, Runnable a) {
            title.setText(t);
            body.setText(b == null ? "" : b);
            action = a;
            button.setText(buttonText == null ? "" : buttonText);
            button.setVisible(buttonText != null);
            revalidate();
            repaint();
        }
    }

    // ================================================================== side areas

    /**
     * Measures how long the reel takes to draw and writes a short line to eduvibe-video.log every 10
     * seconds while a reel is on screen. It only measures; it changes nothing.
     */
    private static final class Perf {
        private static long total, max, count, windowStart = System.currentTimeMillis();
        static volatile Runnable onSlow = () -> { };
        private static boolean told;

        static synchronized void record(long nanos, int postId) {
            total += nanos;
            max = Math.max(max, nanos);
            count++;
            long now = System.currentTimeMillis();
            if (now - windowStart >= 10_000) {
                if (!told && count > 30 && total / 1e6 / count > 30) {
                    told = true;                       // drawing a frame takes long: suggest light mode, once
                    SwingUtilities.invokeLater(onSlow);
                }
                try {
                    String line = java.time.LocalDateTime.now() + "  reel-perf  post " + postId + ": "
                            + count + " frames in " + (now - windowStart) / 1000 + " s ("
                            + String.format(Locale.ROOT, "%.1f", count * 1000.0 / (now - windowStart)) + " fps), draw avg "
                            + String.format(Locale.ROOT, "%.1f", total / 1e6 / Math.max(1, count)) + " ms, worst "
                            + String.format(Locale.ROOT, "%.1f", max / 1e6) + " ms, prism.order="
                            + System.getProperty("prism.order") + ", screen scale "
                            + java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                                    .getDefaultConfiguration().getDefaultTransform().getScaleX() + "\n";
                    java.nio.file.Files.writeString(java.nio.file.Paths.get(System.getProperty("user.dir"),
                            "eduvibe-video.log"), line, java.nio.charset.StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                } catch (Exception ignored) {
                    // measuring is a bonus
                }
                total = 0;
                max = 0;
                count = 0;
                windowStart = now;
            }
        }
    }

    /** A rounded, softly lit card that sits over the ambient background. */
    private static class Glass extends JPanel {
        Glass(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }

        int fillAlpha() {
            return 226;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            RoundRectangle2D.Float r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 24, 24);
            g2.setColor(Theme.alpha(Theme.CARD, fillAlpha()));
            g2.fill(r);
            g2.setPaint(new GradientPaint(0, 0, new java.awt.Color(255, 255, 255, 26), 0, 90,
                    new java.awt.Color(255, 255, 255, 0)));
            g2.fill(r);
            g2.setColor(Theme.alpha(Theme.BORDER, 200));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(r);
            g2.dispose();
        }
    }

    /**
     * A component drawn once into an image and then only copied. The reel's video repaints these
     * (they sit on top of it) on every frame, so redrawing text and shapes each time made playback lag.
     */
    private abstract static class CachedView extends JComponent {
        private java.awt.image.BufferedImage cache;
        private boolean stale = true;
        private double cacheScale = 1;

        /** Something visible changed: draw again next time. */
        protected final void dirty() {
            stale = true;
            repaint();
        }

        protected abstract void render(Graphics2D g2, int w, int h);

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w < 1 || h < 1) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            double sc = Math.max(1, g2.getTransform().getScaleX());      // sharp on 125% / 150% screens
            int iw = (int) Math.ceil(w * sc), ih = (int) Math.ceil(h * sc);
            if (cache == null || cache.getWidth() != iw || cache.getHeight() != ih || cacheScale != sc) {
                cache = new java.awt.image.BufferedImage(iw, ih, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                cacheScale = sc;
                stale = true;
            }
            if (stale) {
                Graphics2D c = cache.createGraphics();
                c.setComposite(AlphaComposite.Clear);
                c.fillRect(0, 0, iw, ih);
                c.setComposite(AlphaComposite.SrcOver);
                c.scale(sc, sc);
                Theme.antialias(c);
                render(c, w, h);
                c.dispose();
                stale = false;
            }
            Graphics2D d = (Graphics2D) g2.create();
            d.drawImage(cache, 0, 0, w, h, null);
            d.dispose();
        }
    }

    private static String clip(String text, FontMetrics fm, int width) {
        if (text == null) {
            return "";
        }
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "...") > width) {
            t = t.substring(0, t.length() - 1);
        }
        return t.trim() + "...";
    }

    private static List<String> wrapWords(String text, FontMetrics fm, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
                if (lines.size() == maxLines) {
                    break;
                }
            } else {
                line = new StringBuilder(test);
            }
        }
        if (lines.size() < maxLines && line.length() > 0) {
            lines.add(line.toString());
        } else if (lines.size() == maxLines) {
            lines.set(maxLines - 1, clip(lines.get(maxLines - 1) + " ...", fm, width));
        }
        return lines;
    }

    /** Right-hand panel: "Up next" list and the comments, docked beside the reel. */
    private final class SidePanel extends Glass {

        final JComponent tabs = new JComponent() {
            private Rectangle nextR = new Rectangle(), comR = new Rectangle();

            {
                setPreferredSize(new Dimension(10, 56));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (nextR.contains(e.getPoint())) {
                            hideComments();
                        } else if (comR.contains(e.getPoint()) && current != null) {
                            showComments();
                        }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                int w = getWidth();
                String a = "Up next";
                int count = current == null ? 0 : current.post.getCommentCount();
                String b = "Comments" + (count > 0 ? "  " + compact(count) : "");
                g2.setFont(Theme.bold(14f));
                FontMetrics fm = g2.getFontMetrics();
                int wa = fm.stringWidth(a) + 28, wb = fm.stringWidth(b) + 28;
                int gap = 6, x0 = (w - (wa + wb + gap)) / 2;
                nextR = new Rectangle(x0, 12, wa, 34);
                comR = new Rectangle(x0 + wa + gap, 12, wb, 34);
                drawTab(g2, nextR, a, !commentsOpen);
                drawTab(g2, comR, b, commentsOpen);
                g2.setColor(Theme.alpha(Theme.BORDER, 150));
                g2.fillRect(18, getHeight() - 1, w - 36, 1);
                g2.dispose();
            }

            private void drawTab(Graphics2D g2, Rectangle r, String text, boolean on) {
                if (on) {
                    g2.setPaint(new GradientPaint(r.x, r.y, accentA, r.x + r.width, r.y, accentB));
                    g2.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, r.height, r.height));
                }
                g2.setColor(on ? java.awt.Color.WHITE : Theme.MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2,
                        r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
            }
        };

        @Override
        int fillAlpha() {
            return 252;                       // solid, so it also works as a drawer over the reel
        }

        private final CardLayout cards = new CardLayout();
        private final JPanel body = new JPanel(cards);
        private final JPanel commentHolder = new JPanel(new BorderLayout());
        private final UpNext upNext = new UpNext();

        SidePanel() {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(1, 1, 8, 1));
            body.setOpaque(false);
            commentHolder.setOpaque(false);
            commentHolder.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 16));
            body.add(upNext, "next");
            body.add(commentHolder, "comments");
            add(tabs, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
        }

        void showNext() {
            cards.show(body, "next");
            commentHolder.removeAll();
            upNext.repaint();
            tabs.repaint();
        }

        void showComments() {
            rebuildComments();
            cards.show(body, "comments");
            tabs.repaint();
        }

        private void rebuildComments() {
            commentHolder.removeAll();
            if (current != null) {
                JComponent c = current.card.createCommentsPanel(ReelsPage.this::hideComments);
                commentHolder.add(c, BorderLayout.CENTER);
            }
            commentHolder.revalidate();
            commentHolder.repaint();
        }

        /** The reel in focus changed. */
        void refresh() {
            if (commentsOpen) {
                rebuildComments();
            }
            upNext.repaint();
            tabs.repaint();
        }

        private final class UpNext extends JComponent {
            private static final int ROW = 88;
            private int hover = -1;

            UpNext() {
                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        int row = e.getY() < 12 ? -1 : (e.getY() - 12) / ROW;
                        boolean over = row >= 0 && index + 1 + row < view().size();
                        setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        if (!over) {
                            row = -1;
                        }
                        if (row != hover) {
                            hover = row;
                            repaint();
                        }
                    }
                });
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = -1;
                        repaint();
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int row = (e.getY() - 12) / ROW;
                        if (row >= 0) {
                            jumpTo(index + 1 + row);
                        }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                List<Post> v = view();
                int w = getWidth();
                int rows = Math.max(0, Math.min(8, (getHeight() - 12) / ROW));
                int shown = 0;
                for (int i = 0; i < rows && index + 1 + i < v.size(); i++) {
                    if (i == hover) {
                        g2.setColor(Theme.alpha(Theme.CARD_LIGHT, 170));
                        g2.fill(new RoundRectangle2D.Float(8, 12 + i * ROW - 2, w - 16, ROW - 6, 16, 16));
                    }
                    row(g2, v.get(index + 1 + i), index + 2 + i, 12 + i * ROW, w);
                    shown++;
                }
                if (shown == 0) {
                    g2.setFont(Theme.regular(13f));
                    g2.setColor(Theme.MUTED);
                    String t = fetchingMore ? "Finding more reels for you..." : "You are all caught up.";
                    g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2, 60);
                }
                g2.dispose();
            }

            private void row(Graphics2D g2, Post p, int number, int y, int w) {
                java.awt.Color[] c = accentFor(p);
                int tw = 52, th = 70, x = 18;
                g2.setPaint(new GradientPaint(x, y, c[0], x + tw, y + th, c[1]));
                g2.fill(new RoundRectangle2D.Float(x, y + 4, tw, th, 14, 14));
                Icons.paint(g2, Icons.Type.PLAY, x + tw / 2 - 11, y + 4 + th / 2 - 11,
                        22, java.awt.Color.WHITE);
                g2.setFont(Theme.bold(11f));
                g2.setColor(new java.awt.Color(255, 255, 255, 210));
                g2.drawString(String.valueOf(number), x + 7, y + 4 + 15);

                int tx = x + tw + 14, avail = w - tx - 18;
                g2.setFont(Theme.bold(13.5f));
                g2.setColor(Theme.TEXT);
                FontMetrics fm = g2.getFontMetrics();
                List<String> t = wrapWords(p.getTitle(), fm, avail, 2);
                int ty = y + 22;
                for (String line : t) {
                    g2.drawString(line, tx, ty);
                    ty += fm.getHeight() - 1;
                }
                g2.setFont(Theme.regular(11.5f));
                g2.setColor(Theme.MUTED);
                String cat = p.getCategory() == null || p.getCategory().isBlank() ? "" : p.getCategory();
                String meta = "@" + p.getUsername() + (cat.isEmpty() ? "" : "  -  " + cat);
                g2.drawString(clip(meta, g2.getFontMetrics(), avail), tx, y + th - 8);
                g2.setColor(Theme.alpha(Theme.BORDER, 120));
                g2.fillRect(18, y + ROW - 6, w - 36, 1);
            }
        }
    }

    /** Left-hand card: the creator and what the reel is about. */
    private final class InfoCard extends Glass {

        private final Avatar avatar = new Avatar(68);
        private final RoundedButton followButton = new RoundedButton("Follow");
        private Slide slide;
        private final java.util.List<Rectangle> hintRects = new java.util.ArrayList<>();
        private final Runnable[] hintActions = {
            () -> goTo(index + 1),
            () -> { if (current != null) { current.toggle(); } },
            ReelsPage.this::toggleMute,
            () -> { if (current != null) { current.like(); } },
            ReelsPage.this::togglePerformance
        };

        InfoCard() {
            super(null);
            add(avatar);
            add(followButton);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (int i = 0; i < hintRects.size(); i++) {
                        if (hintRects.get(i).contains(e.getPoint())) {
                            hintActions[i].run();                  // the key chips work as buttons too
                            return;
                        }
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean over = false;
                    for (Rectangle r : hintRects) {
                        over |= r.contains(e.getPoint());
                    }
                    setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            });
            followButton.addActionListener(e -> {
                if (slide != null && !slide.followed) {
                    slide.follow();
                }
            });
        }

        void bind(Slide s) {
            slide = s;
            if (s != null) {
                avatar.setUser(s.post.getUsername(), s.post.getAuthorProfilePic());
                avatar.showFrameOf(s.post.getUserId());
                boolean own = s.post.getUserId() == userId;
                followButton.setVisible(!own);
                followButton.setText(s.followed ? "Following" : "Follow");
                followButton.setEnabled(!s.followed);
            }
            doLayout();
            repaint();
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            avatar.setBounds((w - 68) / 2, 26, 68, 68);
            followButton.setBounds(24, 168, Math.max(10, w - 48), 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (slide == null) {
                return;
            }
            Post p = slide.post;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), pad = 22, avail = w - pad * 2;
            g2.setFont(Theme.bold(16f));
            g2.setColor(Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            String name = clip("@" + p.getUsername(), fm, avail);
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, 118);

            // category pill
            String cat = p.getCategory() == null || p.getCategory().isBlank() ? "Reel" : p.getCategory();
            g2.setFont(Theme.bold(11.5f));
            fm = g2.getFontMetrics();
            int pw = fm.stringWidth(cat) + 24;
            int px = (w - pw) / 2;
            g2.setPaint(new GradientPaint(px, 130, accentA, px + pw, 130, accentB));
            g2.fill(new RoundRectangle2D.Float(px, 130, pw, 24, 24, 24));
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(cat, px + 12, 130 + 16);

            int y = 232;
            g2.setColor(Theme.alpha(Theme.BORDER, 160));
            g2.fillRect(pad, y - 14, avail, 1);

            g2.setFont(Theme.bold(15f));
            g2.setColor(Theme.TEXT);
            fm = g2.getFontMetrics();
            for (String line : wrapWords(p.getTitle(), fm, avail, 3)) {
                g2.drawString(line, pad, y + 6);
                y += fm.getHeight();
            }
            y += 6;
            g2.setFont(Theme.regular(12.5f));
            g2.setColor(Theme.MUTED);
            fm = g2.getFontMetrics();
            int room = Math.max(0, (getHeight() - 254 - y) / fm.getHeight());
            for (String line : wrapWords(p.getDescription(), fm, avail, Math.min(6, room))) {
                g2.drawString(line, pad, y + 4);
                y += fm.getHeight();
            }

            // stats
            int sy = getHeight() - 230;
            if (sy > y) {
                int cw = avail / 3;
                stat(g2, pad, sy, cw, compact(slide.card.likeCountNow()), "Likes");
                stat(g2, pad + cw, sy, cw, compact(slide.card.commentCountNow()), "Comments");
                stat(g2, pad + cw * 2, sy, cw, slide.card.isSavedNow() ? "Yes" : "-", "Saved");
            }

            // keyboard hints
            int hy = getHeight() - 136;
            if (hy > y) {
                g2.setColor(Theme.alpha(Theme.BORDER, 160));
                g2.fillRect(pad, hy - 14, avail, 1);
                String[][] keys = {{"Up / Down", "next reel"}, {"Space", "pause"}, {"M", "mute"}, {"L", "like"}, {"P", "performance"}};
                hintRects.clear();
                int kx = pad, ky = hy;
                for (int i = 0; i < keys.length; i++) {
                    int cx = pad;
                    int cy = hy + i * 24;
                    g2.setFont(Theme.bold(10.5f));
                    FontMetrics kf = g2.getFontMetrics();
                    int kw = kf.stringWidth(keys[i][0]) + 12;
                    g2.setColor(Theme.alpha(Theme.CARD_LIGHT, 255));
                    g2.fill(new RoundRectangle2D.Float(cx, cy, kw, 20, 8, 8));
                    hintRects.add(new Rectangle(cx, cy, kw + 70, 20));
                    g2.setColor(Theme.TEXT);
                    g2.drawString(keys[i][0], cx + 6, cy + 14);
                    g2.setFont(Theme.regular(11f));
                    g2.setColor(Theme.MUTED);
                    g2.drawString(keys[i][1], cx + kw + 6, cy + 14);
                }
            }
            g2.dispose();
        }

        private void stat(Graphics2D g2, int x, int y, int w, String value, String label) {
            g2.setFont(Theme.bold(18f));
            g2.setColor(Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value, x + (w - fm.stringWidth(value)) / 2, y + 20);
            g2.setFont(Theme.regular(11f));
            g2.setColor(Theme.MUTED);
            fm = g2.getFontMetrics();
            g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + 38);
        }
    }
}
