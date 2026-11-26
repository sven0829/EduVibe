package com.eduvibe.ui;

import com.eduvibe.dao.RewardDAO;
import com.eduvibe.dao.RewardRollDAO;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.PlaytimePanel  (NEW FILE)
 *
 * "Playtime Rewards" (reference screen 7): a treasure chest, a Spin button that costs
 * RewardRollDAO.ROLL_COST points, the list of possible rewards (from the database) and
 * the user's points. The free daily spin stays on the Points & Rewards page.
 */
public class PlaytimePanel extends JPanel {

    private final int userId;
    private final RewardDAO rewardDAO = new RewardDAO();
    private final RewardRollDAO rollDAO = new RewardRollDAO();

    private final com.eduvibe.ui.components.ChestStage chest = new com.eduvibe.ui.components.ChestStage();
    private final RoundedButton equipNow = new RoundedButton("Equip now", RoundedButton.Style.SECONDARY);
    private RewardRollDAO.Result lastWon;
    private final RoundedButton spinButton =
            new RoundedButton("Spin (" + RewardRollDAO.ROLL_COST + " pts)");
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel pointsLabel = new JLabel("…");
    private final com.eduvibe.ui.components.PossibleRewardsView possible =
            new com.eduvibe.ui.components.PossibleRewardsView();

    private Runnable onPointsChanged = () -> { };
    private Consumer<String> navigator = p -> { };

    public PlaytimePanel(int userId) {
        this.userId = userId;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        com.eduvibe.ui.components.PageHero hero = new com.eduvibe.ui.components.PageHero(
                Icons.Type.GIFT, "Playtime Rewards",
                "Spend points for a chance at a random reward. Points have no cash value.",
                com.eduvibe.ui.components.PageHero.PLAYTIME[0], com.eduvibe.ui.components.PageHero.PLAYTIME[1]);
        column.add(hero);
        column.add(Box.createVerticalStrut(16));

        column.add(buildSpinCard());
        column.add(Box.createVerticalStrut(18));
        column.add(buildPossibleCard());

        add(column, BorderLayout.NORTH);
    }

