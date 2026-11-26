package com.eduvibe.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * com.eduvibe.ui.components.RewardFx  (NEW FILE)
 *
 * Visual-effects toolkit used by RewardArt and the chest reveal. Everything is stateless:
 * positions are computed from the clock, so any number of rewards can animate at once.
 *
 *   tier 0 common    - soft rim light and a slow gloss sweep
 *   tier 1 uncommon  - gentle pulsing glow + a few drifting motes
 *   tier 2 rare      - stronger glow, orbiting comet highlight, rising motes
 *   tier 3 epic      - aura, rotating light rays, twinkling star sparkles
 *   tier 4 legendary - golden halo, god rays, shooting sparks, lens flare
 *
 * One shared 30 fps clock repaints only components that are on screen.
 */
public final class RewardFx {

    private RewardFx() {
    }

    // ------------------------------------------------------------------ clock

    private static final List<WeakReference<JComponent>> ANIMATED = new ArrayList<>();
    private static Timer clock;

    /** Keeps {@code c} repainting (about 30 fps) while it is visible. Safe to call many times. */
    public static void animate(JComponent c) {
        synchronized (ANIMATED) {
            for (WeakReference<JComponent> ref : ANIMATED) {
                if (ref.get() == c) {
                    return;
                }
            }
            ANIMATED.add(new WeakReference<>(c));
            if (clock == null) {
                clock = new Timer(33, e -> tick());
                clock.start();
            }
        }
    }

