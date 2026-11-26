package com.eduvibe.ui;

import com.eduvibe.ui.components.FloatingCategoryNav;
import com.eduvibe.ui.components.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.ReelsHost  (NEW FILE)
 *
 * The Reels section: one page with a floating pill on top, ACADEMIC REELS | ENTERTAINMENT REELS. Each choice
 * is its own independent ReelsPage (its own reels, place, Following / For You tab and loading), and only the
 * chosen one is shown, so only its video plays. The pill follows the same scroll-aware behaviour as on Home:
 * moving down through the reels glides it away, moving back up brings it back, and it returns when you stop.
 *
 * This choice is separate from the Home feed's choice.
 */
public final class ReelsHost extends JPanel {

    private static final int IDLE_MS = 2200;        // reels are watched for a while: the pill waits longer here
    private static final int TABS_DROP = 12 + FloatingCategoryNav.HEIGHT + 4;

    private final ReelsPage academic;
    private final ReelsPage entertainment;
    private final JPanel cards = new JPanel(new CardLayout());
    private final FloatingCategoryNav nav;
    private final JLayeredPane stage;
    private ReelsPage active;
    private Consumer<String> onCategoryChanged = c -> { };

    public ReelsHost(int userId) {
        super(new BorderLayout());
        setOpaque(false);

        academic = new ReelsPage(userId, "academic");
        entertainment = new ReelsPage(userId, "entertainment");
        active = academic;

        cards.setOpaque(false);
        cards.add(academic, "academic");
        cards.add(entertainment, "entertainment");

        nav = new FloatingCategoryNav(
                new String[] {"ACADEMIC REELS", "ENTERTAINMENT REELS"},
                new Icons.Type[] {Icons.Type.BOOK, Icons.Type.PLAY},
                new Color[][] {
                    {new Color(59, 130, 246), new Color(6, 182, 212)},
                    {new Color(139, 92, 246), new Color(236, 72, 153)}},
                0, this::onPick);
        nav.setIdleDelay(IDLE_MS);
        nav.setVisibilityListener(v -> follow());

        for (ReelsPage p : new ReelsPage[] {academic, entertainment}) {
            p.setScrollListener(nav::onScroll);          // +1 = next reel (down), -1 = previous (up)
            p.setLayoutListener(this::placeNav);
        }

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
        add(stage, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------ outside API

    public void setNavigator(Consumer<String> navigator) {
        academic.setNavigator(navigator);
        entertainment.setNavigator(navigator);
    }

    /** Only Entertainment Reels count towards (and show) the entertainment time limit. */
    public void setEntertainmentTimerBar(JComponent bar) {
        entertainment.setTimerBar(bar);
    }

    public void setSearchQuery(String q) {
        academic.setSearchQuery(q);
        entertainment.setSearchQuery(q);
    }

    public String getCategory() {
        return active == academic ? "academic" : "entertainment";
    }


    private static final String PREF_KEY = "reelsCategory";

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

    /** Opens on the Reels category used last time. */
    public void restoreCategory() {
        if ("entertainment".equals(savedCategory())) {
            nav.select(1, true);
        }
    }

    public void setOnCategoryChanged(Consumer<String> listener) {
        this.onCategoryChanged = listener == null ? c -> { } : listener;
    }

    // ------------------------------------------------------------------ the floating pill

    private void onPick(int index) {
        active = index == 0 ? academic : entertainment;
        saveCategory(getCategory());
        ((CardLayout) cards.getLayout()).show(cards, getCategory());   // the other page is hidden: its video stops
        nav.reveal();
        follow();
        placeNav();
        onCategoryChanged.accept(getCategory());
    }

    /** The Following | For You tabs move with the pill so nothing overlaps and no gap is left. */
    private void follow() {
        int drop = (int) Math.round(TABS_DROP * nav.visibility());
        academic.setTabsDrop(drop);
        entertainment.setTabsDrop(drop);
        placeNav();
    }

    private void placeNav() {
        if (stage == null || nav == null) {
            return;
        }
        Dimension d = nav.getPreferredSize();
        int shown = 10 - FloatingCategoryNav.shadow();
        int hidden = -(d.height + 6);
        int y = (int) Math.round(hidden + (shown - hidden) * nav.visibility());
        int cx = active.frameCenterX();
        nav.setBounds(cx - d.width / 2, y, d.width, d.height);
    }
}
