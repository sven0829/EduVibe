package com.eduvibe.ui;

import com.eduvibe.dao.SavedPostDAO;
import com.eduvibe.model.Post;
import com.eduvibe.ui.components.EmptyState;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * com.eduvibe.ui.SavedPostsPanel  (REPLACES the old file)
 *
 * Saved posts in the same look as the feed: header with count + Refresh, one centred column of
 * post cards that are never stretched taller than their content. The page scrolls as a whole.
 */
public class SavedPostsPanel extends JPanel {

    private final int currentUserId;
    private final SavedPostDAO savedPostDAO = new SavedPostDAO();

    private final JPanel postsContainer = new JPanel();
    private final JLabel countLabel = new JLabel(" ");
    private final com.eduvibe.ui.components.PageHero hero = new com.eduvibe.ui.components.PageHero(
            Icons.Type.BOOKMARK, "Saved Posts", "Everything you bookmarked, in one place.",
            com.eduvibe.ui.components.PageHero.SAVED[0], com.eduvibe.ui.components.PageHero.SAVED[1]);

    public SavedPostsPanel(int currentUserId) {
        this.currentUserId = currentUserId;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(0, 0, 10, 0));

        add(buildHeader(), BorderLayout.NORTH);

        postsContainer.setLayout(new BoxLayout(postsContainer, BoxLayout.Y_AXIS));
        postsContainer.setBackground(Theme.BACKGROUND);
        add(postsContainer, BorderLayout.CENTER);

        loadSavedPosts();
    }

    private JPanel buildHeader() {

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel subtitle = new JLabel(" ");
        subtitle.setFont(Theme.regular(13));
        subtitle.setForeground(Theme.MUTED);

        countLabel.setFont(Theme.bold(12));
        countLabel.setForeground(Theme.BLUE_LIGHT);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(subtitle);
        text.add(Box.createVerticalStrut(4));
        text.add(countLabel);

        RoundedButton refresh = new RoundedButton("Refresh", RoundedButton.Style.SECONDARY);
        refresh.addActionListener(e -> loadSavedPosts());
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(refresh);

        header.add(text, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);
        wrap.add(hero, BorderLayout.NORTH);
        wrap.add(header, BorderLayout.CENTER);
        return wrap;
    }

    public void loadSavedPosts() {

        postsContainer.removeAll();
        countLabel.setText("Loading saved posts...");
        postsContainer.revalidate();
        postsContainer.repaint();

        new SwingWorker<List<Post>, Void>() {
            @Override
            protected List<Post> doInBackground() throws Exception {
                return savedPostDAO.getSavedPosts(currentUserId);
            }

            @Override
            protected void done() {
                postsContainer.removeAll();
                try {
                    List<Post> saved = get();

                    if (saved == null || saved.isEmpty()) {
                        countLabel.setText(" ");
                        hero.setChip("0 saved", Icons.Type.BOOKMARK);
                        postsContainer.add(emptyState(Icons.Type.BOOKMARK,
                                "No saved posts yet",
                                "Tap Save on any post and it will show up here."));
                    } else {
                        countLabel.setText(saved.size() + (saved.size() == 1 ? " saved post" : " saved posts"));
                        hero.setChip(saved.size() + (saved.size() == 1 ? " saved" : " saved"), Icons.Type.BOOKMARK);
                        for (Post post : saved) {
                            postsContainer.add(wrap(new PostCard(post, currentUserId)));
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    countLabel.setText(" ");
                    postsContainer.add(emptyState(Icons.Type.ALERT,
                            "Unable to load saved posts",
                            "Check your database connection, then press Refresh."));
                } finally {
                    postsContainer.revalidate();
                    postsContainer.repaint();
                }
            }
        }.execute();
    }

    private JPanel wrap(PostCard card) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel emptyState(Icons.Type icon, String title, String body) {
        EmptyState state = new EmptyState(icon, title, body);
        state.setAlignmentX(Component.LEFT_ALIGNMENT);
        state.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        return state;
    }
}
