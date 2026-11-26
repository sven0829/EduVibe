package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * com.eduvibe.ui.components.SpinWheel  (REPLACES the old file; same constructor and spinTo())
 *
 * "Prize ring": a glowing donut with gradient segments, a gold rim of chasing lights, a pointer
 * that flicks as each segment passes, a slow idle drift, and a pulse on the winning segment.
 * The prize itself is still decided by SpinDAO; this only animates to the chosen segment.
 */
public class SpinWheel extends JComponent {

    private static final Color[][] PALETTE = {
        { new Color(59, 130, 246), new Color(37, 99, 235) },
        { new Color(168, 85, 247), new Color(126, 34, 206) },
        { new Color(20, 184, 166), new Color(13, 148, 136) },
        { new Color(244, 114, 182), new Color(219, 39, 119) },
        { new Color(34, 197, 94), new Color(21, 128, 61) },
        { new Color(251, 146, 60), new Color(234, 88, 12) }
    };
    private static final Color GOLD = new Color(251, 191, 36);

    private final String[] labels;
    private double rotation = 0;
    private Timer timer;
    private Timer idle;
    private int winner = -1;
    private long winAt;
    private double pointerKick;          // 0..1, decays
    private int lastSegment = -1;
    private long clock;                  // drives the rim lights

    public SpinWheel(String[] labels) {
        this.labels = labels;
        setPreferredSize(new Dimension(260, 270));
        setOpaque(false);

        idle = new Timer(40, e -> {
            clock += 40;
            if (!isSpinning() && winner < 0) {
                rotation += 0.25;
            }
            if (winner >= 0 && System.currentTimeMillis() - winAt > 3500) {
                winner = -1;
            }
            repaint();
        });
        idle.start();
    }

    @Override
    public void removeNotify() {
        idle.stop();
        super.removeNotify();
    }

    public boolean isSpinning() {
        return timer != null && timer.isRunning();
    }

