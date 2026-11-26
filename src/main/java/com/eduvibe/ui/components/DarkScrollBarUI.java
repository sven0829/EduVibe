package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * NEW FILE - com.eduvibe.ui.components.DarkScrollBarUI
 *
 * Slim, rounded, dark scroll bar. Installed globally by Theme.install().
 */
public class DarkScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new DarkScrollBarUI();
    }

    @Override
    protected void configureScrollBarColors() {
        trackColor = Theme.BACKGROUND;
        thumbColor = Theme.BORDER;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // transparent: the page glow shows through
        if (scrollbar.getParent() != null) {
            scrollbar.setOpaque(false);
        }
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {

        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        boolean active = isDragging || isThumbRollover();
        Color base = new Color(124, 92, 250);
        g2.setColor(Theme.alpha(base, Theme.isLight() ? (active ? 190 : 110) : (active ? 200 : 105)));

        int inset = 2;
        g2.fillRoundRect(r.x + inset, r.y + inset,
                r.width - inset * 2, r.height - inset * 2, 8, 8);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == Adjustable.VERTICAL
                ? new Dimension(10, 0)
                : new Dimension(0, 10);
    }
}
