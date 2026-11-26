package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * com.eduvibe.ui.components.WeeklyPostsChart  (NEW FILE)
 *
 * Column chart of how many posts were created on each of the last days. It only draws the
 * numbers it is given (the admin dashboard reads them from the database).
 */
public final class WeeklyPostsChart extends JComponent {

    private final String title;
    private String[] labels = new String[0];
    private int[] values = new int[0];
    private boolean loaded;

    public WeeklyPostsChart(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 210));
    }

    public void setData(String[] labels, int[] values) {
        this.labels = labels.clone();
        this.values = values.clone();
        this.loaded = true;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

        g2.setFont(Theme.bold(14));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, 18, 28);

        int n = values.length;
        if (!loaded || n == 0) {
            g2.setFont(Theme.regular(12));
            g2.setColor(Theme.MUTED);
            g2.drawString(loaded ? "No data." : "Loading...", 18, 56);
            g2.dispose();
            return;
        }

        int max = 0;
        int total = 0;
        for (int v : values) {
            max = Math.max(max, v);
            total += v;
        }

        g2.setFont(Theme.regular(12));
        g2.setColor(Theme.MUTED);
        String sum = total + (total == 1 ? " post" : " posts") + " in total";
        g2.drawString(sum, w - 18 - g2.getFontMetrics().stringWidth(sum), 28);

        int left = 18;
        int right = 18;
        int top = 48;
        int bottom = 30;
        int chartH = h - top - bottom;
        int slot = (w - left - right) / n;
        int barW = Math.max(14, Math.min(46, slot - 16));

        // baseline
        g2.setColor(Theme.BORDER);
        g2.drawLine(left, top + chartH, w - right, top + chartH);

        for (int i = 0; i < n; i++) {
            int cx = left + slot * i + slot / 2;
            int bh = max == 0 ? 0 : (int) Math.round(values[i] * (chartH - 18) / (double) max);
            int x = cx - barW / 2;
            int y = top + chartH - bh;

            if (bh > 0) {
                g2.setPaint(Theme.brand(x, y, x, y + bh, 0));
                g2.fillRoundRect(x, y, barW, bh, 8, 8);
            } else {
                g2.setColor(Theme.BORDER);
                g2.fillRoundRect(x, top + chartH - 3, barW, 3, 3, 3);
            }

            g2.setFont(Theme.bold(11));
            g2.setColor(values[i] > 0 ? Theme.TEXT : Theme.MUTED);
            String v = String.valueOf(values[i]);
            g2.drawString(v, cx - g2.getFontMetrics().stringWidth(v) / 2, Math.max(top + 10, y - 5));

            g2.setFont(Theme.regular(11));
            g2.setColor(Theme.MUTED);
            String label = labels[i];
            g2.drawString(label, cx - g2.getFontMetrics().stringWidth(label) / 2, h - 10);
        }
        g2.dispose();
    }
}
