package com.eduvibe.ui.components;

import java.awt.Point;
import java.awt.event.MouseWheelListener;

/**
 * com.eduvibe.ui.components.ReelPlayer  (NEW FILE)
 *
 * What the Reels page needs from a video view. Two engines implement it: the JavaFX one
 * (ReelVideoView) and the lighter ffmpeg one (FfmpegReelView).
 */
public interface ReelPlayer {

    void setWheelListener(MouseWheelListener listener);

    boolean isPlaying();

    boolean isReady();

    boolean isFailed();

    int[] sampleColors();

    double ratio();

    void setOnDoubleClick(java.util.function.Consumer<Point> c);

    void setOnRatio(Runnable r);

    void prepare();

    void play();

    void pause();

    void toggle();

    void setMuted(boolean value);

    void release();

    /** A one-line description of how playback is going (frame rate, skipped frames, decode time). */
    /** Playback position 0..1 when the page should draw the progress bar itself (above its shading); -1 = the view draws its own. */
    default double progressNow() {
        return -1;
    }

    /** True while the user drags the progress bar (the bar is drawn thicker). */
    default boolean scrubbingNow() {
        return false;
    }

    default String debugInfo() {
        return "";
    }
}
