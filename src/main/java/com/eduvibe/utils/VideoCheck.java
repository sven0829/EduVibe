package com.eduvibe.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * com.eduvibe.util.VideoCheck  (NEW FILE)
 *
 * Looks inside a video file (without playing it) to tell whether EduVibe's built-in player can
 * play it. The player handles MP4 with H.264 video and AAC sound. Anything else (HEVC / H.265 from
 * phones, AV1, VP9, MOV / MKV / AVI / WEBM containers, AC-3 or Opus sound) fails with a
 * "format not supported" error, so the post screen warns about it before the file is uploaded.
 *
 * It only reads the MP4 header box ("moov"), so it is fast even for large files.
 */
public final class VideoCheck {

    private static final long MAX_MOOV = 24L * 1024 * 1024;

    public record Result(boolean playable, String message) {
        static Result ok() {
            return new Result(true, "");
        }

        static Result bad(String message) {
            return new Result(false, message);
        }
    }

    private VideoCheck() {
    }

    public static Result inspect(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";

        if (!ext.equals("mp4") && !ext.equals("m4v")) {
            return Result.bad("This is a ." + ext + " file. The built-in player only plays MP4 files "
                    + "(H.264 video + AAC sound). Others can still be posted, but they will only open "
                    + "in your computer's own video player.");
        }

        byte[] moov;
        try {
            moov = readMoov(file);
        } catch (IOException e) {
            return Result.ok();            // cannot tell, so do not warn
        }
        if (moov == null) {
            return Result.ok();
        }

        String text = new String(moov, StandardCharsets.ISO_8859_1);
        boolean h264 = text.contains("avc1") || text.contains("avc3");
        String other = null;
        if (text.contains("hvc1") || text.contains("hev1")) {
            other = "H.265 (HEVC)";
        } else if (text.contains("av01")) {
            other = "AV1";
        } else if (text.contains("vp09")) {
            other = "VP9";
        }

        if (!h264 && other != null) {
            return Result.bad("This video uses " + other + ", which the built-in player cannot play. "
                    + "Phones and screen recorders often save it. Convert it to MP4 with H.264 video "
                    + "and AAC sound (HandBrake preset \"Fast 1080p30\") for it to play inside EduVibe.");
        }

        if (h264) {
            boolean aac = text.contains("mp4a");
            String badAudio = text.contains("ac-3") ? "AC-3" : text.contains("ec-3") ? "E-AC-3"
                    : text.contains("Opus") ? "Opus" : text.contains("fLaC") ? "FLAC" : null;
            if (!aac && badAudio != null) {
                return Result.bad("The sound in this video is " + badAudio + ", which the built-in player "
                        + "cannot play. Convert the sound to AAC (HandBrake: Audio > AAC) so it plays "
                        + "inside EduVibe.");
            }
        }
        return Result.ok();
    }

    /** Walks the top-level MP4 boxes and returns the "moov" box contents, or null if not found. */
    private static byte[] readMoov(File file) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
            long pos = 0;
            long len = in.length();
            byte[] head = new byte[16];
            while (pos + 8 <= len) {
                in.seek(pos);
                in.readFully(head, 0, 8);
                long size = ((head[0] & 0xFFL) << 24) | ((head[1] & 0xFFL) << 16)
                        | ((head[2] & 0xFFL) << 8) | (head[3] & 0xFFL);
                String type = new String(head, 4, 4, StandardCharsets.ISO_8859_1);
                long header = 8;
                if (size == 1) {
                    in.readFully(head, 8, 8);
                    size = 0;
                    for (int i = 8; i < 16; i++) {
                        size = (size << 8) | (head[i] & 0xFFL);
                    }
                    header = 16;
                } else if (size == 0) {
                    size = len - pos;
                }
                if (size < header) {
                    return null;
                }
                if (type.equals("moov")) {
                    long body = size - header;
                    if (body <= 0 || body > MAX_MOOV) {
                        return null;
                    }
                    byte[] data = new byte[(int) body];
                    in.seek(pos + header);
                    in.readFully(data);
                    return data;
                }
                pos += size;
            }
        }
        return null;
    }
}
