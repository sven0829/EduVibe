package com.eduvibe.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * NEW FILE - panel that can fade its content in: a background-coloured veil is drawn
 * over the children and eased to transparent. Used for page transitions.
 */
public class FadePanel extends JPanel implements Scrollable {

    private float veil;
    private boolean fillViewport;
    private final Timer timer;

    public FadePanel(LayoutManager layout) {
        super(layout);
        timer = new Timer(15, e -> {
            veil -= 0.09f;
            if (veil <= 0f) {
                veil = 0f;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
    }

    /** Starts a quick fade-in (about 200 ms). */
    public void fadeIn() {
        veil = 0.85f;
        if (!timer.isRunning()) {
            timer.start();
        }
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (veil > 0f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(veil));
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ---- Scrollable: always as wide as the viewport, so wide pages never get cut off ----

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(60, visibleRect.height - 40);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * True for a page that must be exactly as tall as the visible area (the Reels page), instead of
     * being stretched to the tallest page and scrolled.
     */
    public void setFillViewport(boolean fill) {
        if (fillViewport != fill) {
            fillViewport = fill;
            revalidate();
        }
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (fillViewport) {
            return true;
        }
        return getParent() instanceof JViewport vp && vp.getHeight() > getPreferredSize().height;
    }
}
