package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;
import com.eduvibe.util.VideoConverter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * com.eduvibe.ui.components.FfmpegReelView  (NEW FILE)
 *
 * A light video engine for Reels. ffmpeg decodes the file into small raw frames (at most 720 px on the
 * long side, 30 per second) and Swing simply paints them; the sound is decoded by a second ffmpeg and
 * played through javax.sound. The sound line is the clock: a frame is shown when the sound reaches its
 * time, and a late frame is skipped, so picture and sound stay together even when the computer is busy.
 * There is no JavaFX media pipeline, no scene graph and no extra copy of the picture, which is what made
 * Reels slow on a small laptop.
 *
 * Same behaviour as the JavaFX view: loops, tap to pause, drag the thin bar to seek, spinner, error state.
 */
public final class FfmpegReelView extends JPanel implements ReelPlayer {

    /** Quality steps, best first. The engine moves down a step by itself when this computer cannot keep up. */
    private static final int[] SIDES = {720, 560, 420, 320};
    private static final int[] RATES = {30, 30, 24, 20};
    private static volatile int level = 0;
    private static final int RATE = 44100;
    private static final Pattern SIZE = Pattern.compile("Video:.*?[\\s,](\\d{2,5})x(\\d{2,5})[\\s,\\[]");
    private static final Pattern DURATION = Pattern.compile("Duration:\\s*(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)");
    private static final Pattern ROTATION = Pattern.compile("(?:rotate\\s*:\\s*|rotation of\\s*)(-?\\d+)");

    private final File video;
    private final Surface surface = new Surface();
    private final Overlay overlay = new Overlay();
    private final javax.swing.Timer spinner = new javax.swing.Timer(40, e -> overlay.repaint());

    private volatile boolean requested;
    private volatile boolean wantPlay;
    private volatile boolean userPaused;
    private volatile boolean ready;
    private volatile boolean failed;
    private volatile boolean probing;
    private volatile double ratio;
    private volatile double duration;                 // seconds, 0 unknown
    private volatile boolean hasAudio;
    private volatile double progress;
    private volatile double lastPainted = -1;
    private volatile Runnable onRatio = () -> { };
    private java.util.function.Consumer<Point> onDoubleClick = p -> { };
    private volatile String lastError;

    private int frameW, frameH, srcW, srcH;
    private int fps = 30;
    private int appliedLevel = -1;

