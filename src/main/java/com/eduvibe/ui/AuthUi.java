package com.eduvibe.ui;

import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import java.awt.*;

/** NEW FILE - shared building blocks for the Login and Register windows. */
final class AuthUi {

    private AuthUi() {
    }

    /** Full-window backdrop with a soft blue glow, hosting a centered card. */
    static JPanel backdrop(JComponent card) {

        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BACKGROUND);
                g2.fillRect(0, 0, getWidth(), getHeight());

                float radius = Math.max(getWidth(), getHeight()) * 0.55f;
                RadialGradientPaint glow = new RadialGradientPaint(
                        new Point(getWidth() / 2, 0), radius,
                        new float[]{0f, 1f},
                        new Color[]{new Color(59, 130, 246, 45), new Color(59, 130, 246, 0)});
                g2.setPaint(glow);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        root.add(card);

        return root;
    }

    static RoundedPanel card(int width) {
        RoundedPanel card = new RoundedPanel(24, Theme.CARD, Theme.BORDER);
        card.setLayout(new StackLayout(width));
        card.setBorder(BorderFactory.createEmptyBorder(34, 38, 30, 38));
        return card;
    }

    /**
     * Simple, predictable vertical layout: every child gets the full inner
     * width and its own preferred height, stacked top to bottom. The card has
     * a fixed width and a height equal to the sum of its children.
     * (Replaces BoxLayout, which mis-sized the card on some Windows setups.)
     */
    private static final class StackLayout implements LayoutManager {

        private final int fixedWidth;

        StackLayout(int fixedWidth) {
            this.fixedWidth = fixedWidth;
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int height = in.top + in.bottom;
            for (Component c : parent.getComponents()) {
                if (c.isVisible()) {
                    height += c.getPreferredSize().height;
                }
            }
            return new Dimension(fixedWidth, height);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int x = in.left;
            int w = parent.getWidth() - in.left - in.right;
            int y = in.top;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int h = c.getPreferredSize().height;
                c.setBounds(x, y, w, h);
                y += h;
            }
        }
    }

    static JPanel brand() {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logo = new JLabel(Icons.icon(Icons.Type.CAP, 40, Theme.BLUE));
        JLabel name = new com.eduvibe.ui.components.GradientLabel("EduVibe");
        name.setFont(Theme.bold(32));
        name.setForeground(Theme.BLUE);

        row.add(logo);
        row.add(name);

        return row;
    }

    static JLabel centered(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height + 4));
        return label;
    }

    static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bold(11));
        label.setForeground(Theme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(Theme.regular(12));
        label.setForeground(new Color(252, 165, 165));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Wraps long messages so the label never stretches the card. */
    static String html(String message) {
        String safe = message == null ? "" : message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><div style='width:290px'>" + safe + "</div></html>";
    }
}
