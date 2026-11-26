package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Arrays;

/**
 * com.eduvibe.ui.components.PasswordStrengthBar  (NEW FILE)
 *
 * A four-segment bar with a word ("Weak", "Fair", "Good", "Strong") that follows a password field
 * as the person types. It is only a hint; the rules that actually accept or reject a password
 * stay in AuthService.
 */
public final class PasswordStrengthBar extends JComponent {

    private int level;           // 0 = empty, 1..4
    private String text = "";

    public PasswordStrengthBar() {
        setOpaque(false);
        setPreferredSize(new Dimension(300, 22));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** Follows a password field (or any text component). */
    public void attach(JTextComponent field) {
        DocumentListener l = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { update(field); }
            @Override public void removeUpdate(DocumentEvent e) { update(field); }
            @Override public void changedUpdate(DocumentEvent e) { update(field); }
        };
        field.getDocument().addDocumentListener(l);
        update(field);
    }

    private void update(JTextComponent field) {
        char[] pw = field instanceof JPasswordField
                ? ((JPasswordField) field).getPassword() : field.getText().toCharArray();
        level = score(pw);
        Arrays.fill(pw, '\0');
        switch (level) {
            case 0:  text = ""; break;
            case 1:  text = "Weak"; break;
            case 2:  text = "Fair"; break;
            case 3:  text = "Good"; break;
            default: text = "Strong"; break;
        }
        repaint();
    }

    /** 0 for empty, otherwise 1 (weak) to 4 (strong). */
    static int score(char[] pw) {
        if (pw == null || pw.length == 0) {
            return 0;
        }
        boolean lower = false, upper = false, digit = false, symbol = false;
        for (char c : pw) {
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        int kinds = (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);
        if (pw.length < 8) {
            return 1;
        }
        int s = 1;
        if (kinds >= 2) {
            s++;
        }
        if (kinds >= 3) {
            s++;
        }
        if (pw.length >= 12 && kinds >= 3) {
            s++;
        } else if (kinds == 4 && s < 4) {
            s++;
        }
        return Math.min(4, s);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        Color on = level <= 1 ? Theme.RED : level == 2 ? Theme.GOLD : Theme.GREEN;
        int labelW = 56;
        int gap = 6;
        int segW = Math.max(10, (getWidth() - labelW - gap * 3 - 8) / 4);
        int y = getHeight() / 2 - 2;
        for (int i = 0; i < 4; i++) {
            g2.setColor(i < level ? on : Theme.BORDER);
            g2.fillRoundRect(i * (segW + gap), y, segW, 4, 4, 4);
        }
        if (!text.isEmpty()) {
            g2.setFont(Theme.bold(12));
            g2.setColor(on);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 4 * (segW + gap) + 4, getHeight() / 2 + fm.getAscent() / 2 - 1);
        }
        g2.dispose();
    }
}
