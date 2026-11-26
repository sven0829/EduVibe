package com.eduvibe.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * com.eduvibe.ui.components.TwoColumn  (NEW FILE)
 *
 * Puts two blocks side by side when there is room and stacks them when there is not.
 * Both blocks start at the top, each keeps its own natural height (no stretching, no centring),
 * and the panel is exactly as tall as the taller block, so it never adds empty space.
 */
public class TwoColumn extends JPanel {

    private final JComponent first;
    private final JComponent second;
    private final int gap;
    private final int breakpoint;
    private boolean wide = true;

    public TwoColumn(JComponent first, JComponent second, int gap, int breakpoint) {
        super(null);
        this.first = first;
        this.second = second;
        this.gap = gap;
        this.breakpoint = breakpoint;
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        add(first);
        add(second);
        setLayout(new LayoutManager() {
            @Override
            public void addLayoutComponent(String name, Component comp) {
            }

            @Override
            public void removeLayoutComponent(Component comp) {
            }

            @Override
            public Dimension preferredLayoutSize(Container p) {
                boolean w = p.getWidth() <= 0 || p.getWidth() >= breakpoint;
                Dimension a = first.getPreferredSize();
                Dimension b = second.getPreferredSize();
                return w ? new Dimension(a.width + gap + b.width, Math.max(a.height, b.height))
                        : new Dimension(Math.max(a.width, b.width), a.height + gap + b.height);
            }

            @Override
            public Dimension minimumLayoutSize(Container p) {
                return new Dimension(0, preferredLayoutSize(p).height);
            }

            @Override
            public void layoutContainer(Container p) {
                int width = p.getWidth();
                boolean nowWide = width >= breakpoint;
                if (nowWide) {
                    int colW = (width - gap) / 2;
                    first.setBounds(0, 0, colW, first.getPreferredSize().height);
                    second.setBounds(colW + gap, 0, width - colW - gap, second.getPreferredSize().height);
                } else {
                    int h1 = first.getPreferredSize().height;
                    first.setBounds(0, 0, width, h1);
                    second.setBounds(0, h1 + gap, width, second.getPreferredSize().height);
                }
                if (nowWide != wide) {
                    wide = nowWide;
                    SwingUtilities.invokeLater(p::revalidate);   // height changes with the mode
                }
            }
        });
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
