package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * com.eduvibe.ui.components.ReminderToast  (NEW FILE)
 *
 * Non-blocking reminder card that slides in at the bottom-right of the window.
 * It never takes focus away, never redirects, and stays until the user picks
 * "Return to Academic" or "Continue Entertainment" (or the close button).
 * Only one can be on screen at a time.
 */
public final class ReminderToast extends JPanel {

    public static final String MESSAGE =
            "You've been enjoying the Entertainment Feed for a while. "
                    + "How about getting back to your academic activities?";

    private static ReminderToast current;

    private final JLayeredPane layer;
    private final Runnable onReturn;
    private final Runnable onContinue;
    private final ComponentAdapter resizeListener;
    private float progress = 0f;       // 0 off-screen/transparent -> 1 settled
    private boolean closing = false;
    private final Timer anim;

    private static final int W = 380;
    private static final int H = 168;

    /** Pixels the reminder occupies at the bottom-right (0 if hidden) so other toasts stack above it. */
    public static int occupiedHeight() {
        return current == null ? 0 : H + 12;
    }

    public static boolean isVisibleNow() {
        return current != null;
    }

    public static void show(Component anchor, Runnable onReturn, Runnable onContinue) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(anchor, onReturn, onContinue));
            return;
        }
        JRootPane root = anchor == null ? null : SwingUtilities.getRootPane(anchor);
        if (root == null || current != null) {
            return;
        }
        current = new ReminderToast(root.getLayeredPane(), onReturn, onContinue);
    }

    /** Removes the card without running either action (e.g. on logout). */
    public static void dismissNow() {
        if (current != null) {
            current.finish();
        }
    }

    private ReminderToast(JLayeredPane layer, Runnable onReturn, Runnable onContinue) {
        this.layer = layer;
        this.onReturn = onReturn;
        this.onContinue = onContinue;

        setOpaque(false);
        setLayout(null);
        setSize(W, H);

        JTextArea text = new JTextArea(MESSAGE);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setForeground(Theme.TEXT);
        text.setFont(Theme.regular(13));
        text.setBounds(64, 40, W - 64 - 18, 64);
        add(text);

        RoundedButton back = new RoundedButton("Return to Academic", RoundedButton.Style.PRIMARY);
        back.setBounds(18, H - 52, 188, 36);
        back.addActionListener(e -> close(onReturn));
        add(back);

        RoundedButton stay = new RoundedButton("Continue Entertainment", RoundedButton.Style.GHOST);
        stay.setBounds(214, H - 52, W - 214 - 14, 36);
        stay.addActionListener(e -> close(onContinue));
        add(stay);

        JButton x = new JButton(Icons.icon(Icons.Type.CLOSE, 12, Theme.MUTED));
        x.setBorderPainted(false);
        x.setContentAreaFilled(false);
        x.setFocusable(false);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.setToolTipText("Dismiss");
        x.setBounds(W - 34, 8, 26, 26);
        x.addActionListener(e -> close(onContinue));
        add(x);

        getAccessibleContext().setAccessibleName("Entertainment time reminder");

        resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                place();
            }
        };
        layer.addComponentListener(resizeListener);
        layer.add(this, JLayeredPane.POPUP_LAYER);
        place();

        anim = new Timer(16, e -> step());
        anim.start();
    }

    private void place() {
        int rise = (int) ((1f - ease(progress)) * 36);
        setLocation(layer.getWidth() - W - 24, layer.getHeight() - H - 28 + rise);
        layer.repaint();
    }

    private static float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private void step() {
        progress = closing ? Math.max(0f, progress - 0.10f) : Math.min(1f, progress + 0.07f);
        place();
        repaint();
        if (closing && progress <= 0f) {
            finish();
        }
    }

    private void close(Runnable action) {
        if (closing) {
            return;
        }
        closing = true;
        if (action != null) {
            action.run();
        }
    }

    private void finish() {
        anim.stop();
        layer.removeComponentListener(resizeListener);
        Rectangle r = getBounds();
        layer.remove(this);
        layer.repaint(r.x, r.y, r.width, r.height + 40);
        if (current == this) {
            current = null;
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, ease(progress)))));
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        for (int i = 6; i >= 1; i--) {          // soft shadow
            g2.setColor(new Color(0, 0, 0, 14));
            g2.fill(new RoundRectangle2D.Double(i - 1, i + 1, W - 2 * i + 2, H - 2 * i + 4, 24, 24));
        }
        g2.setColor(Theme.CARD_LIGHT);
        g2.fill(new RoundRectangle2D.Double(0, 0, W - 1, H - 1, 20, 20));
        g2.setColor(Theme.alpha(Theme.GOLD, 140));
        g2.draw(new RoundRectangle2D.Double(0, 0, W - 1, H - 1, 20, 20));

        g2.setColor(Theme.alpha(Theme.GOLD, 40));
        g2.fillOval(18, 40, 40, 40);
        Icons.paint(g2, Icons.Type.CLOCK, 28, 52 - 2, 24, Theme.GOLD);

        g2.setFont(Theme.bold(13));
        g2.setColor(Theme.GOLD);
        g2.drawString("Time check", 70, 28);
        g2.dispose();
    }
}
