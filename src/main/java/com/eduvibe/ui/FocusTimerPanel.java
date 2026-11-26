package com.eduvibe.ui;

import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.ui.components.Toast;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.util.prefs.Preferences;

/**
 * com.eduvibe.ui.FocusTimerPanel  (NEW FILE)
 *
 * Study focus timer (Pomodoro style). Choose a length, start, pause or reset.
 * Finished minutes are added to "Focus time today", stored on this computer
 * (Java Preferences, per user), so no database change is needed.
 * Constructor FocusTimerPanel(int userId).
 */
public class FocusTimerPanel extends JPanel {

    private static final int[] PRESETS = {15, 25, 45, 60};

    private final int userId;

    private int totalSeconds = 25 * 60;
    private int remainingSeconds = totalSeconds;
    private boolean running = false;

    private final javax.swing.Timer ticker;
    private final Dial dial = new Dial();
    private final RoundedButton startButton = new RoundedButton("Start");
    private final RoundedButton resetButton = new RoundedButton("Reset", RoundedButton.Style.OUTLINE);
    private final JLabel todayLabel = new JLabel();
    private final RoundedButton[] presetButtons = new RoundedButton[PRESETS.length];

    public FocusTimerPanel(int userId) {

        this.userId = userId;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        com.eduvibe.ui.components.PageHero hero = new com.eduvibe.ui.components.PageHero(Icons.Type.CLOCK,
                "Focus Timer", "Pick a length, press Start and study without distractions.",
                com.eduvibe.ui.components.PageHero.PEOPLE[0], com.eduvibe.ui.components.PageHero.PEOPLE[1]);
        add(hero, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(22, Theme.CARD, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 36, 18, 36));

        JLabel title = new JLabel("Study Focus Timer", Icons.icon(Icons.Type.CLOCK, 24, Theme.BLUE_LIGHT),
                SwingConstants.CENTER);
        title.setIconTextGap(10);
        title.setFont(Theme.bold(20));
        title.setForeground(Theme.TEXT);

        JPanel presets = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        presets.setOpaque(false);
        for (int i = 0; i < PRESETS.length; i++) {
            final int minutes = PRESETS[i];
            RoundedButton b = new RoundedButton(minutes + " min", RoundedButton.Style.SECONDARY);
            b.addActionListener(e -> choosePreset(minutes));
            presetButtons[i] = b;
            presets.add(b);
        }

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(presets, BorderLayout.CENTER);

        startButton.addActionListener(e -> toggle());
        resetButton.addActionListener(e -> reset());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(300, 42));
        buttons.add(resetButton);
        buttons.add(startButton);

        todayLabel.setFont(Theme.regular(13));
        todayLabel.setForeground(Theme.MUTED);
        todayLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonWrap.setOpaque(false);
        buttonWrap.add(buttons);
        bottom.add(buttonWrap, BorderLayout.NORTH);
        bottom.add(todayLabel, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(dial, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;     // top of the page, never cut off on short screens
        gc.weightx = 1;
        gc.weighty = 1;
        gc.insets = new Insets(8, 0, 8, 0);
        center.add(card, gc);
        add(center, BorderLayout.CENTER);

        ticker = new javax.swing.Timer(1000, e -> tick());
        refreshToday();
    }

    /** Called by the dashboard when the page opens. */
    public void load() {
        refreshToday();
    }

    // ---------------- timer logic ----------------

    private void choosePreset(int minutes) {
        if (running) {
            return;
        }
        totalSeconds = minutes * 60;
        remainingSeconds = totalSeconds;
        dial.repaint();
    }

    private void toggle() {
        if (running) {
            running = false;
            ticker.stop();
            startButton.setText("Resume");
        } else {
            if (remainingSeconds <= 0) {
                remainingSeconds = totalSeconds;
            }
            running = true;
            ticker.start();
            startButton.setText("Pause");
        }
        updateEnabled();
    }

    private void reset() {
        running = false;
        ticker.stop();
        remainingSeconds = totalSeconds;
        startButton.setText("Start");
        updateEnabled();
        dial.repaint();
    }

    private void tick() {
        remainingSeconds--;
        dial.repaint();

        if (remainingSeconds <= 0) {
            running = false;
            ticker.stop();
            startButton.setText("Start");
            updateEnabled();

            addFocusMinutes(totalSeconds / 60);
            refreshToday();
            Toolkit.getDefaultToolkit().beep();
            Toast.success(this, "Focus session complete! +" + (totalSeconds / 60) + " min");
        }
    }

    private void updateEnabled() {
        for (RoundedButton b : presetButtons) {
            b.setEnabled(!running);
        }
    }

    // ---------------- stored focus time ----------------

    private Preferences prefs() {
        return Preferences.userRoot().node("eduvibe/focus/" + userId);
    }

    private void addFocusMinutes(int minutes) {
        try {
            Preferences p = prefs();
            String today = LocalDate.now().toString();
            int current = today.equals(p.get("date", "")) ? p.getInt("minutes", 0) : 0;
            int sessions = today.equals(p.get("date", "")) ? p.getInt("sessions", 0) : 0;
            p.put("date", today);
            p.putInt("minutes", current + minutes);
            p.putInt("sessions", sessions + 1);
            p.flush();
        } catch (Exception ignored) {
            // storage unavailable: the timer still works
        }
    }

    private void refreshToday() {
        int minutes = 0;
        int sessions = 0;
        try {
            Preferences p = prefs();
            if (LocalDate.now().toString().equals(p.get("date", ""))) {
                minutes = p.getInt("minutes", 0);
                sessions = p.getInt("sessions", 0);
            }
        } catch (Exception ignored) {
            // show zero
        }
        todayLabel.setText("Focus time today: " + minutes + " min  •  "
                + sessions + (sessions == 1 ? " session" : " sessions"));
    }

    // ---------------- circular dial ----------------

    private final class Dial extends JComponent {

        Dial() {
            // scales with the screen: smaller on laptops, same look on big monitors
            int screen = Toolkit.getDefaultToolkit().getScreenSize().height;
            int size = Math.max(190, Math.min(270, (int) (screen * 0.3)));
            setPreferredSize(new Dimension(size + 20, size));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);

            int size = Math.min(getWidth(), getHeight()) - 20;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.INPUT);
            g2.draw(new Ellipse2D.Double(x, y, size, size));

            double fraction = totalSeconds == 0 ? 0 : (double) remainingSeconds / totalSeconds;
            g2.setColor(running ? Theme.BLUE : Theme.BLUE_LIGHT);
            g2.draw(new Arc2D.Double(x, y, size, size, 90, -360 * fraction, Arc2D.OPEN));

            String time = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);
            g2.setFont(Theme.bold(Math.max(34, Math.min(46, size / 6))));
            g2.setColor(Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(time, (getWidth() - fm.stringWidth(time)) / 2,
                    getHeight() / 2 + fm.getAscent() / 2 - 6);

            String hint = running ? "Stay focused" : "Ready";
            g2.setFont(Theme.regular(13));
            g2.setColor(Theme.MUTED);
            fm = g2.getFontMetrics();
            g2.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2, getHeight() / 2 + 36);
            g2.dispose();
        }
    }
}
