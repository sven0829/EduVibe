package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * com.eduvibe.ui.components.FloatingCategoryNav  (NEW FILE)
 *
 * A floating, pill-shaped selector with two real choices (for example ACADEMIC | ENTERTAINMENT). It is
 * not part of any scrolling content: the page that owns it places it on top of the content and moves
 * it by {@link #visibility()}.
 *
 * Smart visibility, driven by {@link #onScroll(int)}:
 *   - scrolling DOWN hides it (it glides up and out of view),
 *   - scrolling UP shows it again straight away,
 *   - when scrolling stops, it comes back after a short idle delay,
 *   - at the very top it is always shown.
 * Small movements are ignored and direction changes need a few pixels, so it never flickers. Only one
 * animation timer exists, and it stops by itself when nothing is moving.
 */
public final class FloatingCategoryNav extends JComponent {

    public static final int HEIGHT = 46;
    private static final int SHADOW = 10;               // room around the pill for its soft shadow

    private final String[] labels;
    private final Icons.Type[] icons;
    private final Color[][] accents;
    private final IntConsumer onSelect;

    private int selected;
    private int hover = -1;
    private float pillX = -1, pillW;                    // the highlight that slides to the chosen option
    private double visibility = 1;                      // 1 = fully shown, 0 = fully hidden
    private double targetVisibility = 1;
    private boolean atTop = true;

    private int idleDelayMs = 700;
    private int accumulated;                            // scroll pixels in the current direction
    private int lastDirection;

    private DoubleConsumer visibilityListener = v -> { };
    private final Timer animation;
    private final Timer idle;

    private Rectangle[] hit;

    public FloatingCategoryNav(String[] labels, Icons.Type[] icons, Color[][] accents, int selected,
                               IntConsumer onSelect) {
        this.labels = labels;
        this.icons = icons;
        this.accents = accents;
        this.selected = Math.max(0, Math.min(labels.length - 1, selected));
        this.onSelect = onSelect == null ? i -> { } : onSelect;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hit = new Rectangle[labels.length];
        setPreferredSize(new Dimension(contentWidth() + SHADOW * 2, HEIGHT + SHADOW * 2));

        animation = new Timer(15, e -> step());
        idle = new Timer(idleDelayMs, e -> {
            targetVisibility = 1;
            startAnimation();
        });
        idle.setRepeats(false);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int h = indexAt(e.getPoint());
                if (h != hover) {
                    hover = h;
                    startAnimation();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                startAnimation();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i >= 0) {
                    select(i, true);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ------------------------------------------------------------------ selection

    public int getSelected() {
        return selected;
    }

    /** Chooses an option; fireEvent = also tell the owner. Choosing the same option again does nothing. */
    public void select(int index, boolean fireEvent) {
        if (index < 0 || index >= labels.length || index == selected) {
            return;
        }
        selected = index;
        startAnimation();
        if (fireEvent) {
            onSelect.accept(index);
        }
    }

    // ------------------------------------------------------------------ visibility

    /** 0 (hidden) to 1 (shown). The owner uses it to move the bar; it is also called while animating. */
    public double visibility() {
        return visibility;
    }

    public void setVisibilityListener(DoubleConsumer listener) {
        this.visibilityListener = listener == null ? v -> { } : listener;
    }

    /** How long scrolling must be still before the bar returns by itself. */
    public void setIdleDelay(int millis) {
        this.idleDelayMs = Math.max(100, millis);
        idle.setInitialDelay(idleDelayMs);
    }

    /** Tells the bar the content is at its very top (always shown) or not. */
    public void setAtTop(boolean top) {
        this.atTop = top;
        if (top) {
            reveal();
        }
    }

    public void reveal() {
        targetVisibility = 1;
        accumulated = 0;
        startAnimation();
    }

    public void hideNow() {
        if (atTop) {
            return;
        }
        targetVisibility = 0;
        startAnimation();
    }

    /**
     * Reports scrolling: dy &gt; 0 = the content moved down the feed, dy &lt; 0 = back up. Call it for every
     * scroll movement (it restarts the idle timer, which reveals the bar when scrolling stops).
     */
    public void onScroll(int dy) {
        if (dy == 0) {
            return;
        }
        int dir = dy > 0 ? 1 : -1;
        if (dir != lastDirection) {
            accumulated = 0;                            // a direction change must be meant: needs a few pixels
            lastDirection = dir;
        }
        accumulated += Math.abs(dy);

        if (dir > 0 && !atTop && accumulated >= 14) {
            targetVisibility = 0;                       // going down: out of the way
            startAnimation();
        } else if (dir < 0 && accumulated >= 6) {
            targetVisibility = 1;                       // going up: straight back
            startAnimation();
        }
        idle.restart();                                 // when it stops, it comes back
    }

    // ------------------------------------------------------------------ animation

    private void startAnimation() {
        if (!animation.isRunning()) {
            animation.start();
        }
        repaint();
    }

    private void step() {
        boolean moving = false;

        double dv = targetVisibility - visibility;
        if (Math.abs(dv) > 0.004) {
            visibility += dv * 0.24;
            moving = true;
        } else if (visibility != targetVisibility) {
            visibility = targetVisibility;
            visibilityListener.accept(visibility);
        }
        if (moving) {
            visibilityListener.accept(visibility);
        }

        if (hit[selected] != null) {
            float tx = hit[selected].x, tw = hit[selected].width;
            if (pillX < 0) {
                pillX = tx;
                pillW = tw;
            }
            if (Math.abs(tx - pillX) > 0.6f || Math.abs(tw - pillW) > 0.6f) {
                pillX += (tx - pillX) * 0.26f;
                pillW += (tw - pillW) * 0.26f;
                moving = true;
            } else {
                pillX = tx;
                pillW = tw;
            }
        }

        repaint();
        if (!moving && hover == lastPaintedHover) {
            animation.stop();
        }
        lastPaintedHover = hover;
    }

    private int lastPaintedHover = -2;

    // ------------------------------------------------------------------ painting

    private int indexAt(Point p) {
        for (int i = 0; i < hit.length; i++) {
            if (hit[i] != null && hit[i].contains(p)) {
                return i;
            }
        }
        return -1;
    }

    private static FontMetrics metrics(Font f) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        FontMetrics fm = g.getFontMetrics(f);
        g.dispose();
        return fm;
    }

    private int optionWidth(FontMetrics fm, int i) {
        return 22 + 16 + 8 + fm.stringWidth(labels[i]) + 24;
    }

    private int contentWidth() {
        FontMetrics fm = metrics(Theme.bold(13));
        int w = 12;                                     // 6 px inside each end
        for (int i = 0; i < labels.length; i++) {
            w += optionWidth(fm, i) + (i < labels.length - 1 ? 4 : 0);
        }
        return w;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth(), h = getHeight();
        int px = SHADOW, py = SHADOW, pw = w - SHADOW * 2, ph = HEIGHT;

        // soft shadow
        for (int i = SHADOW; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 5 + (SHADOW - i) * 2));
            g2.fill(new RoundRectangle2D.Float(px - i / 2f, py - i / 2f + 3, pw + i, ph + i, ph + i, ph + i));
        }

        // glass body
        RoundRectangle2D.Float body = new RoundRectangle2D.Float(px, py, pw, ph, ph, ph);
        g2.setColor(Theme.alpha(Theme.CARD, 240));
        g2.fill(body);
        g2.setColor(Theme.alpha(Theme.BORDER, 220));
        g2.draw(body);

        // option rectangles
        g2.setFont(Theme.bold(13));
        FontMetrics fm = g2.getFontMetrics();
        int x = px + 6;
        for (int i = 0; i < labels.length; i++) {
            int ow = optionWidth(fm, i);
            hit[i] = new Rectangle(x, py + 5, ow, ph - 10);
            x += ow + 4;
        }
        if (pillX < 0 && hit[selected] != null) {
            pillX = hit[selected].x;
            pillW = hit[selected].width;
        }

        // the highlight, sliding to the chosen option
        Color a = accents[selected][0], b = accents[selected][1];
        if (pillW > 0) {
            g2.setPaint(new GradientPaint(pillX, 0, a, pillX + pillW, 0, b));
            g2.fill(new RoundRectangle2D.Float(pillX, py + 5, pillW, ph - 10, ph - 10, ph - 10));
        }

        // labels + icons
        for (int i = 0; i < labels.length; i++) {
            Rectangle r = hit[i];
            float closeness = pillW <= 0 ? 0 : (float) Math.max(0, 1 - Math.abs((r.x + r.width / 2.0) - (pillX + pillW / 2.0)) / r.width);
            Color base = i == hover ? Theme.TEXT : Theme.MUTED;
            Color c = Anim.mix(base, Color.WHITE, closeness);
            int ix = r.x + 22;
            Icons.paint(g2, icons[i], ix, r.y + (r.height - 16) / 2f, 16, c);
            g2.setColor(c);
            g2.drawString(labels[i], ix + 16 + 8, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }
        g2.dispose();
    }

    /** The bar's own width and height, including the room for its shadow. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(contentWidth() + SHADOW * 2, HEIGHT + SHADOW * 2);
    }

    /** Shadow margin: the owner subtracts it when it centres the bar. */
    public static int shadow() {
        return SHADOW;
    }
}
