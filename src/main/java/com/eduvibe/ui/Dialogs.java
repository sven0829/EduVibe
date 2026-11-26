package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * NEW FILE - dark, consistent replacements for JOptionPane.
 * Must be called on the Swing event thread.
 */
public final class Dialogs {

    private Dialogs() {
    }

    private enum Kind { INFO, SUCCESS, WARNING, ERROR, QUESTION, DANGER, TIME }

    public static void info(Component parent, String title, String message) {
        show(parent, Kind.INFO, title, message, "OK", null);
    }

    public static void success(Component parent, String title, String message) {
        show(parent, Kind.SUCCESS, title, message, "OK", null);
    }

    public static void warning(Component parent, String title, String message) {
        show(parent, Kind.WARNING, title, message, "OK", null);
    }

    public static void error(Component parent, String title, String message) {
        show(parent, Kind.ERROR, title, message, "OK", null);
    }

    /** Returns true when the user presses the confirm button. */
    public static boolean confirm(Component parent, String title, String message,
                                  String confirmText, boolean destructive) {
        return show(parent, destructive ? Kind.DANGER : Kind.QUESTION,
                title, message, confirmText, "Cancel");
    }

    /** Time reminder (reference screen 9). True = go to the Academic feed. */
    public static boolean timeReminder(Component parent, String title, String message) {
        return show(parent, Kind.TIME, title, message, "Go to Academic Feed", "Stay Here");
    }

    /**
     * Content warning for the post's author (reference screen 10).
     * Returns 1 = Edit post, 2 = Delete post, 0 = Back.
     */
    public static int contentWarning(Component parent, String reason) {

        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        int[] result = {0};

        RoundedPanel card = new RoundedPanel(20, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));

        JLabel icon = new JLabel(Icons.icon(Icons.Type.ALERT, 42, Theme.RED));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Content Warning");
        heading.setFont(Theme.bold(20));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel body = new JLabel("<html><div style='text-align:center;width:300px'>"
                + (reason == null ? "This post has been flagged for potentially inappropriate content." : reason)
                + "</div></html>");
        body.setFont(Theme.regular(13));
        body.setForeground(Theme.MUTED);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);

        // blurred preview placeholder
        JComponent preview = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(Theme.INPUT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(Theme.CARD_LIGHT);
                g2.fillRoundRect(16, 16, 64, 52, 10, 10);
                g2.fillRoundRect(96, 20, getWidth() - 130, 12, 6, 6);
                g2.fillRoundRect(96, 42, (getWidth() - 130) * 2 / 3, 12, 6, 6);
                g2.dispose();
            }
        };
        preview.setPreferredSize(new Dimension(300, 84));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        preview.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedButton edit = new RoundedButton("Edit Post", RoundedButton.Style.PRIMARY);
        edit.addActionListener(e -> { result[0] = 1; dialog.dispose(); });
        RoundedButton delete = new RoundedButton("Delete Post", RoundedButton.Style.DANGER);
        delete.addActionListener(e -> { result[0] = 2; dialog.dispose(); });
        RoundedButton back = new RoundedButton("Back", RoundedButton.Style.OUTLINE);
        back.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 10));
        buttons.setOpaque(false);
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(back);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(12));
        card.add(heading);
        card.add(Box.createVerticalStrut(8));
        card.add(body);
        card.add(Box.createVerticalStrut(16));
        card.add(preview);
        card.add(Box.createVerticalStrut(18));
        card.add(buttons);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        root.add(card, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), 420), dialog.getHeight());
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    private static boolean show(Component parent, Kind kind, String title,
                                String message, String okText, String cancelText) {

        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window w) {
            owner = w;
        }

        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        boolean[] result = {false};

        Color accent = switch (kind) {
            case SUCCESS -> Theme.GREEN;
            case WARNING -> Theme.YELLOW;
            case ERROR, DANGER -> Theme.RED;
            case TIME -> Theme.PURPLE;
            default -> Theme.BLUE;
        };

        Icons.Type iconType = switch (kind) {
            case SUCCESS -> Icons.Type.CHECK;
            case WARNING, ERROR, DANGER -> Icons.Type.ALERT;
            case TIME -> Icons.Type.CLOCK;
            default -> Icons.Type.INFO;
        };

        RoundedPanel card = new RoundedPanel(20, Theme.CARD, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(28, 30, 24, 30));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(Icons.icon(iconType, 40, accent));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.bold(20));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        String safeMessage = (message == null ? "" : message)
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        JLabel body = new JLabel("<html><div style='text-align:center;width:320px'>" + safeMessage + "</div></html>");
        body.setFont(Theme.regular(13));
        body.setForeground(Theme.MUTED);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);

        top.add(icon);
        top.add(Box.createVerticalStrut(14));
        top.add(heading);
        top.add(Box.createVerticalStrut(10));
        top.add(body);

        JPanel buttons = new JPanel(new GridLayout(cancelText == null ? 1 : 2, 1, 0, 10));
        buttons.setOpaque(false);

        RoundedButton.Style okStyle = switch (kind) {
            case DANGER -> RoundedButton.Style.DANGER;
            case ERROR, WARNING -> RoundedButton.Style.SECONDARY;
            case SUCCESS -> RoundedButton.Style.SUCCESS;
            default -> RoundedButton.Style.PRIMARY;
        };
        RoundedButton ok = new RoundedButton(okText, okStyle);

        final RoundedButton okButton = ok;
        okButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        buttons.add(okButton);

        if (cancelText != null) {
            RoundedButton cancel = new RoundedButton(cancelText, RoundedButton.Style.OUTLINE);
            cancel.addActionListener(e -> dialog.dispose());
            buttons.add(cancel);
        }

        card.add(top, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        south.add(buttons);
        if (kind == Kind.TIME) {
            JLabel tag = new JLabel("A balanced you builds a brighter future.");
            tag.setFont(Theme.regular(11));
            tag.setForeground(Theme.MUTED);
            tag.setAlignmentX(Component.CENTER_ALIGNMENT);
            south.add(Box.createVerticalStrut(14));
            south.add(tag);
        }
        card.add(south, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        root.add(card, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(400, 0));
        dialog.setSize(Math.max(dialog.getWidth(), 420), dialog.getHeight());
        dialog.setLocationRelativeTo(owner);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                okButton.requestFocusInWindow();
            }
        });

        dialog.setVisible(true);
        return result[0];
    }
}
