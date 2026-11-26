package com.eduvibe.ui.components;

import javax.swing.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.function.Supplier;

/**
 * com.eduvibe.ui.components.ReelSnapScroller  (NEW FILE)
 *
 * TikTok-style scrolling for Reels. One wheel step (or one touchpad swipe) glides to the next reel
 * and stops there, so only one reel is on screen and two videos can never play over each other.
 * A reel taller than the window is scrolled through first, then the next wheel step moves on.
 * The page's own scrollbar still works normally.
 */
public final class ReelSnapScroller implements MouseWheelListener {

    private static final int TOP_GAP = 8;          // a little space above a reel it stops at
    private static final int STEP = 160;           // how far one step moves inside a tall reel
    private static final int DURATION_MS = 280;

    private final JScrollPane scroll;
    private final Supplier<List<? extends JComponent>> rows;
    private final Timer animation = new Timer(15, null);

    private boolean previousWheelEnabled = true;
    private boolean installed;
    private long lastEvent;
    private long lastAccepted;
    private int fromY, toY;
    private long startedAt;

    public ReelSnapScroller(JScrollPane scroll, Supplier<List<? extends JComponent>> rows) {
        this.scroll = scroll;
        this.rows = rows;
        animation.addActionListener(e -> tick());
    }

    public JScrollPane scrollPane() {
        return scroll;
    }

    public void install() {
        if (installed) {
            return;
        }
        installed = true;
        previousWheelEnabled = scroll.isWheelScrollingEnabled();
        scroll.setWheelScrollingEnabled(false);
        scroll.addMouseWheelListener(this);
    }

    public void uninstall() {
        if (!installed) {
            return;
        }
        installed = false;
        animation.stop();
        scroll.removeMouseWheelListener(this);
        scroll.setWheelScrollingEnabled(previousWheelEnabled);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        double precise = e.getPreciseWheelRotation();
        if (precise == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long gap = now - lastEvent;
        lastEvent = now;

        // A touchpad swipe sends a stream of events (with inertia). Only the start of a swipe moves a reel.
        boolean freshGesture = gap > 120;
        if (animation.isRunning() || !freshGesture || now - lastAccepted < 250) {
            return;
        }

        int target = targetFor(precise > 0 ? 1 : -1);
        JViewport viewport = scroll.getViewport();
        int current = viewport.getViewPosition().y;
        if (target == Integer.MIN_VALUE || Math.abs(target - current) < 2) {
            return;
        }
        lastAccepted = now;
        fromY = current;
        toY = target;
        startedAt = now;
        animation.start();
    }

    private void tick() {
        double t = (System.currentTimeMillis() - startedAt) / (double) DURATION_MS;
        if (t >= 1) {
            t = 1;
            animation.stop();
        }
        double eased = 1 - Math.pow(1 - t, 3);
        int y = (int) Math.round(fromY + (toY - fromY) * eased);
        scroll.getViewport().setViewPosition(new Point(0, y));
    }

    /** Where the page should be after one step in this direction (1 = down, -1 = up). */
    private int targetFor(int direction) {

        List<? extends JComponent> list = rows.get();
        JViewport viewport = scroll.getViewport();
        Component view = viewport.getView();
        if (list.isEmpty() || view == null) {
            return Integer.MIN_VALUE;
        }

        int viewY = viewport.getViewPosition().y;
        int viewHeight = viewport.getHeight();
        int maxY = Math.max(0, view.getHeight() - viewHeight);

        int n = list.size();
        int[] top = new int[n];
        int[] bottom = new int[n];
        int index = -1;
        for (int i = 0; i < n; i++) {
            JComponent row = list.get(i);
            top[i] = SwingUtilities.convertPoint(row, 0, 0, view).y;
            bottom[i] = top[i] + row.getHeight();
            if (top[i] <= viewY + 12) {
                index = i;
            }
        }

        int target;
        if (direction > 0) {
            if (index == -1) {
                target = top[0] - TOP_GAP;
            } else if (bottom[index] - (viewY + viewHeight) > 24) {
                target = Math.min(viewY + STEP, bottom[index] - viewHeight);     // rest of a tall reel
            } else if (index + 1 < n) {
                target = top[index + 1] - TOP_GAP;
            } else {
                target = maxY;                                                    // the end of the list
            }
        } else {
            if (index == -1) {
                target = Math.max(0, viewY - STEP);
            } else if (viewY - top[index] > 24) {
                target = Math.max(top[index] - TOP_GAP, viewY - STEP);
            } else if (index > 0) {
                target = top[index - 1] - TOP_GAP;
            } else {
                target = 0;                                                       // back to the page header
            }
        }
        return Math.max(0, Math.min(maxY, target));
    }
}
