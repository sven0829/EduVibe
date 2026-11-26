package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * NEW FILE - rounded input with a leading icon, placeholder text and,
 * for passwords, a show/hide eye button.
 */
public class FieldBox extends JPanel {

    private final JTextComponent field;
    private final JPasswordField passwordField;
    private final Icons.Type leadingIcon;
    private boolean focused;
    private boolean passwordVisible;
    private char defaultEcho;

    private FieldBox(Icons.Type icon, String placeholder, boolean password) {

        this.leadingIcon = icon;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 40, 0, password ? 44 : 12));
        setPreferredSize(new Dimension(300, 46));
        setMinimumSize(new Dimension(100, 46));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        if (password) {
            passwordField = new JPasswordField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintHint(g, this, placeholder, getPassword().length == 0);
                }
            };
            field = passwordField;
            defaultEcho = passwordField.getEchoChar();
        } else {
            passwordField = null;
            field = new JTextField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintHint(g, this, placeholder, getText().isEmpty());
                }
            };
        }

        field.setOpaque(false);
        field.setBorder(null);
        field.setFont(Theme.regular(14));
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.TEXT);
        field.setSelectionColor(Theme.BLUE);
        field.setSelectedTextColor(Color.WHITE);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });

        add(field, BorderLayout.CENTER);

        if (password) {
            JButton eye = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Icons.paint(g2,
                            passwordVisible ? Icons.Type.EYE_OFF : Icons.Type.EYE,
                            (getWidth() - 18) / 2f, (getHeight() - 18) / 2f, 18,
                            Theme.MUTED);
                    g2.dispose();
                }
            };
            eye.setContentAreaFilled(false);
            eye.setBorderPainted(false);
            eye.setFocusPainted(false);
            eye.setFocusable(false);
            eye.setOpaque(false);
            eye.setToolTipText("Show / hide password");
            eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            eye.setPreferredSize(new Dimension(34, 34));
            eye.addActionListener(e -> setPasswordVisible(!passwordVisible));

            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            holder.add(eye);
            // The eye sits in the right padding area, outside the text field.
            setLayout(null);
            removeAll();
            add(field);
            add(holder);
            setLayout(new LayoutManager() {
                @Override public void addLayoutComponent(String n, Component c) { }
                @Override public void removeLayoutComponent(Component c) { }
                @Override public Dimension preferredLayoutSize(Container p) { return getPreferredSize(); }
                @Override public Dimension minimumLayoutSize(Container p) { return getMinimumSize(); }
                @Override
                public void layoutContainer(Container p) {
                    int w = p.getWidth();
                    int h = p.getHeight();
                    field.setBounds(42, 0, Math.max(0, w - 42 - 46), h);
                    holder.setBounds(w - 44, 0, 40, h);
                }
            });
            setBorder(null);
        }
    }

    public static FieldBox text(Icons.Type icon, String placeholder) {
        return new FieldBox(icon, placeholder, false);
    }

    public static FieldBox password(String placeholder) {
        return new FieldBox(Icons.Type.LOCK, placeholder, true);
    }

    private void paintHint(Graphics g, JTextComponent c, String hint, boolean empty) {
        if (!empty || hint == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setColor(Theme.alpha(Theme.MUTED, 170));
        g2.setFont(c.getFont());
        FontMetrics fm = g2.getFontMetrics();
        Insets in = c.getInsets();
        g2.drawString(hint, in.left, (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }

    public JTextComponent getField() {
        return field;
    }

    public String getText() {
        return field.getText();
    }

    public char[] getPassword() {
        return passwordField == null ? new char[0] : passwordField.getPassword();
    }

    public void clear() {
        field.setText("");
    }

    public void setPasswordVisible(boolean visible) {
        if (passwordField == null) {
            return;
        }
        passwordVisible = visible;
        passwordField.setEchoChar(visible ? (char) 0 : (defaultEcho == 0 ? '•' : defaultEcho));
        repaint();
    }

    @Override
    public void requestFocus() {
        field.requestFocus();
    }

    @Override
    public boolean requestFocusInWindow() {
        return field.requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        g2.setColor(Theme.INPUT);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

        g2.setColor(focused ? Theme.BLUE : Theme.BORDER);
        g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

        Icons.paint(g2, leadingIcon, 13, (getHeight() - 18) / 2f, 18,
                focused ? Theme.BLUE_LIGHT : Theme.MUTED);

        g2.dispose();
        super.paintComponent(g);
    }
}
