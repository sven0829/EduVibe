package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * com.eduvibe.ui.components.EmptyState  (NEW FILE)
 *
 * Friendly "nothing here yet" card: icon, title, optional text and an
 * optional action button.
 */
public class EmptyState extends RoundedPanel {

    public EmptyState(Icons.Type icon, String title, String body) {
        this(icon, title, body, null, null);
    }

    public EmptyState(Icons.Type icon, String title, String body,
                      String buttonText, Runnable action) {

        super(18, Theme.CARD, Theme.BORDER);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(34, 24, 34, 24));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(Icons.icon(icon, 44, Theme.BLUE_LIGHT));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(iconLabel);
        add(Box.createVerticalStrut(12));

        JLabel t = new JLabel(title);
        t.setFont(Theme.bold(17));
        t.setForeground(Theme.TEXT);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(t);

        if (body != null && !body.isBlank()) {
            add(Box.createVerticalStrut(8));
            JLabel b = new JLabel(body);
            b.setFont(Theme.regular(13));
            b.setForeground(Theme.MUTED);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(b);
        }

        if (buttonText != null && action != null) {
            add(Box.createVerticalStrut(16));
            RoundedButton button = new RoundedButton(buttonText);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> action.run());
            add(button);
        }
    }
}
