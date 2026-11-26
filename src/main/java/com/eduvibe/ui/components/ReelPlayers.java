package com.eduvibe.ui.components;

import com.eduvibe.util.VideoConverter;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * com.eduvibe.ui.components.ReelPlayers  (NEW FILE)
 *
 * Chooses the video engine for a reel. The ffmpeg engine is used when ffmpeg is installed (it is much
 * lighter on a modest laptop); otherwise, or when the viewer switched it off with V, the JavaFX engine.
 */
public final class ReelPlayers {

    private static final String KEY = "reelEngineJavaFx";

    private ReelPlayers() {
    }

    public static boolean usesFfmpeg() {
        return VideoConverter.isAvailable() && !Preferences.userRoot().node("eduvibe/ui").getBoolean(KEY, false);
    }

    /** Flips the engine for reels opened from now on (and after a restart). Returns true when ffmpeg is now used. */
    public static boolean toggle() {
        Preferences p = Preferences.userRoot().node("eduvibe/ui");
        p.putBoolean(KEY, !p.getBoolean(KEY, false));
        return usesFfmpeg();
    }

    public static ReelPlayer create(File video) {
        return usesFfmpeg() ? new FfmpegReelView(video) : new ReelVideoView(video);
    }
}
