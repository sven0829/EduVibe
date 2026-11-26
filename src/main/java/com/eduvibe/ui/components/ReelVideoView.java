package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;
import com.eduvibe.util.VideoConverter;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * com.eduvibe.ui.components.ReelVideoView  (NEW FILE)
 *
 * One full-frame video for the TikTok-style Reels page: autoplays when told to, loops, taps to
 * pause / play, shows a thin progress line you can drag, and a loading / error state.
 *
 * It owns exactly one JavaFX MediaPlayer, created only when needed (prepare / play) and disposed
 * by release(), so the Reels page can keep a single video playing and just one more preloaded.
 * A video the player cannot open is converted with ffmpeg (when installed) and retried, the same
 * way the normal post video does.
 */
public final class ReelVideoView extends JPanel implements ReelPlayer {


    /** The mute choice carries from reel to reel. */
    private static volatile boolean muted = false;

    public static boolean isMuted() {
        return muted;
    }

    /** Remembers the mute choice for every engine. */
    static void rememberMuted(boolean value) {
        muted = value;
    }

    private final JFXPanel fxPanel = new JFXPanel();
    private final Overlay overlay = new Overlay();
    private final File video;

    private volatile File source;
    private volatile MediaPlayer player;
    private volatile boolean requested;       // prepare() or play() was called and not yet released
    private volatile boolean wantPlay;
    private volatile boolean ready;
    private volatile boolean playing;
    private volatile boolean failed;
    private int softRetries;
    private int lightState;          // 0 not checked, 1 checking / converting, 2 done
    private java.util.function.Consumer<Point> onDoubleClick = p -> { };
    private volatile boolean converting;
    private volatile boolean initQueued;
    private volatile boolean userPaused;
    private volatile double progress;
    private volatile double ratio;                // width / height of the picture, once known
    private volatile Runnable onRatio = () -> { };
    private volatile double lastPainted = -1;
    private volatile String firstError;
    private volatile String lastError;
    private int conversionAttempts;
    private int audioRetries;

    private final javax.swing.Timer spinner = new javax.swing.Timer(40, e -> overlay.repaint());

    public ReelVideoView(File video) {
        super(new BorderLayout());
        this.video = video;
        this.source = video;
        setBackground(java.awt.Color.BLACK);
        setOpaque(true);

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
    }

    /** Wheel events over the video are passed to the Reels page (the video itself would swallow them). */
    public void setWheelListener(MouseWheelListener listener) {
        overlay.addMouseWheelListener(listener);
    }

    // ------------------------------------------------------------------ public controls

    public boolean isPlaying() {
        return playing;
    }

    public boolean isReady() {
        return ready && player != null && !failed;
    }

    public boolean isFailed() {
        return failed;
    }

