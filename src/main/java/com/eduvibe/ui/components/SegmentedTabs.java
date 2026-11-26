package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

/**
 * NEW FILE - segmented control (the blue pill tabs in the reference design).
 * Examples: Academic | Entertainment, All | Likes | Comments | System, Users | Content | System.
 * The blue pill slides to the chosen tab.
 */
public class SegmentedTabs extends JComponent {

    private final String[] labels;
    private int selected;
    private final IntConsumer onSelect;

    private float pillX = -1;
    private final Timer slide;

    public SegmentedTabs(String[] labels, int selected, IntConsumer onSelect) {
        this.labels = labels;
        this.selected = selected;
        this.onSelect = onSelect == null ? i -> { } : onSelect;

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(Math.max(260, labels.length * 120), 44));
        setMinimumSize(new Dimension(200, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        slide = new Timer(15, e -> {
            float target = targetX();
            pillX += (target - pillX) * 0.28f;
            if (Math.abs(target - pillX) < 0.8f) {
                pillX = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = Math.min(labels.length - 1, Math.max(0, (e.getX() - 4) * labels.length / Math.max(1, getWidth() - 8)));
                select(i, true);
            }
        });
    }

    public int getSelected() {
        return selected;
    }

    /** Sets the chosen tab at once, with no animation and no event (to match what the page is showing). */
    public void snapTo(int index) {
        if (index < 0 || index >= labels.length) {
            return;
        }
        selected = index;
        slide.stop();
        pillX = targetX();
        repaint();
    }

    /** Changes the chosen tab; fireEvent = also call the listener. */
    public void select(int index, boolean fireEvent) {
        if (index < 0 || index >= labels.length) {
            return;
        }
        boolean changed = index != selected;
        selected = index;
        if (isShowing()) {
            slide.start();
        } else {
            pillX = targetX();
        }
        repaint();
        if (fireEvent && changed) {
            onSelect.accept(index);
        }
    }

    private float segmentWidth() {
        return (getWidth() - 8f) / labels.length;
    }

    private float targetX() {
        return 4 + segmentWidth() * selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth();
        int h = getHeight();

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        if (pillX < 0) {
            pillX = targetX();
        }

        float sw = segmentWidth();
        boolean neutral = Boolean.TRUE.equals(getClientProperty("neutral"));   // e.g. Home: nothing selected
        if (!neutral) {
            g2.setColor(Theme.BLUE);
            g2.fillRoundRect(Math.round(pillX), 4, Math.round(sw), h - 9, 11, 11);
        }

        g2.setFont(Theme.bold(13));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < labels.length; i++) {
            float cx = 4 + sw * i + sw / 2f;
            float closeness = neutral ? 0f : Math.max(0f, 1f - Math.abs(cx - (pillX + sw / 2f)) / sw);
            g2.setColor(Anim.mix(Theme.MUTED, Color.WHITE, closeness));
            int tx = Math.round(cx - fm.stringWidth(labels[i]) / 2f);
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(labels[i], tx, ty);
        }
        g2.dispose();
    }
}
