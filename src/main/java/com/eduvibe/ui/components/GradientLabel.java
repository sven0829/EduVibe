package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * com.eduvibe.ui.components.GradientLabel  (NEW FILE)
 *
 * A label whose text is painted with the EduVibe brand gradient (blue to violet).
 * Used for the logo.
 */
public class GradientLabel extends JLabel {

    public GradientLabel(String text) {
        super(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        Insets in = getInsets();
        String text = getText() == null ? "" : getText();
        int x = in.left;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setPaint(Theme.brand(x, 0, x + Math.max(1, fm.stringWidth(text)), 0, 0.1f));
        g2.drawString(text, x, y);
        g2.dispose();
    }
}
