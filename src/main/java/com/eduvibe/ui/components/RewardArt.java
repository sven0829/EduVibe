package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;
import com.eduvibe.util.RewardStyle;

import java.awt.*;
import java.awt.geom.*;

/**
 * com.eduvibe.ui.components.RewardArt  (NEW FILE)
 *
 * The single place that DRAWS rewards, so a reward looks identical in the collection, the large
 * preview, the profile header and the My Rewards tab. Everything is vector art generated from the
 * reward's own name and rarity (no image files needed). Epic and legendary items get an animated
 * shimmer / sparkle; common items stay still.
 *
 *   frame  - ring around the avatar (4 styles: solid, double, dashed, beaded)
 *   banner - gradient strip (4 patterns: waves, stripes, dots, rays)
 *   theme  - tint colour + a small mock-up swatch
 *   badge  - shield with a symbol picked from the badge's name
 */
public final class RewardArt {

    private RewardArt() {
    }

    // ------------------------------------------------------------------ helpers

    public static boolean fancy(String rarity) {
        String r = rarity == null ? "" : rarity.toLowerCase();
        return r.equals("epic") || r.equals("legendary");
    }

    public static boolean legendary(String rarity) {
        return rarity != null && rarity.equalsIgnoreCase("legendary");
    }

    private static int hash(String name) {
        return name == null ? 0 : Math.abs(name.hashCode());
    }

