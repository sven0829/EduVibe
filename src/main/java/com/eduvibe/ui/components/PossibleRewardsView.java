package com.eduvibe.ui.components;

import com.eduvibe.dao.EquipDAO;
import com.eduvibe.dao.RewardDAO;
import com.eduvibe.dao.RewardRollDAO;
import com.eduvibe.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * com.eduvibe.ui.components.PossibleRewardsView  (NEW FILE)
 *
 * Everything the RNG chest can give: a summary, the chance of each rarity, filter tabs
 * (All / Frames / Banners / Themes / Badges / Other) and a wrapping grid of reward cards
 * with a real preview, rarity and the chance of winning that exact reward. Rewards you
 * already own are shown dimmed (the chest never gives duplicates). Shows 12 at first;
 * "Show all" expands the rest.
 */
public final class PossibleRewardsView extends JPanel {

    private static final int INITIAL = 12;
    private static final String[] RARITIES = {"legendary", "epic", "rare", "uncommon", "common"};

    private List<RewardDAO.Reward> all = new ArrayList<>();
    private String filter = "all";
    private boolean expanded = false;

    private final JLabel summary = new JLabel(" ");
    private final JPanel oddsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel tabHost = new JPanel(new BorderLayout());
    private final WrapGrid grid = new WrapGrid(148, 172, 12);
    private final RoundedButton toggle = new RoundedButton("Show all", RoundedButton.Style.OUTLINE);
    private final JLabel empty = new JLabel("No rewards in this group.");

    public PossibleRewardsView() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        summary.setFont(Theme.regular(12));
        summary.setForeground(Theme.MUTED);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(summary);
        add(Box.createVerticalStrut(10));

        oddsRow.setOpaque(false);
        oddsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        oddsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        add(oddsRow);
        add(Box.createVerticalStrut(12));

        tabHost.setOpaque(false);
        tabHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        add(tabHost);
        add(Box.createVerticalStrut(12));

        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(grid);

        empty.setFont(Theme.regular(13));
        empty.setForeground(Theme.MUTED);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.setVisible(false);
        add(empty);

