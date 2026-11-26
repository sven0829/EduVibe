package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * com.eduvibe.ui.components.EmojiPicker  (NEW FILE)
 *
 * A small smiley button that opens an emoji grid and inserts the chosen
 * emoji at the caret of a text component.
 *
 *   panel.add(EmojiPicker.button(textArea));
 */
public final class EmojiPicker {

    private static final String[] EMOJIS = {
            "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎",
            "🤔", "😅", "😭", "😡", "🥳", "😴", "🤯", "🙌",
            "👍", "👎", "👏", "🙏", "💪", "🔥", "✨", "🎉",
            "❤️", "💙", "💯", "✅", "📚", "🎓", "💡", "🎮",
            "🎵", "📝", "⏰", "☕", "🌟", "🚀", "🍕", "🥇"
    };

    private EmojiPicker() {
    }

    public static JComponent button(JTextComponent target) {

        RoundedButton button = new RoundedButton("", RoundedButton.Style.GHOST);
        button.setIcon(Icons.icon(Icons.Type.SMILE, 20, Theme.MUTED));
        button.setToolTipText("Add emoji");
        button.addActionListener(e -> showPopup(button, target));
        return button;
    }

    private static void showPopup(Component anchor, JTextComponent target) {

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        popup.setBackground(Theme.CARD);

        JPanel grid = new JPanel(new GridLayout(0, 8, 2, 2));
        grid.setBackground(Theme.CARD);
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 20);

        for (String emoji : EMOJIS) {
            JLabel label = new JLabel(emoji, SwingConstants.CENTER);
            label.setFont(emojiFont);
            label.setForeground(Theme.TEXT);
            label.setOpaque(true);
            label.setBackground(Theme.CARD);
            label.setPreferredSize(new Dimension(38, 36));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    label.setBackground(Theme.CARD_LIGHT);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    label.setBackground(Theme.CARD);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    target.replaceSelection(emoji);
                    target.requestFocusInWindow();
                    popup.setVisible(false);
                }
            });
            grid.add(label);
        }

        popup.add(grid);
        popup.show(anchor, 0, -popup.getPreferredSize().height - 4);
    }
}
