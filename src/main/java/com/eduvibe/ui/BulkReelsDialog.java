package com.eduvibe.ui;

import com.eduvibe.service.PostService;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.util.VideoCheck;
import com.eduvibe.util.VideoConverter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * com.eduvibe.ui.BulkReelsDialog  (NEW FILE)
 *
 * Post many videos as Reels in one go instead of one by one. Each video becomes its own Reel;
 * its title comes from the file name. A video the built-in player cannot play is converted first
 * when ffmpeg is installed. Posting follows the normal rules (content filter, +15 points per post).
 */
public final class BulkReelsDialog extends JDialog {

    private static final int MAX_FILES = 25;
    private static final int MAX_TITLE = 80;

    private final int userId;
    private final Runnable onDone;
    private final PostService postService = new PostService();

    private final List<File> files = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[] {"Video", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JProgressBar bar = new JProgressBar(0, 1);
    private final JLabel summary = new JLabel(" ");
    private final JRadioButton entertainment = new JRadioButton("Entertainment", true);
    private final JRadioButton academic = new JRadioButton("Academic");
    private final RoundedButton addButton = new RoundedButton("Add videos...", RoundedButton.Style.OUTLINE);
    private final RoundedButton clearButton = new RoundedButton("Clear list", RoundedButton.Style.GHOST);
    private final RoundedButton startButton = new RoundedButton("Post all as Reels", RoundedButton.Style.PRIMARY);
    private final RoundedButton closeButton = new RoundedButton("Close", RoundedButton.Style.SECONDARY);

    private boolean running;
    private boolean postedAny;

    public static void open(Component parent, int userId, Runnable onDone) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        BulkReelsDialog dialog = new BulkReelsDialog(owner, userId, onDone);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private BulkReelsDialog(Window owner, int userId, Runnable onDone) {
        super(owner, "Post many reels at once", ModalityType.APPLICATION_MODAL);
        this.userId = userId;
        this.onDone = onDone;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(20, 22, 18, 22));

        // ---- top: title, explanation, category
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Post many reels at once");
        title.setFont(Theme.bold(18));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String ffmpegNote = VideoConverter.isAvailable()
                ? "Videos the app cannot play are converted automatically."
                : "ffmpeg was not found, so videos in other formats will be posted unconverted.";
        JLabel info = new JLabel("<html>Each video becomes its own Reel, titled from its file name. "
                + ffmpegNote + " Every post earns the normal points. Up to " + MAX_FILES + " videos at a time.</html>");
        info.setFont(Theme.regular(12));
        info.setForeground(Theme.MUTED);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel category = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        category.setOpaque(false);
        category.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel catLabel = new JLabel("Category:");
        catLabel.setFont(Theme.bold(12));
        catLabel.setForeground(Theme.TEXT);
        for (JRadioButton rb : new JRadioButton[] {entertainment, academic}) {
            rb.setOpaque(false);
            rb.setFont(Theme.regular(13));
            rb.setForeground(Theme.TEXT);
            rb.setFocusPainted(false);
        }
        ButtonGroup group = new ButtonGroup();
        group.add(entertainment);
        group.add(academic);
        category.add(catLabel);
        category.add(entertainment);
        category.add(academic);

        top.add(title);
        top.add(Box.createVerticalStrut(6));
        top.add(info);
        top.add(Box.createVerticalStrut(10));
        top.add(category);

        // ---- middle: the list
        table.setBackground(Theme.CARD);
        table.setForeground(Theme.TEXT);
        table.setFont(Theme.regular(13));
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(Theme.CARD_LIGHT);
        table.setSelectionForeground(Theme.TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Theme.CARD_LIGHT);
        table.getTableHeader().setForeground(Theme.MUTED);
        table.getTableHeader().setFont(Theme.bold(12));
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.getColumnModel().getColumn(0).setPreferredWidth(330);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.setPreferredSize(new Dimension(620, 230));

        // ---- bottom: progress, summary, buttons
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(100, 8));
        bar.setBorderPainted(false);
        bar.setBackground(Theme.BORDER);
        bar.setForeground(Theme.BLUE);
        bar.setVisible(false);

        summary.setFont(Theme.regular(12));
        summary.setForeground(Theme.MUTED);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(addButton);
        leftButtons.add(clearButton);
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(closeButton);
        rightButtons.add(startButton);
        buttons.add(leftButtons, BorderLayout.WEST);
        buttons.add(rightButtons, BorderLayout.EAST);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottom.add(bar);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(summary);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(buttons);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        addButton.addActionListener(e -> chooseFiles());
        clearButton.addActionListener(e -> clearList());
        startButton.addActionListener(e -> start());
        closeButton.addActionListener(e -> closeDialog());

