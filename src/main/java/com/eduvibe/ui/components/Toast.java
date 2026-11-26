package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * com.eduvibe.ui.components.Toast  (NEW FILE)
 *
 * Small message that fades in at the bottom-right of the window and fades out
 * by itself. Replaces pop-up dialogs for simple confirmations.
 *
 *   Toast.success(this, "Post saved");   Toast.info(...);   Toast.error(...);
 */
public final class Toast extends JComponent {

    public enum Kind { SUCCESS, INFO, ERROR }

    private static final int SHOW_MS = 2600;

    private final String message;
    private final Kind kind;
    private float alpha = 0f;
    private int phase = 0; // 0 fade in, 1 hold, 2 fade out
    private int holdTicks = 0;
    private Timer timer;
    private JLayeredPane layer;
    private ComponentAdapter resizeListener;

    private static Toast current;

    private Toast(String message, Kind kind) {
        this.message = message;
        this.kind = kind;
        setOpaque(false);
    }

    public static void success(Component anchor, String message) {
        show(anchor, message, Kind.SUCCESS);
    }

    public static void info(Component anchor, String message) {
        show(anchor, message, Kind.INFO);
    }

    public static void error(Component anchor, String message) {
        show(anchor, message, Kind.ERROR);
    }

    public static void show(Component anchor, String message, Kind kind) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(anchor, message, kind));
            return;
        }

        JRootPane root = anchor == null ? null : SwingUtilities.getRootPane(anchor);

        if (root == null) {
            Dialogs.info(anchor, "EduVibe", message);
            return;
        }

        if (current != null) {
            current.dismiss();
        }

        Toast toast = new Toast(message, kind);
        current = toast;
        toast.attach(root.getLayeredPane());
    }

    private void attach(JLayeredPane pane) {

        this.layer = pane;

        Font font = Theme.bold(13);
        FontMetrics fm = getFontMetrics(font);
        int w = Math.min(420, fm.stringWidth(message) + 76);
        int h = 46;
        setSize(w, h);
        position();

        resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                position();
            }
        };
        pane.addComponentListener(resizeListener);
        pane.add(this, JLayeredPane.POPUP_LAYER);
        pane.repaint();

        timer = new Timer(16, e -> tick());
        timer.start();
    }

    private void position() {
        if (layer != null) {
            setLocation(layer.getWidth() - getWidth() - 24, layer.getHeight() - getHeight() - 28 - ReminderToast.occupiedHeight());
        }
    }

    private void tick() {
        position();
        switch (phase) {
            case 0 -> {
                alpha = Math.min(1f, alpha + 0.09f);
                if (alpha >= 1f) {
                    phase = 1;
                }
            }
            case 1 -> {
                holdTicks += 16;
                if (holdTicks >= SHOW_MS) {
                    phase = 2;
                }
            }
            default -> {
                alpha = Math.max(0f, alpha - 0.07f);
                if (alpha <= 0f) {
                    dismiss();
                    return;
                }
            }
        }
        repaint();
    }

    private void dismiss() {
        if (timer != null) {
            timer.stop();
        }
        if (layer != null) {
            layer.removeComponentListener(resizeListener);
            layer.remove(this);
            layer.repaint(getX(), getY(), getWidth(), getHeight());
            layer = null;
        }
        if (current == this) {
            current = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));

        Color accent = switch (kind) {
            case SUCCESS -> Theme.GREEN;
            case ERROR -> Theme.RED;
            case INFO -> Theme.BLUE_LIGHT;
        };

        g2.setColor(Theme.CARD_LIGHT);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.setColor(Theme.alpha(accent, 150));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

        Icons.Type type = switch (kind) {
            case SUCCESS -> Icons.Type.CHECK;
            case ERROR -> Icons.Type.ALERT;
            case INFO -> Icons.Type.INFO;
        };
        Icons.paint(g2, type, 14, (getHeight() - 20) / 2f, 20, accent);

        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.bold(13));
        FontMetrics fm = g2.getFontMetrics();
        String text = message;
        int maxW = getWidth() - 52 - 14;
        while (fm.stringWidth(text) > maxW && text.length() > 4) {
            text = text.substring(0, text.length() - 2);
        }
        if (!text.equals(message)) {
            text = text + "…";
        }
        g2.drawString(text, 44, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
