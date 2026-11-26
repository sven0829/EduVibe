package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.File;

/**
 * com.eduvibe.ui.components.InlineVideoPanel  (REPLACES the stage 20/21 version)
 *
 * Plays a post's video INSIDE the post card. The video itself is shown straight away (first
 * frame, paused) with a small play button over it, like a normal video feed: no placeholder
 * screen and no file name. Click the video to play/pause. A control bar sits underneath
 * (play/pause, seek, time, mute). Only one video plays at a time; playback pauses when the
 * card leaves the screen. The height follows the video's own shape (wide clips stay wide,
 * vertical reels get taller). PostCard loads this class by reflection through create().
 */
public final class InlineVideoPanel extends JPanel {

    private static InlineVideoPanel active;

    private final File video;
    private final boolean reel;

    private final JFXPanel fxPanel = new JFXPanel();
    private final Overlay overlay = new Overlay();
    private final JPanel controls = new JPanel(new BorderLayout(12, 0));
    private final RoundedButton playButton = new RoundedButton("Play", RoundedButton.Style.PRIMARY);
    private final RoundedButton muteButton = new RoundedButton("Mute", RoundedButton.Style.GHOST);
    private final JSlider seek = new JSlider(0, 1000, 0);
    private final JLabel timeLabel = new JLabel("0:00 / 0:00");

    private volatile MediaPlayer player;
    private volatile boolean userSeeking;
    private volatile boolean ended;
    private volatile boolean playing;
    private volatile double videoRatio = 0;     // width / height once known
    private boolean muted;
    private boolean failed;
    private int audioRetries;
    private volatile File source;               // what is played: the original, or a converted copy
    private int conversionAttempts;
    private volatile boolean converting;        // a converted copy is being made right now
    private volatile boolean userPaused;        // the viewer paused it, so do not autoplay again
    private volatile boolean initQueued;        // an initFx() call is already waiting
    private volatile String firstError;         // what the player first reported (for the details line)
    private volatile String convertError;       // why ffmpeg could not convert, when it could not
    private volatile boolean convertedCopyMade; // a converted copy exists but the player still refused it
    private final javax.swing.Timer visibilityTimer = new javax.swing.Timer(400, e -> checkVisible());
    private int lastSeekValue = -1;          // only used on the video thread
    private String lastTimeText = "";

    /** Reflective entry point used by PostCard. */
    public static JComponent create(File video, boolean reel) {
        return new InlineVideoPanel(video, reel);
    }

