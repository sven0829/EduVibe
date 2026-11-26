package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * NEW FILE - flat rounded look for every plain JButton (dialog buttons, legacy panels).
 * Installed globally by Theme.install(); respects a button's own background colour and
 * leaves buttons that turned off setContentAreaFilled(false) (RoundedButton, NavButton) alone.
 */
public class FlatButtonUI extends BasicButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new FlatButtonUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setRolloverEnabled(true);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (b.getBorder() == null || b.getBorder() instanceof javax.swing.plaf.UIResource) {
            b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        if (b.isContentAreaFilled()) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            ButtonModel m = b.getModel();
            Color base = b.getBackground();
            if (!b.isEnabled()) {
                base = Theme.CARD;
            } else if (m.isPressed()) {
                base = Anim.mix(base, Color.BLACK, 0.18f);
            } else if (m.isRollover() || b.hasFocus()) {
                base = Anim.mix(base, Theme.isLight() ? Color.BLACK : Color.WHITE, 0.10f);
            }
            g2.setColor(base);
            g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
            g2.dispose();
        }
        super.paint(g, c);
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                              Rectangle textRect, Rectangle iconRect) {
        // no dotted focus rectangle
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        String t = ((AbstractButton) c).getText();
        if (t == null || t.trim().length() < 3) {
            return d;   // icon-only / tiny buttons keep their natural size
        }
        return new Dimension(Math.max(d.width, 80), Math.max(d.height, 36));
    }
}
