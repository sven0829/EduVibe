package com.eduvibe.ui;

import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.FieldBox;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * com.eduvibe.ui.CreatePostPanel  (REPLACES existing file)
 *
 * Redesigned composer: a gradient header, two big category tiles, a glowing content box,
 * a drag-and-drop attachment area, a Reel switch and a LIVE PREVIEW of the post on the right
 * (it drops under the form on narrow windows). The constructors and all publishing logic
 * (SwingWorker, uploads/ folder, +15 points) are unchanged.
 */
public class CreatePostPanel extends JPanel {

    private final int currentUserId;
    private final Runnable onPublished;
    private final Runnable onCancel;
    private final PostService postService = new PostService();

    private FieldBox titleField;
    private JTextArea descriptionArea;
    private JCheckBox reelCheckBox;

    private JLabel fileNameLabel;
    private JLabel fileTypeLabel;
    private JLabel previewLabel;
    private RoundedButton removeFileButton;
    private RoundedButton publishButton;
    private RoundedButton cancelButton;

    private ChoiceTile academicTile;
    private ChoiceTile entertainmentTile;

    // live preview
    private JPanel previewCard;
    private JLabel pvName;
    private JLabel pvMeta;
    private JLabel pvTitle;
    private JTextArea pvBody;
    private JPanel pvMedia;
    private JLabel pvChip;
    private Image previewImage;
    private boolean previewIsVideo;

    /** null until the author chooses: a post is never classified automatically. */
    private Boolean academicChoice = null;
    private Boolean reelChoice = null;
    private ChoiceTile postFormatTile;
    private ChoiceTile reelFormatTile;
    private File selectedFile;

    public CreatePostPanel(int currentUserId, Runnable onPublished) {
        this(currentUserId, onPublished, onPublished);
    }

    public CreatePostPanel(int currentUserId, Runnable onPublished, Runnable onCancel) {
        this.currentUserId = currentUserId;
        this.onPublished = onPublished;
        this.onCancel = onCancel;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        buildForm();
    }

    // ---------------- UI ----------------

