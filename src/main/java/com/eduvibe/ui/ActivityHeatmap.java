package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/**
 * com.eduvibe.ui.components.ActivityHeatmap  (REPLACES existing file)
 *
 * 12-week calendar grid: a brighter blue-violet square = more activity that day. Columns are weeks,
 * rows are Mon-Sun. The squares stretch to the card width, the height is fixed (so no empty space
 * is added under it), and a small Less/More legend sits at the bottom right.
 */
public class ActivityHeatmap extends JComponent {

    private static final int WEEKS = 12;
    private static final int CELL_H = 18;
    private static final int GAP = 4;
    private static final int LEFT = 34;
    private static final int TOP = 20;
    private static final int LEGEND = 26;
    private static final int HEIGHT = TOP + 7 * (CELL_H + GAP) + LEGEND;

    private Map<LocalDate, Integer> counts = Map.of();
    private final LocalDate today = LocalDate.now();

    public ActivityHeatmap() {
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
        setPreferredSize(new Dimension(LEFT + WEEKS * (14 + GAP), HEIGHT));
        setMinimumSize(new Dimension(LEFT + WEEKS * (14 + GAP), HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
    }

    public static LocalDate startDate() {
        LocalDate t = LocalDate.now();
        LocalDate monday = t.with(DayOfWeek.MONDAY);
        return monday.minusWeeks(WEEKS - 1);
    }

    public void setCounts(Map<LocalDate, Integer> counts) {
        this.counts = counts;
        repaint();
    }

    private int cellW() {
        int w = getWidth() > 0 ? getWidth() : 360;
        return Math.max(14, Math.min(44, (w - LEFT) / WEEKS - GAP));
    }

    private LocalDate dateAt(int col, int row) {
        return startDate().plusWeeks(col).plusDays(row);
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent e) {
        int cw = cellW();
        int col = (e.getX() - LEFT) / (cw + GAP);
        int row = (e.getY() - TOP) / (CELL_H + GAP);
        if (e.getX() < LEFT || e.getY() < TOP || col < 0 || col >= WEEKS || row < 0 || row > 6) {
            return null;
        }
        LocalDate d = dateAt(col, row);
        if (d.isAfter(today)) {
            return null;
        }
        int n = counts.getOrDefault(d, 0);
        return d + ": " + n + (n == 1 ? " activity" : " activities");
    }

    private static Color level(float t) {
        if (t <= 0f) {
            return Theme.CARD_LIGHT;
        }
        Color hot = Anim.mix(Theme.BLUE, Theme.VIOLET, t);
        return Anim.mix(Theme.CARD_LIGHT, hot, 0.38f + 0.62f * t);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.regular(10));
        int cw = cellW();

        int lastMonth = -1;
        for (int c = 0; c < WEEKS; c++) {
            LocalDate d = dateAt(c, 0);
            if (d.getMonthValue() != lastMonth) {
                lastMonth = d.getMonthValue();
                g2.setColor(Theme.MUTED);
                g2.drawString(d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        LEFT + c * (cw + GAP), 11);
            }
        }

        g2.setColor(Theme.MUTED);
        for (int r : new int[]{0, 2, 4}) {
            g2.drawString(DayOfWeek.of(r + 1).getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    0, TOP + r * (CELL_H + GAP) + CELL_H - 5);
        }

        int max = 1;
        for (int v : counts.values()) {
            max = Math.max(max, v);
        }

        for (int c = 0; c < WEEKS; c++) {
            for (int r = 0; r < 7; r++) {
                LocalDate d = dateAt(c, r);
                if (d.isAfter(today)) {
                    continue;
                }
                int n = counts.getOrDefault(d, 0);
                int x = LEFT + c * (cw + GAP);
                int y = TOP + r * (CELL_H + GAP);
                g2.setColor(level(n == 0 ? 0f : Math.min(1f, 0.25f + 0.75f * n / max)));
                g2.fillRoundRect(x, y, cw, CELL_H, 6, 6);
                if (d.equals(today)) {
                    g2.setColor(Theme.TEXT);
                    g2.drawRoundRect(x, y, cw - 1, CELL_H - 1, 6, 6);
                }
            }
        }

        // legend: Less [] [] [] [] More
        int ly = HEIGHT - 16;
        int sq = 11;
        int total = 5 * (sq + 3);
        FontMetrics fm = g2.getFontMetrics();
        int more = fm.stringWidth("More");
        int less = fm.stringWidth("Less");
        int right = getWidth() - 2;
        int x = right - more - 6 - total - less - 6;
        g2.setColor(Theme.MUTED);
        g2.drawString("Less", x, ly + sq - 2);
        x += less + 6;
        for (int i = 0; i < 5; i++) {
            g2.setColor(level(i / 4f));
            g2.fillRoundRect(x, ly, sq, sq, 4, 4);
            x += sq + 3;
        }
        g2.setColor(Theme.MUTED);
        g2.drawString("More", x + 3, ly + sq - 2);
        g2.dispose();
    }
}
