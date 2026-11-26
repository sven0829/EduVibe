package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * com.eduvibe.ui.components.Aurora  (NEW FILE)
 *
 * The soft glowing page background. One picture is drawn for the whole window and every flat
 * background panel shows its own slice of it, so the glow runs seamlessly behind all pages,
 * never shifts when you scroll, and needs no change to the pages themselves.
 */
public final class Aurora {

    private static BufferedImage cache;
    private static boolean cacheLight;

    private static volatile boolean reduced = loadReduced();

    private Aurora() {
    }

    private static boolean loadReduced() {
        try {
            return java.util.prefs.Preferences.userRoot().node("eduvibe/ui").getBoolean("reduceEffects", false);
        } catch (Exception e) {
            return false;
        }
    }

    /** True when the person turned the glow off (for slower computers). */
    public static boolean isReduced() {
        return reduced;
    }

    /** Turns the glow off or on right away and remembers the choice. */
    public static void setReduced(boolean value) {
        reduced = value;
        try {
            java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("eduvibe/ui");
            p.putBoolean("reduceEffects", value);
            p.flush();
        } catch (Exception ignored) {
            // the switch still works for this session
        }
        for (Window w : Window.getWindows()) {
            w.repaint();
        }
    }

    /** True for a panel that should show the glow instead of a flat colour. */
    static boolean wants(JComponent c) {
        return !reduced && c.isOpaque() && Theme.BACKGROUND.equals(c.getBackground());
    }

    /** Paints this component's slice of the window-wide glow (flat colour if not on screen yet). */
    static void paint(Graphics g, JComponent c) {
        JRootPane root = SwingUtilities.getRootPane(c);
        if (root == null || root.getWidth() < 2 || root.getHeight() < 2) {
            g.setColor(c.getBackground());
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            return;
        }
        Point p = SwingUtilities.convertPoint(c, 0, 0, root);
        g.drawImage(image(root.getWidth(), root.getHeight()), -p.x, -p.y, null);
    }

    private static synchronized BufferedImage image(int w, int h) {
        if (cache != null && cache.getWidth() == w && cache.getHeight() == h && cacheLight == Theme.isLight()) {
            return cache;
        }
        cacheLight = Theme.isLight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Theme.BACKGROUND);
        g.fillRect(0, 0, w, h);

        boolean light = cacheLight;
        // violet glow, top left
        glow(g, w * 0.12, -h * 0.08, Math.max(w, h) * 0.62, new Color(124, 92, 250), light ? 40 : 70);
        // blue glow, top right
        glow(g, w * 0.98, h * 0.02, Math.max(w, h) * 0.55, new Color(59, 130, 246), light ? 36 : 56);
        // deep indigo bloom, bottom middle
        glow(g, w * 0.55, h * 1.12, Math.max(w, h) * 0.6, new Color(99, 82, 230), light ? 26 : 44);
        // soft magenta whisper, mid left
        glow(g, -w * 0.05, h * 0.62, Math.max(w, h) * 0.38, new Color(190, 90, 230), light ? 14 : 24);
        g.dispose();
        cache = img;
        return img;
    }

    private static void glow(Graphics2D g, double cx, double cy, double radius, Color c, int alpha) {
        float[] stops = {0f, 0.45f, 1f};
        Color[] colors = {
                new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha),
                new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 3),
                new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)};
        g.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Double(cx, cy), (float) radius, stops, colors));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, 6000, 6000));
    }
}
