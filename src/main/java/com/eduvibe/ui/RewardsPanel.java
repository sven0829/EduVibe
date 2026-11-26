package com.eduvibe.ui;

import com.eduvibe.dao.AdminNotificationDAO;
import com.eduvibe.dao.RewardDAO;
import com.eduvibe.dao.SpinDAO;
import com.eduvibe.ui.components.Dialogs;
import com.eduvibe.ui.components.Icons;
import com.eduvibe.ui.components.RoundedButton;
import com.eduvibe.ui.components.RoundedPanel;
import com.eduvibe.ui.components.Toast;
import com.eduvibe.ui.components.SpinWheel;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * com.eduvibe.ui.RewardsPanel  (REPLACES existing file)
 *
 * Points balance, daily reward spin, reward shop and points history.
 * Constructor RewardsPanel(int) and loadData() are unchanged.
 * All database work runs in SwingWorkers (RewardDAO / SpinDAO).
 */
public class RewardsPanel extends JPanel {

    private final int userId;
    private final RewardDAO rewardDAO = new RewardDAO();
    private final SpinDAO spinDAO = new SpinDAO();
    private final JPanel prizePool = new JPanel(new GridLayout(0, 3, 6, 6));

    private final JLabel balanceLabel = new JLabel("…");
    private final JLabel spinStatus = new JLabel(" ");
    private final SpinWheel wheel;
    private final RoundedButton spinButton = new RoundedButton("Spin the wheel");
    private final JPanel shopGrid = new JPanel(new GridLayout(0, 3, 14, 14));
    private final JPanel historyList = new JPanel();
    private Component historyHeading;
    private final JPanel myRewards = new JPanel(new GridLayout(0, 3, 14, 14));

    private Runnable onPointsChanged = () -> { };