    /** 0..1 repeating, used for shimmer sweeps. */
    private static float phase(long periodMs) {
        return (System.currentTimeMillis() % periodMs) / (float) periodMs;
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color themeColor(String name, String rarity) {
        return RewardStyle.primary(name, rarity);
    }

    // -------------------------------------------------------------------- frame

    /**
     * Draws a frame ring around a circle centred at (cx, cy) with diameter d. The ring sits
     * OUTSIDE the circle (thickness about 8% of d), so the avatar itself is untouched.
     */
    public static void paintFrame(Graphics2D g, double cx, double cy, double d, String name, String rarity) {

        Graphics2D g2 = (Graphics2D) g.create();
        RewardFx.quality(g2);

        Color a = RewardStyle.primary(name, rarity);
        Color b = RewardStyle.secondary(name, rarity);
        int tier = RewardFx.tier(rarity);
        if (tier == 4) {
            a = RewardFx.drift(a, 0.035f, 5200);
        }
        double t = Math.max(4, d * 0.085);
        double outer = d / 2 + t + 2;
        int style = hash(name) % 4;
        boolean fancy = fancy(rarity);

        // aura behind the ring: halo for everyone, turning light rays for epic / legendary
        // never let the aura be wider than the room we have (so it fades out instead of being cut off)
        double room = Double.MAX_VALUE;
        Rectangle cb = g2.getClipBounds();
        if (cb != null) {
            room = Math.min(Math.min(cx - cb.x, cb.x + cb.width - cx), Math.min(cy - cb.y, cb.y + cb.height - cy)) - 1;
            room = Math.max(room, outer + 2);
        }
        RewardFx.halo(g2, cx, cy, Math.min(room, outer + t * (1.8 + tier * 0.6)), a, tier);
        if (tier >= 3 && d >= 44) {
            RewardFx.rays(g2, cx, cy, Math.min(room, outer + t * (3.2 + tier)), tier == 4 ? new Color(255, 236, 160) : b,
                    tier == 4 ? 12 : 8, tier == 4 ? 72 : 48);
        }

        double rot = tier >= 1 ? phase(9000 - tier * 1200L) * 360 : 0;
        Paint ringPaint = new GradientPaint((float) (cx - outer), (float) (cy - outer), a,
                (float) (cx + outer), (float) (cy + outer), b);

        switch (style) {
            case 0 -> {                                            // solid gradient ring
                g2.setPaint(ringPaint);
                g2.setStroke(new BasicStroke((float) t));
                g2.draw(new Ellipse2D.Double(cx - d / 2 - t / 2 - 1, cy - d / 2 - t / 2 - 1,
                        d + t + 2, d + t + 2));
            }
            case 1 -> {                                            // double ring
                g2.setPaint(ringPaint);
                float th = (float) (t * 0.42);
                g2.setStroke(new BasicStroke(th));
                g2.draw(new Ellipse2D.Double(cx - d / 2 - th / 2 - 1, cy - d / 2 - th / 2 - 1,
                        d + th + 2, d + th + 2));
                g2.setStroke(new BasicStroke(th));
                double r2 = d / 2 + t * 1.15;
                g2.draw(new Ellipse2D.Double(cx - r2, cy - r2, r2 * 2, r2 * 2));
            }
            case 2 -> {                                            // dashed, slowly rotating
                g2.setPaint(ringPaint);
                float circ = (float) (Math.PI * (d + t));
                float dash = circ / 28f;
                g2.setStroke(new BasicStroke((float) t, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{dash * 0.62f, dash * 0.38f}, 0f));
                g2.rotate(Math.toRadians(rot), cx, cy);
                g2.draw(new Ellipse2D.Double(cx - d / 2 - t / 2 - 1, cy - d / 2 - t / 2 - 1,
                        d + t + 2, d + t + 2));
            }
            default -> {                                           // beads
                int beads = 22;
                double r = d / 2 + t / 2 + 1;
                for (int i = 0; i < beads; i++) {
                    double ang = Math.toRadians(i * 360.0 / beads + rot);
                    double bx = cx + r * Math.cos(ang);
                    double by = cy + r * Math.sin(ang);
                    g2.setColor(i % 2 == 0 ? a : b);
                    double s = t * (i % 2 == 0 ? 0.95 : 0.7);
                    g2.fill(new Ellipse2D.Double(bx - s / 2, by - s / 2, s, s));
                }
            }
        }

        // light travelling around the ring, orbiting sparkles, lens flare on legendary
        double ringR = d / 2 + t / 2 + 1;
        if (tier >= 2) {
            RewardFx.comet(g2, cx, cy, ringR, t * 0.42, tier == 4 ? new Color(255, 240, 190) : Color.WHITE, tier);
        } else if (tier == 1) {
            double ang = phase(4200) * Math.PI * 2;
            RewardFx.glowDot(g2, cx + ringR * Math.cos(ang), cy + ringR * Math.sin(ang), t * 1.3, Color.WHITE, 150);
        } else {
            double ang = phase(6500) * Math.PI * 2;
            RewardFx.glowDot(g2, cx + ringR * Math.cos(ang), cy + ringR * Math.sin(ang), t * 1.0, Color.WHITE, 90);
        }
        int[] counts = {0, 3, 6, 10, 16};
        int n = d < 56 ? Math.min(counts[tier], 4) : counts[tier];
        if (n > 0) {
            RewardFx.motes(g2, cx, cy, outer + t * 0.9, outer + t * 0.9, n, b, tier, hash(name), true, Math.max(1.2, t * 0.33));
        }
        if (tier == 4 && d >= 56) {
            double ang = phase(2600 - tier * 250L) * Math.PI * 2;
            RewardFx.flare(g2, cx + ringR * Math.cos(ang), cy + ringR * Math.sin(ang), t * 1.5,
                    (int) (150 + 80 * RewardFx.pulse(900)));
        }
        g2.dispose();
    }

    private static void sparkle(Graphics2D g, double x, double y, double s) {
        g.setColor(new Color(255, 255, 255, 235));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - s);
        p.quadTo(x, y, x + s, y);
        p.quadTo(x, y, x, y + s);
        p.quadTo(x, y, x - s, y);
        p.quadTo(x, y, x, y - s);
        g.fill(p);
    }

    // ------------------------------------------------------------------- banner

