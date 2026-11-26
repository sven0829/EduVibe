package com.eduvibe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * com.eduvibe.util.VideoConverter  (NEW FILE)
 *
 * Turns a video the built-in player cannot play into an MP4 with H.264 video and AAC sound,
 * using ffmpeg when it is installed. Java and JavaFX cannot convert video themselves, so without
 * ffmpeg the app keeps its normal warning and nothing here runs.
 *
 * ffmpeg is looked for, in this order: the EDUVIBE_FFMPEG environment variable, an ffmpeg
 * file next to the app (ffmpeg.exe or ffmpeg/bin/ffmpeg.exe), common install folders, then PATH.
 * Large videos are scaled down (landscape to 1280 wide, portrait to 720 wide), which also makes
 * playback smoother on small laptops.
 */
public final class VideoConverter {

    private static final long TIMEOUT_MINUTES = 15;

    private static volatile String cachedCommand;
    private static volatile boolean searched;

    private VideoConverter() {
    }

    /** True when an ffmpeg program was found and answers to -version. */
    public static boolean isAvailable() {
        return ffmpegCommand() != null;
    }

    /** The ffmpeg program to run, or null when none was found. */
    public static String command() {
        return ffmpegCommand();
    }

    private static synchronized String ffmpegCommand() {
        if (searched) {
            return cachedCommand;
        }
        searched = true;

        List<String> candidates = new ArrayList<>();
        String env = System.getenv("EDUVIBE_FFMPEG");
        if (env != null && !env.isBlank()) {
            candidates.add(env.trim());
        }
        candidates.add("ffmpeg.exe");
        candidates.add("ffmpeg/bin/ffmpeg.exe");
        candidates.add("ffmpeg");
        candidates.add("C:\\ffmpeg\\bin\\ffmpeg.exe");
        candidates.add("C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe");

        // winget installs ffmpeg under the user's own folders; look there too, so the app finds it
        // right away without a restart or a PATH change.
        String local = System.getenv("LOCALAPPDATA");
        if (local != null && !local.isBlank()) {
            candidates.add(Path.of(local, "Microsoft", "WinGet", "Links", "ffmpeg.exe").toString());
            candidates.addAll(findIn(Path.of(local, "Microsoft", "WinGet", "Packages")));
        }
        String programData = System.getenv("ProgramData");
        if (programData != null && !programData.isBlank()) {
            candidates.addAll(findIn(Path.of(programData, "chocolatey", "lib")));
        }

        for (String c : candidates) {
            if (works(c)) {
                cachedCommand = c;
                break;
            }
        }
        return cachedCommand;
    }