    public RewardsPanel(int userId) {

        this.userId = userId;

        String[] labels = new String[SpinDAO.TABLE.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = SpinDAO.TABLE[i].label;
        }
        wheel = new SpinWheel(labels);

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(Theme.BACKGROUND);

        com.eduvibe.ui.components.PageHero hero = new com.eduvibe.ui.components.PageHero(
                com.eduvibe.ui.components.Icons.Type.COIN, "Points & Rewards",
                "Earn points, spin the wheel and collect rewards to show off.",
                com.eduvibe.ui.components.PageHero.REWARDS[0], com.eduvibe.ui.components.PageHero.REWARDS[1]);
        column.add(hero);
        column.add(Box.createVerticalStrut(16));

        JComponent balance = buildBalanceCard();
        balance.setAlignmentX(Component.LEFT_ALIGNMENT);
        balance.setMaximumSize(new Dimension(Integer.MAX_VALUE, balance.getPreferredSize().height));
        column.add(balance);
        column.add(Box.createVerticalStrut(16));

        JPanel mid = new JPanel(new GridLayout(1, 2, 16, 0));
        mid.setOpaque(false);
        mid.add(buildEarnCard());
        mid.add(buildSpinCard());
        mid.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        column.add(mid);

        column.add(Box.createVerticalStrut(24));
        column.add(heading("Available Rewards"));
        column.add(Box.createVerticalStrut(10));
        shopGrid.setOpaque(false);
        shopGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(shopGrid);

        column.add(Box.createVerticalStrut(24));
        JPanel myHead = new JPanel(new BorderLayout());
        myHead.setOpaque(false);
        myHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        myHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        myHead.add(heading("My Rewards"), BorderLayout.WEST);
        RoundedButton customize = new RoundedButton("Customize profile", RoundedButton.Style.PRIMARY);
        customize.addActionListener(e -> navigator.accept("CUSTOMIZE"));
        myHead.add(customize, BorderLayout.EAST);
        column.add(myHead);
        column.add(Box.createVerticalStrut(10));
        myRewards.setOpaque(false);
        myRewards.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(myRewards);

        column.add(Box.createVerticalStrut(24));
        historyHeading = heading("Points History");
        column.add(historyHeading);
        column.add(Box.createVerticalStrut(10));
        historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));
        historyList.setOpaque(false);
        historyList.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(historyList);

        add(column, BorderLayout.NORTH);
    }

    /** Lets the dashboard refresh its sidebar when points change. */
    public void setOnPointsChanged(Runnable callback) {
        this.onPointsChanged = callback == null ? () -> { } : callback;
    }

    // ---------------- cards ----------------

    private RoundedPanel buildBalanceCard() {

        RoundedPanel card = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel coin = new JLabel(Icons.icon(Icons.Type.COIN, 44, Theme.GOLD));

        balanceLabel.setFont(Theme.bold(34));
        balanceLabel.setForeground(Theme.GOLD);
        JLabel pts = new JLabel(" pts");
        pts.setFont(Theme.bold(20));
        pts.setForeground(Theme.GOLD);

        JPanel numberRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        numberRow.setOpaque(false);
        numberRow.add(balanceLabel);
        numberRow.add(pts);

        JLabel caption = new JLabel("Your current points");
        caption.setFont(Theme.regular(12));
        caption.setForeground(Theme.MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        numberRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(numberRow);
        text.add(caption);

        RoundedButton history = new RoundedButton("History", RoundedButton.Style.SECONDARY);
        history.addActionListener(e -> {
            if (historyHeading != null) {
                ((JComponent) historyHeading.getParent()).scrollRectToVisible(
                        new Rectangle(0, historyHeading.getY(), 10, 400));
            }
        });
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(history);

        card.add(coin, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private RoundedPanel buildEarnCard() {

        RoundedPanel card = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("How to Earn Points");
        title.setFont(Theme.bold(16));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        card.add(earnRow(Icons.Type.PLUS, "Create a post", "+15 pts"));
        card.add(earnRow(Icons.Type.HEART, "Like received on your post", "+5 pts"));
        card.add(earnRow(Icons.Type.COMMENT, "Comment received on your post", "+10 pts"));
        card.add(earnRow(Icons.Type.CLOCK, "Daily login", "+20 pts"));
        card.add(Box.createVerticalStrut(14));

        RoundedPanel tip = new RoundedPanel(12, Theme.CARD_LIGHT, Theme.BORDER);
        tip.setLayout(new BorderLayout(10, 0));
        tip.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tipIcon = new JLabel(Icons.icon(Icons.Type.FLAME, 20, Theme.GOLD));
        tipIcon.setVerticalAlignment(SwingConstants.TOP);
        JLabel tipText = new JLabel("<html><b>Keep your streak alive.</b><br>Log in every day to build a streak "
                + "and earn badges on the Leaderboard.</html>");
        tipText.setFont(Theme.regular(12));
        tipText.setForeground(Theme.MUTED);
        tip.add(tipIcon, BorderLayout.WEST);
        tip.add(tipText, BorderLayout.CENTER);
        tip.setMaximumSize(new Dimension(Integer.MAX_VALUE, tip.getPreferredSize().height + 6));
        card.add(tip);
        card.add(Box.createVerticalGlue());

        RoundedButton refresh = new RoundedButton("Refresh", RoundedButton.Style.OUTLINE);
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.addActionListener(e -> loadData());
        card.add(Box.createVerticalStrut(8));
        card.add(refresh);
        return card;
    }

    private JPanel earnRow(Icons.Type icon, String label, String points) {

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel text = new JLabel(label, Icons.icon(icon, 18, Theme.BLUE_LIGHT), SwingConstants.LEFT);
        text.setIconTextGap(12);
        text.setFont(Theme.regular(13));
        text.setForeground(Theme.TEXT);

        JLabel value = new JLabel(points);
        value.setFont(Theme.bold(13));
        value.setForeground(Theme.GREEN);

        row.add(text, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private RoundedPanel buildSpinCard() {

        RoundedPanel card = new RoundedPanel(18, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 24, 18, 24));

        JLabel title = new JLabel("Daily Reward Spin");
        title.setFont(Theme.bold(16));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);

        wheel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(wheel);

        spinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        spinButton.addActionListener(e -> doSpin());
        card.add(spinButton);
        card.add(Box.createVerticalStrut(8));

        spinStatus.setFont(Theme.regular(12));
        spinStatus.setForeground(Theme.MUTED);
        spinStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(spinStatus);

        card.add(Box.createVerticalStrut(14));
        JLabel poolTitle = new JLabel("Possible rewards");
        poolTitle.setFont(Theme.bold(14));
        poolTitle.setForeground(Theme.TEXT);
        poolTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(poolTitle);

        JLabel poolNote = new JLabel("<html><div style='text-align:center'>Points prizes, or a random reward "
                + "you don't own yet (frame, banner, theme or badge).</div></html>");
        poolNote.setFont(Theme.regular(11));
        poolNote.setForeground(Theme.MUTED);
        poolNote.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(2));
        card.add(poolNote);
        card.add(Box.createVerticalStrut(8));

        prizePool.setOpaque(false);
        prizePool.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4 * 42 + 3 * 6));
        prizePool.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(prizePool);
        renderPrizePool(null);
        return card;
    }

    private static Color slotColor(String slot) {
        return switch (slot == null ? "" : slot) {
            case "frame" -> new Color(45, 212, 191);
            case "banner" -> new Color(244, 114, 182);
            case "theme" -> new Color(251, 146, 60);
            default -> new Color(74, 222, 128);
        };
    }

    private static Color pointsColor(int points) {
        if (points >= 75) {
            return new Color(250, 204, 21);
        }
        if (points >= 50) {
            return new Color(192, 132, 252);
        }
        if (points >= 20) {
            return Theme.BLUE_LIGHT;
        }
        return new Color(148, 163, 184);
    }

    /** Lists every segment of the wheel with its chance, and how many rewards of each kind are still winnable. */
    private void renderPrizePool(List<RewardDAO.Reward> rewards) {

        java.util.Map<String, Integer> left = new java.util.HashMap<>();
        if (rewards != null) {
            for (RewardDAO.Reward r : rewards) {
                if (!r.owned) {
                    left.merge(com.eduvibe.dao.EquipDAO.slotFor(r.type), 1, Integer::sum);
                }
            }
        }

        int total = 0;
        for (SpinDAO.Prize p : SpinDAO.TABLE) {
            total += p.weight;
        }

        prizePool.removeAll();
        for (SpinDAO.Prize p : SpinDAO.TABLE) {
            int pct = (int) Math.round(p.weight * 100.0 / total);
            if (p.isReward()) {
                String sub = rewards == null ? pct + "%" : pct + "% · " + left.getOrDefault(p.slot, 0) + " left";
                prizePool.add(new PrizeChip(p.label, sub, slotColor(p.slot)));
            } else {
                prizePool.add(new PrizeChip(p.label + " pts", pct + "%", pointsColor(p.points)));
            }
        }
        prizePool.revalidate();
        prizePool.repaint();
    }

    /** Small pill showing one possible prize. */
    private static final class PrizeChip extends JComponent {
        private final String title;
        private final String sub;
        private final Color accent;

        PrizeChip(String title, String sub, Color accent) {
            this.title = title;
            this.sub = sub;
            this.accent = accent;
            setPreferredSize(new Dimension(96, 42));
            setToolTipText(title + " (" + sub + ")");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(com.eduvibe.ui.components.Anim.mix(Theme.CARD_LIGHT, accent, 0.14f));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g2.setColor(Theme.alpha(accent, 110));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g2.setColor(accent);
            g2.fillRoundRect(0, 8, 3, h - 16, 3, 3);
            g2.setFont(Theme.bold(12));
            g2.setColor(Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, (w - fm.stringWidth(title)) / 2, 17);
            g2.setFont(Theme.regular(10));
            fm = g2.getFontMetrics();
            g2.setColor(Theme.MUTED);
            g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, 32);
            g2.dispose();
        }
    }

    private JLabel info(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.regular(12));
        l.setForeground(Theme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(18));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ---------------- loading ----------------

    public void loadData() {

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {
                    rewardDAO.loadBalance(userId),
                    rewardDAO.loadRewards(userId),
                    rewardDAO.loadHistory(userId, 30),
                    spinDAO.hasSpunToday(userId),
                    rewardDAO.loadOwned(userId)
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] data = get();
                    balanceLabel.setText(String.format("%,d", (Integer) data[0]));
                    renderShop((List<RewardDAO.Reward>) data[1], (Integer) data[0]);
                    renderPrizePool((List<RewardDAO.Reward>) data[1]);
                    renderHistory((List<RewardDAO.HistoryEntry>) data[2]);
                    applySpinState((Boolean) data[3]);
                    renderMyRewards((List<RewardDAO.OwnedReward>) data[4]);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    balanceLabel.setText("—");
                    Dialogs.error(RewardsPanel.this, "Could not load rewards",
                            String.valueOf(ex.getCause().getMessage()));
                }
            }
        }.execute();
    }

    private void applySpinState(boolean spunToday) {
        if (wheel.isSpinning()) {
            return;
        }
        spinButton.setEnabled(!spunToday);
        spinStatus.setText(spunToday
                ? "You already spun today. Come back tomorrow!"
                : "1 free spin available today");
    }

    private void renderShop(List<RewardDAO.Reward> rewards, int balance) {

        shopGrid.removeAll();

        if (rewards.isEmpty()) {
            JLabel empty = new JLabel("No rewards are available right now.");
            empty.setForeground(Theme.MUTED);
            empty.setFont(Theme.regular(13));
            shopGrid.setLayout(new FlowLayout(FlowLayout.LEFT));
            shopGrid.add(empty);
        } else {
            shopGrid.setLayout(new GridLayout(0, 3, 14, 14));
            for (RewardDAO.Reward r : rewards) {
                shopGrid.add(rewardCard(r, balance));
            }
            // keep the grid cells from stretching when there are only 1–2 rewards
            while (shopGrid.getComponentCount() % 3 != 0) {
                JPanel filler = new JPanel();
                filler.setOpaque(false);
                shopGrid.add(filler);
            }
        }

        shopGrid.revalidate();
        shopGrid.repaint();
    }

    private RoundedPanel rewardCard(RewardDAO.Reward r, int balance) {

        Color accent = rarityColor(r.rarity);

        RoundedPanel card = new RoundedPanel(16, Theme.CARD, Theme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel rarity = new JLabel(safe(r.rarity).toUpperCase() + "  •  "
                + safe(r.type).replace('_', ' ').toUpperCase());
        rarity.setFont(Theme.bold(10));
        rarity.setForeground(accent);
        rarity.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(rarity);
        card.add(Box.createVerticalStrut(8));

        JLabel name = new JLabel(safe(r.name));
        name.setFont(Theme.bold(15));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(name);
        card.add(Box.createVerticalStrut(6));

        JLabel desc = new JLabel("<html><div style='width:190px'>"
                + escape(safe(r.description)) + "</div></html>");
        desc.setFont(Theme.regular(12));
        desc.setForeground(Theme.MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(desc);
        card.add(Box.createVerticalStrut(12));

        JLabel cost = new JLabel(String.format("%,d pts", r.cost),
                Icons.icon(Icons.Type.COIN, 16, Theme.GOLD), SwingConstants.LEFT);
        cost.setIconTextGap(6);
        cost.setFont(Theme.bold(13));
        cost.setForeground(Theme.GOLD);
        cost.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(cost);
        card.add(Box.createVerticalStrut(12));

        RoundedButton button;
        if (r.owned) {
            button = new RoundedButton("Owned", RoundedButton.Style.SECONDARY);
            button.setEnabled(false);
        } else if (balance < r.cost) {
            button = new RoundedButton("Need " + (r.cost - balance) + " more",
                    RoundedButton.Style.SECONDARY);
            button.setEnabled(false);
        } else {
            button = new RoundedButton("Purchase");
            button.addActionListener(e -> purchase(r, button));
        }
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(button);

        return card;
    }

    private java.util.function.Consumer<String> navigator = page -> { };

    public void setNavigator(java.util.function.Consumer<String> navigator) {
        this.navigator = navigator;
    }

    private void renderMyRewards(List<RewardDAO.OwnedReward> owned) {
        new SwingWorker<java.util.Map<String, Integer>, Void>() {
            @Override
            protected java.util.Map<String, Integer> doInBackground() throws Exception {
                return new com.eduvibe.dao.EquipDAO().equipped(userId);
            }

            @Override
            protected void done() {
                java.util.Map<String, Integer> map;
                try {
                    map = get();
                } catch (Exception ex) {
                    map = new java.util.HashMap<>();
                }
                buildMyRewards(owned, map);
            }
        }.execute();
    }

    private void buildMyRewards(List<RewardDAO.OwnedReward> owned, java.util.Map<String, Integer> equipped) {

        myRewards.removeAll();

        if (owned.isEmpty()) {
            myRewards.setLayout(new BorderLayout());
            JLabel none = new JLabel("You don't own any rewards yet. Buy one in the shop or try the Mystery Reward Roll!");
            none.setFont(Theme.regular(13));
            none.setForeground(Theme.MUTED);
            myRewards.add(none, BorderLayout.CENTER);
        } else {
            myRewards.setLayout(new GridLayout(0, 3, 14, 14));
            for (RewardDAO.OwnedReward o : owned) {
                RoundedPanel card = new RoundedPanel(14, Theme.CARD, Theme.BORDER);
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

                final String artSlot = com.eduvibe.dao.EquipDAO.slotFor(o.type);
                JComponent art = new JComponent() {
                    {
                        com.eduvibe.ui.components.RewardFx.animate(this);
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.antialias(g2);
                        g2.setPaint(new GradientPaint(0, 0,
                                com.eduvibe.ui.components.Anim.mix(Theme.BACKGROUND,
                                        com.eduvibe.util.RewardStyle.primary(o.name, o.rarity), 0.2f),
                                0, getHeight(), Theme.BACKGROUND));
                        g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                        com.eduvibe.ui.components.RewardArt.paintThumb(g2, artSlot, 0, 0,
                                getWidth(), getHeight(), o.name, o.rarity);
                        g2.dispose();
                    }
                };
                art.setPreferredSize(new Dimension(100, 96));
                art.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
                art.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(art);
                card.add(Box.createVerticalStrut(10));

                JLabel name = new JLabel(safe(o.name));
                name.setFont(Theme.bold(14));
                name.setForeground(Theme.TEXT);

                JLabel rarity = new JLabel(safe(o.rarity).toUpperCase()
                        + (o.type == null ? "" : "  ·  " + o.type.replace('_', ' ').toUpperCase()));
                rarity.setFont(Theme.bold(10));
                rarity.setForeground(rarityColor(o.rarity));

                JLabel via = new JLabel("Got it by "
                        + ("purchase".equalsIgnoreCase(o.obtainedVia) ? "buying it" : "the reward roll"));
                via.setFont(Theme.regular(11));
                via.setForeground(Theme.MUTED);

                name.setAlignmentX(Component.LEFT_ALIGNMENT);
                rarity.setAlignmentX(Component.LEFT_ALIGNMENT);
                via.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(name);
                card.add(Box.createVerticalStrut(4));
                card.add(rarity);
                card.add(Box.createVerticalStrut(6));
                card.add(via);

                String slot = com.eduvibe.dao.EquipDAO.slotFor(o.type);
                boolean worn = equipped.containsKey(slot) && equipped.get(slot) == o.rewardId;
                card.add(Box.createVerticalStrut(10));
                RoundedButton wear = new RoundedButton(worn ? "Unequip" : "Equip",
                        worn ? RoundedButton.Style.GHOST : RoundedButton.Style.SECONDARY);
                wear.setFont(Theme.bold(11));
                wear.setAlignmentX(Component.LEFT_ALIGNMENT);
                wear.addActionListener(e -> {
                    wear.setEnabled(false);
                    new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            com.eduvibe.dao.EquipDAO dao = new com.eduvibe.dao.EquipDAO();
                            if (worn) {
                                dao.unequip(userId, slot);
                                return true;
                            }
                            return dao.equip(userId, slot, o.rewardId);
                        }

                        @Override
                        protected void done() {
                            try {
                                if (get()) {
                                    java.util.Map<String, Integer> m = new java.util.HashMap<>(equipped);
                                    if (worn) {
                                        m.remove(slot);
                                    } else {
                                        m.put(slot, o.rewardId);
                                    }
                                    buildMyRewards(owned, m);
                                    Toast.success(RewardsPanel.this, worn
                                            ? "Removed from your profile."
                                            : "Equipped! Check your profile.");
                                    return;
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            wear.setEnabled(true);
                            Toast.error(RewardsPanel.this, "Could not change your equipped reward.");
                        }
                    }.execute();
                });
                card.add(wear);
                myRewards.add(card);
            }
        }

        myRewards.revalidate();
        myRewards.repaint();
    }

    private void renderHistory(List<RewardDAO.HistoryEntry> entries) {

        historyList.removeAll();

        if (entries.isEmpty()) {
            JLabel empty = new JLabel("No points history yet. Your activity will appear here.");
            empty.setFont(Theme.regular(13));
            empty.setForeground(Theme.MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyList.add(empty);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a");

        for (RewardDAO.HistoryEntry h : entries) {

            RoundedPanel row = new RoundedPanel(12, Theme.CARD, Theme.BORDER);
            row.setLayout(new BorderLayout(12, 0));
            row.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);

            JLabel action = new JLabel(h.refKey != null && h.refKey.startsWith("goals:")
                    ? "Daily goals bonus" : formatAction(h.action));
            action.setFont(Theme.bold(13));
            action.setForeground(Theme.TEXT);

            JLabel date = new JLabel(h.createdAt == null ? ""
                    : h.createdAt.toLocalDateTime().format(fmt));
            date.setFont(Theme.regular(11));
            date.setForeground(Theme.MUTED);

            text.add(action);
            text.add(date);

            JLabel pts = new JLabel((h.points >= 0 ? "+" : "") + h.points);
            pts.setFont(Theme.bold(15));
            pts.setForeground(h.points >= 0 ? Theme.GREEN : Theme.RED);

            row.add(text, BorderLayout.CENTER);
            row.add(pts, BorderLayout.EAST);

            historyList.add(row);
            historyList.add(Box.createVerticalStrut(8));
        }

        historyList.revalidate();
        historyList.repaint();
    }

    // ---------------- actions ----------------

    private void doSpin() {

        spinButton.setEnabled(false);
        spinStatus.setText("Spinning…");

        new SwingWorker<SpinDAO.SpinResult, Void>() {
            @Override
            protected SpinDAO.SpinResult doInBackground() throws Exception {
                return spinDAO.spin(userId);
            }

            @Override
            protected void done() {
                try {
                    SpinDAO.SpinResult result = get();
                    // The prize is already saved; the animation only reveals it.
                    wheel.spinTo(result.prizeIndex, () -> {
                        balanceLabel.setText(String.format("%,d", result.newBalance));
                        if (result.wonReward()) {
                            spinStatus.setText("You won the " + result.rewardName + "!");
                            Dialogs.success(RewardsPanel.this, "Congratulations!",
                                    "You won the " + result.rewardName + " ("
                                            + String.valueOf(result.rewardRarity).toLowerCase()
                                            + "). Equip it from Customize profile.");
                        } else if (result.fallback) {
                            spinStatus.setText("You won " + result.points + " points!");
                            Dialogs.success(RewardsPanel.this, "Congratulations!",
                                    "You already own every " + result.rewardSlot + ", so you got "
                                            + result.points + " points instead.");
                        } else {
                            spinStatus.setText("You won " + result.points + " points!");
                            Dialogs.success(RewardsPanel.this, "Congratulations!",
                                    "You won " + result.points + " points from the daily spin.");
                        }
                        onPointsChanged.run();
                        loadData();
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof IllegalStateException) {
                        Dialogs.info(RewardsPanel.this, "Spin unavailable", cause.getMessage());
                    } else {
                        cause.printStackTrace();
                        Dialogs.error(RewardsPanel.this, "Spin failed", String.valueOf(cause.getMessage()));
                    }
                    loadData();
                }
            }
        }.execute();
    }

    private void purchase(RewardDAO.Reward reward, RoundedButton button) {

        if (!Dialogs.confirm(this, "Confirm purchase",
                "Purchase " + reward.name + " for " + String.format("%,d", reward.cost) + " points?",
                "Purchase", false)) {
            return;
        }

        button.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return rewardDAO.purchase(userId, reward.id);
            }

            @Override
            protected void done() {
                try {
                    String username = get();

                    try {
                        AdminNotificationDAO.notifyAdmins("@" + username
                                + " purchased the reward \"" + reward.name
                                + "\". Reward ID: " + reward.id);
                    } catch (SQLException notifyError) {
                        notifyError.printStackTrace(); // must not undo the purchase
                    }

                    Dialogs.success(RewardsPanel.this, "Purchase successful",
                            "You purchased " + reward.name + "!");
                    onPointsChanged.run();

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    Dialogs.error(RewardsPanel.this, "Purchase failed", String.valueOf(cause.getMessage()));
                }
                loadData();
            }
        }.execute();
    }

    // ---------------- helpers ----------------

    private String formatAction(String action) {
        if (action == null) {
            return "Points activity";
        }
        return switch (action.toLowerCase()) {
            case "like" -> "Post like received";
            case "comment" -> "Comment received";
            case "post" -> "Post created";
            case "daily_login" -> "Daily login";
            case "rng_spin" -> "Daily reward spin";
            case "reward_purchase" -> "Reward purchased";
            case "admin_adjust" -> "Points adjustment";
            default -> action.replace("_", " ");
        };
    }

    private Color rarityColor(String rarity) {
        if (rarity == null) {
            return Theme.BLUE;
        }
        return switch (rarity.toLowerCase()) {
            case "common" -> Theme.MUTED;
            case "rare" -> Theme.BLUE_LIGHT;
            case "epic" -> Theme.PURPLE;
            case "legendary" -> Theme.GOLD;
            default -> Theme.BLUE;
        };
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
