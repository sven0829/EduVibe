package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** NEW FILE - rounded card container with optional border. */
public class RoundedPanel extends JPanel {

    private int arc;
    private Color fill;
    private Color borderColor;

    public RoundedPanel(int arc, Color fill) {
        this(arc, fill, null);
    }

    public RoundedPanel(int arc, Color fill, Color borderColor) {
        this.arc = arc;
        this.fill = fill;
        this.borderColor = borderColor;
        setOpaque(false);
        setBackground(fill);
    }

    public static RoundedPanel card() {
        return new RoundedPanel(16, Theme.CARD, Theme.BORDER);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        setBackground(fill);
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        if (!Theme.isLight() && fill.getAlpha() == 255 && getHeight() > 24) {
            // soft top light so cards read as raised, not flat
            Color top = new Color(Math.min(255, fill.getRed() + 9), Math.min(255, fill.getGreen() + 10),
                    Math.min(255, fill.getBlue() + 16));
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), fill));
        } else {
            g2.setColor(fill);
        }
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (!Theme.isLight() && getHeight() > 24 && getWidth() > arc * 2) {
                // thin light edge along the top, fading out to the sides
                g2.setPaint(new GradientPaint(arc, 0, new Color(255, 255, 255, 46),
                        getWidth() - arc, 0, new Color(255, 255, 255, 6)));
                g2.drawLine(arc, 1, getWidth() - arc, 1);
            }
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
