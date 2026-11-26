package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * com.eduvibe.ui.components.SkeletonCard  (NEW FILE)
 *
 * Placeholder post card with a soft moving shimmer, shown while a feed loads.
 * The animation stops by itself when the card is removed from the window.
 */
public class SkeletonCard extends JComponent {

    private float shimmer = -0.3f;
    private final Timer timer;

    public SkeletonCard() {
        setPreferredSize(new Dimension(700, 170));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setOpaque(false);

        timer = new Timer(33, e -> {
            if (!isDisplayable()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            shimmer += 0.02f;
            if (shimmer > 1.3f) {
                shimmer = -0.3f;
            }
            repaint();
        });
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);

        Color base = Theme.CARD_LIGHT;

        g2.setColor(base);
        g2.fillOval(22, 22, 44, 44);
        bar(g2, 78, 26, 140, 12, base);
        bar(g2, 78, 48, 90, 10, base);
        bar(g2, 22, 90, getWidth() - 44, 12, base);
        bar(g2, 22, 112, (int) ((getWidth() - 44) * 0.7), 12, base);
        bar(g2, 22, 140, 80, 14, base);

        // moving highlight
        int x = (int) (shimmer * getWidth());
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 18, 18));
        g2.setPaint(new GradientPaint(x, 0, Theme.alpha(Color.WHITE, 0),
                x + 110, 0, Theme.alpha(Color.WHITE, 14), true));
        g2.fillRect(x, 0, 220, getHeight());
        g2.dispose();
    }

    private void bar(Graphics2D g2, int x, int y, int w, int h, Color c) {
        g2.setColor(c);
        g2.fillRoundRect(x, y, Math.max(10, w), h, h, h);
    }
}