    /** Animates (~5 s: quick start, long smooth slow-down) so segment {@code index} ends under the pointer. */
    public void spinTo(int index, Runnable onDone) {

        winner = -1;
        double seg = 360.0 / labels.length;
        double centre = index * seg + seg / 2;

        double current = ((rotation % 360) + 360) % 360;
        double target = ((360 - centre) % 360 + 360) % 360;
        double delta = (target - current + 360) % 360 + 360 * 6;

        final double start = rotation;
        final long begin = System.currentTimeMillis();
        final long duration = 5200;

        timer = new Timer(12, null);
        timer.addActionListener(e -> {
            double t = Math.min(1.0, (System.currentTimeMillis() - begin) / (double) duration);
            double eased = 1 - Math.pow(1 - t, 4);
            rotation = start + delta * eased;

            // pointer flick every time a segment boundary passes under it
            double under = (((360 - rotation) % 360) + 360) % 360;
            int segNow = (int) (under / seg);
            if (segNow != lastSegment) {
                lastSegment = segNow;
                pointerKick = 1.0;
            }

            if (t >= 1.0) {
                timer.stop();
                winner = index;
                winAt = System.currentTimeMillis();
                if (onDone != null) {
                    onDone.run();
                }
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        pointerKick = Math.max(0, pointerKick - 0.12);

        int top = 22;
        double size = Math.min(getWidth(), getHeight() - top) - 10;
        double cx = getWidth() / 2.0;
        double cy = top + (getHeight() - top) / 2.0;
        double outer = size / 2.0;          // rim outer radius
        double rim = outer - 11;            // segments outer radius
        double inner = rim * 0.40;          // hole radius
        double seg = 360.0 / labels.length;

        // soft glow behind the ring
        boolean won = winner >= 0;
        float pulse = won ? (float) (0.5 + 0.5 * Math.sin((System.currentTimeMillis() - winAt) / 130.0)) : 0f;
        RadialGradientPaint glow = new RadialGradientPaint(
                (float) cx, (float) cy, (float) (outer + 14),
                new float[] { 0.7f, 1f },
                new Color[] { new Color(59, 130, 246, won ? 90 + (int) (60 * pulse) : 55), new Color(59, 130, 246, 0) });
        g2.setPaint(glow);
        g2.fill(new Ellipse2D.Double(cx - outer - 14, cy - outer - 14, (outer + 14) * 2, (outer + 14) * 2));

        // rotating ring
        Graphics2D w = (Graphics2D) g2.create();
        w.rotate(Math.toRadians(rotation), cx, cy);

        for (int i = 0; i < labels.length; i++) {
            Color[] pair = PALETTE[i % PALETTE.length];
            double start = 90 - (i + 1) * seg;
            Arc2D.Double arc = new Arc2D.Double(cx - rim, cy - rim, rim * 2, rim * 2, start, seg, Arc2D.PIE);

            double mid = Math.toRadians((i + 0.5) * seg);
            w.setPaint(new GradientPaint(
                    (float) (cx + inner * Math.sin(mid)), (float) (cy - inner * Math.cos(mid)), pair[0],
                    (float) (cx + rim * Math.sin(mid)), (float) (cy - rim * Math.cos(mid)), pair[1]));
            w.fill(arc);

            if (won && i == winner) {
                w.setColor(new Color(255, 255, 255, 60 + (int) (110 * pulse)));
                w.fill(arc);
            }

            w.setColor(new Color(15, 23, 42, 200));
            w.setStroke(new BasicStroke(2.2f));
            w.draw(arc);
        }

        // cut the hole -> ring look
        w.setColor(Theme.CARD);
        w.fill(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2));
        w.setColor(new Color(255, 255, 255, 40));
        w.setStroke(new BasicStroke(1.5f));
        w.draw(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2));

        // labels
        double lr = (rim + inner) / 2.0;
        // longest label must fit the ring band; shrink the font for many / long labels
        float fontSize = 15f;
        double band = (rim - inner) * 0.92;
        w.setFont(Theme.bold(fontSize));
        FontMetrics fm = w.getFontMetrics();
        for (String l : labels) {
            while (fontSize > 9f && fm.stringWidth(l) > band) {
                fontSize -= 1f;
                w.setFont(Theme.bold(fontSize));
                fm = w.getFontMetrics();
            }
        }
        for (int i = 0; i < labels.length; i++) {
            double phi = Math.toRadians((i + 0.5) * seg);
            double tx = cx + lr * Math.sin(phi);
            double ty = cy - lr * Math.cos(phi);
            Graphics2D t = (Graphics2D) w.create();
            t.rotate(phi, tx, ty);
            t.setColor(new Color(0, 0, 0, 70));
            t.drawString(labels[i], (float) (tx - fm.stringWidth(labels[i]) / 2.0 + 1),
                    (float) (ty + fm.getAscent() / 2.0 - 1));
            t.setColor(Color.WHITE);
            t.drawString(labels[i], (float) (tx - fm.stringWidth(labels[i]) / 2.0),
                    (float) (ty + fm.getAscent() / 2.0 - 2));
            t.dispose();
        }
        w.dispose();

        // gold rim (fixed) with chasing lights
        Area rimShape = new Area(new Ellipse2D.Double(cx - outer, cy - outer, outer * 2, outer * 2));
        rimShape.subtract(new Area(new Ellipse2D.Double(cx - rim - 1, cy - rim - 1, (rim + 1) * 2, (rim + 1) * 2)));
        g2.setPaint(new GradientPaint((float) (cx - outer), (float) (cy - outer), new Color(253, 224, 71),
                (float) (cx + outer), (float) (cy + outer), new Color(180, 120, 10)));
        g2.fill(rimShape);

        int bulbs = labels.length * 4;
        double br = (outer + rim) / 2.0;
        boolean fast = isSpinning() || won;
        for (int b = 0; b < bulbs; b++) {
            double a = Math.toRadians(b * 360.0 / bulbs);
            double phase = fast ? (clock / 70.0) : (clock / 260.0);
            boolean on = ((int) Math.floor(b - phase) % 3 + 3) % 3 == 0;
            double bx = cx + br * Math.sin(a);
            double by = cy - br * Math.cos(a);
            g2.setColor(on ? Color.WHITE : new Color(120, 70, 5));
            g2.fill(new Ellipse2D.Double(bx - 2.2, by - 2.2, 4.4, 4.4));
        }

        // hub
        double hub = inner * 0.78;
        g2.setPaint(new GradientPaint((float) (cx - hub), (float) (cy - hub), new Color(30, 41, 59),
                (float) (cx + hub), (float) (cy + hub), new Color(15, 23, 42)));
        g2.fill(new Ellipse2D.Double(cx - hub, cy - hub, hub * 2, hub * 2));
        g2.setColor(GOLD);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Ellipse2D.Double(cx - hub, cy - hub, hub * 2, hub * 2));
        Icons.paint(g2, Icons.Type.STAR, (int) (cx - 11), (int) (cy - 11), 22, GOLD);

        // pointer (flicks sideways on every tick)
        Graphics2D p = (Graphics2D) g2.create();
        p.rotate(Math.toRadians(-18 * pointerKick), cx, 14);
        Path2D pointer = new Path2D.Double();
        pointer.moveTo(cx - 11, 0);
        pointer.lineTo(cx + 11, 0);
        pointer.lineTo(cx, 30);
        pointer.closePath();
        p.setColor(new Color(0, 0, 0, 90));
        p.translate(1.5, 2);
        p.fill(pointer);
        p.translate(-1.5, -2);
        p.setPaint(new GradientPaint((float) cx, 0, Color.WHITE, (float) cx, 30, new Color(203, 213, 225)));
        p.fill(pointer);
        p.setColor(GOLD);
        p.setStroke(new BasicStroke(1.6f));
        p.draw(pointer);
        p.dispose();

        g2.dispose();
    }
}
