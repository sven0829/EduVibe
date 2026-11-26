package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * com.eduvibe.ui.components.ProfilePreview  (NEW FILE)
 *
 * Large live preview of a profile: banner, avatar with frame, name, badge and theme tint.
 * Changing the look cross-fades from the old picture to the new one. Used by the Customize page.
 */
public class ProfilePreview extends JPanel {

    /** One reward shown in a slot (name + rarity are all the art needs). */
    public record Piece(int id, String name, String rarity) {
    }

    /** What the preview currently wears; any field may be null. */
    public record Look(Piece frame, Piece banner, Piece theme, Piece badge) {
        public static final Look EMPTY = new Look(null, null, null, null);
    }

    private final Avatar avatar = new Avatar(104);
    private String fullName = "Your Name";
    private String username = "username";

    private Look look = Look.EMPTY;
    private String caption = "";
    private Color captionColor = Theme.BLUE;

    private BufferedImage snapshot;
    private float snapAlpha;
    private final Timer fadeTimer;
    private long fadeStart;

    public ProfilePreview() {
        setLayout(null);
        setOpaque(false);
        add(avatar);
        RewardFx.animate(this);
        setPreferredSize(new Dimension(640, 300));
        setMinimumSize(new Dimension(320, 300));

        fadeTimer = new Timer(15, e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - fadeStart) / 320f);
            snapAlpha = 1f - (float) (1 - Math.pow(1 - t, 3));
            if (t >= 1f) {
                snapshot = null;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
    }

    public void setUser(String fullName, String username, String profilePic) {
        this.fullName = fullName == null || fullName.isBlank() ? "Your Name" : fullName;
        this.username = username == null ? "" : username;
        avatar.setUser(fullName, profilePic);
        repaint();
    }

    public void setCaption(String text, Color color) {
        this.caption = text == null ? "" : text;
        this.captionColor = color == null ? Theme.BLUE : color;
        repaint();
    }

    /** Switches to a new look with a smooth cross-fade. */
    public void setLook(Look next) {

        Look target = next == null ? Look.EMPTY : next;
        if (target.equals(look)) {
            return;
        }

        if (getWidth() > 0 && getHeight() > 0 && isShowing()) {
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            paint(g);
            g.dispose();
            snapshot = img;
            snapAlpha = 1f;
            fadeStart = System.currentTimeMillis();
            fadeTimer.restart();
        }
        look = target;
        repaint();
    }

    private int bannerHeight() {
        return (int) (getHeight() * 0.46);
    }

    @Override
    public void doLayout() {
        avatar.setBounds(34, bannerHeight() - 52, 104, 104);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth();
        int h = getHeight();
        int arc = 24;
        int bh = bannerHeight();

        Shape card = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, arc, arc);

        // theme tint
        Color top = Theme.CARD;
        Color bottom = Theme.CARD;
        Color border = Theme.BORDER;
        if (look.theme() != null) {
            Color tint = RewardArt.themeColor(look.theme().name(), look.theme().rarity());
            top = Anim.mix(Theme.CARD, tint, 0.34f);
            bottom = Anim.mix(Theme.CARD, tint, 0.10f);
            border = RewardArt.alpha(tint, 170);
        }
        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fill(card);

        // banner
        Graphics2D b = (Graphics2D) g2.create();
        b.clip(card);
        b.clipRect(0, 0, w, bh);
        if (look.banner() != null) {
            RewardArt.paintBanner(b, 0, 0, w, bh + 30, look.banner().name(), look.banner().rarity(), arc);
        } else {
            boolean lt = Theme.isLight();
            b.setPaint(lt ? new GradientPaint(0, 0, new Color(203, 213, 225), w, bh, new Color(226, 232, 240))
                    : new GradientPaint(0, 0, new Color(51, 65, 85), w, bh, new Color(30, 41, 59)));
            b.fillRect(0, 0, w, bh);
            b.setFont(Theme.regular(12));
            b.setColor(lt ? new Color(71, 85, 105, 190) : new Color(148, 163, 184, 150));
            String t = "No banner equipped";
            b.drawString(t, w - 18 - b.getFontMetrics().stringWidth(t), bh - 16);
        }
        b.dispose();

        // avatar ring
        double acx = 34 + 52;
        double acy = bh - 52 + 52;
        g2.setColor(top);
        g2.fill(new Ellipse2D.Double(acx - 57, acy - 57, 114, 114));
        if (look.frame() != null) {
            RewardArt.paintFrame(g2, acx, acy, 104, look.frame().name(), look.frame().rarity());
        } else {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(acx - 54, acy - 54, 108, 108));
        }

        // name + username
        int tx = 34 + 104 + 22;
        g2.setFont(Theme.bold(22));
        g2.setColor(Theme.TEXT);
        g2.drawString(fullName, tx, bh + 36);
        g2.setFont(Theme.regular(13));
        g2.setColor(Theme.MUTED);
        g2.drawString("@" + username, tx, bh + 58);

        // badge
        int by = bh + 68;
        if (look.badge() != null) {
            RewardArt.paintBadge(g2, tx + 17, by + 18, 30, look.badge().name(), look.badge().rarity());
            g2.setFont(Theme.bold(13));
            g2.setColor(Theme.TEXT);
            g2.drawString(look.badge().name(), tx + 42, by + 15);
            g2.setFont(Theme.regular(11));
            g2.setColor(RewardArt.themeColor(look.badge().name(), look.badge().rarity()));
            g2.drawString(look.badge().rarity() == null ? "" : look.badge().rarity().toUpperCase(), tx + 42, by + 31);
        } else {
            g2.setFont(Theme.regular(12));
            g2.setColor(Theme.MUTED);
            g2.drawString("No badge equipped", tx, by + 20);
        }

        // mock stats row (shape only, so the layout reads like a real profile)
        int sy = h - 56;
        int sw = (w - 68 - 24) / 3;
        for (int i = 0; i < 3; i++) {
            int sx = 34 + i * (sw + 12);
            boolean lt = Theme.isLight();
            g2.setColor(lt ? new Color(15, 23, 42, 14) : new Color(255, 255, 255, 14));
            g2.fill(new RoundRectangle2D.Double(sx, sy, sw, 40, 12, 12));
            g2.setColor(lt ? new Color(15, 23, 42, 60) : new Color(255, 255, 255, 70));
            g2.fill(new RoundRectangle2D.Double(sx + 14, sy + 11, Math.min(46, sw * 0.4), 7, 7, 7));
            g2.setColor(lt ? new Color(15, 23, 42, 32) : new Color(255, 255, 255, 36));
            g2.fill(new RoundRectangle2D.Double(sx + 14, sy + 24, Math.min(72, sw * 0.6), 6, 6, 6));
        }

        // caption pill (top right over the banner)
        if (!caption.isEmpty()) {
            g2.setFont(Theme.bold(11));
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(caption) + 36;
            int px = w - pw - 14;
            g2.setColor(new Color(15, 23, 42, 190));
            g2.fill(new RoundRectangle2D.Double(px, 14, pw, 26, 26, 26));
            g2.setColor(captionColor);
            g2.fill(new Ellipse2D.Double(px + 10, 24, 7, 7));
            g2.setColor(Color.WHITE);
            g2.drawString(caption, px + 22, 31);
        }

        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(card);
        g2.dispose();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);   // card, avatar child
        if (snapshot != null && snapAlpha > 0f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(snapAlpha));
            g2.drawImage(snapshot, 0, 0, null);
            g2.dispose();
        }
    }
}