    /** Sizes the frames for the current quality step (reallocating the picture buffers when it changed). */
    private void applyLevel() {
        int lv = level;
        if (lv == appliedLevel && frameW > 0) {
            return;
        }
        double scale = Math.min(1.0, SIDES[lv] / (double) Math.max(srcW, srcH));
        int fw = Math.max(2, ((int) Math.round(srcW * scale)) & ~1);
        int fh = Math.max(2, ((int) Math.round(srcH * scale)) & ~1);
        BufferedImage[] fresh = new BufferedImage[buffers.length];
        for (int i = 0; i < fresh.length; i++) {
            fresh[i] = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_RGB);
        }
        System.arraycopy(fresh, 0, buffers, 0, fresh.length);
        frameW = fw;
        frameH = fh;
        fps = RATES[lv];
        appliedLevel = lv;
    }
    private final Object lock = new Object();
    private volatile int generation;                  // a new session (start, seek, loop) invalidates the old threads
    private volatile Process videoProc;
    private volatile Process audioProc;
    private SourceDataLine line;
    private volatile long linePos0;                   // line position when the session's sound began
    private volatile double sessionStart;             // media time the session started from
    private volatile boolean audioClock;              // true while the sound line drives the clock
    private long wallStart;                           // nanoTime when playing began (wall clock fallback)
    private long wallPausedAt;
    private long wallPausedTotal;

    // playback statistics (shown with the D key on Reels)
    private final java.util.concurrent.atomic.AtomicInteger statShown = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger statSkipped = new java.util.concurrent.atomic.AtomicInteger();
    private volatile double decodeMs;                 // average time to receive one frame from ffmpeg
    private int lastShown, lastSkipped;
    private long lastStatNanos = System.nanoTime();

    @Override
    public String debugInfo() {
        long now = System.nanoTime();
        double secs = Math.max(0.2, (now - lastStatNanos) / 1e9);
        int shown = statShown.get(), skipped = statSkipped.get();
        String text = String.format(java.util.Locale.ROOT,
                "ffmpeg %dpx@%dfps | shown %.0f/s | skipped %.0f/s | decode %.0f ms/frame | %s",
                SIDES[Math.min(level, SIDES.length - 1)], fps,
                (shown - lastShown) / secs, (skipped - lastSkipped) / secs, decodeMs,
                failed ? "FAILED" : isPlaying() ? "playing" : ready ? "paused" : "loading");
        lastShown = shown;
        lastSkipped = skipped;
        lastStatNanos = now;
        return text;
    }

    private final BufferedImage[] buffers = new BufferedImage[3];
    private volatile BufferedImage front;
    private int nextBuffer;

    public FfmpegReelView(File video) {
        super(new BorderLayout());
        this.video = video;
        setBackground(Color.BLACK);
        setOpaque(true);
        JLayeredPane stage = new JLayeredPane() {
            @Override
            public void doLayout() {
                surface.setBounds(0, 0, getWidth(), getHeight());
                overlay.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        stage.setOpaque(true);
        stage.setBackground(Color.BLACK);
        stage.add(surface, JLayeredPane.DEFAULT_LAYER);
        stage.add(overlay, JLayeredPane.PALETTE_LAYER);
        add(stage, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------ ReelPlayer

    @Override
    public void setWheelListener(MouseWheelListener listener) {
        overlay.addMouseWheelListener(listener);
    }

    @Override
    public boolean isPlaying() {
        return ready && wantPlay && !userPaused && !failed;
    }

    @Override
    public boolean isReady() {
        return ready && !failed;
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public double ratio() {
        return ratio;
    }

    @Override
    public void setOnDoubleClick(java.util.function.Consumer<Point> c) {
        this.onDoubleClick = c == null ? p -> { } : c;
    }

    @Override
    public void setOnRatio(Runnable r) {
        this.onRatio = r == null ? () -> { } : r;
    }

    @Override
    public int[] sampleColors() {
        BufferedImage img = front;
        if (img == null || !ready || failed) {
            return null;
        }
        try {
            int sw = 32, sh = 48;
            BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, sw, sh, null);
            g.dispose();
            int[] out = new int[3];
            for (int band = 0; band < 3; band++) {
                long r = 0, gr = 0, b = 0;
                int n = 0;
                for (int y = band * (sh / 3); y < (band + 1) * (sh / 3); y++) {
                    for (int x = 0; x < sw; x++) {
                        int rgb = small.getRGB(x, y);
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

    @Override
    public void prepare() {
        requested = true;
        if (failed) {
            return;
        }
        synchronized (lock) {
            if (ready || probing || videoProc != null) {
                refresh();
                return;
            }
            probing = true;
        }
        refresh();
        new Thread(() -> {
            try {
                if (frameW == 0 && !probe()) {
                    fail("ffmpeg could not read this video.");
                    return;
                }
                if (requested) {
                    startSession(0);
                }
            } finally {
                probing = false;
                refresh();
            }
        }, "reel-probe").start();
    }

    @Override
    public void play() {
        wantPlay = true;
        userPaused = false;
        prepare();
        applyPlayState();
        refresh();
    }

    @Override
    public void pause() {
        wantPlay = false;
        applyPlayState();
        refresh();
    }

    @Override
    public void toggle() {
        if (failed) {
            openExternally();
            return;
        }
        if (isPlaying() || (wantPlay && !userPaused && !ready)) {
            userPaused = true;
            pause();
        } else {
            play();
        }
        refresh();
    }

    @Override
    public void setMuted(boolean value) {
        ReelVideoView.rememberMuted(value);          // the audio thread reads the shared flag
    }

    @Override
    public void release() {
        requested = false;
        wantPlay = false;
        ready = false;
        stopSession();
        closeLine();
        front = null;
        refresh();
    }

    @Override
    public void removeNotify() {
        spinner.stop();
        release();
        super.removeNotify();
    }

    // ------------------------------------------------------------------ probing

    /** Reads size, rotation, duration and whether there is sound, by asking ffmpeg about the file. */
    private boolean probe() {
        String ff = VideoConverter.command();
        if (ff == null) {
            return false;
        }
        try {
            Process p = new ProcessBuilder(ff, "-hide_banner", "-i", video.getAbsolutePath())
                    .redirectErrorStream(true).start();
            String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(8, java.util.concurrent.TimeUnit.SECONDS);
            p.destroyForcibly();

            Matcher m = SIZE.matcher(text);
            if (!m.find()) {
                return false;
            }
            int w = Integer.parseInt(m.group(1)), h = Integer.parseInt(m.group(2));
            Matcher r = ROTATION.matcher(text);
            if (r.find()) {
                int rot = Math.abs(Integer.parseInt(r.group(1))) % 180;
                if (rot == 90) {
                    int t = w;
                    w = h;
                    h = t;
                }
            }
            srcW = w;
            srcH = h;
            applyLevel();
            Matcher d = DURATION.matcher(text);
            if (d.find()) {
                duration = Integer.parseInt(d.group(1)) * 3600 + Integer.parseInt(d.group(2)) * 60
                        + Double.parseDouble(d.group(3));
            }
            hasAudio = text.contains("Audio:");
            double newRatio = frameW / (double) frameH;
            if (Math.abs(ratio - newRatio) > 0.001) {
                ratio = newRatio;
                SwingUtilities.invokeLater(onRatio);
            }
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    // ------------------------------------------------------------------ sessions

    /** Starts (or restarts, for a loop or a seek) decoding from a media time; the first picture is shown at once. */
    private void startSession(double from) {
        stopSession();
        applyLevel();
        final int gen;
        synchronized (lock) {
            gen = ++generation;
            sessionStart = from;
            audioClock = false;
            clockStarted = false;
            sessionBegan = System.nanoTime();
            wallStart = System.nanoTime();
            wallPausedTotal = 0;
            wallPausedAt = 0;
        }
        Thread t = new Thread(() -> videoLoop(gen, from), "reel-video");
        t.setPriority(Thread.NORM_PRIORITY + 1);
        t.setDaemon(true);
        t.start();
    }

    private void stopSession() {
        generation++;
        Process v = videoProc, a = audioProc;
        videoProc = null;
        audioProc = null;
        if (v != null) {
            v.destroyForcibly();
        }
        if (a != null) {
            a.destroyForcibly();
        }
        SourceDataLine l = line;
        if (l != null) {
            try {
                l.stop();
                l.flush();
            } catch (Throwable ignored) {
                // closing anyway
            }
        }
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void closeLine() {
        SourceDataLine l = line;
        line = null;
        if (l != null) {
            try {
                l.stop();
                l.close();
            } catch (Throwable ignored) {
                // already closed
            }
        }
    }

    private boolean live(int gen) {
        return gen == generation && requested;
    }

    /** True while the picture should be moving. */
    private boolean running() {
        return wantPlay && !userPaused;
    }

    private void applyPlayState() {
        synchronized (lock) {
            if (running()) {
                if (wallPausedAt != 0) {
                    wallPausedTotal += System.nanoTime() - wallPausedAt;
                    wallPausedAt = 0;
                }
                SourceDataLine l = line;
                if (l != null) {
                    l.start();
                }
                if (ready && !hasAudio) {
                    startClock();                                    // no sound track: nothing to wait for
                }
                if (ready && audioProc == null && hasAudio) {
                    startAudio(generation);
                }
            } else {
                if (clockStarted && wallPausedAt == 0) {
                    wallPausedAt = System.nanoTime();
                }
                SourceDataLine l = line;
                if (l != null) {
                    l.stop();
                }
            }
            lock.notifyAll();
        }
    }

    private volatile boolean clockStarted;
    private volatile long sessionBegan = System.nanoTime();

    /**
     * Starts the picture clock. It is the system clock, not the sound card's position: the picture then stays
     * smooth even when the sound device reports its position unevenly. It begins when the sound begins.
     */
    private void startClock() {
        synchronized (lock) {
            if (!clockStarted) {
                wallStart = System.nanoTime();
                wallPausedTotal = 0;
                wallPausedAt = running() ? 0 : wallStart;
                clockStarted = true;
            }
        }
    }

    /** The current media time in seconds (frozen until the clock starts). */
    private double clock() {
        if (!clockStarted) {
            return sessionStart;
        }
        long now = wallPausedAt != 0 ? wallPausedAt : System.nanoTime();
        return sessionStart + (now - wallStart - wallPausedTotal) / 1e9;
    }

    private void videoLoop(int gen, double from) {
        String ff = VideoConverter.command();
        Process p = null;
        try {
            p = new ProcessBuilder(ff, "-v", "error", "-threads", "2",
                    "-ss", String.format(java.util.Locale.ROOT, "%.3f", from),
                    "-i", video.getAbsolutePath(), "-an", "-sn",
                    "-vf", "fps=" + fps + ",scale=" + frameW + ":" + frameH + ":flags=fast_bilinear",
                    "-pix_fmt", "bgra", "-f", "rawvideo", "-")
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            videoProc = p;
            InputStream in = new java.io.BufferedInputStream(p.getInputStream(), frameW * frameH * 4 * 2);
            byte[] bytes = new byte[frameW * frameH * 4];
            int n = 0, window = 0, late = 0;
            while (live(gen)) {
                long readStart = System.nanoTime();
                if (in.readNBytes(bytes, 0, bytes.length) < bytes.length) {
                    break;                                              // end of the video (or a failure)
                }
                decodeMs = decodeMs * 0.9 + ((System.nanoTime() - readStart) / 1e6) * 0.1;
                BufferedImage img = buffers[nextBuffer = (nextBuffer + 1) % buffers.length];
                int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(px);

                double pts = from + n / (double) fps;
                if (n > 0) {
                    // wait for this frame's time, or drop it when the picture is already late
                    while (live(gen) && running() && pts - clock() > 0.004) {
                        if (!clockStarted && System.nanoTime() - sessionBegan > 1_500_000_000L) {
                            startClock();                               // the sound never began: carry on without it
                        }
                        long ms = (long) Math.min(8, Math.max(1, (pts - clock()) * 1000));
                        Thread.sleep(ms);
                    }
                    if (live(gen) && running()) {
                        window++;
                        if (clock() - pts > 0.12 && n % 2 == 1) {
                            late++;
                            statSkipped.incrementAndGet();
                            n++;
                            continue;                                    // skip: the sound is ahead
                        }
                        if (window >= 90) {
                            // 90 frames seen: if many were late, step down a quality level for the next loop
                            if (late >= 12 && level < SIDES.length - 1 && level == appliedLevel) {
                                level++;
                                log("slow computer: video quality lowered to " + SIDES[level] + " px / "
                                        + RATES[level] + " fps");
                            }
                            window = 0;
                            late = 0;
                        }
                    }
                }
                // paused: hold this picture until playing again
                if (n > 0 && !running()) {
                    synchronized (lock) {
                        while (live(gen) && !running()) {
                            lock.wait(100);
                        }
                    }
                    if (!live(gen)) {
                        break;
                    }
                }
                front = img;
                statShown.incrementAndGet();
                if (!ready || n == 0) {                                 // first picture of every session, loops included
                    ready = true;
                    if (running() && !hasAudio) {
                        startClock();
                    }
                    if (running() && hasAudio && audioProc == null) {
                        startAudio(gen);
                    }
                    refresh();
                }
                surface.repaint();
                if (duration > 0) {
                    progress = Math.max(0, Math.min(1, pts / duration));
                    // no separate strip repaint: it skipped the page's shade layer and made the bottom edge flash
                }
                if (n == 0 && !running()) {
                    synchronized (lock) {
                        while (live(gen) && !running()) {
                            lock.wait(100);
                        }
                    }
                }
                n++;
            }
            if (live(gen)) {
                if (n == 0) {
                    fail("ffmpeg produced no picture.");
                } else {
                    startSessionLater(gen);                              // loop
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (live(gen)) {
                fail(e.getMessage());
            }
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }

    private void startSessionLater(int gen) {
        if (live(gen)) {
            startSession(0);
        }
    }

    // ------------------------------------------------------------------ sound

    private volatile int audioStartedGen = -1;

    private void startAudio(int gen) {
        synchronized (lock) {
            if (audioStartedGen == gen) {
                return;                                            // this session already has its sound
            }
            audioStartedGen = gen;
        }
        final String ff = VideoConverter.command();
        final double from = sessionStart;
        Thread t = new Thread(() -> {
            Process p = null;
            try {
                AudioFormat fmt = new AudioFormat(RATE, 16, 2, true, false);
                SourceDataLine l = line;
                if (l == null) {
                    l = AudioSystem.getSourceDataLine(fmt);
                    l.open(fmt, RATE / 2 * 4);                           // about 0.5 s: survives short CPU hiccups
                    line = l;
                }
                p = new ProcessBuilder(ff, "-v", "error", "-ss", String.format(java.util.Locale.ROOT, "%.3f", from),
                        "-i", video.getAbsolutePath(), "-vn", "-sn", "-f", "s16le", "-ac", "2", "-ar", "" + RATE, "-")
                        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                audioProc = p;
                InputStream in = p.getInputStream();
                byte[] buf = new byte[RATE / 25 * 4];
                byte[] silence = new byte[buf.length];
                if (!live(gen)) {
                    return;
                }
                l.flush();
                linePos0 = l.getLongFramePosition();
                if (running()) {
                    l.start();
                }
                boolean clockGo = false;
                boolean ended = false;
                while (live(gen)) {
                    int got = 0;
                    if (!ended) {
                        got = in.readNBytes(buf, 0, buf.length);
                        got -= got % 4;
                        if (got <= 0) {
                            ended = true;                                // keep the clock going with silence
                        }
                    }
                    byte[] data = ended ? silence : buf;
                    int len = ended ? silence.length : got;
                    if (ReelVideoView.isMuted() && !ended) {
                        java.util.Arrays.fill(buf, 0, len, (byte) 0);
                    }
                    int off = 0;
                    while (off < len && live(gen)) {
                        if (!running()) {
                            synchronized (lock) {
                                lock.wait(100);
                            }
                            continue;
                        }
                        off += l.write(data, off, len - off);
                        if (!clockGo) {
                            clockGo = true;
                            startClock();                               // the picture follows the sound from here
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                // no sound device or ffmpeg trouble: the picture simply runs on the wall clock
                if (live(gen)) {
                    audioClock = false;
                    startClock();                                    // this session runs without sound; the next loop tries again
                }
            } finally {
                if (p != null) {
                    p.destroyForcibly();
                }
                if (audioProc == p) {
                    audioProc = null;
                }
            }
        }, "reel-audio");
        t.setPriority(Thread.MAX_PRIORITY);                      // the sound must never run dry
        t.setDaemon(true);
        t.start();
    }

    private double clockSafe() {
        try {
            return clock();
        } catch (Throwable t) {
            return sessionStart;
        }
    }

    // ------------------------------------------------------------------ seeking, errors

    private void seekToFraction(double f) {
        if (!ready || duration <= 0) {
            return;
        }
        progress = Math.max(0, Math.min(1, f));
        overlay.repaint();
        final double target = progress * duration;
        new Thread(() -> {
            ready = true;
            startSession(Math.min(target, Math.max(0, duration - 0.3)));
        }, "reel-seek").start();
    }

    private void log(String text) {
        try {
            Files.writeString(Paths.get(System.getProperty("user.dir"), "eduvibe-video.log"),
                    LocalDateTime.now() + "  reel(ffmpeg)  " + text + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // the log is a bonus
        }
    }

    private void fail(String message) {
        failed = true;
        ready = false;
        lastError = message;
        stopSession();
        try {
            Files.writeString(Paths.get(System.getProperty("user.dir"), "eduvibe-video.log"),
                    LocalDateTime.now() + "  reel(ffmpeg)  " + video.getAbsolutePath() + "\n  " + message + "\n",
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

    private void refresh() {
        SwingUtilities.invokeLater(() -> {
            boolean busy = requested && !failed && !ready;
            if (busy && !spinner.isRunning()) {
                spinner.start();
            } else if (!busy && spinner.isRunning()) {
                spinner.stop();
            }
            overlay.repaint();
            surface.repaint();
        });
    }

    // ------------------------------------------------------------------ painting

    private final class Surface extends JComponent {
        Surface() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            BufferedImage img = front;
            if (img == null) {
                return;
            }
            int w = getWidth(), h = getHeight();
            double s = Math.min(w / (double) img.getWidth(), h / (double) img.getHeight());
            int dw = (int) Math.round(img.getWidth() * s), dh = (int) Math.round(img.getHeight() * s);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        }
    }

    @Override
    public double progressNow() {
        return ready && !failed ? Math.max(0, Math.min(1, progress)) : -1;
    }

    @Override
    public boolean scrubbingNow() {
        return overlay.scrubbing;
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
                public void mouseReleased(MouseEvent e) {
                    if (scrubbing) {
                        scrubbing = false;                       // the bar was staying thick after a click
                        surface.repaint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (scrubbing) {
                        seekToFraction(e.getX() / (double) Math.max(1, getWidth()));
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getY() >= getHeight() - 18 && ready) {
                        return;
                    }
                    if (e.getClickCount() >= 2) {
                        toggle();
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
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.bold(16));
                centered(g2, "This video can't be played here", w, h / 2 - 8);
                g2.setFont(Theme.regular(13));
                g2.setColor(new Color(210, 218, 240));
                centered(g2, "Click to open it in your video player", w, h / 2 + 16);
            } else if (requested && !ready) {
                int d = 46;
                int x = (w - d) / 2, y = (h - d) / 2 - 10;
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 255, 255, 50));
                g2.drawOval(x, y, d, d);
                g2.setColor(Color.WHITE);
                int angle = (int) ((System.currentTimeMillis() / 4) % 360);
                g2.drawArc(x, y, d, d, -angle, 100);
                g2.setFont(Theme.regular(13));
                g2.setColor(new Color(220, 226, 245));
                centered(g2, "Loading...", w, y + d + 26);
            } else if (ready && userPaused) {
                int d = 78;
                int x = (w - d) / 2, y = (h - d) / 2;
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillOval(x, y, d, d);
                g2.setColor(Color.WHITE);
                g2.fillPolygon(new int[]{x + 31, x + 31, x + 57}, new int[]{y + 24, y + 54, y + 39}, 3);
            }
            g2.dispose();
        }

        private void centered(Graphics2D g2, String text, int w, int baseline) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, baseline);
        }
    }
}
