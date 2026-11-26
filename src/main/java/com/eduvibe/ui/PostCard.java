
package com.eduvibe.ui;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.AdminNotificationDAO;
import com.eduvibe.dao.CommentDAO;
import com.eduvibe.dao.SavedPostDAO;
import com.eduvibe.dao.ReportDAO;
import com.eduvibe.model.Post;
import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.Avatar;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.EmojiPicker;
import com.eduvibe.ui.components.ImageViewerDialog;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.Toast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class PostCard extends JPanel {

    private final Post post;
    private final int currentUserId;

    private final SavedPostDAO savedPostDAO = new SavedPostDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final PostService postService = new PostService();

    private RoundedButton likeButton;
    private RoundedButton commentButton;
    private RoundedButton bookmarkButton;
    private final CommentDAO commentDAO = new CommentDAO();
    private JLabel likeCountLabel;

    private int currentLikeCount;
    private int currentCommentCount;

    private boolean likedByCurrentUser;
    private boolean savedByCurrentUser;

    private boolean likeRequestRunning;
    private boolean commentRequestRunning;
    private boolean bookmarkRequestRunning;
    private boolean reportRequestRunning;

    private static final Color CARD = Theme.CARD;
    private static final Color PANEL = Theme.INPUT;
    private static final Color HOVER = Theme.CARD_LIGHT;
    private static final Color TEXT = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private static final Color BLUE = Theme.BLUE;
    private static final Color BLUE_LIGHT = Theme.BLUE_LIGHT;
    private static final Color RED = Theme.RED;
    private static final Color BORDER = Theme.BORDER;

    public PostCard(Post post) {
        this(post, 0);
    }

    public PostCard(Post post, int currentUserId) {
        this(post, currentUserId, false);
    }

    /**
     * @param controllerOnly true for the Reels page, which never shows this card: it only borrows the
     *                       like / save / comment / share / report logic, so no video player is created
     */
    PostCard(Post post, int currentUserId, boolean controllerOnly) {
        this.controllerOnly = controllerOnly;
        if (post == null) {
            throw new IllegalArgumentException("Post cannot be null.");
        }

        this.post = post;
        this.currentUserId = currentUserId;

        currentLikeCount = post.getLikeCount();
        currentCommentCount = post.getCommentCount();
        likedByCurrentUser = post.isLikedByCurrentUser();

        if (currentUserId > 0 && controllerOnly) {
            // Reels: a card is made for each reel as you scroll; the database is asked in the background
            // so scrolling never waits for it.
            final int pid = post.getId();
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return savedPostDAO.isPostSaved(currentUserId, pid);
                }

                @Override
                protected void done() {
                    try {
                        savedByCurrentUser = get();
                        notifyState();
                    } catch (Exception ignored) {
                        // stays "not saved"
                    }
                }
            }.execute();
        } else if (currentUserId > 0) {
            try {
                savedByCurrentUser = savedPostDAO.isPostSaved(
                        currentUserId, post.getId()
                );
            } catch (SQLException ex) {
                savedByCurrentUser = false;
                ex.printStackTrace();
            }
        }

        initializeCard();
        buildCard();
    }

    /** Cards are never taller than their content, so a short list does not stretch them. */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(780, getPreferredSize().height);
    }

    /** Tallest picture/video allowed: about half the screen, so posts fit small laptop screens. */
    public static int maxMediaHeight() {
        int screen = Toolkit.getDefaultToolkit().getScreenSize().height;
        return Math.max(260, Math.min(460, (int) (screen * 0.5)));
    }

    private void initializeCard() {
        setLayout(new BorderLayout(0, 17));
        setBackground(CARD);
        setOpaque(false);                                   // the card paints its own rounded body

        setBorder(new EmptyBorder(19, 24, 15, 20));

        setAlignmentX(Component.CENTER_ALIGNMENT);
        installHoverGlow();
    }

    private Color borderTint = BORDER;
    private float hoverAmount;

    private Color accentColor() {
        String c = post.getCategory() == null ? "" : post.getCategory().trim();
        return "entertainment".equalsIgnoreCase(c) ? Theme.PURPLE : Theme.BLUE_LIGHT;
    }

    /** A rounded body, a coloured edge for the category, and a soft glow when the mouse is over the card. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth(), h = getHeight();
        int arc = 20;
        java.awt.geom.RoundRectangle2D.Float body = new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, arc, arc);

        if (hoverAmount > 0) {                              // faint glow
            g2.setColor(new Color(59, 130, 246, (int) (hoverAmount * 26)));
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(-3, -3, w + 6, h + 6, arc + 6, arc + 6));
        }
        g2.setColor(CARD);
        g2.fill(body);

        Color accent = accentColor();                       // the category stripe on the left edge
        g2.setClip(body);
        g2.setPaint(new GradientPaint(0, 0, Theme.alpha(accent, 230), 0, h, Theme.alpha(accent, 60)));
        g2.fillRect(0, 0, 4, h);
        g2.setClip(null);

        g2.setColor(borderTint);
        g2.draw(body);
        g2.dispose();
    }

    /** Border fades from grey to soft blue while the mouse is over the card. */
    private void installHoverGlow() {

        final Color from = BORDER;
        final Color to = new Color(59, 130, 246, 170);
        final float[] t = {0f};
        final boolean[] inside = {false};

        final javax.swing.Timer fade = new javax.swing.Timer(16, null);
        fade.addActionListener(e -> {
            t[0] += inside[0] ? 0.12f : -0.12f;
            t[0] = Math.max(0f, Math.min(1f, t[0]));

            Color c = new Color(
                    (int) (from.getRed() + (to.getRed() - from.getRed()) * t[0]),
                    (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t[0]),
                    (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t[0]));

            borderTint = c;
            hoverAmount = t[0];
            repaint();

            if (t[0] == 0f || t[0] == 1f) {
                fade.stop();
            }
        });

        java.awt.event.MouseAdapter hover = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                inside[0] = true;
                fade.start();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // Ignore exits caused by moving onto a child component.
                Point p = SwingUtilities.convertPoint(
                        (Component) e.getSource(), e.getPoint(), PostCard.this);
                if (!new Rectangle(0, 0, getWidth(), getHeight()).contains(p)) {
                    inside[0] = false;
                    fade.start();
                }
            }
        };
        addMouseListener(hover);
    }

    /** True once the viewer chose "View Post" on a content warning. */
    private boolean warningAccepted = false;

    /** True when the automatic language check flags this post's title or description. */
    private boolean flaggedByFilter() {
        return !com.eduvibe.util.ContentFilter.isClean(post.getTitle())
                || !com.eduvibe.util.ContentFilter.isClean(post.getDescription());
    }

    private boolean isFlagged() {
        return post.getPendingReportCount() > 0 || flaggedByFilter();
    }

    private boolean needsContentWarning() {
        return !warningAccepted
                && isFlagged()
                && post.getUserId() != currentUserId;
    }

    /** Banner shown to the post's own author: explains the flag and offers Edit / Delete. */
    private JPanel buildOwnerFlagBanner() {

        JPanel box = new JPanel(new BorderLayout(12, 0));
        box.setBackground(Theme.isLight() ? new Color(254, 249, 195) : new Color(52, 44, 24));
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.YELLOW, 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        String reason = flaggedByFilter()
                ? "This post may contain inappropriate language."
                : "This post was reported and is under review.";

        JLabel text = new JLabel("<html><b>Content warning.</b> " + reason
                + " Others will see a warning before viewing it. "
                + "Edit the post to remove the flagged content, or delete it.</html>");
        text.setForeground(TEXT);
        text.setFont(Theme.regular(12));

        RoundedButton review = new RoundedButton("Review", RoundedButton.Style.PRIMARY);
        review.addActionListener(e -> {
            int choice = com.eduvibe.ui.components.Dialogs.contentWarning(this,
                    flaggedByFilter()
                            ? "This post has been flagged for potentially inappropriate content."
                            : "This post was reported and is under review.");
            if (choice == 1) {
                editPost();
            } else if (choice == 2) {
                deletePost();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(review);

        box.add(text, BorderLayout.CENTER);
        box.add(buttons, BorderLayout.EAST);
        return box;
    }

    private JPanel buildContentWarning() {

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Theme.isLight() ? new Color(254, 242, 242) : new Color(42, 30, 40));
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.isLight() ? new Color(252, 165, 165) : new Color(127, 29, 29), 1, true),
                new EmptyBorder(26, 24, 24, 24)));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(Icons.icon(Icons.Type.ALERT, 44, Theme.YELLOW));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Content Warning");
        heading.setForeground(TEXT);
        heading.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 18));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel message = new JLabel("<html><div style='text-align:center;width:360px'>"
                + (flaggedByFilter()
                    ? "This post may contain inappropriate language or content."
                    : "This post has been reported by community members and is "
                      + "under review. It may contain sensitive content.")
                + "</div></html>");
        message.setForeground(MUTED);
        message.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedButton view = new RoundedButton("View Post", RoundedButton.Style.OUTLINE);
        view.addActionListener(e -> {
            warningAccepted = true;
            removeAll();
            buildCard();
            revalidate();
            repaint();
        });

        RoundedButton skip = new RoundedButton("Skip", RoundedButton.Style.PRIMARY);
        skip.addActionListener(e -> {
            Container parent = getParent();
            setVisible(false);
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(view);
        buttons.add(skip);

        box.add(icon);
        box.add(Box.createVerticalStrut(10));
        box.add(heading);
        box.add(Box.createVerticalStrut(8));
        box.add(message);
        box.add(Box.createVerticalStrut(18));
        box.add(buttons);
        return box;
    }

    private void buildCard() {
        add(buildHeader(), BorderLayout.NORTH);

        if (needsContentWarning()) {
            JPanel warn = new JPanel(new BorderLayout());
            warn.setBackground(CARD);
            warn.setBorder(new EmptyBorder(4, 0, 2, 0));
            warn.add(buildContentWarning(), BorderLayout.CENTER);
            add(warn, BorderLayout.CENTER);
            return;
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(CARD);
        content.setBorder(new EmptyBorder(4, 0, 2, 0));

        if (post.getUserId() == currentUserId && currentUserId > 0 && isFlagged()) {
            content.add(buildOwnerFlagBanner());
            content.add(Box.createVerticalStrut(10));
        }

        String titleText = post.getTitle();

        if (titleText != null && !titleText.isBlank()) {
            JLabel title = new JLabel(escapeHtml(titleText));
            title.setForeground(TEXT);
            title.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 18));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            content.add(title);
            content.add(Box.createVerticalStrut(10));
        }

        String descriptionText = post.getDescription();

        if (descriptionText != null && !descriptionText.isBlank()) {
            JTextArea description = new JTextArea(descriptionText);
            description.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
            description.setForeground(Theme.isLight() ? new Color(51, 65, 85) : new Color(219, 228, 240));
            description.setBackground(CARD);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setEditable(false);
            description.setFocusable(false);
            description.setBorder(null);
            description.setAlignmentX(Component.LEFT_ALIGNMENT);

            content.add(description);
            content.add(Box.createVerticalStrut(15));
        }

        JPanel media = buildMediaPreview();

        if (media.isVisible()) {
            content.add(media);
            content.add(Box.createVerticalStrut(15));
        }

        add(content, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(CARD);

        String username = post.getUsername();

        if (username == null || username.isBlank()) {
            username = "Unknown user";
        }

        JPanel userSection = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0)
        );
        userSection.setBackground(CARD);

        Avatar avatar = new Avatar(45);
        avatar.setUser(username, post.getAuthorProfilePic());
        avatar.showFrameOf(post.getUserId());

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBackground(CARD);
        details.setBorder(new EmptyBorder(2, 12, 0, 0));

        JLabel author = new JLabel(username);
        author.setAlignmentX(Component.LEFT_ALIGNMENT);
        author.setForeground(TEXT);
        author.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 13));
        com.eduvibe.ui.components.RewardArt.attachBadge(author, post.getUserId(), 18);

        String cat = post.getCategory() == null ? "post" : post.getCategory().replace("_", " ");
        String catName = cat.isEmpty() ? cat : Character.toUpperCase(cat.charAt(0)) + cat.substring(1).toLowerCase();
        Color catColor = "entertainment".equalsIgnoreCase(cat) ? Theme.PURPLE : Theme.BLUE_LIGHT;
        String catHex = String.format("#%02x%02x%02x", catColor.getRed(), catColor.getGreen(), catColor.getBlue());

        JPanel subtitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subtitle.setOpaque(false);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel when = new JLabel(relativeTime(post.getCreatedAt()));
        when.setForeground(MUTED);
        when.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        subtitle.add(when);
        subtitle.add(Box.createHorizontalStrut(8));
        subtitle.add(new CategoryChip(catName.toUpperCase() + (post.isReel() ? "  ·  REEL" : ""), catColor));
        subtitle.setToolTipText(post.getCreatedAt() == null ? null
                : post.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")));

        details.add(author);
        details.add(Box.createVerticalStrut(5));
        details.add(subtitle);

        userSection.add(avatar);
        userSection.add(details);

        header.add(userSection, BorderLayout.WEST);

        return header;
    }

    /** A small rounded label for the category (and Reel). */
    private static final class CategoryChip extends JComponent {
        private final String text;
        private final Color color;

        CategoryChip(String text, Color color) {
            this.text = text;
            this.color = color;
            setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 10));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 18, 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Theme.alpha(color, 38));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(Theme.alpha(color, 110));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(color);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 9, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** "Just now", "2m ago", "3h ago", "2d ago" or a date. */
    private static String relativeTime(java.time.LocalDateTime t) {
        if (t == null) {
            return "EduVibe Community";
        }
        long secs = Math.max(0, java.time.Duration.between(t, java.time.LocalDateTime.now()).getSeconds());
        if (secs < 60) {
            return "Just now";
        }
        if (secs < 3600) {
            return (secs / 60) + "m ago";
        }
        if (secs < 86400) {
            return (secs / 3600) + "h ago";
        }
        if (secs < 7 * 86400) {
            return (secs / 86400) + "d ago";
        }
        return t.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    private String getInitials(String username) {
        if (username == null || username.isBlank()) {
            return "EV";
        }

        String clean = username.trim();

        return clean.substring(0, Math.min(2, clean.length()))
                .toUpperCase();
    }

    private JPanel buildMediaPreview() {
        JPanel media = new com.eduvibe.ui.components.RoundedPanel(14, Theme.BACKGROUND, BORDER);
        media.setLayout(new BorderLayout());
        media.setBorder(new EmptyBorder(1, 1, 1, 1));
        media.setAlignmentX(Component.LEFT_ALIGNMENT);
        media.setMaximumSize(new Dimension(Integer.MAX_VALUE, 640));
        if (controllerOnly) {
            return media;
        }

        String type = post.getMediaType();
        String path = post.getMediaPath();

        if (type == null || type.equalsIgnoreCase("none")
                || path == null || path.isBlank()) {
            media.setVisible(false);
            return media;
        }

        File file = new File(path);

        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }

        if (!file.exists() || !file.isFile()) {
            JLabel missing = new JLabel(
                    "Media file is unavailable", SwingConstants.CENTER
            );
            missing.setForeground(MUTED);
            missing.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 13));
            missing.setPreferredSize(new Dimension(600, 200));
            media.add(missing, BorderLayout.CENTER);
            return media;
        }

        if (type.equalsIgnoreCase("image")) {
            buildImagePreview(media, file);
        } else if (type.equalsIgnoreCase("video")) {
            buildVideoPreview(media, file);
        }

        return media;
    }

    private void buildImagePreview(JPanel media, File file) {
        ImageIcon original = new ImageIcon(file.getAbsolutePath());

        final int width = original.getIconWidth();
        final int height = original.getIconHeight();

        if (width <= 0 || height <= 0) {
            JLabel error = new JLabel(
                    "Unable to display image", SwingConstants.CENTER
            );
            error.setForeground(MUTED);
            error.setPreferredSize(new Dimension(600, 160));
            media.add(error, BorderLayout.CENTER);
            return;
        }

        final Image source = original.getImage();
        final File imageFile = file;

        // Fits the whole picture inside a frame that is 16:9 for wide photos and taller for
        // portrait ones, centred on a dark background, so every post lines up the same way.
        JComponent image = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                double ratio = (double) height / width;
                int h = (int) Math.max(220, Math.min(maxMediaHeight(), 700 * ratio));
                return new Dimension(300, h);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                double scale = Math.min((double) getWidth() / width, (double) getHeight() / height);
                int w = Math.max(1, (int) (width * scale));
                int h = Math.max(1, (int) (height * scale));
                g2.drawImage(source, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
                g2.dispose();
            }
        };
        image.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        image.setToolTipText("Click to view full size");
        image.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ImageViewerDialog.show(SwingUtilities.getWindowAncestor(PostCard.this), imageFile);
            }
        });

        media.add(image, BorderLayout.CENTER);
    }

    private void buildVideoPreview(JPanel media, File file) {

        // Preferred: plays right here inside the post (no floating window).
        try {
            Object inline = Class.forName("com.eduvibe.ui.components.InlineVideoPanel")
                    .getMethod("create", File.class, boolean.class)
                    .invoke(null, file, post.isReel());
            media.add((JComponent) inline, BorderLayout.CENTER);
            return;
        } catch (Throwable ignored) {
            // JavaFX not available: use the old "Play Video" card below.
        }

        JPanel video = new JPanel(new GridBagLayout());
        video.setBackground(PANEL);
        video.setPreferredSize(new Dimension(650, 260));

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBackground(PANEL);

        JLabel playIcon = new JLabel(Icons.icon(Icons.Type.PLAY, 56, BLUE_LIGHT));
        playIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("VIDEO POST");
        heading.setForeground(TEXT);
        heading.setFont(com.eduvibe.ui.Theme.font(Font.BOLD, 15));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel fileName = new JLabel(file.getName());
        fileName.setForeground(MUTED);
        fileName.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));
        fileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedButton playButton = new RoundedButton("Play Video").withIcon(Icons.Type.PLAY);
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.addActionListener(e -> openVideo(file));

        details.add(playIcon);
        details.add(Box.createVerticalStrut(8));
        details.add(heading);
        details.add(Box.createVerticalStrut(6));
        details.add(fileName);
        details.add(Box.createVerticalStrut(17));
        details.add(playButton);

        video.add(details);
        media.add(video, BorderLayout.CENTER);
    }

    private void openVideo(File file) {

        // Preferred: in-app JavaFX player (class is optional, see pom.xml).
        try {
            Class<?> player = Class.forName(
                    "com.eduvibe.ui.components.VideoPlayerDialog");
            player.getMethod("show", Window.class, File.class)
                    .invoke(null, SwingUtilities.getWindowAncestor(this), file);
            return;
        } catch (Throwable ignored) {
            // JavaFX not available: fall back to the system video player.
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                showMessage(
                        "Opening videos is not supported on this computer."
                );
                return;
            }

            Desktop.getDesktop().open(file);

        } catch (Exception ex) {
            showMessage("Unable to open video: " + ex.getMessage());
        }
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(6, 0, 0, 0)));

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 2, 0)
        );
        actions.setBackground(CARD);

        likeButton = new RoundedButton("", RoundedButton.Style.GHOST);
        styleActionButton(likeButton);
        likeButton.addActionListener(e -> toggleLike());

        likeCountLabel = new JLabel(currentLikeCount + " likes");
        likeCountLabel.setForeground(MUTED);
        likeCountLabel.setFont(com.eduvibe.ui.Theme.font(Font.PLAIN, 11));

        commentButton = new RoundedButton("", RoundedButton.Style.GHOST);
        styleActionButton(commentButton);
        commentButton.addActionListener(e -> openComments());

        bookmarkButton = new RoundedButton("", RoundedButton.Style.GHOST);
        styleActionButton(bookmarkButton);
        bookmarkButton.addActionListener(e -> toggleBookmark());

        updateLikeButton();
        updateCommentButton();
        updateBookmarkButton();

        actions.add(likeButton);
        actions.add(likeCountLabel);
        actions.add(commentButton);
        actions.add(bookmarkButton);

        RoundedButton shareButton = new RoundedButton("Share", RoundedButton.Style.GHOST).withIcon(Icons.Type.SHARE);
        styleActionButton(shareButton);
        shareButton.addActionListener(e -> sharePost());
        actions.add(shareButton);

        if (hasDownloadableMedia()) {
            downloadButton = new RoundedButton("Download", RoundedButton.Style.GHOST)
                    .withIcon(Icons.Type.DOWNLOAD);
            styleActionButton(downloadButton);
            downloadButton.addActionListener(e -> actionDownload());
            actions.add(downloadButton);

            if (currentUserId > 0 && post.getUserId() == currentUserId) {
                downloadToggle = new RoundedButton("Downloads: On", RoundedButton.Style.GHOST);
                styleActionButton(downloadToggle);
                downloadToggle.addActionListener(e -> actionToggleDownloads());
                downloadToggle.setVisible(false);          // shown once the database answered
                actions.add(downloadToggle);
            }
            loadDownloadState(() -> { });
        }

        if (currentUserId > 0 && post.getUserId() != currentUserId) {
            RoundedButton reportButton = new RoundedButton("Report", RoundedButton.Style.GHOST).withIcon(Icons.Type.FLAG);
            styleActionButton(reportButton);
            reportButton.setAccent(RED);
            reportButton.addActionListener(e -> reportPost());
            actions.add(reportButton);
        }

        if (currentUserId > 0 && post.getUserId() == currentUserId) {
            RoundedButton editButton = new RoundedButton("Edit", RoundedButton.Style.GHOST);
            styleActionButton(editButton);
            editButton.addActionListener(e -> editPost());

            RoundedButton deleteButton = new RoundedButton("Delete", RoundedButton.Style.GHOST);
            styleActionButton(deleteButton);
            deleteButton.setAccent(RED);
            deleteButton.addActionListener(e -> deletePost());

            actions.add(editButton);
            actions.add(buildMoveCategoryButton());
            actions.add(deleteButton);
        }

        footer.add(actions, BorderLayout.WEST);
        return footer;
    }

    private void styleActionButton(JButton button) {
        button.setFont(Theme.bold(12));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 10, 8, 10));
        if (button instanceof RoundedButton rb) {
            rb.compact(9);
        }
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // SHARE

    private void sharePost() {
        StringBuilder shareText = new StringBuilder();
        shareText.append("Check out this post on EduVibe!\n\n");

        if (post.getUsername() != null) {
            shareText.append("Posted by: @")
                    .append(post.getUsername()).append("\n");
        }

        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            shareText.append("\n").append(post.getTitle()).append("\n");
        }

        if (post.getDescription() != null
                && !post.getDescription().isBlank()) {
            shareText.append("\n").append(post.getDescription()).append("\n");
        }

        shareText.append("\nPost ID: ").append(post.getId());
        shareText.append("\nShared from EduVibe.");

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(shareText.toString()), null);

            showMessage(
                    "Post details copied! You can now paste and share them."
            );

        } catch (Exception ex) {
            showError("Unable to copy post details.", ex);
        }
    }

    // REPORT

    private void reportPost() {
        if (currentUserId <= 0) {
            showMessage("Please log in to report posts.");
            return;
        }

        if (post.getUserId() == currentUserId) {
            showMessage("You cannot report your own post.");
            return;
        }

        if (reportRequestRunning) {
            return;
        }

        String[] reasons = {
            "Spam or misleading content",
            "Harassment or bullying",
            "Inappropriate content",
            "Hate speech",
            "False information",
            "Other"
        };

        String selectedReason = (String) JOptionPane.showInputDialog(
                anchorOrThis(),
                "Why are you reporting this post?",
                "Report Post",
                JOptionPane.QUESTION_MESSAGE,
                null,
                reasons,
                reasons[0]
        );

        if (selectedReason == null) {
            return;
        }

        if ("Other".equals(selectedReason)) {
            selectedReason = JOptionPane.showInputDialog(
                    anchorOrThis(),
                    "Please explain your reason:",
                    "Other Report Reason",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (selectedReason == null || selectedReason.isBlank()) {
                return;
            }

            selectedReason = selectedReason.trim();
        }

        if (selectedReason.length() > 255) {
            showMessage("Your report reason must not exceed 255 characters.");
            return;
        }

        final String reason = selectedReason;

        if (!Dialogs.confirm(anchorOrThis(), "Report this post?",
                "Reason: " + reason + "\n\nPosts that receive 3 pending reports are "
                + "hidden automatically until an administrator reviews them.",
                "Submit Report", true)) {
            return;
        }

        reportRequestRunning = true;

        new SwingWorker<ReportDAO.Result, Void>() {
            @Override
            protected ReportDAO.Result doInBackground() throws Exception {
                return reportDAO.submitReport(
                        post.getId(), currentUserId, reason
                );
            }

            @Override
            protected void done() {
                try {
                    ReportDAO.Result result = get();

                    switch (result) {
                        case SUBMITTED -> showMessage(
                                "Your report has been submitted. "
                                + "An administrator will review it.");
                        case DUPLICATE -> showMessage(
                                "You already have a pending report for this post.");
                        case POST_NOT_FOUND -> showMessage(
                                "This post no longer exists.");
                    }

                } catch (Exception ex) {
                    showError("Unable to submit report.", ex);

                } finally {
                    reportRequestRunning = false;
                }
            }
        }.execute();
    }

    private void updateLikeButton() {
        if (likeButton == null) {
            return;
        }

        likeButton.withIcon(likedByCurrentUser
                ? Icons.Type.HEART_FILLED : Icons.Type.HEART);
        likeButton.setText("Like");
        likeButton.setAccent(likedByCurrentUser ? RED : null);
    }

    private void updateCommentButton() {
        if (commentButton != null) {
            commentButton.withIcon(Icons.Type.COMMENT);
            commentButton.setText("Comment (" + currentCommentCount + ")");
        }
    }

    // BOOKMARK

    private void updateBookmarkButton() {
        if (bookmarkButton == null) {
            return;
        }

        bookmarkButton.withIcon(Icons.Type.BOOKMARK);
        bookmarkButton.setText(savedByCurrentUser ? "Saved" : "Save");
        bookmarkButton.setAccent(savedByCurrentUser ? BLUE_LIGHT : null);
    }

    private void toggleBookmark() {
        if (currentUserId <= 0) {
            showMessage("Please log in to save posts.");
            return;
        }

        if (bookmarkRequestRunning) {
            return;
        }

        bookmarkRequestRunning = true;
        bookmarkButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (savedByCurrentUser) {
                    savedPostDAO.removeSavedPost(
                            currentUserId, post.getId()
                    );
                    return false;
                }

                // Save the post first. Only a successful new save triggers
                // an administrator notification; removing a saved post does not.
                savedPostDAO.savePost(currentUserId, post.getId());

                try {
                    String username = "Unknown user";
                    String usernameSQL =
                            "SELECT username FROM users WHERE id=?";

                    try (Connection connection = DatabaseConnection.getConnection();
                         PreparedStatement statement =
                                 connection.prepareStatement(usernameSQL)) {
                        statement.setInt(1, currentUserId);

                        try (ResultSet rs = statement.executeQuery()) {
                            if (rs.next()) {
                                username = rs.getString("username");
                            }
                        }
                    }

                    AdminNotificationDAO.notifyAdmins(
                            "@" + username
                            + " saved a post. Post ID: " + post.getId()
                    );
                } catch (SQLException notificationError) {
                    // The bookmark has already been saved. A notification
                    // failure must not make the save appear unsuccessful.
                    notificationError.printStackTrace();
                }

                return true;
            }

            @Override
            protected void done() {
                try {
                    savedByCurrentUser = get();
                    updateBookmarkButton();
                    notifyState();

                    showMessage(
                            savedByCurrentUser
                                    ? "Post saved successfully!"
                                    : "Post removed from saved posts."
                    );

                } catch (Exception ex) {
                    showError("Unable to update bookmark.", ex);

                } finally {
                    bookmarkRequestRunning = false;
                    bookmarkButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // LIKE / UNLIKE + ADMIN NOTIFICATION

    private void toggleLike() {
        if (currentUserId <= 0) {
            showMessage("Please log in to like posts.");
            return;
        }

        if (likeRequestRunning) {
            return;
        }

        likeRequestRunning = true;
        likeButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return toggleLikeInDatabase();
            }

            @Override
            protected void done() {
                try {
                    likedByCurrentUser = get();
                    currentLikeCount = getLikeCountFromDatabase();

                    likeCountLabel.setText(currentLikeCount + " likes");
                    updateLikeButton();
                    if (likedByCurrentUser) {
                        likeButton.pulseIcon();                    // a little heart "pop"
                    }
                    notifyState();

                } catch (Exception ex) {
                    showError("Unable to update like.", ex);

                } finally {
                    likeRequestRunning = false;
                    likeButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private boolean toggleLikeInDatabase() throws SQLException {
        String checkSQL =
                "SELECT id FROM post_likes WHERE post_id=? AND user_id=?";
        String insertSQL =
                "INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)";
        String deleteSQL =
                "DELETE FROM post_likes WHERE post_id=? AND user_id=?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                boolean alreadyLiked;

                try (PreparedStatement check =
                             connection.prepareStatement(checkSQL)) {
                    check.setInt(1, post.getId());
                    check.setInt(2, currentUserId);

                    try (ResultSet rs = check.executeQuery()) {
                        alreadyLiked = rs.next();
                    }
                }

                if (alreadyLiked) {
                    // Remove the like. No admin notification for unlikes.
                    try (PreparedStatement delete =
                                 connection.prepareStatement(deleteSQL)) {
                        delete.setInt(1, post.getId());
                        delete.setInt(2, currentUserId);
                        delete.executeUpdate();
                    }

                } else {
                    // Insert the new like.
                    try (PreparedStatement insert =
                                 connection.prepareStatement(insertSQL)) {
                        insert.setInt(1, post.getId());
                        insert.setInt(2, currentUserId);
                        insert.executeUpdate();
                    }

                    // Award 5 points to the post owner.
                    awardPoints(
                            connection, post.getUserId(), 5,
                            "like",
                            "like:" + post.getId() + ":" + currentUserId
                    );

                    // Notify the post owner.
                    createNotification(
                            connection, post.getUserId(),
                            "like", "Someone liked your post."
                    );

                    // Get the username of the person who liked the post.
                    String username = "Unknown user";

                    String usernameSQL =
                            "SELECT username FROM users WHERE id=?";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(usernameSQL)) {
                        statement.setInt(1, currentUserId);

                        try (ResultSet rs = statement.executeQuery()) {
                            if (rs.next()) {
                                username = rs.getString("username");
                            }
                        }
                    }

                    // Notify all active administrators.
                    AdminNotificationDAO.notifyAdmins(
                            connection,
                            "@" + username
                            + " liked a post. Post ID: " + post.getId()
                    );
                }

                connection.commit();
                return !alreadyLiked;

            } catch (SQLException ex) {
                connection.rollback();
                throw ex;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int getLikeCountFromDatabase() throws SQLException {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE post_id=?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, post.getId());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // COMMENTS

    private void openComments() {
        if (currentUserId <= 0) {
            showMessage("Please log in to access comments.");
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(anchorOrThis());
        JDialog dialog = new JDialog(owner, "EduVibe | Comments",
                Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setSize(560, 600);
        dialog.setMinimumSize(new Dimension(430, 460));
        dialog.setLocationRelativeTo(anchorOrThis());
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Theme.BACKGROUND);
        dialog.add(buildCommentsPanel(null), BorderLayout.CENTER);

        dialog.setVisible(true);
        notifyState();
    }

    /**
     * The comments (list, replies, likes, new comment box) as a panel that can be docked inside a page.
     * The Reels page shows it beside the video. {@code onClose} adds a close button when not null.
     */
    JComponent createCommentsPanel(Runnable onClose) {
        if (currentUserId <= 0) {
            JLabel label = new JLabel("Please log in to access comments.", SwingConstants.CENTER);
            label.setForeground(Theme.MUTED);
            return label;
        }
        return buildCommentsPanel(onClose);
    }

    private JPanel buildCommentsPanel(Runnable onClose) {

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BACKGROUND);
        header.setBorder(new EmptyBorder(18, 20, 6, 20));

        JLabel heading = new JLabel("Comments");
        heading.setForeground(TEXT);
        heading.setFont(Theme.bold(21));
        header.add(heading, BorderLayout.WEST);

        if (onClose != null) {
            RoundedButton close = new RoundedButton("Close");
            close.addActionListener(e -> onClose.run());
            header.add(close, BorderLayout.EAST);
        }

        JPanel commentsContainer = new JPanel();
        commentsContainer.setLayout(
                new BoxLayout(commentsContainer, BoxLayout.Y_AXIS)
        );
        commentsContainer.setBackground(Theme.BACKGROUND);
        commentsContainer.setBorder(new EmptyBorder(8, 17, 8, 17));

        JScrollPane commentsScroll = new JScrollPane(commentsContainer);
        commentsScroll.setBorder(null);
        commentsScroll.getViewport().setBackground(Theme.BACKGROUND);
        commentsScroll.getVerticalScrollBar().setUnitIncrement(16);

        JTextArea commentInput = new JTextArea(3, 30);
        commentInput.setLineWrap(true);
        commentInput.setWrapStyleWord(true);
        commentInput.setFont(Theme.regular(13));
        commentInput.setBackground(Theme.INPUT);
        commentInput.setForeground(TEXT);
        commentInput.setCaretColor(TEXT);
        commentInput.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane inputScroll = new JScrollPane(commentInput);
        inputScroll.setBorder(new LineBorder(BORDER, 1, true));

        RoundedButton sendButton = new RoundedButton("Post Comment");

        JPanel sendRow = new JPanel(new BorderLayout(8, 0));
        sendRow.setOpaque(false);
        sendRow.add(EmojiPicker.button(commentInput), BorderLayout.WEST);
        sendRow.add(sendButton, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setBackground(Theme.BACKGROUND);
        bottom.setBorder(new EmptyBorder(6, 17, 17, 17));
        bottom.add(inputScroll, BorderLayout.CENTER);
        bottom.add(sendRow, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(commentsScroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        loadComments(commentsContainer);

        sendButton.addActionListener(e -> {
            String content = commentInput.getText().trim();

            if (content.isEmpty()) {
                showMessage("Please write a comment first.");
                return;
            }

            if (!com.eduvibe.util.ContentFilter.isClean(content)) {
                Toast.error(anchorOrThis(), com.eduvibe.util.ContentFilter.MESSAGE);
                return;
            }

            if (commentRequestRunning) {
                return;
            }

            commentRequestRunning = true;
            sendButton.setEnabled(false);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    saveComment(content);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        commentInput.setText("");

                        currentCommentCount = commentDAO.countByPost(post.getId());
                        updateCommentButton();
                        loadComments(commentsContainer);
                        notifyState();

                    } catch (Exception ex) {
                        showError("Unable to save comment.", ex);

                    } finally {
                        commentRequestRunning = false;
                        sendButton.setEnabled(true);
                    }
                }
            }.execute();
        });

        return root;
    }

    // SAVE COMMENT + OWNER AND ADMIN NOTIFICATIONS

    private void saveComment(String content) throws SQLException {
        String sql =
                "INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int commentId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sql, Statement.RETURN_GENERATED_KEYS
                             )) {
                    statement.setInt(1, post.getId());
                    statement.setInt(2, currentUserId);
                    statement.setString(3, content);
                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve comment ID."
                            );
                        }

                        commentId = keys.getInt(1);
                    }
                }

                awardPoints(
                        connection, post.getUserId(), 10,
                        "comment", "comment:" + commentId
                );

                createNotification(
                        connection, post.getUserId(),
                        "comment", "Someone commented on your post."
                );

                String username = "Unknown user";

                String usernameSQL =
                        "SELECT username FROM users WHERE id=?";

                try (PreparedStatement statement =
                             connection.prepareStatement(usernameSQL)) {
                    statement.setInt(1, currentUserId);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            username = rs.getString("username");
                        }
                    }
                }

                AdminNotificationDAO.notifyAdmins(
                        connection,
                        "@" + username
                        + " commented on a post. Post ID: " + post.getId()
                );

                connection.commit();

            } catch (SQLException ex) {
                connection.rollback();
                throw ex;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // POINTS

    private void awardPoints(
            Connection connection,
            int recipientUserId,
            int points,
            String actionType,
            String referenceKey
    ) throws SQLException {

        if (recipientUserId <= 0 || points <= 0
                || recipientUserId == currentUserId) {
            return;
        }

        String checkSQL =
                "SELECT id FROM point_transactions "
                + "WHERE user_id=? AND action_type=? AND ref_key=?";

        try (PreparedStatement check =
                     connection.prepareStatement(checkSQL)) {
            check.setInt(1, recipientUserId);
            check.setString(2, actionType);
            check.setString(3, referenceKey);

            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String updateSQL =
                "UPDATE users SET points=points+? WHERE id=?";

        try (PreparedStatement update =
                     connection.prepareStatement(updateSQL)) {
            update.setInt(1, points);
            update.setInt(2, recipientUserId);

            if (update.executeUpdate() == 0) {
                throw new SQLException("Points recipient was not found.");
            }
        }

        String insertSQL =
                "INSERT INTO point_transactions "
                + "(user_id, points, action_type, ref_key) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement insert =
                     connection.prepareStatement(insertSQL)) {
            insert.setInt(1, recipientUserId);
            insert.setInt(2, points);
            insert.setString(3, actionType);
            insert.setString(4, referenceKey);
            insert.executeUpdate();
        }
    }

    // NOTIFICATIONS

    private void createNotification(
            Connection connection,
            int recipientUserId,
            String type,
            String message
    ) throws SQLException {

        if (recipientUserId <= 0 || recipientUserId == currentUserId) {
            return;
        }

        String usernameSQL = "SELECT username FROM users WHERE id=?";
        String actorUsername = "Someone";

        try (PreparedStatement statement =
                     connection.prepareStatement(usernameSQL)) {
            statement.setInt(1, currentUserId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    actorUsername = rs.getString("username");
                }
            }
        }

        String notificationMessage;

        if ("like".equalsIgnoreCase(type)) {
            notificationMessage = "@" + actorUsername + " liked your post.";
        } else if ("comment".equalsIgnoreCase(type)) {
            notificationMessage =
                    "@" + actorUsername + " commented on your post.";
        } else {
            notificationMessage = message;
        }

        String sql =
                "INSERT INTO notifications "
                + "(user_id, type, message, is_read) VALUES (?, ?, ?, 0)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setInt(1, recipientUserId);
            statement.setString(2, type);
            statement.setString(3, notificationMessage);
            statement.executeUpdate();
        }
    }

    // LOAD COMMENTS (background thread, with edit / delete)

    private void loadComments(JPanel container) {

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                java.util.List<CommentDAO.CommentRow> found = commentDAO.findByPost(post.getId());
                java.util.List<Integer> ids = new java.util.ArrayList<>();
                for (CommentDAO.CommentRow r : found) {
                    ids.add(r.id);
                }
                com.eduvibe.dao.CommentExtrasDAO.Bundle extras;
                try {
                    extras = new com.eduvibe.dao.CommentExtrasDAO().load(ids, currentUserId);
                } catch (Exception ex) {
                    extras = new com.eduvibe.dao.CommentExtrasDAO.Bundle();
                }
                return new Object[] { found, commentDAO.isAdmin(currentUserId), extras };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                container.removeAll();
                try {
                    Object[] data = get();
                    java.util.List<CommentDAO.CommentRow> rows =
                            (java.util.List<CommentDAO.CommentRow>) data[0];
                    boolean admin = (Boolean) data[1];
                    com.eduvibe.dao.CommentExtrasDAO.Bundle extras =
                            (com.eduvibe.dao.CommentExtrasDAO.Bundle) data[2];

                    if (rows.isEmpty()) {
                        JLabel empty = new JLabel(
                                "No comments yet. Be the first to comment!");
                        empty.setForeground(MUTED);
                        empty.setBorder(new EmptyBorder(20, 5, 20, 5));
                        container.add(empty);
                    }

                    for (CommentDAO.CommentRow row : rows) {
                        container.add(buildCommentCard(row, admin, container, extras));
                        container.add(Box.createVerticalStrut(10));
                    }

                } catch (Exception ex) {
                    JLabel error = new JLabel("Unable to load comments.");
                    error.setForeground(RED);
                    container.add(error);
                    ex.printStackTrace();
                }

                container.revalidate();
                container.repaint();
            }
        }.execute();
    }

    private JPanel buildCommentCard(CommentDAO.CommentRow row,
                                    boolean viewerIsAdmin, JPanel container,
                                    com.eduvibe.dao.CommentExtrasDAO.Bundle extras) {

        boolean mine = row.userId == currentUserId;

        com.eduvibe.ui.components.RoundedPanel card =
                new com.eduvibe.ui.components.RoundedPanel(14, CARD, BORDER) {
                    @Override
                    public Dimension getMaximumSize() {
                        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                    }
                };
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel name = new JLabel(row.username == null ? "Unknown user" : row.username);
        name.setFont(Theme.bold(13));
        name.setForeground(BLUE_LIGHT);

        JLabel date = new JLabel(row.createdAt == null ? ""
                : row.createdAt.toLocalDateTime().format(
                        DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")));
        date.setForeground(MUTED);
        date.setFont(Theme.regular(10));

        top.add(name, BorderLayout.WEST);
        top.add(date, BorderLayout.EAST);

        JTextArea text = new JTextArea(row.content);
        text.setFont(Theme.regular(13));
        text.setForeground(TEXT);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setBorder(null);

        card.add(top, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        java.util.List<com.eduvibe.dao.CommentExtrasDAO.Reply> replies =
                extras.replies.getOrDefault(row.id, new java.util.ArrayList<>());

        if (extras.available) {
            final boolean[] liked = { extras.likedByMe.contains(row.id) };
            final int[] likes = { extras.likeCounts.getOrDefault(row.id, 0) };

            RoundedButton like = new RoundedButton("", RoundedButton.Style.GHOST);
            like.setFont(Theme.bold(11));
            Runnable paint = () -> {
                like.setText((liked[0] ? "Liked " : "Like ") + likes[0]);
                like.setAccent(liked[0] ? BLUE_LIGHT : MUTED);
            };
            paint.run();
            like.addActionListener(e -> {
                like.setEnabled(false);
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return new com.eduvibe.dao.CommentExtrasDAO().toggleLike(row.id, currentUserId);
                    }

                    @Override
                    protected void done() {
                        try {
                            boolean now = get();
                            likes[0] += now ? 1 : -1;
                            liked[0] = now;
                            paint.run();
                        } catch (Exception ex) {
                            Toast.error(PostCard.this, "Could not update the like.");
                        }
                        like.setEnabled(true);
                    }
                }.execute();
            });
            actions.add(like);

            RoundedButton reply = new RoundedButton(
                    replies.isEmpty() ? "Reply" : "Reply (" + replies.size() + ")",
                    RoundedButton.Style.GHOST);
            reply.setFont(Theme.bold(11));
            reply.setAccent(MUTED);
            actions.add(reply);

            JPanel replyBox = new JPanel();
            replyBox.setLayout(new BoxLayout(replyBox, BoxLayout.Y_AXIS));
            replyBox.setOpaque(false);
            replyBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            replyBox.setBorder(new EmptyBorder(6, 18, 0, 0));
            replyBox.setVisible(!replies.isEmpty());

            for (com.eduvibe.dao.CommentExtrasDAO.Reply r : replies) {
                JPanel line = new JPanel(new BorderLayout(8, 0));
                line.setOpaque(false);
                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                line.setBorder(new EmptyBorder(3, 0, 3, 0));

                JLabel who = new JLabel(r.username == null ? "Unknown" : r.username);
                who.setFont(Theme.bold(12));
                who.setForeground(BLUE_LIGHT);
                who.setVerticalAlignment(SwingConstants.TOP);

                JTextArea body = new JTextArea(r.content);
                body.setFont(Theme.regular(12));
                body.setForeground(TEXT);
                body.setOpaque(false);
                body.setLineWrap(true);
                body.setWrapStyleWord(true);
                body.setEditable(false);
                body.setFocusable(false);
                body.setBorder(null);

                line.add(who, BorderLayout.WEST);
                line.add(body, BorderLayout.CENTER);

                if (r.userId == currentUserId || viewerIsAdmin) {
                    RoundedButton del = new RoundedButton("Delete", RoundedButton.Style.GHOST);
                    del.setFont(Theme.bold(10));
                    del.setAccent(RED);
                    del.addActionListener(e -> new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            new com.eduvibe.dao.CommentExtrasDAO()
                                    .deleteReply(r.id, currentUserId, viewerIsAdmin);
                            return null;
                        }

                        @Override
                        protected void done() {
                            loadComments(container);
                        }
                    }.execute());
                    line.add(del, BorderLayout.EAST);
                }
                replyBox.add(line);
            }

            JPanel input = new JPanel(new BorderLayout(8, 0));
            input.setOpaque(false);
            input.setAlignmentX(Component.LEFT_ALIGNMENT);
            input.setBorder(new EmptyBorder(6, 0, 0, 0));
            input.setVisible(false);

            JTextField field = new JTextField();
            field.setFont(Theme.regular(12));
            field.setBackground(Theme.INPUT);
            field.setForeground(TEXT);
            field.setCaretColor(TEXT);
            field.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));

            RoundedButton send = new RoundedButton("Send", RoundedButton.Style.PRIMARY);
            send.setFont(Theme.bold(11));

            Runnable submit = () -> {
                String t = field.getText().trim();
                if (t.isEmpty()) {
                    return;
                }
                if (!com.eduvibe.util.ContentFilter.isClean(t)) {
                    Toast.error(PostCard.this, com.eduvibe.util.ContentFilter.MESSAGE);
                    return;
                }
                send.setEnabled(false);
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        new com.eduvibe.dao.CommentExtrasDAO().addReply(row.id, currentUserId, t);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            loadComments(container);
                        } catch (Exception ex) {
                            send.setEnabled(true);
                            Toast.error(PostCard.this, "Could not post the reply.");
                        }
                    }
                }.execute();
            };
            send.addActionListener(e -> submit.run());
            field.addActionListener(e -> submit.run());

            input.add(field, BorderLayout.CENTER);
            input.add(send, BorderLayout.EAST);
            replyBox.add(input);

            reply.addActionListener(e -> {
                boolean show = !input.isVisible();
                replyBox.setVisible(show || !replies.isEmpty());
                input.setVisible(show);
                if (show) {
                    field.requestFocusInWindow();
                }
                container.revalidate();
                container.repaint();
            });

            south.add(actions);
            south.add(replyBox);
            actions = null;
        }

        JPanel owner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        owner.setOpaque(false);
        owner.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (mine) {
            RoundedButton edit = new RoundedButton("Edit", RoundedButton.Style.GHOST);
            edit.setFont(Theme.bold(11));
            edit.addActionListener(e -> editComment(row, container));
            owner.add(edit);
        }
        if (mine || viewerIsAdmin) {
            RoundedButton delete = new RoundedButton("Delete", RoundedButton.Style.GHOST);
            delete.setFont(Theme.bold(11));
            delete.setAccent(RED);
            delete.addActionListener(e -> deleteComment(row, viewerIsAdmin, container));
            owner.add(delete);
        }

        if (actions != null) {
            // extras unavailable: only the owner/admin buttons are shown
            if (owner.getComponentCount() > 0) {
                south.add(owner);
            }
        } else if (owner.getComponentCount() > 0) {
            // merge owner buttons into the first row, right-aligned
            JPanel first = (JPanel) south.getComponent(0);
            JPanel row1 = new JPanel(new BorderLayout());
            row1.setOpaque(false);
            row1.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.remove(first);
            row1.add(first, BorderLayout.WEST);
            row1.add(owner, BorderLayout.EAST);
            south.add(row1, 0);
        }

        if (south.getComponentCount() > 0) {
            card.add(south, BorderLayout.SOUTH);
        }

        return card;
    }

    private void editComment(CommentDAO.CommentRow row, JPanel container) {

        JTextArea area = new JTextArea(row.content, 5, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.regular(13));
        area.setBackground(Theme.INPUT);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.setPreferredSize(new Dimension(420, 130));

        int choice = JOptionPane.showConfirmDialog(this, scroll, "Edit comment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String updated = area.getText().trim();

        if (updated.isEmpty()) {
            showMessage("A comment cannot be empty.");
            return;
        }

        if (updated.equals(row.content)) {
            return;
        }

        if (!com.eduvibe.util.ContentFilter.isClean(updated)) {
            Toast.error(this, com.eduvibe.util.ContentFilter.MESSAGE);
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return commentDAO.update(row.id, currentUserId, updated);
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        showMessage("The comment could not be updated.");
                    }
                    loadComments(container);
                } catch (Exception ex) {
                    showError("Unable to edit comment.", ex);
                }
            }
        }.execute();
    }

    private void deleteComment(CommentDAO.CommentRow row, boolean admin, JPanel container) {

        if (!Dialogs.confirm(this, "Delete comment",
                "Delete this comment? This cannot be undone.", "Delete", true)) {
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return commentDAO.delete(row.id, currentUserId, admin);
            }

            @Override
            protected void done() {
                try {
                    get();
                    currentCommentCount = commentDAO.countByPost(post.getId());
                    updateCommentButton();
                    loadComments(container);
                } catch (Exception ex) {
                    showError("Unable to delete comment.", ex);
                }
            }
        }.execute();
    }

    private int getCommentCountFromDatabase() throws SQLException {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id=?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, post.getId());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // EDIT POST

    private void editPost() {
        JTextField titleField = new JTextField(
                post.getTitle() == null ? "" : post.getTitle()
        );

        JTextArea descriptionArea = new JTextArea(
                post.getDescription() == null ? "" : post.getDescription(),
                6, 30
        );

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(CARD);
        descriptionArea.setForeground(TEXT);
        descriptionArea.setCaretColor(TEXT);

        titleField.setBackground(CARD);
        titleField.setForeground(TEXT);
        titleField.setCaretColor(TEXT);

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setPreferredSize(new Dimension(400, 220));
        form.setBackground(PANEL);

        JPanel titlePanel = new JPanel(new BorderLayout(0, 5));
        titlePanel.setBackground(PANEL);

        JLabel titleLabel = new JLabel("Post title");
        titleLabel.setForeground(TEXT);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(titleField, BorderLayout.CENTER);

        JPanel descriptionPanel = new JPanel(new BorderLayout(0, 5));
        descriptionPanel.setBackground(PANEL);

        JLabel descriptionLabel = new JLabel("Post description");
        descriptionLabel.setForeground(TEXT);

        descriptionPanel.add(descriptionLabel, BorderLayout.NORTH);
        descriptionPanel.add(
                new JScrollPane(descriptionArea), BorderLayout.CENTER
        );

        form.add(titlePanel, BorderLayout.NORTH);
        form.add(descriptionPanel, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(
                this, form, "Edit Your Post",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (description.isEmpty()) {
            showMessage("Post description cannot be empty.");
            return;
        }

        if (!com.eduvibe.util.ContentFilter.isClean(title)
                || !com.eduvibe.util.ContentFilter.isClean(description)) {
            Toast.error(this, com.eduvibe.util.ContentFilter.MESSAGE);
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return postService.updatePost(
                        post.getId(), currentUserId, title, description
                );
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        post.setTitle(title);
                        post.setDescription(description);

                        try {
                            logPostActivity(
                                    post.getId(),
                                    "edited_by_owner",
                                    "Owner edited post ID " + post.getId()
                            );
                        } catch (SQLException logError) {
                            logError.printStackTrace();
                            showMessage(
                                    "Post updated, but its activity log "
                                    + "could not be saved."
                            );
                        }

                        removeAll();
                        buildCard();
                        revalidate();
                        repaint();

                        showMessage("Your post was updated!");
                    } else {
                        showMessage(
                                "Post could not be updated. "
                                + "Make sure you own this post."
                        );
                    }

                } catch (Exception ex) {
                    showError("Unable to edit post.", ex);
                }
            }
        }.execute();
    }

    // DELETE POST

    /** "Move to Entertainment" / "Move to Academic": lets the author fix a category chosen by mistake. */
    private RoundedButton buildMoveCategoryButton() {
        RoundedButton move = new RoundedButton(moveLabel(), RoundedButton.Style.GHOST);
        styleActionButton(move);
        move.addActionListener(e -> moveCategory(move));
        return move;
    }

    private Runnable onCategoryMoved;

    /** The Reels page asks the card to start the same "move to the other category" flow. */
    void actionMoveCategory(Runnable afterMove) {
        onCategoryMoved = afterMove;
        moveCategory(null);
    }

    boolean isAcademicNow() {
        return isAcademicPost();
    }

    private boolean isAcademicPost() {
        return "academic".equalsIgnoreCase(post.getCategory() == null ? "" : post.getCategory().trim());
    }

    private String moveLabel() {
        return isAcademicPost() ? "Move to Entertainment" : "Move to Academic";
    }

    private void moveCategory(RoundedButton button) {
        final String target = isAcademicPost() ? "entertainment" : "academic";
        final String nice = target.equals("academic") ? "Academic" : "Entertainment";
        if (!Dialogs.confirm(anchorOrThis(), "Move this post?",
                "Move this " + (post.isReel() ? "reel" : "post") + " to " + nice + "?\n"
                        + "It will leave its current feed and appear in the " + nice
                        + (post.isReel() ? " Reels" : " feed") + " instead.",
                "Move to " + nice, false)) {
            return;
        }
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return postService.changeOwnCategory(post.getId(), currentUserId, target);
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        showMessage("The category could not be changed. Make sure this is your post.");
                        return;
                    }
                    String before = post.getCategory();
                    post.setCategory(target);
                    try {
                        logPostActivity(post.getId(), "category_changed_by_owner",
                                "Category changed from '" + before + "' to '" + target + "' for post ID " + post.getId());
                    } catch (SQLException logError) {
                        logError.printStackTrace();
                    }
                    if (button != null) {
                        button.setText(moveLabel());
                    }
                    if (onCategoryMoved != null) {
                        onCategoryMoved.run();
                    }
                    Container parent = getParent();
                    if (parent != null) {
                        parent.remove(PostCard.this);        // it no longer belongs in this feed
                        parent.revalidate();
                        parent.repaint();
                    }
                    showMessage("Moved to " + nice + ".");
                } catch (Exception ex) {
                    showError("Unable to change the category.", ex);
                }
            }
        }.execute();
    }

    private void deletePost() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this post?\n"
                + "This action cannot be undone.",
                "Delete Post",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return postService.deletePost(
                        post.getId(), currentUserId
                );
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        try {
                            logPostActivity(
                                    null,
                                    "deleted_by_owner",
                                    "Owner deleted post ID " + post.getId()
                            );
                        } catch (SQLException logError) {
                            logError.printStackTrace();
                            showMessage(
                                    "Post deleted, but its activity log "
                                    + "could not be saved."
                            );
                        }

                        Container parent = getParent();

                        if (parent != null) {
                            parent.remove(PostCard.this);
                            parent.revalidate();
                            parent.repaint();
                        }

                        showMessage("Your post was deleted.");
                    } else {
                        showMessage(
                                "Post could not be deleted. "
                                + "Make sure you own this post."
                        );
                    }

                } catch (Exception ex) {
                    showError("Unable to delete post.", ex);
                }
            }
        }.execute();
    }

    // ACTIVITY LOG + ADMIN NOTIFICATION

    private void logPostActivity(
            Integer postId,
            String action,
            String note
    ) throws SQLException {

        String sql = """
                INSERT INTO moderation_logs
                (post_id, admin_id, action, note)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    if (postId == null) {
                        statement.setNull(1, Types.INTEGER);
                    } else {
                        statement.setInt(1, postId);
                    }

                    statement.setInt(2, currentUserId);
                    statement.setString(3, action);
                    statement.setString(4, note);

                    statement.executeUpdate();
                }

                String notificationMessage;

                if ("edited_by_owner".equals(action)) {
                    notificationMessage =
                            "A user edited their post. " + note;
                } else if ("deleted_by_owner".equals(action)) {
                    notificationMessage =
                            "A user deleted their post. " + note;
                } else {
                    notificationMessage =
                            "A user performed an action: " + note;
                }

                AdminNotificationDAO.notifyAdmins(
                        connection,
                        notificationMessage
                );

                connection.commit();

            } catch (SQLException ex) {
                connection.rollback();
                throw ex;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // UTILITIES

    private String escapeHtml(String value) {
        return "<html>"
                + value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;")
                + "</html>";
    }

    // ---- hooks for the full-screen Reels page ---------------------------------------------------
    // The Reels page keeps one hidden PostCard per reel and reuses its like / save / comment / share /
    // report logic, so the rules (notifications, points, duplicate checks) stay in a single place.

    private final boolean controllerOnly;
    private Component anchor;
    private Runnable stateListener;

    /** The visible component that dialogs and toasts should appear over. */
    void setAnchor(Component anchor) { this.anchor = anchor; }

    void setStateListener(Runnable listener) { this.stateListener = listener; }

    private Component anchorOrThis() { return anchor != null ? anchor : this; }

    private void notifyState() {
        if (stateListener != null) {
            stateListener.run();
        }
    }

    boolean isLikedNow() { return likedByCurrentUser; }
    int likeCountNow() { return currentLikeCount; }
    int commentCountNow() { return currentCommentCount; }
    boolean isSavedNow() { return savedByCurrentUser; }
    Post postNow() { return post; }

    void actionToggleLike() { toggleLike(); }
    void actionToggleSave() { toggleBookmark(); }
    void actionOpenComments() { openComments(); }
    void actionShare() { sharePost(); }
    void actionReport() { reportPost(); }

    // ------------------------------------------------------------------ download

    private File mediaFile() {
        String type = post.getMediaType();
        String path = post.getMediaPath();
        if (type == null || type.equalsIgnoreCase("none") || path == null || path.isBlank()) {
            return null;
        }
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(System.getProperty("user.dir"), path);
        }
        return f.isFile() ? f : null;
    }

    private boolean hasDownloadableMedia() {
        return mediaFile() != null;
    }

    private RoundedButton downloadButton;
    private RoundedButton downloadToggle;
    private volatile Boolean downloadBlocked;                 // null until the database answered
    private final com.eduvibe.dao.PostDownloadDAO downloadDAO = new com.eduvibe.dao.PostDownloadDAO();

    /** Can the viewer download this post's media? The author always can; others unless the author switched it off. */
    boolean canDownloadNow() {
        return hasDownloadableMedia()
                && (post.getUserId() == currentUserId || !Boolean.TRUE.equals(downloadBlocked));
    }

    /** Asks (in the background) whether the author switched downloads off, then runs `then` on the screen thread. */
    void loadDownloadState(Runnable then) {
        if (downloadBlocked != null) {
            refreshDownloadButtons();
            then.run();
            return;
        }
        final int pid = post.getId();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return downloadDAO.isBlocked(pid);
            }

            @Override
            protected void done() {
                try {
                    downloadBlocked = get();
                } catch (Exception ex) {
                    downloadBlocked = Boolean.FALSE;            // cannot tell: allow
                }
                refreshDownloadButtons();
                then.run();
            }
        }.execute();
    }

    private void refreshDownloadButtons() {
        boolean blocked = Boolean.TRUE.equals(downloadBlocked);
        if (downloadButton != null) {
            downloadButton.setVisible(canDownloadNow());
        }
        if (downloadToggle != null) {
            downloadToggle.setText(blocked ? "Downloads: Off" : "Downloads: On");
            downloadToggle.setVisible(true);
        }
        revalidate();
        repaint();
    }

    void actionDownload() {
        if (!canDownloadNow()) {
            Dialogs.info(anchorOrThis(), "Download", "The author turned off downloads for this post.");
            return;
        }
        downloadMedia();
    }

    /** The author switches downloads of this post on or off. */
    void actionToggleDownloads() {
        if (post.getUserId() != currentUserId) {
            return;
        }
        final boolean block = !Boolean.TRUE.equals(downloadBlocked);
        final int pid = post.getId();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                downloadDAO.setBlocked(pid, currentUserId, block);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    downloadBlocked = block;
                    refreshDownloadButtons();
                } catch (Exception ex) {
                    Dialogs.error(anchorOrThis(), "Downloads", "Could not change it: "
                            + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                }
            }
        }.execute();
    }

    /** Saves a copy of the post's picture or video wherever the viewer chooses. */
    void downloadMedia() {
        final File source = mediaFile();
        if (source == null) {
            Dialogs.error(anchorOrThis(), "Download", "This post's media file is not available.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save " + ("video".equalsIgnoreCase(post.getMediaType()) ? "video" : "picture"));
        File home = new File(System.getProperty("user.home"), "Downloads");
        chooser.setCurrentDirectory(home.isDirectory() ? home : new File(System.getProperty("user.home")));
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), source.getName()));
        if (chooser.showSaveDialog(anchorOrThis()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File target = chooser.getSelectedFile();
        if (target.exists() && !Dialogs.confirm(anchorOrThis(), "Replace file?",
                target.getName() + " already exists. Replace it?", "Replace", true)) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                java.nio.file.Files.copy(source.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Dialogs.info(anchorOrThis(), "Saved", "Saved to " + target.getAbsolutePath());
                } catch (Exception ex) {
                    Dialogs.error(anchorOrThis(), "Download", "Could not save the file: "
                            + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                }
            }
        }.execute();
    }

    private void showMessage(String message) {
        Toast.info(anchorOrThis(), message);
    }

    private void showError(String message, Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        ex.printStackTrace();
        Dialogs.error(anchorOrThis(), "EduVibe", message + "\n" + cause.getMessage());
    }
}
