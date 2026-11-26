package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;
import com.eduvibe.util.EntertainmentTimeTracker;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntSupplier;

/**
 * com.eduvibe.ui.components.EntertainmentTimerBar  (NEW FILE)
 *
 * Slim timer strip shown at the top of the Entertainment / Reels feeds: clock icon,
 * elapsed active time (MM:SS / H:MM:SS), a status dot and a progress bar toward the
 * next reminder. Purely a view - the owner calls refresh() once a second.
 */
public final class EntertainmentTimerBar extends JComponent {

    private final EntertainmentTimeTracker tracker;
    private final IntSupplier limitMinutes;
    private String time = "00:00";
    private double progress = 0;
    private double shown = 0;
    private boolean running = false;
    private int limit = 0;
    private long nextSec = 0;

    public EntertainmentTimerBar(EntertainmentTimeTracker tracker, IntSupplier limitMinutes) {
        this.tracker = tracker;
        this.limitMinutes = limitMinutes;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 46));
        setMinimumSize(new Dimension(200, 46));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        refresh();
    }

    /** Call once a second (only visible bars need it). */
    public void refresh() {
        limit = limitMinutes.getAsInt();
        time = EntertainmentTimeTracker.format(tracker.secondsUsed());
        progress = tracker.progress(limit);
        nextSec = tracker.nextReminderSeconds(limit);
        running = tracker.isRunning();
        shown += (progress - shown) * 0.5;          // smooth bar movement
        if (Math.abs(progress - shown) < 0.001) {
            shown = progress;
        }
        setToolTipText(limit > 0
                ? "Active time on Entertainment & Reels today. Next reminder at "
                        + EntertainmentTimeTracker.format(nextSec) + "."
                : "Active time on Entertainment & Reels today. Reminders are off (Settings).");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();

        g2.setColor(Theme.CARD);
        g2.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 16, 16));
        g2.setColor(Theme.BORDER);
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 16, 16));

        boolean near = limit > 0 && shown >= 0.8;
        Color accent = near ? Theme.GOLD : Theme.BLUE_LIGHT;

        // clock chip
        g2.setColor(Theme.alpha(accent, 40));
        g2.fillOval(10, (h - 28) / 2, 28, 28);
        Icons.paint(g2, Icons.Type.CLOCK, 16, (h - 16) / 2f, 16, accent);

        // time
        g2.setFont(Theme.bold(16));
        g2.setColor(Theme.TEXT);
        g2.drawString(time, 46, h / 2 + 6);
        int tw = g2.getFontMetrics().stringWidth(time);

        // status
        int sx = 46 + tw + 12;
        g2.setColor(running ? Theme.GREEN : Theme.MUTED);
        g2.fillOval(sx, h / 2 - 3, 7, 7);
        g2.setFont(Theme.regular(11));
        g2.setColor(Theme.MUTED);
        g2.drawString(running ? "Active" : "Paused", sx + 12, h / 2 + 5);

        // progress (right side)
        int barW = Math.max(60, Math.min(220, w / 3));
        int bx = w - barW - 16;
        String label = limit > 0
                ? "Reminder at " + (nextSec / 60) + " min"
                : "Reminders off";
        g2.setFont(Theme.regular(11));
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(label);
        if (bx - lw - 12 > sx + 60) {
            g2.setColor(Theme.MUTED);
            g2.drawString(label, bx - lw - 12, h / 2 + 4);
        }
        g2.setColor(new Color(255, 255, 255, 22));
        g2.fill(new RoundRectangle2D.Double(bx, h / 2 - 3, barW, 7, 7, 7));
        if (limit > 0) {
            g2.setPaint(new GradientPaint(bx, 0, Theme.BLUE, bx + barW, 0, near ? Theme.GOLD : Theme.BLUE_LIGHT));
            g2.fill(new RoundRectangle2D.Double(bx, h / 2 - 3, Math.max(7, barW * shown), 7, 7, 7));
        }
        g2.dispose();
    }
}