        updateButtons();
        pack();
        setMinimumSize(new Dimension(560, 420));
    }

    // ---------------------------------------------------------------- list handling

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose videos");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Videos (mp4, mov, avi, mkv, webm)",
                "mp4", "mov", "avi", "mkv", "webm"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        int skipped = 0;
        for (File f : chooser.getSelectedFiles()) {
            if (files.size() >= MAX_FILES) {
                skipped++;
                continue;
            }
            if (!f.isFile() || !isVideo(f.getName()) || contains(f)) {
                continue;
            }
            files.add(f);
            model.addRow(new Object[] {f.getName(), "Waiting"});
        }
        if (skipped > 0) {
            Dialogs.warning(this, "Too many videos",
                    "Only " + MAX_FILES + " videos can be posted at a time. The rest were not added.");
        }
        summary.setText(files.size() + (files.size() == 1 ? " video ready." : " videos ready."));
        updateButtons();
    }

    private boolean contains(File candidate) {
        for (File f : files) {
            if (f.getAbsolutePath().equals(candidate.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    private void clearList() {
        files.clear();
        model.setRowCount(0);
        summary.setText(" ");
        bar.setVisible(false);
        updateButtons();
    }

    private void updateButtons() {
        addButton.setEnabled(!running);
        clearButton.setEnabled(!running && !files.isEmpty());
        startButton.setEnabled(!running && !files.isEmpty());
        closeButton.setEnabled(!running);
        entertainment.setEnabled(!running);
        academic.setEnabled(!running);
    }

    private static boolean isVideo(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".avi")
                || n.endsWith(".mkv") || n.endsWith(".webm");
    }

    private void closeDialog() {
        if (running) {
            return;                 // finish posting first, so nothing is left half done
        }
        dispose();
        if (postedAny && onDone != null) {
            onDone.run();
        }
    }

    // ---------------------------------------------------------------- posting

    private void start() {

        final List<File> batch = new ArrayList<>(files);
        final String category = academic.isSelected() ? "academic" : "entertainment";

        running = true;
        postedAny = false;
        bar.setMaximum(batch.size());
        bar.setValue(0);
        bar.setVisible(true);
        summary.setText("Working...");
        for (int i = 0; i < batch.size(); i++) {
            model.setValueAt("Waiting", i, 1);
        }
        updateButtons();

        new SwingWorker<int[], Object[]>() {

            @Override
            protected int[] doInBackground() {
                int posted = 0, skipped = 0, failed = 0;

                for (int i = 0; i < batch.size(); i++) {
                    File original = batch.get(i);
                    String title = titleFrom(original);

                    try {
                        publish(new Object[] {i, "Checking..."});
                        File toPost = original;
                        String ext = extension(original.getName());
                        String note = "";

                        if (!VideoCheck.inspect(original).playable()) {
                            if (VideoConverter.isAvailable()) {
                                publish(new Object[] {i, "Converting..."});
                                try {
                                    toPost = VideoConverter.convertToPlayableMp4(original);
                                    ext = "mp4";
                                } catch (IOException ex) {
                                    skipped++;
                                    publish(new Object[] {i, "Skipped: could not convert"});
                                    continue;
                                }
                            } else {
                                note = " (may not play in the app)";
                            }
                        }

                        publish(new Object[] {i, "Posting..."});
                        Path uploads = Paths.get(System.getProperty("user.dir"), "uploads");
                        Files.createDirectories(uploads);
                        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
                        Path destination = uploads.resolve(stored);
                        Files.copy(toPost.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                        boolean ok = false;
                        String problem = "not saved";
                        try {
                            ok = postService.createPost(userId, category, title, title,
                                    "video", "uploads/" + stored, true);
                        } catch (Exception ex) {
                            problem = ex.getMessage() == null ? "not saved" : ex.getMessage();
                        }

                        if (ok) {
                            posted++;
                            publish(new Object[] {i, "Posted" + note});
                        } else {
                            Files.deleteIfExists(destination);      // nothing is left behind for a failed post
                            failed++;
                            publish(new Object[] {i, "Failed: " + shorten(problem)});
                        }
                    } catch (Exception ex) {
                        failed++;
                        publish(new Object[] {i, "Failed: " + shorten(ex.getMessage())});
                    }
                }
                return new int[] {posted, skipped, failed};
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] c : chunks) {
                    int row = (Integer) c[0];
                    String status = (String) c[1];
                    model.setValueAt(status, row, 1);
                    if (status.startsWith("Posted") || status.startsWith("Failed") || status.startsWith("Skipped")) {
                        bar.setValue(Math.min(bar.getMaximum(), bar.getValue() + 1));
                    }
                }
            }

            @Override
            protected void done() {
                running = false;
                try {
                    int[] r = get();
                    postedAny = r[0] > 0;
                    summary.setText(r[0] + " posted, " + r[1] + " skipped, " + r[2] + " failed.");
                } catch (Exception ex) {
                    summary.setText("Stopped: " + shorten(ex.getMessage()));
                }
                updateButtons();
            }
        }.execute();
    }

    private static String titleFrom(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        if (dot > 0) {
            n = n.substring(0, dot);
        }
        n = n.replaceAll("[_\\-]+", " ").replaceAll("\\s+", " ").trim();
        if (n.isEmpty()) {
            n = "New reel";
        }
        if (n.length() > MAX_TITLE) {
            n = n.substring(0, MAX_TITLE).trim();
        }
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "mp4";
    }

    private static String shorten(String text) {
        if (text == null || text.isBlank()) {
            return "unknown problem";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() > 40 ? t.substring(0, 40) + "..." : t;
    }

    /** Colours the status text: green when posted, red when it did not work, gold while busy. */
    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            String s = value == null ? "" : value.toString();
            setBorder(new EmptyBorder(0, 8, 0, 8));
            if (s.startsWith("Posted")) {
                setForeground(Theme.GREEN);
            } else if (s.startsWith("Failed") || s.startsWith("Skipped")) {
                setForeground(Theme.RED);
            } else if (s.endsWith("...")) {
                setForeground(Theme.GOLD);
            } else {
                setForeground(Theme.MUTED);
            }
            return this;
        }
    }
}