        add(Box.createVerticalStrut(12));
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(e -> {
            expanded = !expanded;
            rebuildGrid();
        });
        add(toggle);
    }

    public void setRewards(List<RewardDAO.Reward> rewards) {
        all = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        // rarest first, owned last
        all.sort((a, b) -> {
            if (a.owned != b.owned) {
                return a.owned ? 1 : -1;
            }
            int ra = rarityRank(a.rarity);
            int rb = rarityRank(b.rarity);
            if (ra != rb) {
                return Integer.compare(ra, rb);
            }
            return String.valueOf(a.name).compareToIgnoreCase(String.valueOf(b.name));
        });
        rebuildSummary();
        rebuildTabs();
        rebuildGrid();
    }

    // ---------------- helpers ----------------

    private static int rarityRank(String rarity) {
        String r = rarity == null ? "" : rarity.trim().toLowerCase();
        for (int i = 0; i < RARITIES.length; i++) {
            if (RARITIES[i].equals(r)) {
                return i;
            }
        }
        return RARITIES.length;
    }

    private static Color rarityColor(String rarity) {
        String r = rarity == null ? "" : rarity.trim().toLowerCase();
        return switch (r) {
            case "uncommon" -> Theme.GREEN;
            case "rare" -> Theme.BLUE_LIGHT;
            case "epic" -> Theme.PURPLE;
            case "legendary" -> Theme.GOLD;
            default -> Theme.MUTED;
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "Common";
        }
        String t = s.trim().toLowerCase();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** frame / banner / theme / badge, or "other" for anything the profile cannot show (stickers...). */
    private static String kind(RewardDAO.Reward r) {
        String t = r.type == null ? "" : r.type.toLowerCase();
        if (t.contains("frame") || t.contains("avatar")) {
            return "frame";
        }
        if (t.contains("banner")) {
            return "banner";
        }
        if (t.contains("theme")) {
            return "theme";
        }
        if (t.contains("badge")) {
            return "badge";
        }
        return EquipDAO.slotFor(r.type).equals("badge") ? "other" : EquipDAO.slotFor(r.type);
    }

    private int totalWeight() {
        int total = 0;
        for (RewardDAO.Reward r : all) {
            if (!r.owned) {
                total += RewardRollDAO.weight(r.rarity);
            }
        }
        return total;
    }

    private void rebuildSummary() {
        int owned = 0;
        for (RewardDAO.Reward r : all) {
            if (r.owned) {
                owned++;
            }
        }
        int left = all.size() - owned;
        summary.setText(all.isEmpty()
                ? "No rewards have been added yet."
                : left + " reward" + (left == 1 ? "" : "s") + " you can still win"
                        + (owned > 0 ? "  ·  " + owned + " already owned" : "")
                        + "  ·  each spin gives one you don't have yet");

        oddsRow.removeAll();
        int total = totalWeight();
        if (total > 0) {
            for (String rar : RARITIES) {
                int w = 0;
                for (RewardDAO.Reward r : all) {
                    if (!r.owned && rar.equals(String.valueOf(r.rarity).trim().toLowerCase())) {
                        w += RewardRollDAO.weight(r.rarity);
                    }
                }
                if (w > 0) {
                    if (oddsRow.getComponentCount() > 0) {
                        oddsRow.add(Box.createHorizontalStrut(8));
                    }
                    oddsRow.add(new OddsChip(capitalize(rar), w * 100.0 / total, rarityColor(rar)));
                }
            }
        }
        oddsRow.revalidate();
        oddsRow.repaint();
    }

    private void rebuildTabs() {
        int[] counts = new int[5];     // frame, banner, theme, badge, other
        for (RewardDAO.Reward r : all) {
            switch (kind(r)) {
                case "frame" -> counts[0]++;
                case "banner" -> counts[1]++;
                case "theme" -> counts[2]++;
                case "badge" -> counts[3]++;
                default -> counts[4]++;
            }
        }
        List<String> labels = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        labels.add("All (" + all.size() + ")");
        keys.add("all");
        String[] names = {"Frames", "Banners", "Themes", "Badges", "Other"};
        String[] k = {"frame", "banner", "theme", "badge", "other"};
        for (int i = 0; i < 5; i++) {
            if (counts[i] > 0) {
                labels.add(names[i] + " (" + counts[i] + ")");
                keys.add(k[i]);
            }
        }
        if (!keys.contains(filter)) {
            filter = "all";
        }
        SegmentedTabs tabs = new SegmentedTabs(labels.toArray(new String[0]),
                Math.max(0, keys.indexOf(filter)), i -> {
                    filter = keys.get(i);
                    expanded = false;
                    rebuildGrid();
                });
        tabs.putClientProperty("neutral", Boolean.TRUE);
        tabHost.removeAll();
        tabHost.add(tabs, BorderLayout.CENTER);
        tabHost.revalidate();
        tabHost.repaint();
    }

    private void rebuildGrid() {
        List<RewardDAO.Reward> shown = new ArrayList<>();
        for (RewardDAO.Reward r : all) {
            if (filter.equals("all") || filter.equals(kind(r))) {
                shown.add(r);
            }
        }
        int total = totalWeight();
        int limit = expanded ? shown.size() : Math.min(INITIAL, shown.size());

        grid.removeAll();
        for (int i = 0; i < limit; i++) {
            RewardDAO.Reward r = shown.get(i);
            double chance = (!r.owned && total > 0) ? RewardRollDAO.weight(r.rarity) * 100.0 / total : 0;
            grid.add(new RewardTile(r, kind(r), chance));
        }
        empty.setVisible(shown.isEmpty());
        grid.setVisible(!shown.isEmpty());
        toggle.setVisible(shown.size() > INITIAL);
        toggle.setText(expanded ? "Show fewer" : "Show all " + shown.size());
        revalidate();
        repaint();
    }

    // ---------------- pieces ----------------

    private static final class OddsChip extends JComponent {
        private final String text;
        private final Color color;

        OddsChip(String rarity, double pct, Color color) {
            this.color = color;
            this.text = rarity + "  " + (pct < 1 ? "<1" : String.valueOf(Math.round(pct))) + "%";
            FontMetrics fm = getFontMetrics(Theme.bold(11));
            setPreferredSize(new Dimension(fm.stringWidth(text) + 26, 26));
            setToolTipText("Chance that a spin gives a " + rarity.toLowerCase() + " reward");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Theme.alpha(color, 36));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.setColor(Theme.alpha(color, 120));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.setColor(color);
            g2.fillOval(9, getHeight() / 2 - 3, 6, 6);
            g2.setFont(Theme.bold(11));
            g2.setColor(Theme.TEXT);
            g2.drawString(text, 20, getHeight() / 2 + 4);
            g2.dispose();
        }
    }

    private static final class RewardTile extends JComponent {
        private final RewardDAO.Reward reward;
        private final String kind;
        private final double chance;
        private boolean hover;

        RewardTile(RewardDAO.Reward reward, String kind, double chance) {
            this.reward = reward;
            this.kind = kind;
            this.chance = chance;
            RewardFx.animate(this);
            setToolTipText(reward.description == null || reward.description.isBlank()
                    ? reward.name : reward.name + " - " + reward.description);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            Color rc = rarityColor(reward.rarity);
            boolean owned = reward.owned;

            if (owned) {
                g2.setComposite(AlphaComposite.SrcOver.derive(0.55f));
            }

            g2.setColor(Anim.mix(Theme.CARD_LIGHT, rc, hover ? 0.16f : 0.07f));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.setColor(Theme.alpha(rc, hover ? 220 : 90));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

            // preview
            int artH = 92;
            g2.setColor(new Color(0, 0, 0, 38));
            g2.fillRoundRect(8, 8, w - 17, artH, 12, 12);
            switch (kind) {
                case "frame", "banner", "theme", "badge" ->
                        RewardArt.paintThumb(g2, kind, 8, 8, w - 16, artH, String.valueOf(reward.name), reward.rarity);
                default -> {
                    g2.setColor(Theme.alpha(rc, 50));
                    g2.fillOval(w / 2 - 24, 8 + artH / 2 - 24, 48, 48);
                    Icons.paint(g2, Icons.Type.GIFT, w / 2f - 14, 8 + artH / 2f - 14, 28, rc);
                }
            }

            // name
            g2.setFont(Theme.bold(12));
            g2.setColor(Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            String name = String.valueOf(reward.name);
            while (name.length() > 3 && fm.stringWidth(name) > w - 18) {
                name = name.substring(0, name.length() - 2).trim() + "…";
            }
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, 8 + artH + 22);

            // rarity
            g2.setFont(Theme.bold(11));
            fm = g2.getFontMetrics();
            String rarity = capitalize(reward.rarity);
            g2.setColor(rc);
            g2.drawString(rarity, (w - fm.stringWidth(rarity)) / 2, 8 + artH + 38);

            // chance / owned
            g2.setFont(Theme.regular(11));
            fm = g2.getFontMetrics();
            String bottom = owned ? "Owned"
                    : (chance < 0.1 ? "<0.1" : String.format("%.1f", chance)) + "% chance";
            g2.setColor(owned ? Theme.GREEN : Theme.MUTED);
            g2.drawString(bottom, (w - fm.stringWidth(bottom)) / 2, 8 + artH + 54);
            g2.dispose();
        }
    }

    /**
     * Cards wrap to the available width and stretch evenly so every row fills the full width
     * with identical gaps (no ragged right edge). Height is derived from the width only,
     * so scrolling and resizing never make the rows jump.
     */
    private static final class WrapGrid extends JPanel {
        private final int cw;
        private final int ch;
        private final int gap;

        WrapGrid(int cw, int ch, int gap) {
            super(null);
            this.cw = cw;
            this.ch = ch;
            this.gap = gap;
            setOpaque(false);
        }

        private int columns(int width) {
            return Math.max(1, (width + gap) / (cw + gap));
        }

        private int widthHint() {
            int w = getWidth();
            if (w <= 0 && getParent() != null) {
                w = getParent().getWidth();
            }
            return w > 0 ? w : 700;
        }

        private int heightFor(int width) {
            int n = getComponentCount();
            int rows = (int) Math.ceil(n / (double) columns(width));
            return rows == 0 ? 0 : rows * ch + (rows - 1) * gap;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = widthHint();
            return new Dimension(w, heightFor(w));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        public void doLayout() {
            int width = getWidth();
            int cols = columns(width);
            int n = getComponentCount();
            int usable = width - gap * (cols - 1);
            int base = usable / cols;
            int extra = usable - base * cols;
            int i = 0;
            for (Component c : getComponents()) {
                int col = i % cols;
                int x = col * (base + gap) + Math.min(col, extra);
                int w = base + (col < extra ? 1 : 0);
                c.setBounds(x, (i / cols) * (ch + gap), w, ch);
                i++;
            }
            if (n > 0 && heightFor(width) != getHeight()) {
                SwingUtilities.invokeLater(() -> {
                    revalidate();
                    repaint();
                });
            }
        }
    }
}
