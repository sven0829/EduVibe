package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import javax.swing.SwingWorker;

/** NEW FILE - circular avatar showing the profile picture or the user's initials. */
public class Avatar extends JComponent {

    private String initials = "EV";
    private Image image;
    private final int diameter;
    private String frameName;
    private String frameRarity;
    private int frameRequest;

    public Avatar(int diameter) {
        this.diameter = diameter;
        setPreferredSize(new Dimension(diameter, diameter));
        setMinimumSize(new Dimension(diameter, diameter));
        setMaximumSize(new Dimension(diameter, diameter));
        setOpaque(false);
    }

    public void setUser(String fullName, String profilePath) {
        this.initials = initialsOf(fullName);
        this.image = loadImage(profilePath);
        repaint();
    }

    /** Draws a reward frame around the avatar (null clears it). */
    public void setFrame(String name, String rarity) {
        this.frameName = name;
        this.frameRarity = rarity;
        repaint();
    }

    /** Looks up the user's equipped frame in the background and shows it. */
    public void showFrameOf(int userId) {
        if (userId <= 0) {
            return;
        }
        final int req = ++frameRequest;
        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                return com.eduvibe.dao.EquipDAO.frameOf(userId);
            }

            @Override
            protected void done() {
                try {
                    if (req != frameRequest) {
                        return;
                    }
                    String[] f = get();
                    setFrame(f == null ? null : f[0], f == null ? null : f[1]);
                } catch (Exception ignored) {
                    // no frame
                }
            }
        }.execute();
    }

    public static String initialsOf(String name) {
        if (name == null || name.isBlank()) {
            return "EV";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.isAbsolute() && !file.exists()) {
            file = new File(System.getProperty("user.dir"), path);
        }
        if (!file.isFile()) {
            return null;
        }
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        return icon.getIconWidth() > 0 ? icon.getImage() : null;
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int full = Math.min(getWidth(), getHeight());
        int d = full;
        if (frameName != null) {
            d = (int) Math.round(full * 0.58);
            RewardArt.paintFrame(g2, full / 2.0, full / 2.0, d, frameName, frameRarity);
            g2.translate((full - d) / 2.0, (full - d) / 2.0);
        }

        g2.setColor(Theme.BLUE_SOFT);
        g2.fillOval(0, 0, d, d);

        if (image != null) {
            Shape old = g2.getClip();
            g2.setClip(new Ellipse2D.Float(0, 0, d, d));
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            if (w > 0 && h > 0) {
                double scale = Math.max((double) d / w, (double) d / h);
                int dw = (int) (w * scale);
                int dh = (int) (h * scale);
                g2.drawImage(image, (d - dw) / 2, (d - dh) / 2, dw, dh, null);
            }
            g2.setClip(old);
        } else {
            g2.setColor(Theme.BLUE_LIGHT);
            g2.setFont(Theme.bold(d * 0.36f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials,
                    (d - fm.stringWidth(initials)) / 2,
                    (d - fm.getHeight()) / 2 + fm.getAscent());
        }

        g2.setColor(Theme.BORDER);
        g2.drawOval(0, 0, d - 1, d - 1);

        g2.dispose();
    }
}