    public void setOnPointsChanged(Runnable r) {
        this.onPointsChanged = r == null ? () -> { } : r;
    }

    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator == null ? p -> { } : navigator;
    }

    // ---------------- spin card ----------------

    private RoundedPanel buildSpinCard() {

        RoundedPanel card = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        chest.setAlignmentX(Component.CENTER_ALIGNMENT);
        chest.setPreferredSize(new Dimension(420, 230));
        chest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        spinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        spinButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        spinButton.addActionListener(e -> spin());

        status.setFont(Theme.regular(12));
        status.setForeground(Theme.MUTED);
        status.setAlignmentX(Component.CENTER_ALIGNMENT);

        equipNow.setAlignmentX(Component.CENTER_ALIGNMENT);
        equipNow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        equipNow.setVisible(false);
        equipNow.addActionListener(e -> equipLastWon());
        chest.setOnDismiss(() -> equipNow.setVisible(false));

        card.add(chest);
        card.add(Box.createVerticalStrut(14));
        card.add(spinButton);

        // the Equip button and its gap exist only while the button is visible
        JPanel equipRow = new JPanel(new BorderLayout());
        equipRow.setOpaque(false);
        equipRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        equipRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        equipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        equipRow.add(equipNow, BorderLayout.CENTER);
        equipRow.setVisible(false);
        equipNow.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                equipRow.setVisible(true);
                equipRow.revalidate();
            }

            @Override
            public void componentHidden(java.awt.event.ComponentEvent e) {
                equipRow.setVisible(false);
                equipRow.revalidate();
            }
        });
        card.add(equipRow);
        card.add(Box.createVerticalStrut(6));
        card.add(status);
        return card;
    }

    // ---------------- possible rewards ----------------

    private RoundedPanel buildPossibleCard() {

        RoundedPanel card = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Possible Rewards");
        title.setFont(Theme.bold(14));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        possible.setAlignmentX(Component.LEFT_ALIGNMENT);

        // your points row
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel yours = new JLabel("Your Points", Icons.icon(Icons.Type.COIN, 18, Theme.GOLD), SwingConstants.LEFT);
        yours.setIconTextGap(8);
        yours.setFont(Theme.regular(12));
        yours.setForeground(Theme.MUTED);

        pointsLabel.setFont(Theme.bold(16));
        pointsLabel.setForeground(Theme.GOLD);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        yours.setAlignmentX(Component.LEFT_ALIGNMENT);
        pointsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(yours);
        left.add(Box.createVerticalStrut(2));
        left.add(pointsLabel);

        RoundedButton more = new RoundedButton("Get More", RoundedButton.Style.SECONDARY);
        more.addActionListener(e -> navigator.accept("REWARDS"));

        foot.add(left, BorderLayout.WEST);
        foot.add(more, BorderLayout.EAST);

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(possible);
        card.add(Box.createVerticalStrut(16));
        card.add(foot);
        return card;
    }

    /** Reloads the reward list and the user's points. */
    public void load() {

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {rewardDAO.loadRewards(userId), rewardDAO.loadBalance(userId)};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] data = get();
                    renderPossible((List<RewardDAO.Reward>) data[0]);
                    pointsLabel.setText(String.format("%,d pts", (Integer) data[1]));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    possible.setRewards(null);
                    Dialogs.error(PlaytimePanel.this, "Could not load rewards",
                            "Check your connection and try again.");
                }
            }
        }.execute();
    }

    private void renderPossible(List<RewardDAO.Reward> rewards) {
        possible.setRewards(rewards);
    }

    private JComponent rewardTile(RewardDAO.Reward r) {

        RoundedPanel tile = new RoundedPanel(14, Theme.INPUT, Theme.BORDER);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(14, 10, 12, 10));

        Color rc = rarityColor(r.rarity);

        JLabel icon = new JLabel(Icons.icon(iconFor(r.type), 30, rc));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(r.name == null ? "Reward" : r.name, SwingConstants.CENTER);
        name.setFont(Theme.bold(12));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel rarity = new JLabel("(" + (r.rarity == null ? "Common" : capitalize(r.rarity)) + ")",
                SwingConstants.CENTER);
        rarity.setFont(Theme.regular(11));
        rarity.setForeground(rc);
        rarity.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(icon);
        tile.add(Box.createVerticalStrut(8));
        tile.add(name);
        tile.add(Box.createVerticalStrut(2));
        tile.add(rarity);
        if (r.owned) {
            JLabel owned = new JLabel("Owned", SwingConstants.CENTER);
            owned.setFont(Theme.bold(10));
            owned.setForeground(Theme.GREEN);
            owned.setAlignmentX(Component.CENTER_ALIGNMENT);
            tile.add(Box.createVerticalStrut(4));
            tile.add(owned);
        }
        return tile;
    }

    static Icons.Type iconFor(String type) {
        String t = type == null ? "" : type.toLowerCase();
        if (t.contains("frame") || t.contains("avatar")) {
            return Icons.Type.USER;
        }
        if (t.contains("banner")) {
            return Icons.Type.IMAGE;
        }
        if (t.contains("theme")) {
            return Icons.Type.SPARKLE;
        }
        if (t.contains("sticker") || t.contains("emoji")) {
            return Icons.Type.SMILE;
        }
        if (t.contains("badge")) {
            return Icons.Type.STAR;
        }
        return Icons.Type.GIFT;
    }

    static Color rarityColor(String rarity) {
        String r = rarity == null ? "" : rarity.toLowerCase();
        return switch (r) {
            case "uncommon" -> Theme.GREEN;
            case "rare" -> Theme.BLUE_LIGHT;
            case "epic" -> Theme.PURPLE;
            case "legendary" -> Theme.GOLD;
            default -> Theme.MUTED;
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ---------------- spin ----------------

    private void spin() {

        // clicking Spin while the last reward card is still showing simply starts the next spin
        chest.cancel();
        equipNow.setVisible(false);

        spinButton.setEnabled(false);
        status.setText("Opening the chest…");
        chest.startCharging();

        final long startedAt = System.currentTimeMillis();

        new SwingWorker<RewardRollDAO.Result, Void>() {
            @Override
            protected RewardRollDAO.Result doInBackground() throws Exception {
                RewardRollDAO.Result r = rollDAO.roll(userId);
                // the reward is already saved; the chest builds up suspense and hints at its colour
                final String rarity = r.rarity;
                SwingUtilities.invokeLater(() -> chest.tease(rarity));
                long left = 2800 - (System.currentTimeMillis() - startedAt);
                if (left > 0) {
                    Thread.sleep(left);
                }
                return r;
            }

            @Override
            protected void done() {
                try {
                    RewardRollDAO.Result r = get();
                    lastWon = r;
                    pointsLabel.setText(String.format("%,d pts", r.newBalance));
                    status.setText("You won: " + r.name + (r.rarity == null ? "" : " (" + capitalize(r.rarity) + ")"));
                    chest.open(r.name, r.rarity, r.type);

                    String kind = com.eduvibe.ui.components.ChestStage.kindOf(r.type);
                    equipNow.setText("Equip " + r.name);
                    equipNow.setVisible(!"other".equals(kind));

                    onPointsChanged.run();
                    load();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    chest.cancel();
                } catch (ExecutionException ex) {
                    chest.cancel();
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof IllegalStateException) {
                        status.setText(cause.getMessage());
                        Dialogs.info(PlaytimePanel.this, "Spin unavailable", cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        status.setText(" ");
                        Dialogs.error(PlaytimePanel.this, "Spin failed", String.valueOf(cause.getMessage()));
                    }
                } finally {
                    spinButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void equipLastWon() {

        final RewardRollDAO.Result r = lastWon;
        if (r == null) {
            return;
        }
        equipNow.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return new com.eduvibe.dao.EquipDAO().equip(userId,
                        com.eduvibe.dao.EquipDAO.slotFor(r.type), r.rewardId);
            }

            @Override
            protected void done() {
                equipNow.setEnabled(true);
                try {
                    if (get()) {
                        com.eduvibe.ui.components.Toast.success(PlaytimePanel.this,
                                r.name + " is now equipped on your profile.");
                        equipNow.setVisible(false);
                        onPointsChanged.run();
                    } else {
                        com.eduvibe.ui.components.Toast.error(PlaytimePanel.this, "Could not equip it. Try from Customize.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    com.eduvibe.ui.components.Toast.error(PlaytimePanel.this, "Could not equip it. Try from Customize.");
                }
            }
        }.execute();
    }

}
