package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** NEW FILE - sidebar navigation item: vector icon, label, optional unread badge. */
public class NavButton extends JButton {

    private final Icons.Type icon;
    private final String label;
    private boolean selectedItem;
    private boolean hover;
    private final Anim.Fade fade = new Anim.Fade(this);
    private int badge;

    public NavButton(Icons.Type icon, String label) {
        this.icon = icon;
        this.label = label;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setHorizontalAlignment(LEFT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(200, 42));
        setMinimumSize(new Dimension(100, 42));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        getAccessibleContext().setAccessibleName(label);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                fade.to(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                fade.to(false);
            }
        });
    }

    public void setActive(boolean active) {
        this.selectedItem = active;
        repaint();
    }

    public void setBadge(int count) {
        this.badge = Math.max(0, count);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        if (selectedItem) {
            g2.setPaint(Theme.brand(0, 0, getWidth(), 0, 0));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fillRoundRect(3, getHeight() / 2 - 8, 3, 16, 3, 3);
        } else if (fade.value() > 0f) {
            g2.setColor(Theme.alpha(Theme.CARD, Math.round(255 * fade.value())));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        }

        Color fg = selectedItem ? Color.WHITE : Anim.mix(Theme.MUTED, Theme.TEXT, fade.value());

        Icons.paint(g2, icon, 14, (getHeight() - 20) / 2f, 20, fg);

        g2.setColor(fg);
        g2.setFont(selectedItem ? Theme.bold(13) : Theme.regular(13));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, 44, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());

        if (badge > 0) {
            String text = badge > 99 ? "99+" : String.valueOf(badge);
            g2.setFont(Theme.bold(10));
            FontMetrics bf = g2.getFontMetrics();
            int w = Math.max(20, bf.stringWidth(text) + 12);
            int h = 18;
            int x = getWidth() - w - 12;
            int y = (getHeight() - h) / 2;
            g2.setColor(selectedItem ? Color.WHITE : Theme.RED);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(selectedItem ? Theme.BLUE : Color.WHITE);
            g2.drawString(text, x + (w - bf.stringWidth(text)) / 2,
                    y + (h - bf.getHeight()) / 2 + bf.getAscent());
        }

        g2.dispose();
    }
}