    /**
     * Average colours of the top, middle and bottom of the picture on screen (for the ambient glow
     * around the reel), or null when no picture is showing yet.
     */
    public int[] sampleColors() {
        int w = fxPanel.getWidth(), h = fxPanel.getHeight();
        if (!ready || failed || w < 8 || h < 8) {
            return null;
        }
        try {
            int sw = 32, sh = 48;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(sw, sh,
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.scale(sw / (double) w, sh / (double) h);
            fxPanel.paint(g);
            g.dispose();
            int[] out = new int[3];
            for (int band = 0; band < 3; band++) {
                long r = 0, gr = 0, b = 0;
                int n = 0;
                for (int y = band * (sh / 3); y < (band + 1) * (sh / 3); y++) {
                    for (int x = 0; x < sw; x++) {
                        int rgb = img.getRGB(x, y);
                        r += (rgb >> 16) & 255;
                        gr += (rgb >> 8) & 255;
                        b += rgb & 255;
                        n++;
                    }
                }
                out[band] = ((int) (r / n) << 16) | ((int) (gr / n) << 8) | (int) (b / n);
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Width / height of the video, or 0 until the player has read it. */
    public double ratio() {
        return ratio;
    }

    /** Called (on the Swing thread) when the video's proportions become known. */
    public void setOnDoubleClick(java.util.function.Consumer<Point> c) {
        this.onDoubleClick = c == null ? p -> { } : c;
    }

    public void setOnRatio(Runnable r) {
        this.onRatio = r == null ? () -> { } : r;
    }

    /** Creates the player paused on the first frame, so a later play() starts at once. */
    public void prepare() {
        requested = true;
        if (lightState == 0 && VideoConverter.isAvailable()) {
            // First time: a big or fast video is first turned into a lighter playback copy (kept for next time).
            lightState = 1;
            converting = true;
            refresh();
            new Thread(() -> {
                final File light = VideoConverter.lightCopyIfHeavy(video, wantPlay, () -> requested);
                Platform.runLater(() -> {
                    converting = false;
                    if (light == VideoConverter.SKIPPED) {
                        lightState = 0;             // skipped, not checked: it will be checked when it is wanted again
                        refresh();
                        return;
                    }
                    lightState = 2;
                    if (light != null && player == null) {
                        source = light;
                    }
                    if (requested) {
                        initFx();
                    }
                    refresh();
                });
            }, "reel-light").start();
            return;
        }
        if (player != null || initQueued || failed || converting) {
            refresh();
            return;
        }
        initQueued = true;
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                initFx();
            } finally {
                initQueued = false;
            }
        });
        refresh();
    }

    public void play() {
        wantPlay = true;
        userPaused = false;
        prepare();
        MediaPlayer mp = player;
        if (mp != null) {
            Platform.runLater(mp::play);
        }
    }

    public void pause() {
        wantPlay = false;
        MediaPlayer mp = player;
        if (mp != null) {
            Platform.runLater(mp::pause);
        }
    }

    /** Tap: pause when playing, play when paused. */
    public void toggle() {
        if (failed) {
            openExternally();
            return;
        }
        if (playing || (wantPlay && !userPaused && !ready)) {
            userPaused = true;
            pause();
        } else {
            play();
        }
        refresh();
    }

    public void setMuted(boolean value) {
        muted = value;
        MediaPlayer mp = player;
        if (mp != null) {
            Platform.runLater(() -> mp.setMute(value));
        }
    }

    /** Stops and frees the player. The view can be prepared again later. */
    public void release() {
        requested = false;
        wantPlay = false;
        ready = false;
        playing = false;
        MediaPlayer mp = player;
        player = null;
        if (mp != null) {
            Platform.runLater(() -> {
                try {
                    mp.stop();
                    mp.dispose();
                } catch (Throwable ignored) {
                    // already gone
                }
            });
        }
        refresh();
    }

    @Override
    public void removeNotify() {
        spinner.stop();
        release();
        super.removeNotify();
    }

    // ------------------------------------------------------------------ player (JavaFX thread)

    private void initFx() {
        if (!requested || player != null || failed) {
            return;
        }
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

            mp.setCycleCount(MediaPlayer.INDEFINITE);      // reels loop
            mp.setMute(muted);

            mp.currentTimeProperty().addListener((obs, old, now) -> onTime(mp, now));
            mp.setOnReady(() -> {
                if (player != mp) {
                    return;
                }
                ready = true;
                int vw = media.getWidth(), vh = media.getHeight();
                if (vw > 0 && vh > 0 && Math.abs(ratio - (double) vw / vh) > 0.001) {
                    ratio = (double) vw / vh;
                    SwingUtilities.invokeLater(onRatio);
                }
                if (wantPlay) {
                    mp.play();
                } else {
                    mp.seek(Duration.millis(150));          // a still first frame instead of black
                }
                refresh();
            });
            mp.setOnPlaying(() -> {
                if (player == mp) {
                    playing = true;
                    refresh();
                }
            });
            mp.setOnPaused(() -> {
                if (player == mp) {
                    playing = false;
                    refresh();
                }
            });
            mp.setOnError(() -> handleError(mp, mp.getError() == null
                    ? "Unknown playback error." : mp.getError().getMessage()));
            media.setOnError(() -> handleError(mp, media.getError() == null
                    ? "Unsupported video format." : media.getError().getMessage()));

            player = mp;
            if (!wantPlay) {
                mp.pause();
            }
        } catch (Throwable t) {
            if (!convertAndRetry(null)) {
                fail(t.getMessage());
            }
        }
    }

