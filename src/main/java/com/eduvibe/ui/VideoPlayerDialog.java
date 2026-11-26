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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * com.eduvibe.ui.components.VideoPlayerDialog  (NEW FILE)
 *
 * In-app video player (JavaFX Media hosted in Swing). Needs the JavaFX
 * dependencies in pom.xml. PostCard loads this class by reflection, so if the
 * dependencies are missing the app falls back to the system video player.
 *
 * Entry point: VideoPlayerDialog.show(Window owner, File video)
 */
public final class VideoPlayerDialog extends JDialog {

    private final JFXPanel fxPanel = new JFXPanel();
    private final JSlider seek = new JSlider(0, 1000, 0);
    private final JLabel timeLabel = new JLabel("0:00 / 0:00");
    private final RoundedButton playButton = new RoundedButton("Pause", RoundedButton.Style.PRIMARY);

    private volatile MediaPlayer player;
    private volatile boolean userSeeking = false;
    private volatile boolean ended = false;

    /** Called reflectively by PostCard. */
    public static void show(Window owner, File video) {
        SwingUtilities.invokeLater(() -> new VideoPlayerDialog(owner, video).setVisible(true));
    }

    private VideoPlayerDialog(Window owner, File video) {
        super(owner, "EduVibe | " + video.getName(), ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 560);
        setMinimumSize(new Dimension(520, 380));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        fxPanel.setBackground(java.awt.Color.BLACK);
        root.add(fxPanel, BorderLayout.CENTER);
        root.add(buildControls(), BorderLayout.SOUTH);
        setContentPane(root);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                disposePlayer();
            }
        });

        Platform.setImplicitExit(false);
        Platform.runLater(() -> initFx(video));
    }

    private JPanel buildControls() {

        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(Theme.SIDEBAR);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        playButton.addActionListener(e -> togglePlay());

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

        bar.add(playButton, BorderLayout.WEST);
        bar.add(seek, BorderLayout.CENTER);
        bar.add(timeLabel, BorderLayout.EAST);
        return bar;
    }

    private void initFx(File video) {
        try {
            Media media = new Media(video.toURI().toString());
            MediaPlayer mp = new MediaPlayer(media);
            MediaView view = new MediaView(mp);
            view.setPreserveRatio(true);

            StackPane pane = new StackPane(view);
            pane.setStyle("-fx-background-color: black;");
            view.fitWidthProperty().bind(pane.widthProperty());
            view.fitHeightProperty().bind(pane.heightProperty());

            fxPanel.setScene(new Scene(pane, Color.BLACK));

            mp.currentTimeProperty().addListener((obs, old, now) -> updateTime(mp, now));
            mp.setOnEndOfMedia(() -> {
                ended = true;
                SwingUtilities.invokeLater(() -> playButton.setText("Replay"));
            });
            mp.setOnError(() -> showError(mp.getError() == null
                    ? "Unknown playback error." : mp.getError().getMessage()));
            media.setOnError(() -> showError(media.getError() == null
                    ? "Unsupported video format." : media.getError().getMessage()));

            player = mp;
            mp.play();

        } catch (Throwable t) {
            showError(t.getMessage());
        }
    }

    private void togglePlay() {
        MediaPlayer mp = player;
        if (mp == null) {
            return;
        }
        Platform.runLater(() -> {
            if (ended) {
                ended = false;
                mp.seek(Duration.ZERO);
                mp.play();
                SwingUtilities.invokeLater(() -> playButton.setText("Pause"));
            } else if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                mp.pause();
                SwingUtilities.invokeLater(() -> playButton.setText("Play"));
            } else {
                mp.play();
                SwingUtilities.invokeLater(() -> playButton.setText("Pause"));
            }
        });
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

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> Dialogs.error(this, "Video error",
                "This video could not be played in the app.\n\n"
                + (message == null ? "" : message)));
    }

    private void disposePlayer() {
        MediaPlayer mp = player;
        player = null;
        if (mp != null) {
            Platform.runLater(() -> {
                mp.stop();
                mp.dispose();
            });
        }
    }
}
