package com.eduvibe.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;

/**
 * com.eduvibe.util.WindowFit  (NEW FILE)
 *
 * Keeps windows and dialogs inside the visible screen (the area not covered by the
 * Windows taskbar). Without it a 1500x900 window on a 1366x768 laptop hangs below
 * the taskbar and dialogs centred on it lose their OK buttons.
 */
public final class WindowFit {

    private static boolean installed;

    private WindowFit() {
    }

    /** Usable screen area for the window's monitor (taskbar excluded). */
    public static Rectangle usable(Window w) {
        GraphicsConfiguration gc = w != null && w.getGraphicsConfiguration() != null
                ? w.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle b = gc.getBounds();
        Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(b.x + in.left, b.y + in.top,
                b.width - in.left - in.right, b.height - in.top - in.bottom);
    }

    /** Shrinks the window if it is larger than the screen and moves it fully on-screen. */
    public static void fit(Window w) {
        if (w == null) {
            return;
        }
        Rectangle u = usable(w);
        Dimension min = w.getMinimumSize();
        if (min != null && (min.width > u.width || min.height > u.height)) {
            w.setMinimumSize(new Dimension(Math.min(min.width, u.width), Math.min(min.height, u.height)));
        }
        int width = Math.min(w.getWidth(), u.width);
        int height = Math.min(w.getHeight(), u.height);
        if (width != w.getWidth() || height != w.getHeight()) {
            w.setSize(width, height);
        }
        int x = Math.max(u.x, Math.min(w.getX(), u.x + u.width - width));
        int y = Math.max(u.y, Math.min(w.getY(), u.y + u.height - height));
        if (x != w.getX() || y != w.getY()) {
            w.setLocation(x, y);
        }
    }

    /** Sizes a window (up to wanted x wanted) to the screen and centres it. */
    public static void sizeAndCenter(Window w, int wantedW, int wantedH) {
        Rectangle u = usable(w);
        int width = Math.min(wantedW, u.width);
        int height = Math.min(wantedH, u.height);
        w.setSize(width, height);
        w.setLocation(u.x + (u.width - width) / 2, u.y + (u.height - height) / 2);
    }

    /** Every window that opens afterwards (dialogs included) is kept on-screen. */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        Toolkit.getDefaultToolkit().addAWTEventListener((AWTEventListener) event -> {
            if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof Window w) {
                SwingUtilities.invokeLater(() -> fit(w));
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }
}