    public static void paintBanner(Graphics2D g, int x, int y, int w, int h, String name, String rarity, int arc) {

        Graphics2D g2 = (Graphics2D) g.create();
        RewardFx.quality(g2);
        int tier = RewardFx.tier(rarity);
        Shape clip = new RoundRectangle2D.Double(x, y, w, h, arc, arc);
        g2.clip(clip);

        Color a = RewardStyle.primary(name, rarity);
        Color b = RewardStyle.secondary(name, rarity);
        g2.setPaint(new GradientPaint(x, y, a, x + w, y + h, b));
        g2.fillRect(x, y, w, h);

        int style = hash(name) % 4;
        g2.setColor(new Color(255, 255, 255, 40));

        switch (style) {
            case 0 -> {                                                  // waves
                for (int k = 0; k < 3; k++) {
                    Path2D p = new Path2D.Double();
                    double base = y + h * (0.45 + k * 0.22);
                    p.moveTo(x, y + h);
                    for (int i = 0; i <= w; i += 8) {
                        p.lineTo(x + i, base + Math.sin((i + k * 40) / 34.0) * h * 0.09);
                    }
                    p.lineTo(x + w, y + h);
                    p.closePath();
                    g2.setColor(new Color(255, 255, 255, 26 + k * 10));
                    g2.fill(p);
                }
            }
            case 1 -> {                                                  // diagonal stripes
                g2.setStroke(new BasicStroke(Math.max(6f, h / 5f)));
                for (int i = -h; i < w + h; i += Math.max(22, h / 2)) {
                    g2.drawLine(x + i, y + h, x + i + h, y);
                }
            }
            case 2 -> {                                                  // dot grid
                int step = Math.max(14, h / 4);
                for (int yy = y + step / 2; yy < y + h; yy += step) {
                    for (int xx = x + ((yy / step) % 2) * step / 2; xx < x + w; xx += step) {
                        g2.fill(new Ellipse2D.Double(xx, yy, 4, 4));
                    }
                }
            }
            default -> {                                                 // light rays
                double cx = x + w * 0.78;
                double cy = y + h * 1.2;
                for (int i = 0; i < 9; i++) {
                    double ang = Math.toRadians(190 + i * 18);
                    Path2D p = new Path2D.Double();
                    p.moveTo(cx, cy);
                    p.lineTo(cx + Math.cos(ang) * w * 1.4, cy + Math.sin(ang) * w * 1.4);
                    p.lineTo(cx + Math.cos(ang + 0.1) * w * 1.4, cy + Math.sin(ang + 0.1) * w * 1.4);
                    p.closePath();
                    g2.setColor(new Color(255, 255, 255, i % 2 == 0 ? 34 : 14));
                    g2.fill(p);
                }
            }
        }

        // bottom fade so text on top stays readable
        g2.setPaint(new GradientPaint(0, y + h * 0.5f, new Color(0, 0, 0, 0), 0, y + h, new Color(0, 0, 0, 70)));
        g2.fillRect(x, y, w, h);

        Color glow = RewardStyle.primary(name, rarity);
        if (tier >= 3) {
            RewardFx.rays(g2, x + w * 0.5, y + h * 1.15, w * 0.85, tier == 4 ? new Color(255, 236, 160) : Color.WHITE,
                    7, tier == 4 ? 46 : 30);
        }
        RewardFx.gloss(g2, x, y, w, h, tier);
        if (tier >= 1) {
            int[] counts = {0, 3, 5, 9, 14};
            RewardFx.motes(g2, x + w / 2.0, y + h / 2.0, w / 2.0 - 4, h / 2.0 - 3, counts[tier], Color.WHITE,
                    tier, hash(name), false, Math.max(1.2, Math.min(3.2, h * 0.045)));
            RewardFx.edgeGlow(g2, x, y, w, h, arc, glow, tier);
        }
        if (tier == 4) {
            double fx = x + w * (0.12 + 0.76 * phase(5200));
            RewardFx.flare(g2, fx, y + h * 0.38, Math.max(8, h * 0.22), (int) (110 + 90 * RewardFx.pulse(1100)));
        }
        g2.dispose();
    }

    // -------------------------------------------------------------------- theme