    private static void tick() {
        synchronized (ANIMATED) {
            Iterator<WeakReference<JComponent>> it = ANIMATED.iterator();
            while (it.hasNext()) {
                JComponent c = it.next().get();
                if (c == null) {
                    it.remove();
                    continue;
                }
                if (c.isShowing() && !c.getVisibleRect().isEmpty()) {
                    c.repaint();
                }
            }
            if (ANIMATED.isEmpty() && clock != null) {
                clock.stop();
                clock = null;
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    public static int tier(String rarity) {
        String r = rarity == null ? "" : rarity.trim().toLowerCase();
        return switch (r) {
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            default -> 0;
        };
    }

    /** Best-quality hints (smooth shapes, gradients and sprites). */
    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /** 0..1 repeating. */
    public static float phase(long periodMs) {
        return (System.currentTimeMillis() % periodMs) / (float) periodMs;
    }

    /** 0..1..0 smooth pulse. */
    public static float pulse(long periodMs) {
        return 0.5f + 0.5f * (float) Math.sin(phase(periodMs) * Math.PI * 2);
    }

    /** Deterministic pseudo-random 0..1 from a seed. */
    public static double rnd(long seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= (x >>> 29);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 32);
        return (x & 0xFFFFFFL) / (double) 0x1000000L;
    }

    /** Shifts hue a little over time (used for the legendary shimmer). */
    public static Color drift(Color c, float amount, long periodMs) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float h = (hsb[0] + amount * (float) Math.sin(phase(periodMs) * Math.PI * 2) + 1f) % 1f;
        return Color.getHSBColor(h, Math.min(1f, hsb[1] * 0.92f), Math.min(1f, hsb[2] + 0.05f));
    }

    // ---------------------------------------------------------------- shapes

    /** Four-point star sparkle. */
    public static void star(Graphics2D g, double x, double y, double s, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - s);
        p.quadTo(x, y, x + s, y);
        p.quadTo(x, y, x, y + s);
        p.quadTo(x, y, x - s, y);
        p.quadTo(x, y, x, y - s);
        g.setColor(c);
        g.fill(p);
        g.setPaint(new RadialGradientPaint((float) x, (float) y, (float) Math.max(1, s * 0.9),
                new float[] {0f, 1f}, new Color[] {alpha(Color.WHITE, 200), alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(x - s * 0.9, y - s * 0.9, s * 1.8, s * 1.8));
    }

    /** Soft glowing dot. */
    public static void glowDot(Graphics2D g, double x, double y, double r, Color c, int a) {
        g.setPaint(new RadialGradientPaint((float) x, (float) y, (float) Math.max(1, r),
                new float[] {0f, 0.45f, 1f},
                new Color[] {alpha(Color.WHITE, Math.min(255, a)), alpha(c, (int) (a * 0.7)), alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }

    // ---------------------------------------------------------------- effects

    /** Pulsing circular halo. */
    public static void halo(Graphics2D g, double cx, double cy, double r, Color c, int tier) {
        float p = pulse(2800 - tier * 330L);
        int a = (int) (26 + tier * 24 + p * (20 + tier * 10));
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r,
                new float[] {0.35f, 0.7f, 1f},
                new Color[] {alpha(c, a), alpha(c, (int) (a * 0.45)), alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    /** Slowly turning wedges of light behind an object (epic / legendary). */
    public static void rays(Graphics2D g, double cx, double cy, double r, Color c, int count, int alphaMax) {
        Graphics2D g2 = (Graphics2D) g.create();
        double rot = phase(14000) * Math.PI * 2;
        float breathe = 0.7f + 0.3f * pulse(3000);
        for (int i = 0; i < count; i++) {
            double a = rot + i * Math.PI * 2 / count;
            double w = Math.PI / count * 0.55;
            Path2D wedge = new Path2D.Double();
            wedge.moveTo(cx, cy);
            wedge.lineTo(cx + Math.cos(a - w) * r, cy + Math.sin(a - w) * r);
            wedge.lineTo(cx + Math.cos(a + w) * r, cy + Math.sin(a + w) * r);
            wedge.closePath();
            g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r,
                    new float[] {0f, 1f},
                    new Color[] {alpha(c, (int) (alphaMax * breathe)), alpha(c, 0)}));
            g2.fill(wedge);
        }
        g2.dispose();
    }

    /**
     * Drifting sparkles. {@code orbit}: circle/ellipse around the centre; otherwise they rise from
     * the bottom of the box (cx +/- rx, cy +/- ry) and fade out. Stars twinkle for tier 3 and up.
     */
    public static void motes(Graphics2D g, double cx, double cy, double rx, double ry, int count,
                             Color c, int tier, long seed, boolean orbit, double size) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            long s = seed * 31 + i * 7919L;
            long period = 2600 + (long) (rnd(s) * 2200);
            double p = ((now + (long) (rnd(s + 1) * period)) % period) / (double) period;
            double fade = Math.sin(Math.PI * p);
            double x;
            double y;
            if (orbit) {
                double ang = Math.PI * 2 * (p + i / (double) count);
                double wobble = 1 + 0.06 * Math.sin(now / 700.0 + i);
                x = cx + rx * wobble * Math.cos(ang);
                y = cy + ry * wobble * Math.sin(ang);
            } else {
                x = cx + (rnd(s + 2) * 2 - 1) * rx + Math.sin(now / 900.0 + i) * rx * 0.08;
                y = cy + ry - p * 2 * ry;
            }
            double sz = size * (0.55 + rnd(s + 3) * 0.9);
            int a = (int) (230 * fade);
            if (tier >= 3 && i % 2 == 0) {
                star(g, x, y, sz * 1.5 * (0.7 + 0.3 * Math.sin(now / 160.0 + i)), alpha(Color.WHITE, a));
            } else {
                glowDot(g, x, y, sz * 1.6, c, a);
            }
        }
    }

    /** Bright comet travelling around a circle, with a fading tail. */
    public static void comet(Graphics2D g, double cx, double cy, double r, double thickness, Color c, int tier) {
        double head = phase(2600 - tier * 250L) * Math.PI * 2;
        int segs = 14;
        for (int i = segs; i >= 0; i--) {
            double a = head - i * 0.045;
            double x = cx + r * Math.cos(a);
            double y = cy + r * Math.sin(a);
            double f = 1 - i / (double) segs;
            glowDot(g, x, y, thickness * (0.5 + 1.1 * f), c, (int) (210 * f * f));
        }
        glowDot(g, cx + r * Math.cos(head), cy + r * Math.sin(head), thickness * 1.9, Color.WHITE, 230);
    }

    /** Anamorphic lens flare: two thin crossing streaks and a core. */
    public static void flare(Graphics2D g, double x, double y, double s, int a) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new LinearGradientPaint((float) (x - s * 2.4), (float) y, (float) (x + s * 2.4), (float) y,
                new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, a), new Color(255, 255, 255, 0)}));
        g2.fill(new RoundRectangle2D.Double(x - s * 2.4, y - s * 0.07, s * 4.8, s * 0.14, s * 0.14, s * 0.14));
        g2.setPaint(new LinearGradientPaint((float) x, (float) (y - s * 1.5), (float) x, (float) (y + s * 1.5),
                new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, (int) (a * 0.8)), new Color(255, 255, 255, 0)}));
        g2.fill(new RoundRectangle2D.Double(x - s * 0.06, y - s * 1.5, s * 0.12, s * 3, s * 0.12, s * 0.12));
        glowDot(g2, x, y, s * 0.8, new Color(255, 245, 200), a);
        g2.dispose();
    }

    /** Diagonal gloss sweep across a rectangle (call with the clip already set). */
    public static void gloss(Graphics2D g, double x, double y, double w, double h, int tier) {
        long period = 4200 - tier * 520L;
        float p = phase(period);
        // pause between sweeps: the highlight only travels during the first 62% of the cycle
        double t = Math.min(1.0, p / 0.62);
        double sx = x - w * 0.45 + t * w * 1.9;
        int a = 34 + tier * 22;
        g.setPaint(new LinearGradientPaint((float) sx, (float) y, (float) (sx + w * 0.34), (float) (y + h * 0.5),
                new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, p < 0.62 ? a : 0), new Color(255, 255, 255, 0)}));
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }

    /** Glowing pulsing border following a rounded rectangle. */
    public static void edgeGlow(Graphics2D g, double x, double y, double w, double h, double arc, Color c, int tier) {
        float p = pulse(2400 - tier * 260L);
        for (int i = 3; i >= 1; i--) {
            g.setStroke(new BasicStroke(i * 2.2f));
            g.setColor(alpha(c, (int) ((10 + tier * 8 + p * 16) / i)));
            g.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, arc, arc));
        }
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(alpha(Color.WHITE, (int) (40 + tier * 12 + p * 30)));
        g.draw(new RoundRectangle2D.Double(x + 0.7, y + 0.7, w - 1.4, h - 1.4, arc, arc));
    }
}
