package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** NEW FILE - flat rounded button with hover/press states and optional vector icon. */
public class RoundedButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, OUTLINE, GHOST, DANGER, SUCCESS }

    private Style style;
    private Icons.Type iconType;
    private int iconSize = 16;
    private boolean hover;
    private boolean pressed;
    private final Anim.Fade fade = new Anim.Fade(this);
    private int arc = 12;

    public RoundedButton(String text) {
        this(text, Style.PRIMARY);
    }

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(Theme.bold(13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                fade.to(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                fade.to(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    private int compactPad = -1;

    /** Sizes the button to exactly its text/icon plus {@code pad} on each side (no minimum width). */
    public RoundedButton compact(int pad) {
        this.compactPad = pad;
        revalidate();
        return this;
    }

    private float iconPop = 1f;
    private javax.swing.Timer popTimer;

    /** A quick "pop" of the icon (grows, overshoots, settles): used when a heart is liked. */
    public void pulseIcon() {
        if (popTimer != null) {
            popTimer.stop();
        }
        final long start = System.nanoTime();
        popTimer = new javax.swing.Timer(15, null);
        popTimer.addActionListener(e -> {
            double t = (System.nanoTime() - start) / 380e6;
            if (t >= 1) {
                iconPop = 1f;
                popTimer.stop();
            } else {
                // up to 1.55x quickly, then a small springy settle back to 1
                iconPop = (float) (1 + 0.55 * Math.sin(Math.PI * Math.min(1, t * 1.15)) * Math.pow(1 - t, 0.6));
            }
            repaint();
        });
        popTimer.start();
    }

    public RoundedButton withIcon(Icons.Type type) {
        this.iconType = type;
        return this;
    }

    public RoundedButton withArc(int arc) {
        this.arc = arc;
        return this;
    }

    public void setStyle(Style style) {
        this.style = style;
        repaint();
    }

    private Color accent;

    /** Overrides the text/icon colour (e.g. red for a liked heart). Pass null to reset. */
    public void setAccent(Color accent) {
        this.accent = accent;
        repaint();
    }

    private Color background() {
        if (!isEnabled() || pressed) {
            return background(hover);
        }
        return Anim.mix(background(false), background(true), fade.value());
    }

    private Color background(boolean hover) {
        if (!isEnabled()) {
            return Theme.CARD_LIGHT;
        }
        return switch (style) {
            case PRIMARY -> pressed ? new Color(29, 78, 216)
                    : hover ? Theme.BLUE_HOVER : Theme.BLUE;
            case SECONDARY -> hover ? new Color(51, 65, 85) : Theme.CARD_LIGHT;
            case OUTLINE -> hover ? Theme.CARD_LIGHT : Theme.alpha(Theme.CARD_LIGHT, 0);
            case GHOST -> hover ? Theme.CARD_LIGHT : Theme.alpha(Theme.CARD_LIGHT, 0);
            case DANGER -> hover ? new Color(220, 38, 38) : Theme.alpha(Theme.RED, 40);
            case SUCCESS -> hover ? new Color(22, 163, 74) : Theme.GREEN;
        };
    }

    private Color foreground() {
        if (!isEnabled() || accent != null) {
            return foreground(hover);
        }
        return Anim.mix(foreground(false), foreground(true), fade.value());
    }

    private Color foreground(boolean hover) {
        if (!isEnabled()) {
            return Theme.MUTED;
        }
        if (accent != null) {
            return accent;
        }
        return switch (style) {
            case PRIMARY, SUCCESS -> Color.WHITE;
            case DANGER -> hover ? Color.WHITE : (Theme.isLight() ? Theme.RED : new Color(252, 165, 165));
            case GHOST -> hover ? Theme.TEXT : Theme.MUTED;
            default -> Theme.TEXT;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        if (style == Style.PRIMARY && isEnabled()) {
            float lift = pressed ? -0.2f : hover ? 0.14f : 0f;
            g2.setPaint(Theme.brand(0, 0, getWidth(), getHeight() * 0.6f, lift));
        } else {
            g2.setColor(background());
        }
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        if (style == Style.OUTLINE) {
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }

        Color fg = foreground();
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();

        String text = getText() == null ? "" : getText();
        int textWidth = fm.stringWidth(text);
        int iconSpace = iconType == null ? 0 : iconSize + (text.isEmpty() ? 0 : 8);
        int totalWidth = textWidth + iconSpace;

        int x = (getWidth() - totalWidth) / 2;

        if (iconType != null) {
            if (iconPop != 1f) {
                java.awt.geom.AffineTransform saved = g2.getTransform();
                g2.translate(x + iconSize / 2.0, getHeight() / 2.0);
                g2.scale(iconPop, iconPop);
                g2.translate(-(x + iconSize / 2.0), -getHeight() / 2.0);
                Icons.paint(g2, iconType, x, (getHeight() - iconSize) / 2f, iconSize, fg);
                g2.setTransform(saved);
            } else {
                Icons.paint(g2, iconType, x, (getHeight() - iconSize) / 2f, iconSize, fg);
            }
            x += iconSpace;
        }

        g2.setColor(fg);
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (compactPad >= 0) {
            FontMetrics fm = getFontMetrics(getFont());
            String t = getText() == null ? "" : getText();
            int icon = iconType == null ? 0 : iconSize + (t.isEmpty() ? 0 : 8);
            return new Dimension(fm.stringWidth(t) + icon + compactPad * 2, 36);
        }
        Dimension d = super.getPreferredSize();
        if (iconType != null) {
            d = new Dimension(d.width + iconSize + 8, d.height);
        }
        return new Dimension(d.width, Math.max(d.height, 38));
    }
}