    private InlineVideoPanel(File video, boolean reel) {
        super(new BorderLayout());
        this.video = video;
        this.source = video;
        this.reel = reel;

        setBackground(java.awt.Color.BLACK);

        JLayeredPane stage = new JLayeredPane() {
            @Override
            public void doLayout() {
                fxPanel.setBounds(0, 0, getWidth(), getHeight());
                overlay.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        stage.setOpaque(true);
        stage.setBackground(java.awt.Color.BLACK);
        fxPanel.setBackground(java.awt.Color.BLACK);
        stage.add(fxPanel, JLayeredPane.DEFAULT_LAYER);
        stage.add(overlay, JLayeredPane.PALETTE_LAYER);
        add(stage, BorderLayout.CENTER);

        buildControls();
        add(controls, BorderLayout.SOUTH);

        // Only the reel(s) actually on screen get a player. This is what lets Reels scroll
        // continuously without loading every video at once.
        visibilityTimer.setInitialDelay(60);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                visibilityTimer.start();
            } else {
                visibilityTimer.stop();
                pauseQuietly();
            }
        });
    }

    private void autoPlay() {
        MediaPlayer mp = player;
        if (mp == null) {
            return;
        }
        if (active != null && active != this) {
            active.pauseQuietly();
        }
        active = this;
        Platform.runLater(mp::play);
    }

    /** Called a few times a second while shown: start a player when on screen, free it when scrolled away. */
    private void checkVisible() {
        int h = getHeight();
        if (h <= 0 || !isShowing()) {
            return;
        }
        Rectangle visible = getVisibleRect();
        if (visible.height >= h * 0.25) {
            if (reel && visible.height >= h * 0.6 && player != null && videoRatio > 0 && !playing
                    && !ended && !userPaused && !failed && !converting
                    && (active == null || active == this || !active.playing)) {
                autoPlay();         // Reels start by themselves when they fill the screen
            }
            if (player == null && !failed && !converting && !initQueued) {
                initQueued = true;
                Platform.setImplicitExit(false);
                Platform.runLater(() -> {
                    try {
                        initFx();
                    } finally {
                        initQueued = false;
                    }
                });
            }
        } else if (visible.height < h * 0.4 && playing) {
            pauseQuietly();         // scrolled mostly out of view
        }
        if (visible.height < h * 0.05) {
            userPaused = false;     // coming back to it starts it again
        }
        if (visible.height < h * 0.05 && player != null && !converting) {
            MediaPlayer mp = player;
            player = null;
            if (active == this) {
                active = null;
            }
            ended = false;
            Platform.runLater(() -> {
                try {
                    mp.stop();
                    mp.dispose();
                } catch (Throwable ignored) {
                    // already gone
                }
            });
            setPlaying(false);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int w = Math.max(getWidth(), 480);
        int screen = Toolkit.getDefaultToolkit().getScreenSize().height;
        int max = reel ? Math.max(300, Math.min(560, (int) (screen * 0.62)))
                       : Math.max(240, Math.min(420, (int) (screen * 0.5)));
        int min = Math.min(max, reel ? 320 : 240);
        int h = videoRatio > 0 ? (int) Math.round(w / videoRatio) : (reel ? 480 : 340);
        h = Math.max(min, Math.min(max, h));
        return new Dimension(300, h + controls.getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    // ----------------------------------------------------------------- overlay

    /** Transparent layer over the video: draws the play button when paused and handles clicks. */
    private final class Overlay extends JComponent {

        private boolean hover;

        Overlay() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

                @Override
                public void mouseClicked(MouseEvent e) {
                    togglePlay();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();

            if (!playing) {
                double d = hover ? 76 : 68;
                double cx = w / 2.0;
                double cy = h / 2.0;
                g2.setColor(new java.awt.Color(0, 0, 0, 90));
                g2.fill(new Ellipse2D.Double(cx - d / 2 - 8, cy - d / 2 - 8, d + 16, d + 16));
                g2.setColor(new java.awt.Color(15, 23, 42, 210));
                g2.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
                g2.setColor(hover ? Theme.BLUE_LIGHT : java.awt.Color.WHITE);
                g2.setStroke(new BasicStroke(2.4f));
                g2.draw(new Ellipse2D.Double(cx - d / 2 + 1, cy - d / 2 + 1, d - 2, d - 2));

                // plain play triangle
                int tri = 24;
                int[] xs = {(int) (cx - tri / 3.0), (int) (cx - tri / 3.0), (int) (cx + tri * 0.7)};
                int[] ys = {(int) (cy - tri / 2.0), (int) (cy + tri / 2.0), (int) cy};
                g2.setColor(hover ? Theme.BLUE_LIGHT : java.awt.Color.WHITE);
                g2.fillPolygon(xs, ys, 3);
            }

            if (reel) {
                g2.setFont(Theme.bold(11));
                FontMetrics fm = g2.getFontMetrics();
                String tag = "REEL";
                int tw = fm.stringWidth(tag) + 18;
                g2.setColor(new java.awt.Color(0, 0, 0, 120));
                g2.fillRoundRect(12, 12, tw, 24, 24, 24);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(tag, 21, 28);
            }
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- controls

    private void buildControls() {

        controls.setBackground(Theme.SIDEBAR);
        controls.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        playButton.addActionListener(e -> togglePlay());
        muteButton.addActionListener(e -> toggleMute());

        seek.setOpaque(false);
        seek.addChangeListener(e -> {
            if (seek.getValueIsAdjusting()) {
                userSeeking = true;
            } else if (userSeeking) {
                userSeeking = false;
                seekToSlider();
            }
        });

        timeLabel.setFont(Theme.regular(12));
        timeLabel.setForeground(Theme.MUTED);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(timeLabel);
        right.add(muteButton);

        controls.add(playButton, BorderLayout.WEST);
        controls.add(seek, BorderLayout.CENTER);
        controls.add(right, BorderLayout.EAST);
    }

    // ---------------------------------------------------------------- playback

    private void initFx() {
        try {
            Media media = new Media(source.toURI().toString());
            MediaPlayer mp = new MediaPlayer(media);
            MediaView view = new MediaView(mp);
            view.setPreserveRatio(true);

            StackPane pane = new StackPane(view);
            pane.setStyle("-fx-background-color: black;");
            view.fitWidthProperty().bind(pane.widthProperty());
            view.fitHeightProperty().bind(pane.heightProperty());

            fxPanel.setScene(new Scene(pane, Color.BLACK));

            mp.currentTimeProperty().addListener((obs, old, now) -> updateTime(mp, now));

            mp.setOnReady(() -> {
                int vw = media.getWidth();
                int vh = media.getHeight();
                if (vw > 0 && vh > 0) {
                    videoRatio = (double) vw / vh;
                }
                // show a still frame instead of a black box
                if (!playing) {
                    mp.seek(Duration.millis(150));
                }
                SwingUtilities.invokeLater(() -> {
                    revalidate();
                    Container p = getParent();
                    while (p != null) {
                        p.revalidate();
                        p = p.getParent();
                    }
                    repaint();
                });
            });
            mp.setOnPlaying(() -> setPlaying(true));
            mp.setOnPaused(() -> setPlaying(false));
            mp.setOnEndOfMedia(() -> {
                ended = true;
                setPlaying(false);
                SwingUtilities.invokeLater(() -> playButton.setText("Replay"));
            });
            mp.setOnError(() -> handleError(mp, mp.getError() == null
                    ? "Unknown playback error." : mp.getError().getMessage()));
            media.setOnError(() -> handleError(mp, media.getError() == null
                    ? "Unsupported video format." : media.getError().getMessage()));

            mp.setMute(muted);
            player = mp;
            mp.pause();     // ready, paused, first frame visible

        } catch (Throwable t) {
            if (!convertAndRetry(null, t.getMessage())) {
                showError(t.getMessage());
            }
        }
    }

    private void setPlaying(boolean value) {
        playing = value;
        SwingUtilities.invokeLater(() -> {
            if (value) {
                playButton.setText("Pause");
            } else if (!ended) {
                playButton.setText("Play");
            }
            overlay.repaint();
        });
    }

    private void togglePlay() {
        MediaPlayer mp = player;
        if (mp == null) {
            return;
        }
        userPaused = playing;       // pressing the button while it plays is a deliberate pause
        if (active != null && active != this) {
            active.pauseQuietly();
        }
        active = this;
        Platform.runLater(() -> {
            if (ended) {
                ended = false;
                mp.seek(Duration.ZERO);
                mp.play();
            } else if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                mp.pause();
            } else {
                mp.play();
            }
        });
    }

    private void toggleMute() {
        muted = !muted;
        muteButton.setText(muted ? "Unmute" : "Mute");
        MediaPlayer mp = player;
        if (mp != null) {
            Platform.runLater(() -> mp.setMute(muted));
        }
    }

    private void pauseQuietly() {
        MediaPlayer mp = player;
        if (mp != null) {
            Platform.runLater(() -> {
                if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                    mp.pause();
                }
            });
        }
    }

    private void seekToSlider() {
        MediaPlayer mp = player;
        if (mp == null) {
            return;
        }
        double fraction = seek.getValue() / 1000.0;
        Platform.runLater(() -> {
            Duration total = mp.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                ended = false;
                mp.seek(total.multiply(fraction));
            }
        });
    }

    private void updateTime(MediaPlayer mp, Duration now) {
        Duration total = mp.getTotalDuration();
        if (total == null || total.isUnknown() || total.toMillis() <= 0) {
            return;
        }
        int value = (int) (now.toMillis() / total.toMillis() * 1000);
        String text = format(now) + " / " + format(total);
        // The player reports its time many times a second; only touch Swing when something visible changes.
        if (value == lastSeekValue && text.equals(lastTimeText)) {
            return;
        }
        lastSeekValue = value;
        lastTimeText = text;
        SwingUtilities.invokeLater(() -> {
            if (!userSeeking) {
                seek.setValue(Math.max(0, Math.min(1000, value)));
            }
            timeLabel.setText(text);
        });
    }

    private static String format(Duration d) {
        int seconds = (int) Math.round(d.toSeconds());
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    /**
     * Windows sometimes reports a temporary audio-device error (DirectSound) while the
     * sound card wakes up or switches output. Try once or twice again before giving up.
     */
    private void handleError(MediaPlayer broken, String message) {
        if (failed || converting) {
            return;            // the player reports one failure twice; the first one is being handled
        }
        if (broken != null && player != null && player != broken) {
            return;            // a late report from a player that was already replaced
        }
        if (firstError == null) {
            firstError = message;
        }
        String m = message == null ? "" : message.toLowerCase();
        boolean audio = m.contains("directsound") || m.contains("audio") || m.contains("sound");
        if (audio && audioRetries < 2 && player == broken) {
            audioRetries++;
            player = null;
            boolean wasPlaying = playing;
            new Thread(() -> {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    try {
                        broken.dispose();
                    } catch (Throwable ignored) {
                        // already gone
                    }
                    initFx();
                    if (wasPlaying && player != null) {
                        player.play();
                    }
                });
            }, "video-retry").start();
            return;
        }

        if (convertAndRetry(broken, message)) {
            return;
        }
        showError(message);
    }

    /**
     * Not an audio hiccup: most likely a format the player cannot read. When ffmpeg is installed,
     * make a playable copy (the stored file is untouched) and try again once.
     *
     * @return true when a conversion was started, so the error should not be shown yet
     */
    private boolean convertAndRetry(MediaPlayer broken, String message) {

        // attempt 1: a careful copy with sound; attempt 2: a silent copy as the last resort
        if (conversionAttempts >= 2 || !com.eduvibe.util.VideoConverter.isAvailable()) {
            return false;
        }
        final int attempt = ++conversionAttempts;
        converting = true;
        player = null;
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(false);
            timeLabel.setText("Preparing video...");
        });
        new Thread(() -> {
            File copy = null;
            try {
                copy = com.eduvibe.util.VideoConverter.playableCopy(video, attempt);
            } catch (Exception ex) {
                convertError = ex.getMessage();      // shown in the details line and the log
            }
            final File ready = copy;
            Platform.runLater(() -> {
                converting = false;
                if (broken != null) {
                    try {
                        broken.dispose();
                    } catch (Throwable ignored) {
                        // already gone
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    playButton.setEnabled(true);
                    timeLabel.setText("0:00 / 0:00");
                });
                if (ready == null) {
                    showError(message);
                    return;
                }
                convertedCopyMade = true;
                source = ready;
                initFx();
            });
        }, "video-convert").start();
        return true;
    }

    /** The real reasons, shortened for the dialog and written in full to eduvibe-video.log. */
    private String describeFailure(String message) {
        StringBuilder log = new StringBuilder();
        log.append(java.time.LocalDateTime.now()).append("  ").append(video.getAbsolutePath()).append('\n');
        log.append("  player said: ").append(firstError == null ? message : firstError).append('\n');
        if (message != null && !message.equals(firstError)) {
            log.append("  after conversion the player said: ").append(message).append('\n');
        }
        if (convertError != null) {
            log.append("  ffmpeg said: ").append(convertError).append('\n');
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get(System.getProperty("user.dir"), "eduvibe-video.log"),
                    log.toString(), java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // the log is a bonus
        }

        String shortPlayer = trim(firstError == null ? message : firstError);
        StringBuilder sb = new StringBuilder("\n\nDetails: ");
        if (convertError != null) {
            sb.append("ffmpeg could not convert it (").append(trim(convertError)).append(").");
        } else if (convertedCopyMade) {
            sb.append("a converted copy was made, but the player still refused it (")
              .append(trim(message)).append(").");
        } else {
            sb.append("the player said: ").append(shortPlayer);
        }
        return sb.toString();
    }

    private static String trim(String t) {
        if (t == null || t.isBlank()) {
            return "no message";
        }
        String one = t.replaceAll("\\s+", " ").trim();
        return one.length() > 160 ? one.substring(0, 160) + "..." : one;
    }

    private void showError(String message) {
        if (failed) {
            return;
        }
        failed = true;
        SwingUtilities.invokeLater(() -> {
            String m = message == null ? "" : message.toUpperCase();
            boolean format = m.contains("INVALID") || m.contains("UNSUPPORTED") || m.contains("FORMAT");
            String why = format
                    ? "This video uses a format the built-in player does not support.\n"
                    + "The player only handles MP4 with H.264 video and AAC sound.\n"
                    + "Videos from phones, editors or screen recorders are often H.265 (HEVC) or use other audio."
                    : "This is usually a temporary audio-device problem on Windows.";
            String convertNote = format
                    ? (com.eduvibe.util.VideoConverter.isAvailable()
                        ? (conversionAttempts > 0 ? "\n\nEduVibe tried to convert it with ffmpeg but that did not work."
                                           : "")
                        : "\n\nffmpeg was not found, so EduVibe could not convert it. "
                          + "Install ffmpeg and restart EduVibe to play videos like this.")
                    : "";
            String details = describeFailure(message);
            boolean open = Dialogs.confirm(this, "Video could not be played",
                    "EduVibe could not play this video inside the app.\n" + why + convertNote
                            + details
                            + "\n\nOpen it in your computer's video player instead?",
                    "Open video", false);
            if (open) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(video);
                    }
                } catch (Exception ignored) {
                    // nothing more to try
                }
            }
        });
    }

    @Override
    public void removeNotify() {
        visibilityTimer.stop();
        MediaPlayer mp = player;
        player = null;
        if (active == this) {
            active = null;
        }
        if (mp != null) {
            Platform.runLater(() -> {
                mp.stop();
                mp.dispose();
            });
        }
        super.removeNotify();
    }
}