    private void buildForm() {

        // ===== left column: the composer =====
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        left.add(new Hero());
        left.add(Box.createVerticalStrut(18));

        left.add(section("Category (required)", buildTiles()));
        left.add(Box.createVerticalStrut(14));
        left.add(section("Format (required)", buildFormatTiles()));
        left.add(Box.createVerticalStrut(14));

        titleField = FieldBox.text(Icons.Type.BOOK, "Give your post a clear title");
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleField.getField().getDocument().addDocumentListener(simpleListener(this::refreshPreview));
        left.add(section("Title (optional)", titleField));
        left.add(Box.createVerticalStrut(14));

        left.add(section("What do you want to share?", buildContentBox()));
        left.add(Box.createVerticalStrut(8));
        left.add(buildTagRow());
        left.add(Box.createVerticalStrut(14));

        left.add(section("Attachment", buildAttachment()));
        left.add(Box.createVerticalStrut(14));

        reelCheckBox = new SwitchBox("Publish as a Reel");      // kept in sync with the Format tiles; not shown
        left.add(Box.createVerticalStrut(10));

        RoundedButton bulkReels = new RoundedButton("Post many videos as Reels at once", RoundedButton.Style.OUTLINE);
        bulkReels.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkReels.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        bulkReels.addActionListener(e -> BulkReelsDialog.open(this, currentUserId, onPublished));
        left.add(bulkReels);
        left.add(Box.createVerticalStrut(18));

        left.add(buildActions());

        // ===== right column: live preview =====
        final boolean[] wideMode = {true};
        JPanel right = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return wideMode[0] ? new Dimension(310, d.height) : d;
            }
        };
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel pvHead = new JLabel("LIVE PREVIEW");
        pvHead.setFont(Theme.bold(11));
        pvHead.setForeground(Theme.MUTED);
        pvHead.setBorder(new EmptyBorder(2, 4, 8, 0));
        pvHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(pvHead);
        right.add(buildPreview());
        right.add(Box.createVerticalStrut(14));
        right.add(buildTips());

        // ===== page =====
        JPanel page = new JPanel(new GridBagLayout());
        page.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.NORTH;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.weightx = 1;
        page.add(left, gc);
        gc.gridx = 1;
        gc.weightx = 0;
        gc.insets = new Insets(0, 20, 0, 0);
        page.add(right, gc);

        // narrow window: the preview is hidden instead of squeezing the form
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boolean wide = getWidth() >= 820;
                if (wideMode[0] != wide || !right.isVisible()) {
                    wideMode[0] = wide;
                    right.setVisible(true);
                    GridBagConstraints c2 = new GridBagConstraints();
                    c2.anchor = GridBagConstraints.NORTH;
                    c2.fill = GridBagConstraints.HORIZONTAL;
                    if (wide) {
                        c2.gridx = 1;
                        c2.gridy = 0;
                        c2.insets = new Insets(0, 20, 0, 0);
                    } else {
                        c2.gridx = 0;
                        c2.gridy = 1;
                        c2.weightx = 1;
                        c2.insets = new Insets(20, 0, 0, 0);
                    }
                    ((GridBagLayout) page.getLayout()).setConstraints(right, c2);
                    page.revalidate();
                    page.repaint();
                }
            }
        });

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setBorder(new EmptyBorder(4, 0, 24, 0));
        holder.add(page, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);

        refreshPreview();
    }

    private JPanel section(String title, JComponent body) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(title);
        l.setFont(Theme.bold(13));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 2, 8, 0));
        p.add(l);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(body);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 400));
        return p;
    }

    private static DocumentListener simpleListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    // ----- category tiles -----

    private JPanel buildTiles() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        academicTile = new ChoiceTile(Icons.Type.BOOK, "Academic",
                "Study help, notes, questions", () -> setAcademic(true));
        entertainmentTile = new ChoiceTile(Icons.Type.SPARKLE, "Entertainment",
                "Fun, games, videos, hangouts", () -> setAcademic(false));
        row.add(academicTile);
        row.add(entertainmentTile);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return row;
    }

    private JPanel buildFormatTiles() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        postFormatTile = new ChoiceTile(Icons.Type.IMAGE, "Regular Post",
                "Appears in the Home feed", () -> setReel(false));
        reelFormatTile = new ChoiceTile(Icons.Type.PLAY, "Reel",
                "Vertical video, appears in Reels", () -> setReel(true));
        row.add(postFormatTile);
        row.add(reelFormatTile);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return row;
    }

    private void setReel(boolean value) {
        reelChoice = value;
        reelCheckBox.setSelected(value);
        postFormatTile.setSelected(!value);
        reelFormatTile.setSelected(value);
        refreshPreview();
    }

    private void setAcademic(boolean value) {
        academicChoice = value;
        academicTile.setSelected(value);
        entertainmentTile.setSelected(!value);
        refreshPreview();
    }

    // ----- content box with glow on focus -----

    private JComponent buildContentBox() {

        descriptionArea = new JTextArea(6, 30);
        descriptionArea.setFont(Theme.regular(14));
        descriptionArea.setForeground(Theme.TEXT);
        descriptionArea.setOpaque(false);
        descriptionArea.setCaretColor(Theme.TEXT);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(new EmptyBorder(12, 14, 6, 14));

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(null);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);

        final boolean[] focused = {false};
        RoundedPanel box = new RoundedPanel(14, Theme.INPUT, Theme.BORDER) {
            @Override
            protected void paintBorder(Graphics g) {
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                if (focused[0]) {
                    g2.setStroke(new BasicStroke(2f));
                    g2.setPaint(Theme.brand(0, 0, getWidth(), 0, 0));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                }
                g2.dispose();
            }
        };
        box.setLayout(new BorderLayout());
        box.add(descScroll, BorderLayout.CENTER);
        descriptionArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                focused[0] = true;
                box.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                focused[0] = false;
                box.repaint();
            }
        });

        // emoji button on the left, character counter on the right
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 8, 6, 14));
        JPanel emojiLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        emojiLeft.setOpaque(false);
        emojiLeft.add(com.eduvibe.ui.components.EmojiPicker.button(descriptionArea));
        bar.add(emojiLeft, BorderLayout.WEST);

        final int maxChars = 2000;
        JLabel counter = new JLabel("0/" + maxChars);
        counter.setFont(Theme.regular(11));
        counter.setForeground(Theme.MUTED);
        descriptionArea.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                SwingUtilities.invokeLater(() -> {
                    String t = descriptionArea.getText();
                    if (t.length() > maxChars) {
                        descriptionArea.setText(t.substring(0, maxChars));
                        t = descriptionArea.getText();
                    }
                    counter.setText(t.length() + "/" + maxChars);
                    counter.setForeground(t.length() >= maxChars ? Theme.RED : Theme.MUTED);
                    refreshPreview();
                });
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
        bar.add(counter, BorderLayout.EAST);
        JComponent progress = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                int w = getWidth() - 20, y = getHeight() / 2 - 2;
                g2.setColor(Theme.alpha(Theme.BORDER, 200));
                g2.fillRoundRect(10, y, w, 4, 4, 4);
                double r = Math.min(1.0, descriptionArea.getText().length() / (double) maxChars);
                if (r > 0) {
                    g2.setPaint(r > 0.92 ? new GradientPaint(0, 0, Theme.GOLD, w, 0, Theme.RED)
                            : Theme.brand(10, 0, 10 + w, 0, 0));
                    g2.fillRoundRect(10, y, Math.max(4, (int) (w * r)), 4, 4, 4);
                }
                g2.dispose();
            }
        };
        progress.setPreferredSize(new Dimension(10, 24));
        descriptionArea.getDocument().addDocumentListener(simpleListener(progress::repaint));
        bar.add(progress, BorderLayout.CENTER);
        box.add(bar, BorderLayout.SOUTH);
        return box;
    }

    /** Quick hashtag chips: a click adds the tag to the text where the cursor is. */
    private JPanel buildTagRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = new JLabel("Quick tags:");
        hint.setFont(Theme.regular(11));
        hint.setForeground(Theme.MUTED);
        row.add(hint);
        for (String t : new String[]{"Question", "StudyTip", "Notes", "Announcement", "Showcase", "Fun"}) {
            row.add(new TagChip(t, () -> {
                String tag = "#" + t + " ";
                int pos = Math.min(descriptionArea.getCaretPosition(), descriptionArea.getDocument().getLength());
                descriptionArea.insert(tag, pos);
                descriptionArea.requestFocusInWindow();
            }));
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return row;
    }

    private JPanel reelRow(JCheckBox sw) {
        RoundedPanel row = new RoundedPanel(14, Theme.CARD, Theme.BORDER);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(new EmptyBorder(12, 16, 12, 16));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = new JLabel("Reels appear in the vertical video feed");
        hint.setFont(Theme.regular(12));
        hint.setForeground(Theme.MUTED);
        row.add(sw, BorderLayout.WEST);
        row.add(hint, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return row;
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel earn = new JLabel("  Earn +15 points when you post");
        earn.setFont(Theme.bold(12));
        earn.setForeground(Theme.GOLD);
        earn.setIcon(Icons.icon(Icons.Type.COIN, 16, Theme.GOLD));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        cancelButton = new RoundedButton("Cancel", RoundedButton.Style.SECONDARY);
        cancelButton.addActionListener(e -> {
            clearForm();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        publishButton = new RoundedButton("Post").withIcon(Icons.Type.SEND);
        publishButton.setPreferredSize(new Dimension(140, 44));
        publishButton.addActionListener(e -> publishPost());

        buttons.add(cancelButton);
        buttons.add(publishButton);
        actions.add(earn, BorderLayout.WEST);
        actions.add(buttons, BorderLayout.EAST);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return actions;
    }

    // ----- attachment drop zone -----

    private JPanel buildAttachment() {

        DropZone box = new DropZone();
        box.setLayout(new BorderLayout(14, 0));
        box.setBorder(new EmptyBorder(14, 16, 14, 16));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        previewLabel = new JLabel("FILE", SwingConstants.CENTER);
        previewLabel.setFont(Theme.bold(10));
        previewLabel.setForeground(Theme.MUTED);
        previewLabel.setOpaque(true);
        previewLabel.setBackground(Theme.CARD_LIGHT);
        previewLabel.setPreferredSize(new Dimension(64, 64));

        fileNameLabel = new JLabel("Drop an image or video here");
        fileNameLabel.setFont(Theme.bold(13));
        fileNameLabel.setForeground(Theme.MUTED);

        fileTypeLabel = new JLabel("or press Choose file  -  optional");
        fileTypeLabel.setFont(Theme.regular(11));
        fileTypeLabel.setForeground(Theme.MUTED);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(Box.createVerticalGlue());
        text.add(fileNameLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(fileTypeLabel);
        text.add(Box.createVerticalGlue());

        RoundedButton choose = new RoundedButton("Choose file", RoundedButton.Style.OUTLINE);
        choose.addActionListener(e -> chooseFile());

        removeFileButton = new RoundedButton("Remove", RoundedButton.Style.DANGER);
        removeFileButton.setVisible(false);
        removeFileButton.addActionListener(e -> clearSelectedFile());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 14));
        buttons.setOpaque(false);
        buttons.add(choose);
        buttons.add(removeFileButton);

        box.add(previewLabel, BorderLayout.WEST);
        box.add(text, BorderLayout.CENTER);
        box.add(buttons, BorderLayout.EAST);

        box.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport s) {
                return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport s) {
                try {
                    List<File> files = (List<File>) s.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        acceptFile(files.get(0));
                        return true;
                    }
                } catch (Exception ignored) {
                    // fall through: the drop is simply not accepted
                }
                return false;
            }
        });
        return box;
    }

    // ----- live preview -----

    private JPanel buildPreview() {
        previewCard = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        previewCard.setLayout(new BoxLayout(previewCard, BoxLayout.Y_AXIS));
        previewCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        previewCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel head = new JPanel(new BorderLayout(10, 0));
        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComponent avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setPaint(Theme.brand(0, 0, getWidth(), getHeight(), 0));
                g2.fill(new Ellipse2D.Float(0, 0, getWidth() - 1, getHeight() - 1));
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.bold(12));
                FontMetrics fm = g2.getFontMetrics();
                String s = "YOU";
                g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(38, 38));
        pvName = new JLabel("You");
        pvName.setFont(Theme.bold(13));
        pvName.setForeground(Theme.TEXT);
        pvMeta = new JLabel("Just now");
        pvMeta.setFont(Theme.regular(11));
        pvMeta.setForeground(Theme.MUTED);
        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(pvName);
        who.add(pvMeta);
        head.add(avatar, BorderLayout.WEST);
        head.add(who, BorderLayout.CENTER);
        pvChip = new JLabel("Academic");
        pvChip.setFont(Theme.bold(10));
        pvChip.setForeground(Theme.BLUE_LIGHT);
        head.add(pvChip, BorderLayout.EAST);
        previewCard.add(head);
        previewCard.add(Box.createVerticalStrut(12));

        pvTitle = new JLabel(" ");
        pvTitle.setFont(Theme.bold(15));
        pvTitle.setForeground(Theme.TEXT);
        pvTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(pvTitle);
        previewCard.add(Box.createVerticalStrut(4));

        pvBody = new JTextArea();
        pvBody.setEditable(false);
        pvBody.setFocusable(false);
        pvBody.setOpaque(false);
        pvBody.setLineWrap(true);
        pvBody.setWrapStyleWord(true);
        pvBody.setFont(Theme.regular(13));
        pvBody.setForeground(Theme.TEXT);
        pvBody.setBorder(null);
        pvBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(pvBody);
        previewCard.add(Box.createVerticalStrut(10));

        pvMedia = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                int w = getWidth(), h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12);
                g2.setClip(shape);
                if (previewImage != null) {
                    int iw = previewImage.getWidth(null), ih = previewImage.getHeight(null);
                    if (iw > 0 && ih > 0) {
                        double s = Math.max((double) w / iw, (double) h / ih);
                        int dw = (int) (iw * s), dh = (int) (ih * s);
                        g2.drawImage(previewImage, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                    }
                } else {
                    g2.setPaint(new GradientPaint(0, 0, new Color(30, 40, 90), w, h, new Color(70, 40, 130)));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(new Color(255, 255, 255, 220));
                    g2.fill(new Ellipse2D.Float(w / 2f - 22, h / 2f - 22, 44, 44));
                    g2.setColor(new Color(60, 50, 140));
                    g2.fillPolygon(new int[]{w / 2 - 6, w / 2 - 6, w / 2 + 10},
                            new int[]{h / 2 - 11, h / 2 + 11, h / 2}, 3);
                }
                g2.setClip(null);
                g2.setColor(Theme.BORDER);
                g2.draw(shape);
                g2.dispose();
            }
        };
        pvMedia.setOpaque(false);
        pvMedia.setPreferredSize(new Dimension(100, 150));
        pvMedia.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        pvMedia.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(pvMedia);
        previewCard.add(Box.createVerticalStrut(12));

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        acts.setOpaque(false);
        acts.setAlignmentX(Component.LEFT_ALIGNMENT);
        acts.add(mini(Icons.Type.HEART, "Like"));
        acts.add(mini(Icons.Type.COMMENT, "Comment"));
        acts.add(mini(Icons.Type.SHARE, "Share"));
        previewCard.add(acts);
        return previewCard;
    }

    private JLabel mini(Icons.Type type, String text) {
        JLabel l = new JLabel(" " + text);
        l.setIcon(Icons.icon(type, 15, Theme.MUTED));
        l.setFont(Theme.regular(11));
        l.setForeground(Theme.MUTED);
        return l;
    }

    private JPanel buildTips() {
        RoundedPanel tips = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        tips.setLayout(new BoxLayout(tips, BoxLayout.Y_AXIS));
        tips.setBorder(new EmptyBorder(14, 16, 14, 16));
        tips.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel h = new JLabel("Tips for a great post");
        h.setFont(Theme.bold(13));
        h.setForeground(Theme.TEXT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        tips.add(h);
        tips.add(Box.createVerticalStrut(8));
        for (String t : new String[]{
                "A clear title gets more likes",
                "Add a picture or short video",
                "Videos should be MP4 (H.264)",
                "Be kind - rude words are blocked"}) {
            JLabel l = new JLabel("  " + t);
            l.setIcon(Icons.icon(Icons.Type.CHECK, 14, Theme.GREEN));
            l.setFont(Theme.regular(12));
            l.setForeground(Theme.MUTED);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.setBorder(new EmptyBorder(3, 0, 3, 0));
            tips.add(l);
        }
        return tips;
    }

    private void refreshPreview() {
        if (pvTitle == null || titleField == null || descriptionArea == null) {
            return;
        }
        String title = titleField.getText().trim();
        String body = descriptionArea.getText().trim();
        pvTitle.setText(title.isEmpty() ? (body.isEmpty() ? "Your title appears here" : " ") : title);
        pvTitle.setForeground(title.isEmpty() && body.isEmpty() ? Theme.MUTED : Theme.TEXT);
        if (body.isEmpty()) {
            pvBody.setText("Start typing and your post shows up here, just like it will in the feed.");
            pvBody.setForeground(Theme.MUTED);
        } else {
            pvBody.setText(body.length() > 220 ? body.substring(0, 220) + "..." : body);
            pvBody.setForeground(Theme.TEXT);
        }
        boolean reel = reelCheckBox != null && reelCheckBox.isSelected();
        pvChip.setText((academicChoice == null ? "NO CATEGORY YET" : academicChoice ? "ACADEMIC" : "ENTERTAINMENT")
                + (reelChoice == null ? "" : reel ? "  -  REEL" : "  -  POST"));
        pvChip.setForeground(academicChoice == null ? Theme.MUTED : academicChoice ? Theme.BLUE_LIGHT : Theme.PURPLE);
        pvMedia.setVisible(selectedFile != null);
        previewCard.revalidate();
        previewCard.repaint();
    }

    // ================= small custom components =================

    /** Gradient header with the page title and a points chip. */
    private static final class Hero extends JPanel {
        Hero() {
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(100, 92));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 20, 20);
            g2.setPaint(Theme.brand(0, 0, w, h, 0));
            g2.fill(shape);
            g2.setClip(shape);
            g2.setColor(new Color(255, 255, 255, 28));
            g2.fill(new Ellipse2D.Float(w - 150, -70, 220, 220));
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fill(new Ellipse2D.Float(w - 260, 30, 150, 150));
            g2.setClip(null);

            g2.setColor(Color.WHITE);
            g2.setFont(Theme.bold(24));
            g2.drawString("What's on your mind?", 24, 40);
            g2.setColor(new Color(255, 255, 255, 215));
            g2.setFont(Theme.regular(13));
            g2.drawString("Write it, add a picture or video, and see it live before you post.", 24, 64);

            String chip = "+15 pts";
            g2.setFont(Theme.bold(12));
            FontMetrics fm = g2.getFontMetrics();
            int cw = fm.stringWidth(chip) + 34, ch = 28;
            int cx = w - cw - 20, cy = 18;
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(cx, cy, cw, ch, ch, ch);
            Icons.paint(g2, Icons.Type.COIN, cx + 9, cy + 6, 16, Color.WHITE);
            g2.setColor(Color.WHITE);
            g2.drawString(chip, cx + 28, cy + (ch - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Small pill that runs an action when clicked. */
    private static final class TagChip extends JComponent {
        private final String text;
        private boolean hover;

        TagChip(String text, Runnable action) {
            this.text = "#" + text;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.bold(11));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth(text) + 22, 26);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            if (hover) {
                g2.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.BLUE, 60), w, 0, Theme.alpha(Theme.VIOLET, 60)));
            } else {
                g2.setColor(Theme.CARD);
            }
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(hover ? Theme.BLUE_LIGHT : Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setFont(getFont());
            g2.setColor(hover ? Theme.TEXT : Theme.BLUE_LIGHT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Big selectable card used for the two feeds. */
    private static final class ChoiceTile extends JPanel {
        private final Icons.Type icon;
        private final String title;
        private final String desc;
        private boolean selected;
        private boolean hover;

        ChoiceTile(Icons.Type icon, String title, String desc, Runnable onPick) {
            this.icon = icon;
            this.title = title;
            this.desc = desc;
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onPick.run();
                    requestFocusInWindow();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        onPick.run();
                    }
                }
            });
            getAccessibleContext().setAccessibleName(title);
        }

        void setSelected(boolean s) {
            selected = s;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 76);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(hover && !selected ? Theme.CARD_LIGHT : Theme.CARD);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            if (selected) {
                g2.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.BLUE, 50), w, h, Theme.alpha(Theme.VIOLET, 50)));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.setStroke(new BasicStroke(2f));
                g2.setPaint(Theme.brand(0, 0, w, 0, 0));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
            } else {
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            }
            // icon bubble
            int b = 44, bx = 16, by = (h - b) / 2;
            if (selected) {
                g2.setPaint(Theme.brand(bx, by, bx + b, by, 0));
            } else {
                g2.setColor(Theme.CARD_LIGHT);
            }
            g2.fillOval(bx, by, b, b);
            Icons.paint(g2, icon, bx + 11, by + 11, 22, selected ? Color.WHITE : Theme.BLUE_LIGHT);

            g2.setFont(Theme.bold(14));
            g2.setColor(Theme.TEXT);
            g2.drawString(title, 72, h / 2 - 3);
            g2.setFont(Theme.regular(11));
            g2.setColor(Theme.MUTED);
            g2.drawString(desc, 72, h / 2 + 15);

            if (selected) {
                int r = 20, rx = w - r - 14, ry = 14;
                g2.setPaint(Theme.brand(rx, ry, rx + r, ry, 0));
                g2.fillOval(rx, ry, r, r);
                Icons.paint(g2, Icons.Type.CHECK, rx + 4, ry + 4, 12, Color.WHITE);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.alpha(Theme.BLUE_LIGHT, 140));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                g2.drawRoundRect(4, 4, w - 9, h - 9, 12, 12);
            }
            g2.dispose();
        }
    }

    /** Check box drawn as an on/off switch. It is still a JCheckBox, so isSelected() works as before. */
    private static final class SwitchBox extends JCheckBox {
        SwitchBox(String text) {
            super(text);
            setOpaque(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFont(Theme.bold(13));
            setForeground(Theme.TEXT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(52 + fm.stringWidth(getText()), 28);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int ty = (getHeight() - 22) / 2;
            if (isSelected()) {
                g2.setPaint(Theme.brand(0, ty, 40, ty, 0));
            } else {
                g2.setColor(Theme.CARD_LIGHT);
            }
            g2.fillRoundRect(0, ty, 40, 22, 22, 22);
            if (!isSelected()) {
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, ty, 39, 21, 22, 22);
            }
            g2.setColor(Color.WHITE);
            g2.fillOval(isSelected() ? 20 : 3, ty + 3, 16, 16);
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 52, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Dashed drop area that lights up while a file is dragged over it. */
    private static final class DropZone extends JPanel {
        private boolean hover;

        DropZone() {
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(hover ? Theme.alpha(Theme.BLUE, 28) : Theme.INPUT);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.setColor(hover ? Theme.BLUE_LIGHT : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[]{6f, 5f}, 0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
            g2.dispose();
        }
    }

    // ---------------- file handling ----------------

    private void chooseFile() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose an Image or Video");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        acceptFile(chooser.getSelectedFile());
    }

    /** Validates and takes a file chosen with the dialog or dropped on the attachment box. */
    private void acceptFile(File file) {

        if (file == null || !file.isFile()) {
            Dialogs.error(this, "Invalid file", "Please select a valid file.");
            return;
        }

        String extension = getExtension(file.getName());

        if (!isImage(extension) && !isVideo(extension)) {
            Dialogs.error(this, "Unsupported file type",
                    "Allowed: JPG, PNG, GIF, WEBP, MP4, MOV, AVI, MKV, WEBM.");
            return;
        }

        if (reelCheckBox.isSelected() && !isVideo(extension)) {
            Dialogs.error(this, "Reel needs a video", "A Reel must have a video attachment.");
            return;
        }

        selectedFile = file;
        updateFileInformation();

        if (isVideo(extension)) {
            checkVideoFormat(file);
        }
    }

    /** Looks inside the video in the background and warns when the built-in player could not play it. */
    private void checkVideoFormat(File file) {
        new SwingWorker<com.eduvibe.util.VideoCheck.Result, Void>() {
            @Override
            protected com.eduvibe.util.VideoCheck.Result doInBackground() {
                return com.eduvibe.util.VideoCheck.inspect(file);
            }

            @Override
            protected void done() {
                try {
                    com.eduvibe.util.VideoCheck.Result r = get();
                    if (r.playable() || selectedFile != file) {
                        return;
                    }
                    fileTypeLabel.setText("Video attachment - may not play inside the app");
                    fileTypeLabel.setForeground(Theme.GOLD);

                    // ffmpeg is installed: convert right away, no question asked
                    if (com.eduvibe.util.VideoConverter.isAvailable()) {
                        convertVideo(file);
                        return;
                    }

                    Dialogs.warning(CreatePostPanel.this, "This video may not play", r.message()
                            + "\n\nTip: install ffmpeg and EduVibe can convert videos for you.");
                } catch (Exception ignored) {
                    // the check is only a helper; never block posting because of it
                }
            }
        }.execute();
    }

    /** Converts the chosen video to MP4 (H.264 + AAC) in the background and swaps it in when done. */
    private void convertVideo(File original) {

        publishButton.setEnabled(false);
        fileTypeLabel.setText("Converting video... please wait");
        fileTypeLabel.setForeground(Theme.GOLD);

        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                File converted = com.eduvibe.util.VideoConverter.convertToPlayableMp4(original);
                if (!com.eduvibe.util.VideoCheck.inspect(converted).playable()) {
                    throw new java.io.IOException("The converted video still cannot be played by EduVibe.");
                }
                return converted;
            }

            @Override
            protected void done() {
                publishButton.setEnabled(true);
                if (selectedFile != original) {
                    return;            // the person chose another file or removed this one meanwhile
                }
                try {
                    selectedFile = get();
                    updateFileInformation();
                    fileTypeLabel.setText("Converted to MP4 (H.264 + AAC) - ready to post");
                    fileTypeLabel.setForeground(Theme.MUTED);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    fileTypeLabel.setText("Video attachment - may not play inside the app");
                    fileTypeLabel.setForeground(Theme.GOLD);
                    Dialogs.error(CreatePostPanel.this, "Conversion failed",
                            cause.getMessage() + "\n\nYour original video is still attached. "
                                    + "You can post it anyway or convert it with HandBrake.");
                }
            }
        }.execute();
    }

    private void updateFileInformation() {

        if (selectedFile == null) {
            return;
        }

        String extension = getExtension(selectedFile.getName());

        fileNameLabel.setText(selectedFile.getName());
        fileNameLabel.setForeground(Theme.TEXT);

        if (isImage(extension)) {
            fileTypeLabel.setText("Image attachment");
            previewLabel.setText("IMAGE");
            previewLabel.setIcon(null);

            ImageIcon icon = new ImageIcon(selectedFile.getAbsolutePath());
            if (icon.getIconWidth() > 0) {
                previewLabel.setText("");
                previewLabel.setIcon(new ImageIcon(
                        icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH)));
                previewImage = icon.getImage();
            }
            previewIsVideo = false;
        } else {
            fileTypeLabel.setText("Video attachment");
            fileTypeLabel.setForeground(Theme.MUTED);
            previewLabel.setText("VIDEO");
            previewLabel.setIcon(null);
            previewImage = null;
            previewIsVideo = true;
        }

        removeFileButton.setVisible(true);
        refreshPreview();
        revalidate();
        repaint();
    }

    private void clearSelectedFile() {
        selectedFile = null;
        fileNameLabel.setText("No file selected");
        fileNameLabel.setForeground(Theme.MUTED);
        fileTypeLabel.setText("Optional image or video");
        fileTypeLabel.setForeground(Theme.MUTED);
        previewLabel.setText("FILE");
        previewLabel.setIcon(null);
        previewImage = null;
        previewIsVideo = false;
        removeFileButton.setVisible(false);
        refreshPreview();
        revalidate();
        repaint();
    }

    // ---------------- publishing ----------------

    private void publishPost() {

        final String title = titleField.getText().trim();
        final String description = descriptionArea.getText().trim();
        if (academicChoice == null) {
            Dialogs.error(this, "Choose a category", "Pick Academic or Entertainment before publishing.");
            return;
        }
        if (reelChoice == null) {
            Dialogs.error(this, "Choose a format", "Pick Regular Post or Reel before publishing.");
            return;
        }
        final String category = academicChoice ? "academic" : "entertainment";
        final boolean isReel = reelChoice;
        if (isReel && (selectedFile == null || !isVideo(getExtension(selectedFile.getName())))) {
            Dialogs.error(this, "Reel needs a video", "A Reel must have a video attachment.");
            return;
        }
        final File source = selectedFile;

        // one last check: the author confirms the choices, since a wrong category puts the post in the wrong feed
        String shownTitle = titleField.getField().getText().trim();
        String where = (academicChoice ? "Academic" : "Entertainment") + (isReel ? " Reels" : " feed");
        if (!Dialogs.confirm(this, "Is this correct?",
                "Title:  " + (shownTitle.isEmpty() ? "(none)" : shownTitle) + "\n"
                        + "Category:  " + (academicChoice ? "Academic" : "Entertainment") + "\n"
                        + "Format:  " + (isReel ? "Reel" : "Regular Post") + "\n"
                        + "Media:  " + (source == null ? "none" : source.getName()) + "\n\n"
                        + "It will be published in the " + where + ".\n"
                        + "You can still move it later from the post.",
                "Yes, publish", false)) {
            return;
        }

        if (currentUserId <= 0) {
            Dialogs.error(this, "Not logged in", "Invalid logged-in user.");
            return;
        }

        if (!com.eduvibe.util.ContentFilter.isClean(title)
                || !com.eduvibe.util.ContentFilter.isClean(description)) {
            Dialogs.warning(this, "Language not allowed",
                    com.eduvibe.util.ContentFilter.MESSAGE);
            return;
        }

        if (description.isEmpty()) {
            Dialogs.warning(this, "Description needed", "Please enter a post description.");
            descriptionArea.requestFocusInWindow();
            return;
        }

        String mediaType = "none";

        if (source != null) {
            String ext = getExtension(source.getName());
            mediaType = isImage(ext) ? "image" : isVideo(ext) ? "video" : null;

            if (mediaType == null) {
                Dialogs.error(this, "Unsupported file", "Unsupported media file.");
                return;
            }
            if (isReel && !"video".equals(mediaType)) {
                Dialogs.error(this, "Reel needs a video", "A Reel must have a video attachment.");
                return;
            }
        } else if (isReel) {
            Dialogs.warning(this, "Reel needs a video", "Please attach a video for your Reel.");
            return;
        }

        final String finalMediaType = mediaType;

        publishButton.setEnabled(false);
        cancelButton.setEnabled(false);
        publishButton.setText("Posting...");

        new SwingWorker<Boolean, Void>() {

            private Path savedMedia;

            @Override
            protected Boolean doInBackground() throws Exception {

                String mediaPath = null;

                if (source != null) {
                    savedMedia = saveMediaFile(source);
                    mediaPath = "uploads/" + savedMedia.getFileName();
                }

                try {
                    return postService.createPost(currentUserId, category, title,
                            description, finalMediaType, mediaPath, isReel);
                } catch (Exception ex) {
                    deleteSavedMedia(savedMedia);
                    throw ex;
                }
            }

            @Override
            protected void done() {

                publishButton.setEnabled(true);
                cancelButton.setEnabled(true);
                publishButton.setText("Post");

                try {
                    if (get()) {
                        Dialogs.success(CreatePostPanel.this, "Post published",
                                "Your post is live. You earned +15 points!");
                        clearForm();
                        if (onPublished != null) {
                            onPublished.run();
                        }
                    } else {
                        deleteSavedMedia(savedMedia);
                        Dialogs.error(CreatePostPanel.this, "Not saved",
                                "The post could not be saved.");
                    }

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    Dialogs.error(CreatePostPanel.this, "Unable to publish post",
                            String.valueOf(cause.getMessage()));
                }
            }
        }.execute();
    }

    private Path saveMediaFile(File source) throws IOException {

        Path uploadDirectory = Paths.get(System.getProperty("user.dir"), "uploads");
        Files.createDirectories(uploadDirectory);

        String newFileName = UUID.randomUUID().toString().replace("-", "")
                + "." + getExtension(source.getName());

        Path destination = uploadDirectory.resolve(newFileName);
        Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private void deleteSavedMedia(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // the original publishing error is the one worth reporting
        }
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private boolean isImage(String extension) {
        return extension.matches("jpg|jpeg|png|gif|webp");
    }

    private boolean isVideo(String extension) {
        return extension.matches("mp4|mov|avi|mkv|webm");
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.setText("");
        academicChoice = null;
        reelChoice = null;
        academicTile.setSelected(false);
        entertainmentTile.setSelected(false);
        postFormatTile.setSelected(false);
        reelFormatTile.setSelected(false);
        reelCheckBox.setSelected(false);
        refreshPreview();
        clearSelectedFile();
    }
}