    /** ffmpeg.exe files below a folder (a few levels deep); empty when the folder does not exist. */
    private static List<String> findIn(Path root) {
        List<String> found = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return found;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(root, 6)) {
            walk.filter(f -> f.getFileName() != null
                            && f.getFileName().toString().equalsIgnoreCase("ffmpeg.exe"))
                    .limit(5)
                    .forEach(f -> found.add(f.toString()));
        } catch (Exception ignored) {
            // an unreadable folder just means nothing is found there
        }
        return found;
    }

    private static boolean works(String command) {
        try {
            Process p = new ProcessBuilder(command, "-version")
                    .redirectErrorStream(true).start();
            drain(p, 5);
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts the file and returns the new MP4. The original is never changed.
     *
     * @throws IOException with a readable message when ffmpeg is missing or the conversion fails
     */
    public static File convertToPlayableMp4(File source) throws IOException {

        String ffmpeg = ffmpegCommand();
        if (ffmpeg == null) {
            throw new IOException("ffmpeg was not found on this computer.");
        }

        String name = source.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;

        Path dir = Files.createTempDirectory("eduvibe-video-");
        File out = dir.resolve(base + ".mp4").toFile();
        out.deleteOnExit();
        dir.toFile().deleteOnExit();
        return convertTo(ffmpeg, source, out, false);
    }

    private static final Object CACHE_LOCK = new Object();

    /**
     * A playable copy of an already stored video, for playback only. The stored file is never
     * changed. The copy is kept in the system temp folder, so a video is converted once and then
     * reused; conversions run one at a time so a small laptop is not overloaded.
     *
     * @throws IOException when ffmpeg is missing or the conversion fails
     */
    public static File playableCopy(File original) throws IOException {
        return playableCopy(original, 1);
    }

    /**
     * Same as {@link #playableCopy(File)} with a choice of how careful the conversion is:
     * attempt 1 keeps the sound, attempt 2 is the last resort and makes a silent copy
     * (some files carry an audio track the built-in player refuses).
     */
    public static File playableCopy(File original, int attempt) throws IOException {
        final boolean noAudio = attempt >= 2;

        String ffmpeg = ffmpegCommand();
        if (ffmpeg == null) {
            throw new IOException("ffmpeg was not found on this computer.");
        }

        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "eduvibe-video-cache");
        Files.createDirectories(cacheDir);

        String safe = original.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        String key = safe + "-" + original.lastModified() + "-" + original.length()
                + (noAudio ? "-s2" : "-s1") + ".mp4";
        File finalFile = cacheDir.resolve(key).toFile();

        synchronized (CACHE_LOCK) {
            if (finalFile.isFile() && finalFile.length() > 0) {
                return finalFile;
            }
            File part = cacheDir.resolve(key + ".part.mp4").toFile();
            try {
                convertTo(ffmpeg, original, part, noAudio);
                Files.move(part.toPath(), finalFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(part.toPath());
            }
            return finalFile;
        }
    }

    /** Picture size and frame rate of a video, read from ffmpeg's own report. */
    private record Probe(int width, int height, double fps) { }

    private static Probe probe(String ffmpeg, File file) {
        try {
            Process process = new ProcessBuilder(ffmpeg, "-hide_banner", "-nostdin", "-i", file.getAbsolutePath())
                    .redirectErrorStream(true).start();
            StringBuilder out = new StringBuilder();
            try (java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("Video:")) {
                        out.append(line);
                        break;
                    }
                }
            }
            process.destroyForcibly();
            java.util.regex.Matcher size = java.util.regex.Pattern.compile(",\\s*(\\d{2,5})x(\\d{2,5})")
                    .matcher(out);
            java.util.regex.Matcher fps = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*fps")
                    .matcher(out);
            if (!size.find()) {
                return null;
            }
            return new Probe(Integer.parseInt(size.group(1)), Integer.parseInt(size.group(2)),
                    fps.find() ? Double.parseDouble(fps.group(1)) : 30);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * For playback only: when a video is bigger than the reel will ever show (more than 1000 px on its long
     * side) or runs above 30 frames a second, a lighter copy is made and cached in the temp folder. It looks
     * the same in the reel frame but is far easier on a small laptop. The stored video is never touched.
     * A reel that is about to be watched (urgent) may use more of the processor than one prepared in
     * the background, so a video that is playing is never slowed down by the next one being prepared.
     * Returns null when the video is already light, or when ffmpeg is not available.
     */
    /** Returned by {@link #lightCopyIfHeavy} when the work was skipped because nobody wants the video any more. */
    public static final File SKIPPED = new File("skipped");

    public static File lightCopyIfHeavy(File original, boolean urgent, java.util.function.BooleanSupplier stillWanted) {
        try {
            String ffmpeg = ffmpegCommand();
            if (ffmpeg == null) {
                return null;
            }
            Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "eduvibe-video-cache");
            Files.createDirectories(cacheDir);
            String safe = original.getName().replaceAll("[^A-Za-z0-9._-]", "_");
            File finalFile = cacheDir.resolve(safe + "-" + original.lastModified() + "-" + original.length()
                    + "-lt2.mp4").toFile();
            if (finalFile.isFile() && finalFile.length() > 0) {
                return finalFile;
            }
            File marker = cacheDir.resolve(finalFile.getName() + ".light").toFile();
            if (marker.isFile()) {
                return null;                        // checked before: this one is already light
            }
            Probe info = probe(ffmpeg, original);
            if (info == null) {
                return null;
            }
            boolean bigPicture = Math.max(info.width(), info.height()) > 1000;
            boolean fastRate = info.fps() > 31;
            if (!bigPicture && !fastRate) {
                Files.writeString(marker.toPath(), "light");
                return null;
            }
            String vf = (fastRate ? "fps=30," : "")
                    + "scale='if(gt(iw,ih),trunc(min(960,iw)/2)*2,-2)':'if(gt(iw,ih),-2,trunc(min(960,ih)/2)*2)'"
                    + ",format=yuv420p";
            synchronized (CACHE_LOCK) {
                if (finalFile.isFile() && finalFile.length() > 0) {
                    return finalFile;
                }
                if (stillWanted != null && !stillWanted.getAsBoolean()) {
                    return SKIPPED;                 // scrolled past while waiting its turn: do no work for it
                }
                File part = cacheDir.resolve(finalFile.getName() + ".part.mp4").toFile();
                try {
                    convertTo(ffmpeg, original, part, false, vf, urgent ? 2 : 1);
                    Files.move(part.toPath(), finalFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(part.toPath());
                }
                return finalFile;
            }
        } catch (Exception e) {
            return null;                            // the original is simply played as it is
        }
    }

    /**
     * The settings are deliberately plain, because the built-in player (JavaFX) is picky: one video and
     * one audio track only (phone videos carry extra timecode/data tracks), a constant frame rate
     * (screen recordings and downloaded clips often vary), no B-frames, H.264 Main profile,
     * even picture size, and stereo AAC at 44.1 kHz.
     */
    private static File convertTo(String ffmpeg, File source, File out, boolean noAudio) throws IOException {
        return convertTo(ffmpeg, source, out, noAudio,
                "scale='trunc(if(gt(iw,ih),min(1920,iw),min(1080,iw))/2)*2':-2,format=yuv420p", 0);
    }

    private static File convertTo(String ffmpeg, File source, File out, boolean noAudio, String vf, int threads)
            throws IOException {

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpeg);
        cmd.add("-nostdin");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(source.getAbsolutePath());
        cmd.add("-map");
        cmd.add("0:v:0");
        if (!noAudio) {
            cmd.add("-map");
            cmd.add("0:a:0?");          // the ? keeps videos that have no sound working
        }
        cmd.add("-sn");
        cmd.add("-dn");
        cmd.add("-map_metadata");
        cmd.add("-1");
        cmd.add("-fps_mode");
        cmd.add("cfr");                 // constant frame rate, but the video's own rate is kept
        cmd.add("-vf");
        // never enlarged and only shrunk beyond Full HD, so the picture keeps its quality and proportions
        cmd.add(vf);
        boolean gentle = threads > 0;
        if (gentle) {
            cmd.add("-threads");
            cmd.add(String.valueOf(threads));   // leaves the processor free for the reel that is playing
        }
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-profile:v");
        cmd.add("main");
        cmd.add("-level");
        cmd.add("4.2");
        cmd.add("-preset");
        cmd.add(gentle ? "ultrafast" : "veryfast");     // the playback copy: least work, size does not matter
        cmd.add("-crf");
        cmd.add(gentle ? "23" : "20");
        cmd.add("-bf");
        cmd.add("0");
        cmd.add("-g");
        cmd.add("30");
        cmd.add("-pix_fmt");
        cmd.add("yuv420p");
        if (noAudio) {
            cmd.add("-an");
        } else {
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-ac");
            cmd.add("2");
            cmd.add("-ar");
            cmd.add("44100");
            cmd.add("-b:a");
            cmd.add("128k");
        }
        cmd.add("-movflags");
        cmd.add("+faststart");
        cmd.add(out.getAbsolutePath());

        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        Deque<String> tail = drain(process, 12);

        try {
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IOException("The conversion took longer than " + TIMEOUT_MINUTES
                        + " minutes and was stopped.");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("The conversion was interrupted.");
        }

        if (process.exitValue() != 0 || !out.isFile() || out.length() == 0) {
            String detail = tail.isEmpty() ? "" : " " + tail.peekLast();
            throw new IOException("ffmpeg could not convert this video." + detail);
        }
        return out;
    }

    /** Reads the program's output on a helper thread so it can never fill up and stall ffmpeg. */
    private static Deque<String> drain(Process process, int keepLines) {
        Deque<String> lines = new ArrayDeque<>();
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    synchronized (lines) {
                        if (!line.isBlank()) {
                            lines.addLast(line.trim());
                            while (lines.size() > keepLines) {
                                lines.removeFirst();
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
                // the process ended
            }
        }, "ffmpeg-output");
        t.setDaemon(true);
        t.start();
        return lines;
    }
}