    private void onTime(MediaPlayer mp, Duration now) {
        if (player != mp || now == null) {
            return;
        }
        Duration total = mp.getTotalDuration();
        if (total == null || total.isUnknown() || total.toMillis() <= 0) {
            return;
        }
        double f = Math.max(0, Math.min(1, now.toMillis() / total.toMillis()));
        progress = f;
        if (Math.abs(f - lastPainted) > 0.003) {
            lastPainted = f;
            SwingUtilities.invokeLater(() -> overlay.repaint());
        }
    }

    private void seekToFraction(double f) {
        MediaPlayer mp = player;
        if (mp == null || !ready) {
            return;
        }
        Platform.runLater(() -> {
            Duration total = mp.getTotalDuration();
            if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                mp.seek(Duration.millis(total.toMillis() * Math.max(0, Math.min(1, f))));
            }
        });
        progress = f;
        overlay.repaint();
    }

    // ------------------------------------------------------------------ errors and conversion

    private void handleError(MediaPlayer broken, String message) {
        if (failed || converting || !requested) {
            return;                         // one failure is reported twice; the first is being handled
        }
        if (player != null && player != broken) {
            return;                         // a late report from a player that was already replaced
        }
        if (firstError == null) {
            firstError = message;
        }
        lastError = message;

        if (!wantPlay) {
            // A reel that is only being warmed up for later: drop it quietly. It is set up again,
            // from scratch, when it is really played, so no "can't play" flash for the next reel.
            player = null;
            ready = false;
            try {
                broken.dispose();
            } catch (Throwable ignored) {
                // already gone
            }
            refresh();
            return;
        }

        String m = message == null ? "" : message.toLowerCase();
        boolean audio = m.contains("directsound") || m.contains("audio") || m.contains("sound");
        if (audio && audioRetries < 2) {                 // a temporary Windows sound-device hiccup
            audioRetries++;
            player = null;
            ready = false;
            new Thread(() -> {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    try {
                        broken.dispose();
                    } catch (Throwable ignored) {
                        // already gone
                    }
                    initFx();
                    MediaPlayer mp = player;
                    if (wantPlay && mp != null) {
                        mp.play();
                    }
                });
            }, "reel-audio-retry").start();
            return;
        }

        if (convertAndRetry(broken)) {
            return;
        }
        if (softRetries < 2) {                           // a hiccup under load: try the same file again
            softRetries++;
            player = null;
            ready = false;
            final int tries = softRetries;
            new Thread(() -> {
                try {
                    Thread.sleep(500L * tries);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    try {
                        broken.dispose();
                    } catch (Throwable ignored) {
                        // already gone
                    }
                    initFx();
                    MediaPlayer mp = player;
                    if (wantPlay && mp != null) {
                        mp.play();
                    }
                });
            }, "reel-soft-retry").start();
            refresh();
            return;
        }
        fail(message);
    }

    /** Attempt 1 makes a careful copy with sound, attempt 2 a silent copy; each is tried once. */
    private boolean convertAndRetry(MediaPlayer broken) {
        if (conversionAttempts >= 2 || !VideoConverter.isAvailable()) {
            return false;
        }
        final int attempt = ++conversionAttempts;
        converting = true;
        player = null;
        ready = false;
        playing = false;
        refresh();
        new Thread(() -> {
            File copy = null;
            try {
                copy = VideoConverter.playableCopy(video, attempt);
            } catch (Exception ex) {
                lastError = ex.getMessage();
            }
            final File ready2 = copy;
            Platform.runLater(() -> {
                converting = false;
                if (broken != null) {
                    try {
                        broken.dispose();
                    } catch (Throwable ignored) {
                        // already gone
                    }
                }
                if (ready2 == null) {
                    fail(lastError);
                    return;
                }
                source = ready2;
                initFx();
                MediaPlayer mp = player;
                if (wantPlay && mp != null) {
                    mp.play();
                }
            });
        }, "reel-convert").start();
        return true;
    }

    private void fail(String message) {
        failed = true;
        playing = false;
        ready = false;
        lastError = message;
        try {
            Files.writeString(Paths.get(System.getProperty("user.dir"), "eduvibe-video.log"),
                    LocalDateTime.now() + "  reel  " + video.getAbsolutePath() + "\n  first: " + firstError
                            + "\n  last: " + message + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // the log is a bonus
        }
        refresh();
    }

    private void openExternally() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(video);
            }
        } catch (Exception ignored) {
            // nothing more to try
        }
    }

    // ------------------------------------------------------------------ overlay

    private void refresh() {
        SwingUtilities.invokeLater(() -> {
            boolean busy = requested && !failed && (converting || !ready);
            if (busy && !spinner.isRunning()) {
                spinner.start();
            } else if (!busy && spinner.isRunning()) {
                spinner.stop();
            }
            overlay.repaint();
        });
    }

    private final class Overlay extends JComponent {

        private boolean scrubbing;

        Overlay() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    scrubbing = ready && e.getY() >= getHeight() - 18;
                    if (scrubbing) {
                        seekToFraction(e.getX() / (double) Math.max(1, getWidth()));
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (scrubbing) {
                        seekToFraction(e.getX() / (double) Math.max(1, getWidth()));
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    // the click that follows a scrub must not also pause the video
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getY() >= getHeight() - 18 && ready) {
                        return;
                    }
                    if (e.getClickCount() >= 2) {
                        toggle();                       // the first click of the pair paused it: undo that
                        onDoubleClick.accept(e.getPoint());
                        return;
                    }
                    toggle();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();

            if (failed) {
                g2.setColor(new java.awt.Color(0, 0, 0, 200));
                g2.fillRect(0, 0, w, h);
                g2.setColor(java.awt.Color.WHITE);
                g2.setFont(Theme.bold(16));
                centered(g2, "This video can't be played here", w, h / 2 - 8);
                g2.setFont(Theme.regular(13));
                g2.setColor(new java.awt.Color(210, 218, 240));
                centered(g2, "Click to open it in your video player", w, h / 2 + 16);
            } else if (requested && (converting || !ready)) {
                int d = 46;
                int x = (w - d) / 2, y = (h - d) / 2 - 10;
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new java.awt.Color(255, 255, 255, 50));
                g2.drawOval(x, y, d, d);
                g2.setColor(java.awt.Color.WHITE);
                int angle = (int) ((System.currentTimeMillis() / 4) % 360);
                g2.drawArc(x, y, d, d, -angle, 100);
                g2.setFont(Theme.regular(13));
                g2.setColor(new java.awt.Color(220, 226, 245));
                centered(g2, converting ? "Preparing video..." : "Loading...", w, y + d + 26);
            } else if (ready && !playing && userPaused) {
                int d = 78;
                int x = (w - d) / 2, y = (h - d) / 2;
                g2.setColor(new java.awt.Color(0, 0, 0, 120));
                g2.fillOval(x, y, d, d);
                g2.setColor(java.awt.Color.WHITE);
                int[] px = {x + 31, x + 31, x + 57};
                int[] py = {y + 24, y + 54, y + 39};
                g2.fillPolygon(px, py, 3);
            }

            if (ready && !failed) {
                int barH = scrubbing ? 6 : 3;
                g2.setColor(new java.awt.Color(255, 255, 255, 70));
                g2.fillRect(0, h - barH, w, barH);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, h - barH, (int) Math.round(w * progress), barH);
            }
            g2.dispose();
        }

        private void centered(Graphics2D g2, String text, int w, int baseline) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, baseline);
        }
    }
}
