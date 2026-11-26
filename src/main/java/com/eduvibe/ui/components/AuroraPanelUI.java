package com.eduvibe.ui.components;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;

/** com.eduvibe.ui.components.AuroraPanelUI (NEW FILE): flat background panels show the Aurora glow. */
public class AuroraPanelUI extends BasicPanelUI {

    private static final AuroraPanelUI INSTANCE = new AuroraPanelUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
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
