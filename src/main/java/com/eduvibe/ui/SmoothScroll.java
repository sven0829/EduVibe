package com.eduvibe.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

/**
 * NEW FILE - eased mouse-wheel scrolling for JScrollPane.
 * SmoothScroll.installAll(rootComponent) upgrades every vertical scroll pane below it
 * (safe to call repeatedly).
 */
public final class SmoothScroll {

    private static final String KEY = "eduvibe.smoothScroll";

    private SmoothScroll() {
    }

    public static void installAll(Component root) {
        if (root instanceof JScrollPane sp) {
            install(sp);
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                installAll(child);
            }
        }
    }

    public static void install(JScrollPane sp) {
        if (sp.getClientProperty(KEY) != null
                || sp.getVerticalScrollBarPolicy() == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
            return;
        }
        sp.putClientProperty(KEY, Boolean.TRUE);
        sp.setWheelScrollingEnabled(false);

        JScrollBar bar = sp.getVerticalScrollBar();
        double[] target = {0};
        boolean[] animating = {false};

        Timer timer = new Timer(10, null);
        timer.addActionListener(e -> {
            int max = bar.getMaximum() - bar.getVisibleAmount();
            target[0] = Math.max(0, Math.min(max, target[0]));
            double diff = target[0] - bar.getValue();
            if (Math.abs(diff) < 1) {
                bar.setValue((int) Math.round(target[0]));
                animating[0] = false;
                timer.stop();
                return;
            }
            bar.setValue(bar.getValue() + (int) Math.signum(diff) * Math.max(1, (int) Math.round(Math.abs(diff) * 0.22)));
        });

        sp.addMouseWheelListener(ev -> {
            int max = bar.getMaximum() - bar.getVisibleAmount();
            boolean atTop = bar.getValue() <= 0 && ev.getPreciseWheelRotation() < 0;
            boolean atBottom = bar.getValue() >= max && ev.getPreciseWheelRotation() > 0;
            if (max <= 0 || (!animating[0] && (atTop || atBottom))) {
                // let an outer scroll pane take over
                Container parent = sp.getParent();
                if (parent != null) {
                    parent.dispatchEvent(new MouseWheelEvent(parent, ev.getID(), ev.getWhen(),
                            ev.getModifiersEx(), 1, 1, 1, 1, ev.getClickCount(), false,
                            ev.getScrollType(), ev.getScrollAmount(), ev.getWheelRotation(),
                            ev.getPreciseWheelRotation()));
                }
                return;
            }
            if (!animating[0]) {
                target[0] = bar.getValue();
            }
            int unit = Math.max(40, bar.getUnitIncrement() * 3);
            target[0] += ev.getPreciseWheelRotation() * unit;
            animating[0] = true;
            if (!timer.isRunning()) {
                timer.start();
            }
            ev.consume();
        });
    }
}
