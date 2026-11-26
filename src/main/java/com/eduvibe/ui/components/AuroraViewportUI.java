package com.eduvibe.ui.components;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicViewportUI;
import java.awt.*;

/** com.eduvibe.ui.components.AuroraViewportUI (NEW FILE): scroll areas show the Aurora glow too. */
public class AuroraViewportUI extends BasicViewportUI {

    private static final AuroraViewportUI INSTANCE = new AuroraViewportUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // whole-area repaint while scrolling, so the fixed glow never smears
        if (c instanceof JViewport v) {
            v.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        }
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if (Aurora.wants(c)) {
            Aurora.paint(g, c);
            paint(g, c);
        } else {
            super.update(g, c);
        }
    }
}
