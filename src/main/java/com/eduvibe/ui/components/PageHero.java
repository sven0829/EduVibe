package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * com.eduvibe.ui.components.PageHero  (NEW FILE)
 *
 * The coloured banner at the top of a page: its own two-colour gradient, a big faint icon,
 * a title, a one-line subtitle and an optional chip on the right (for example "12 posts").
 * Every page picks its own colours so the app feels alive instead of one flat look.
 */
public class PageHero extends JComponent {

    private final Icons.Type icon;
    private final String title;
    private String subtitle;
    private final Color a;
    private final Color b;
    private String chip;
    private Icons.Type chipIcon;

    public PageHero(Icons.Type icon, String title, String subtitle, Color a, Color b) {
        this.icon = icon;
        this.title = title;
        this.subtitle = subtitle;
        this.a = a;
        this.b = b;
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(100, 92));
        setMinimumSize(new Dimension(100, 92));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
    }

    /** Small pill on the right side of the banner; null or empty hides it. */
    public void setChip(String text, Icons.Type chipIcon) {
        this.chip = text;
        this.chipIcon = chipIcon;
        repaint();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth(), h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 20, 20);
        g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
        g2.fill(shape);
        g2.setClip(shape);

        // soft light blobs
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fill(new Ellipse2D.Float(w - 170, -80, 240, 240));
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fill(new Ellipse2D.Float(w - 300, 28, 160, 160));
        g2.setPaint(new GradientPaint(0, h * 0.5f, new Color(255, 255, 255, 0), 0, h, new Color(0, 0, 0, 40)));
        g2.fillRect(0, 0, w, h);

        // big ghost icon
        Icons.paint(g2, icon, w - 104, h - 76, 84, new Color(255, 255, 255, 34));
        g2.setClip(null);

        // icon bubble + text
        int bub = 48;
        int bx = 22, by = (h - bub) / 2;
        g2.setColor(new Color(255, 255, 255, 48));
        g2.fillOval(bx, by, bub, bub);
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawOval(bx, by, bub - 1, bub - 1);
        Icons.paint(g2, icon, bx + 12, by + 12, 24, Color.WHITE);

        int tx = bx + bub + 16;
        g2.setColor(Color.WHITE);
        g2.setFont(Theme.font(Font.BOLD, 22));
        g2.drawString(title, tx, h / 2 - 2);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.setFont(Theme.font(Font.PLAIN, 13));
        g2.drawString(fit(g2, subtitle, w - tx - 150), tx, h / 2 + 20);

        if (chip != null && !chip.isEmpty()) {
            g2.setFont(Theme.font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int iconW = chipIcon == null ? 0 : 20;
            int cw = fm.stringWidth(chip) + 28 + iconW, ch = 30;
            int cx = w - cw - 20, cy = 16;
            g2.setColor(new Color(0, 0, 0, 46));
            g2.fillRoundRect(cx, cy, cw, ch, ch, ch);
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawRoundRect(cx, cy, cw - 1, ch - 1, ch, ch);
            if (chipIcon != null) {
                Icons.paint(g2, chipIcon, cx + 10, cy + 7, 16, Color.WHITE);
            }
            g2.setColor(Color.WHITE);
            g2.drawString(chip, cx + 14 + iconW, cy + (ch - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }

    private static String fit(Graphics2D g2, String s, int max) {
        if (s == null) {
            return "";
        }
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(s) <= max || max < 40) {
            return s;
        }
        while (s.length() > 4 && fm.stringWidth(s + "...") > max) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    // ---- ready-made colour pairs, one per page ----
    public static final Color[] ACADEMIC = {new Color(37, 99, 235), new Color(14, 165, 233)};
    public static final Color[] ENTERTAINMENT = {new Color(124, 58, 237), new Color(236, 72, 153)};
    public static final Color[] FOLLOWING = {new Color(249, 115, 22), new Color(225, 29, 72)};
    public static final Color[] REELS = {new Color(219, 39, 119), new Color(109, 40, 217)};
    public static final Color[] LEADERBOARD = {new Color(245, 158, 11), new Color(220, 38, 38)};
    public static final Color[] NOTIFICATIONS = {new Color(59, 130, 246), new Color(139, 92, 246)};
    public static final Color[] PEOPLE = {new Color(16, 185, 129), new Color(37, 99, 235)};
    public static final Color[] HOME = {new Color(79, 70, 229), new Color(6, 182, 212)};
    public static final Color[] SAVED = {new Color(13, 148, 136), new Color(37, 99, 235)};
    public static final Color[] REWARDS = {new Color(234, 179, 8), new Color(234, 88, 12)};
    public static final Color[] PLAYTIME = {new Color(147, 51, 234), new Color(236, 72, 153)};
    public static final Color[] CUSTOMIZE = {new Color(236, 72, 153), new Color(99, 102, 241)};
    public static final Color[] ADMIN = {new Color(30, 41, 59), new Color(37, 99, 235)};
    public static final Color[] SETTINGS = {new Color(71, 85, 130), new Color(124, 92, 250)};
}
