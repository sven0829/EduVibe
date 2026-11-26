package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * com.eduvibe.ui.components.ImageViewerDialog  (NEW FILE)
 *
 * Full-size image viewer: mouse wheel or +/- to zoom, drag to move,
 * double-click to reset, Esc or the close button to leave.
 *
 * Usage: ImageViewerDialog.show(SwingUtilities.getWindowAncestor(this), file);
 */
public final class ImageViewerDialog extends JDialog {

    private final BufferedImage image;
    private double zoom = 1.0;   // 1.0 = fit to window
    private int offsetX = 0;
    private int offsetY = 0;
    private Point dragStart;

    private final JPanel canvas;

    public static void show(Window owner, File file) {

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                Dialogs.warning(owner, "Image", "This image format cannot be displayed.");
                return;
            }
            new ImageViewerDialog(owner, img, file.getName()).setVisible(true);
        } catch (Exception ex) {
            Dialogs.error(owner, "Image", "Unable to open the image.\n" + ex.getMessage());
        }
    }

    private ImageViewerDialog(Window owner, BufferedImage image, String title) {

        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.image = image;

        setUndecorated(true);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        setSize((int) (screen.width * 0.86), (int) (screen.height * 0.88));
        setLocationRelativeTo(owner);

        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                double fit = Math.min((double) getWidth() / image.getWidth(),
                        (double) getHeight() / image.getHeight());
                fit = Math.min(fit, 1.0);
                double scale = fit * zoom;
                int w = (int) (image.getWidth() * scale);
                int h = (int) (image.getHeight() * scale);
                int x = (getWidth() - w) / 2 + offsetX;
                int y = (getHeight() - h) / 2 + offsetY;
                g2.drawImage(image, x, y, w, h, null);
                g2.dispose();
            }
        };
        canvas.setBackground(new Color(5, 9, 18));
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        canvas.addMouseWheelListener(e -> changeZoom(e.getWheelRotation() < 0 ? 1.15 : 1 / 1.15));
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    reset();
                }
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    offsetX += e.getX() - dragStart.x;
                    offsetY += e.getY() - dragStart.y;
                    dragStart = e.getPoint();
                    canvas.repaint();
                }
            }
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bar.setBackground(Theme.SIDEBAR);

        JLabel name = new JLabel(title);
        name.setForeground(Theme.MUTED);
        name.setFont(Theme.regular(12));

        RoundedButton minus = new RoundedButton("−", RoundedButton.Style.SECONDARY);
        RoundedButton plus = new RoundedButton("+", RoundedButton.Style.SECONDARY);
        RoundedButton fit = new RoundedButton("Fit", RoundedButton.Style.SECONDARY);
        RoundedButton close = new RoundedButton("Close", RoundedButton.Style.PRIMARY);
        minus.addActionListener(e -> changeZoom(1 / 1.25));
        plus.addActionListener(e -> changeZoom(1.25));
        fit.addActionListener(e -> reset());
        close.addActionListener(e -> dispose());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        left.setOpaque(false);
        left.add(name);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.SIDEBAR);
        top.add(left, BorderLayout.WEST);
        bar.setOpaque(false);
        bar.add(minus);
        bar.add(plus);
        bar.add(fit);
        bar.add(close);
        top.add(bar, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        root.add(top, BorderLayout.NORTH);
        root.add(canvas, BorderLayout.CENTER);
        setContentPane(root);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void changeZoom(double factor) {
        zoom = Math.max(0.25, Math.min(8.0, zoom * factor));
        canvas.repaint();
    }

    private void reset() {
        zoom = 1.0;
        offsetX = 0;
        offsetY = 0;
        canvas.repaint();
    }
}