    /** Small mock profile card tinted with the theme colour. */
    public static void paintTheme(Graphics2D g, int x, int y, int w, int h, String name, String rarity, int arc) {

        Graphics2D g2 = (Graphics2D) g.create();
        RewardFx.quality(g2);
        int tier = RewardFx.tier(rarity);
        Color tint = themeColor(name, rarity);
        Color base = Anim.mix(Theme.CARD, tint, 0.28f);
        Color base2 = Anim.mix(Theme.CARD, tint, 0.08f);

        g2.setPaint(new GradientPaint(x, y, base, x, y + h, base2));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
        g2.setColor(alpha(tint, 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(x + 0.75, y + 0.75, w - 1.5, h - 1.5, arc, arc));

        // mock content
        int pad = Math.max(8, w / 12);
        double d = Math.min(h * 0.34, 34);
        g2.setColor(alpha(tint, 230));
        g2.fill(new Ellipse2D.Double(x + pad, y + pad, d, d));
        g2.setColor(new Color(255, 255, 255, 190));
        g2.fill(new RoundRectangle2D.Double(x + pad + d + 8, y + pad + 3, w * 0.34, 7, 7, 7));
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fill(new RoundRectangle2D.Double(x + pad + d + 8, y + pad + 16, w * 0.22, 6, 6, 6));
        g2.setColor(alpha(tint, 90));
        g2.fill(new RoundRectangle2D.Double(x + pad, y + pad * 2 + d, w - pad * 2, h - pad * 3 - d, 10, 10));

        g2.clip(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
        RewardFx.gloss(g2, x, y, w, h, tier);
        if (tier >= 1) {
            RewardFx.edgeGlow(g2, x, y, w, h, arc, tint, tier);
        }
        if (tier >= 2) {
            int[] counts = {0, 0, 5, 9, 14};
            RewardFx.motes(g2, x + w / 2.0, y + h / 2.0, w / 2.0 - 4, h / 2.0 - 3, counts[tier], tint, tier,
                    hash(name), false, Math.max(1.2, Math.min(3.0, h * 0.04)));
        }
        g2.dispose();
    }

    // -------------------------------------------------------------------- badge

    /** Shield-shaped badge centred at (cx, cy); {@code size} is its width. */
    public static void paintBadge(Graphics2D g, double cx, double cy, double size, String name, String rarity) {

        Graphics2D g2 = (Graphics2D) g.create();
        RewardFx.quality(g2);

        int tier = RewardFx.tier(rarity);
        Color a = RewardStyle.primary(name, rarity);
        Color b = RewardStyle.secondary(name, rarity);
        if (tier == 4) {
            a = RewardFx.drift(a, 0.03f, 5200);
        }
        double w = size;
        double h = size * 1.12;
        double x = cx - w / 2;
        double y = cy - h / 2;

        Path2D shield = new Path2D.Double();
        shield.moveTo(x + w * 0.5, y);
        shield.curveTo(x + w * 0.72, y + h * 0.1, x + w * 0.9, y + h * 0.12, x + w, y + h * 0.14);
        shield.lineTo(x + w, y + h * 0.5);
        shield.curveTo(x + w, y + h * 0.78, x + w * 0.72, y + h * 0.93, x + w * 0.5, y + h);
        shield.curveTo(x + w * 0.28, y + h * 0.93, x, y + h * 0.78, x, y + h * 0.5);
        shield.lineTo(x, y + h * 0.14);
        shield.curveTo(x + w * 0.1, y + h * 0.12, x + w * 0.28, y + h * 0.1, x + w * 0.5, y);
        shield.closePath();

        RewardFx.halo(g2, cx, cy, size * (0.78 + 0.08 * tier), a, tier);
        if (tier >= 3) {
            RewardFx.rays(g2, cx, cy, size * 1.25, tier == 4 ? new Color(255, 236, 160) : b,
                    tier == 4 ? 12 : 8, tier == 4 ? 70 : 46);
        }

        g2.setColor(new Color(0, 0, 0, 70));
        g2.translate(0, size * 0.04);
        g2.fill(shield);
        g2.translate(0, -size * 0.04);

        g2.setPaint(new GradientPaint((float) x, (float) y, a, (float) (x + w), (float) (y + h), b));
        g2.fill(shield);

        // inner bevel
        g2.setColor(new Color(255, 255, 255, 90));
        g2.setStroke(new BasicStroke((float) Math.max(1.2, size * 0.035)));
        g2.draw(shield);
        AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
        at.scale(0.76, 0.76);
        at.translate(-cx, -cy);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.draw(at.createTransformedShape(shield));

        // symbol
        float is = (float) (size * 0.46);
        Icons.paint(g2, symbolFor(name), (float) (cx - is / 2), (float) (cy - is / 2 - size * 0.02), is, Color.WHITE);

        Graphics2D sh = (Graphics2D) g2.create();
        sh.clip(shield);
        RewardFx.gloss(sh, x, y, w, h, tier);
        sh.dispose();
        if (tier >= 2) {
            int[] counts = {0, 0, 4, 7, 12};
            RewardFx.motes(g2, cx, cy, size * 0.62, size * 0.68, counts[tier], b, tier, hash(name), true,
                    Math.max(1.2, size * 0.04));
        }
        if (tier == 4) {
            RewardFx.flare(g2, x + w * (0.18 + 0.64 * phase(3600)), y + h * 0.28, size * 0.2,
                    (int) (120 + 90 * RewardFx.pulse(1000)));
        }
        g2.dispose();
    }

    private static Icons.Type symbolFor(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.contains("book") || n.contains("scholar") || n.contains("library") || n.contains("note")) {
            return Icons.Type.BOOK;
        }
        if (n.contains("streak") || n.contains("flame") || n.contains("fire")) {
            return Icons.Type.FLAME;
        }
        if (n.contains("legend") || n.contains("valedictorian") || n.contains("champ") || n.contains("titan")) {
            return Icons.Type.TROPHY;
        }
        if (n.contains("team") || n.contains("mentor") || n.contains("group")) {
            return Icons.Type.USERS;
        }
        if (n.contains("early") || n.contains("night") || n.contains("focus")) {
            return Icons.Type.CLOCK;
        }
        if (n.contains("comment") || n.contains("debate")) {
            return Icons.Type.COMMENT;
        }
        if (n.contains("creator") || n.contains("idea") || n.contains("spark") || n.contains("brain")) {
            return Icons.Type.SPARKLE;
        }
        if (n.contains("new")) {
            return Icons.Type.SMILE;
        }
        return Icons.Type.STAR;
    }

    // ------------------------------------------------------------ one-call helper

    /** Draws a reward of the given slot ("frame","banner","theme","badge") into a thumbnail box. */
    public static void paintThumb(Graphics2D g, String slot, int x, int y, int w, int h, String name, String rarity) {

        Graphics2D g2 = (Graphics2D) g.create();
        RewardFx.quality(g2);
        g2.clip(new RoundRectangle2D.Double(x, y, w, h, 12, 12));
        switch (slot) {
            case "frame" -> {
                double d = Math.min(w, h) * 0.52;
                double cx = x + w / 2.0;
                double cy = y + h / 2.0;
                paintFrame(g2, cx, cy, d, name, rarity);
                g2.setPaint(new GradientPaint((float) cx, (float) (cy - d / 2), new Color(71, 85, 105),
                        (float) cx, (float) (cy + d / 2), new Color(51, 65, 85)));
                g2.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
                g2.setColor(new Color(148, 163, 184));
                g2.fill(new Ellipse2D.Double(cx - d * 0.17, cy - d * 0.27, d * 0.34, d * 0.34));
                g2.fill(new Arc2D.Double(cx - d * 0.3, cy + d * 0.02, d * 0.6, d * 0.6, 0, 180, Arc2D.CHORD));
                g2.setClip(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
                g2.setClip(null);
            }
            case "banner" -> paintBanner(g2, x + 6, y + (int) (h * 0.2), w - 12, (int) (h * 0.6), name, rarity, 14);
            case "theme" -> paintTheme(g2, x + 10, y + 8, w - 20, h - 16, name, rarity, 14);
            default -> paintBadge(g2, x + w / 2.0, y + h / 2.0, Math.min(w, h) * 0.56, name, rarity);
        }
        g2.dispose();
    }

    /** Small badge icon as an Icon (for labels next to names). */
    public static javax.swing.Icon badgeIcon(String name, String rarity, int size) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paintBadge(g, size / 2.0, size / 2.0, size * 0.9, name, rarity);
        g.dispose();
        return new javax.swing.ImageIcon(img);
    }

    /** Looks up the user's equipped badge in the background and shows it on the label (icon after the text). */
    public static void attachBadge(javax.swing.JLabel label, int userId, int size) {
        if (userId <= 0) {
            return;
        }
        new javax.swing.SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                return com.eduvibe.dao.EquipDAO.badgeOf(userId);
            }

            @Override
            protected void done() {
                try {
                    String[] b = get();
                    if (b != null) {
                        label.setIcon(badgeIcon(b[0], b[1], size));
                        label.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
                        label.setIconTextGap(6);
                        label.setToolTipText(b[0]);
                    }
                } catch (Exception ignored) {
                    // no badge
                }
            }
        }.execute();
    }
}
