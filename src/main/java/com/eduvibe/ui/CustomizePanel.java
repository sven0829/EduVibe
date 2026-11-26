package com.eduvibe.ui;

import com.eduvibe.dao.EquipDAO;
import com.eduvibe.dao.RewardDAO;
import com.eduvibe.ui.components.*;
import com.eduvibe.ui.components.ProfilePreview.Look;
import com.eduvibe.ui.components.ProfilePreview.Piece;
import com.eduvibe.util.RewardStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * com.eduvibe.ui.CustomizePanel  (NEW FILE)
 *
 * The equipment showcase: a large live preview of the profile, the four equipment slots
 * (frame / banner / theme / badge), and the collection of rewards to inspect and equip.
 *
 *   click a reward   -> previews it on the big profile (nothing is saved yet)
 *   Equip / Unequip  -> saves to user_equipped; the change shows on the profile immediately
 *   locked rewards   -> can be previewed, cannot be equipped
 */
public class CustomizePanel extends JPanel {

    private static final String[] SLOTS = {EquipDAO.FRAME, EquipDAO.BANNER, EquipDAO.THEME, EquipDAO.BADGE};
    private static final String[] SLOT_TITLES = {"Avatar Frames", "Banners", "Themes", "Badges"};
    private static final String[] SLOT_SINGLE = {"Avatar frame", "Banner", "Theme", "Badge"};

    /** One reward as shown here. */
    private static final class Item {
        int id;
        String name;
        String description;
        String rarity;
        int cost;
        boolean owned;
        String slot;

        Piece piece() {
            return new Piece(id, name, rarity);
        }
    }

    private final int userId;
    private final String fullName;
    private final String username;
    private final String profilePic;

    private final EquipDAO equipDAO = new EquipDAO();
    private final RewardDAO rewardDAO = new RewardDAO();

    private Consumer<String> navigator = page -> { };
    private Runnable onChanged = () -> { };

    private final ProfilePreview preview = new ProfilePreview();
    private final JLabel itemTitle = new JLabel(" ");
    private final JLabel itemMeta = new JLabel(" ");
    private final JTextArea itemDesc = new JTextArea();
    private final RoundedButton equipButton = new RoundedButton("Equip", RoundedButton.Style.PRIMARY);
    private final RoundedButton clearButton = new RoundedButton("Clear preview", RoundedButton.Style.GHOST);
    private final RoundedButton shopButton = new RoundedButton("Get it in Rewards", RoundedButton.Style.SECONDARY);

    private final JPanel slotRow = new JPanel(new GridLayout(1, 4, 12, 0));
    private final JPanel gridHost = new JPanel(new BorderLayout());
    private final JLabel statusLabel = new JLabel("Loading your collection...");
    private final JCheckBox showLocked = new JCheckBox("Show locked rewards", true);
    private SegmentedTabs tabs;

    private List<Item> items = new ArrayList<>();
    private final Map<String, Integer> equipped = new HashMap<>();
    private Item inspected;
    private int tabIndex = 0;
    private boolean busy;
    private final javax.swing.Timer animator;

    public CustomizePanel(int userId, String fullName, String username, String profilePic) {

        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.profilePic = profilePic;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(0, 0, 24, 0));

        buildUi();

        // one shared clock for the shimmer on epic / legendary art
        animator = new javax.swing.Timer(40, e -> {
            if (isShowing()) {
                preview.repaint();
                slotRow.repaint();
                gridHost.repaint();
            }
        });
        animator.start();
    }

    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void removeNotify() {
        animator.stop();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    // ----------------------------------------------------------------- layout

    private void buildUi() {

        com.eduvibe.ui.components.PageHero pageHero = new com.eduvibe.ui.components.PageHero(
                com.eduvibe.ui.components.Icons.Type.STAR, "Customize Profile",
                "Click a reward to preview it, then equip it to wear it on your profile.",
                com.eduvibe.ui.components.PageHero.CUSTOMIZE[0], com.eduvibe.ui.components.PageHero.CUSTOMIZE[1]);
        add(pageHero);
        add(Box.createVerticalStrut(16));

        // ---- hero: big preview + inspector
        preview.setUser(fullName, username, profilePic);
        preview.setCaption("EQUIPPED LOOK", Theme.GREEN);
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        add(preview);
        add(Box.createVerticalStrut(12));
        add(buildInspector());
        add(Box.createVerticalStrut(22));

        // ---- slots
        add(heading("Currently equipped"));
        add(Box.createVerticalStrut(10));
        slotRow.setOpaque(false);
        slotRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        slotRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        add(slotRow);
        add(Box.createVerticalStrut(24));

        // ---- collection
        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        head.add(heading("Your collection"), BorderLayout.WEST);

        showLocked.setOpaque(false);
        showLocked.setForeground(Theme.MUTED);
        showLocked.setFont(Theme.regular(12));
        showLocked.setFocusPainted(false);
        showLocked.addActionListener(e -> renderGrid());
        head.add(showLocked, BorderLayout.EAST);
        add(head);
        add(Box.createVerticalStrut(10));

        tabs = new SegmentedTabs(new String[]{"Frames", "Banners", "Themes", "Badges"}, 0, i -> {
            tabIndex = i;
            renderGrid();
        });
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setMaximumSize(new Dimension(520, 44));
        add(tabs);
        add(Box.createVerticalStrut(14));

        gridHost.setOpaque(false);
        gridHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(gridHost);

        statusLabel.setFont(Theme.regular(13));
        statusLabel.setForeground(Theme.MUTED);
        gridHost.add(statusLabel, BorderLayout.NORTH);

        refreshInspector();
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(16));
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel buildInspector() {

        RoundedPanel card = new RoundedPanel(16, Theme.CARD, Theme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        itemTitle.setFont(Theme.bold(16));
        itemTitle.setForeground(Theme.TEXT);
        itemMeta.setFont(Theme.bold(11));
        itemDesc.setFont(Theme.regular(12));
        itemDesc.setForeground(Theme.MUTED);
        itemDesc.setOpaque(false);
        itemDesc.setEditable(false);
        itemDesc.setFocusable(false);
        itemDesc.setLineWrap(true);
        itemDesc.setWrapStyleWord(true);
        itemDesc.setBorder(null);

        for (JComponent c : new JComponent[]{itemTitle, itemMeta, itemDesc}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        text.add(itemTitle);
        text.add(Box.createVerticalStrut(3));
        text.add(itemMeta);
        text.add(Box.createVerticalStrut(6));
        text.add(itemDesc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        equipButton.addActionListener(e -> onEquipClicked());
        clearButton.addActionListener(e -> {
            inspected = null;
            applyLook();
            refreshInspector();
            renderGrid();
        });
        shopButton.addActionListener(e -> navigator.accept("REWARDS"));
        actions.add(clearButton);
        actions.add(shopButton);
        actions.add(equipButton);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(actions);

        card.add(text, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    // ----------------------------------------------------------------- loading

    public void load() {

        statusLabel.setText("Loading your collection...");
        statusLabel.setVisible(true);

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {

                Map<Integer, Item> byId = new LinkedHashMap<>();

                for (RewardDAO.Reward r : rewardDAO.loadRewards(userId)) {
                    Item it = new Item();
                    it.id = r.id;
                    it.name = r.name;
                    it.description = r.description;
                    it.rarity = r.rarity;
                    it.cost = r.cost;
                    it.owned = r.owned;
                    it.slot = EquipDAO.slotFor(r.type);
                    byId.put(it.id, it);
                }

                // rewards the user owns but that are no longer on sale
                for (RewardDAO.OwnedReward o : rewardDAO.loadOwned(userId)) {
                    Item it = byId.get(o.rewardId);
                    if (it == null) {
                        it = new Item();
                        it.id = o.rewardId;
                        it.name = o.name;
                        it.description = o.description;
                        it.rarity = o.rarity;
                        it.slot = EquipDAO.slotFor(o.type);
                        byId.put(it.id, it);
                    }
                    it.owned = true;
                }

                return new Object[]{new ArrayList<>(byId.values()), equipDAO.equipped(userId)};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] data = get();
                    items = (List<Item>) data[0];
                    equipped.clear();
                    equipped.putAll((Map<String, Integer>) data[1]);

                    // keep the inspected reward if it still exists
                    if (inspected != null) {
                        inspected = find(inspected.id);
                    }
                    statusLabel.setVisible(false);
                    applyLook();
                    refreshSlots();
                    refreshInspector();
                    renderGrid();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusLabel.setText("Could not load your rewards. Check your connection and reopen this page.");
                    statusLabel.setVisible(true);
                }
            }
        }.execute();
    }

    private Item find(int id) {
        for (Item it : items) {
            if (it.id == id) {
                return it;
            }
        }
        return null;
    }

    private Item equippedItem(String slot) {
        Integer id = equipped.get(slot);
        return id == null ? null : find(id);
    }

    // -------------------------------------------------------------- the look

    private void applyLook() {

        Piece[] p = new Piece[4];
        for (int i = 0; i < 4; i++) {
            Item it = (inspected != null && inspected.slot.equals(SLOTS[i])) ? inspected : equippedItem(SLOTS[i]);
            p[i] = it == null ? null : it.piece();
        }
        preview.setLook(new Look(p[0], p[1], p[2], p[3]));

        boolean previewing = inspected != null && !isEquipped(inspected);
        preview.setCaption(previewing ? "PREVIEW - not equipped yet" : "EQUIPPED LOOK",
                previewing ? new Color(251, 191, 36) : Theme.GREEN);
    }

    private boolean isEquipped(Item it) {
        Integer id = equipped.get(it.slot);
        return id != null && id == it.id;
    }

    // ------------------------------------------------------------- inspector

    private void refreshInspector() {

        if (inspected == null) {
            itemTitle.setText("Select a reward");
            itemMeta.setText(" ");
            itemDesc.setText("Click any reward in your collection to see how it looks on your profile before you equip it.");
            equipButton.setVisible(false);
            clearButton.setVisible(false);
            shopButton.setVisible(false);
            return;
        }

        Color rc = RewardStyle.rarityColor(inspected.rarity);
        itemTitle.setText(inspected.name);
        itemMeta.setForeground(rc);
        String rarity = inspected.rarity == null ? "COMMON" : inspected.rarity.toUpperCase();
        itemMeta.setText(rarity + "  •  " + slotName(inspected.slot).toUpperCase()
                + (isEquipped(inspected) ? "  •  EQUIPPED" : inspected.owned ? "" : "  •  LOCKED"));
        itemDesc.setText(inspected.description == null || inspected.description.isBlank()
                ? "No description." : inspected.description);

        clearButton.setVisible(true);
        equipButton.setVisible(inspected.owned);
        shopButton.setVisible(!inspected.owned);
        shopButton.setText("Get it for " + inspected.cost + " pts");
        equipButton.setText(isEquipped(inspected) ? "Unequip" : "Equip");
        equipButton.setEnabled(!busy);
        revalidate();
        repaint();
    }

    private static String slotName(String slot) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i].equals(slot)) {
                return SLOT_SINGLE[i];
            }
        }
        return slot;
    }

    private void onEquipClicked() {

        if (inspected == null || !inspected.owned || busy) {
            return;
        }

        final Item target = inspected;
        final boolean unequip = isEquipped(target);
        busy = true;
        equipButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (unequip) {
                    equipDAO.unequip(userId, target.slot);
                    return true;
                }
                return equipDAO.equip(userId, target.slot, target.id);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    if (get()) {
                        if (unequip) {
                            equipped.remove(target.slot);
                        } else {
                            equipped.put(target.slot, target.id);
                        }
                        applyLook();
                        refreshSlots();
                        refreshInspector();
                        renderGrid();
                        Toast.success(CustomizePanel.this, unequip
                                ? target.name + " removed."
                                : target.name + " equipped! It now shows on your profile.");
                        onChanged.run();
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                refreshInspector();
                Toast.error(CustomizePanel.this,
                        "Could not equip. If this keeps happening, run eduvibe_extras.sql in MySQL.");
            }
        }.execute();
    }

    // ----------------------------------------------------------------- slots

    private void refreshSlots() {
        slotRow.removeAll();
        for (int i = 0; i < 4; i++) {
            final int index = i;
            slotRow.add(new SlotTile(SLOTS[i], SLOT_SINGLE[i], equippedItem(SLOTS[i]), () -> {
                tabIndex = index;
                tabs.select(index, false);
                renderGrid();
            }));
        }
        slotRow.revalidate();
        slotRow.repaint();
    }

    private final class SlotTile extends JComponent {

        private final String slot;
        private final String title;
        private final Item item;
        private boolean hover;

        SlotTile(String slot, String title, Item item, Runnable onClick) {
            this.slot = slot;
            this.title = title;
            this.item = item;
            setPreferredSize(new Dimension(150, 86));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth();
            int h = getHeight();

            Color edge = item == null ? Theme.BORDER : RewardStyle.primary(item.name, item.rarity);
            g2.setColor(hover ? Anim.mix(Theme.CARD, Theme.CARD_LIGHT, 0.6f) : Theme.CARD);
            g2.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 16, 16));
            g2.setColor(hover || item != null ? edge : Theme.BORDER);
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 2, h - 2, 16, 16));

            // thumbnail
            if (item != null) {
                RewardArt.paintThumb(g2, slot, 6, 8, 70, 70, item.name, item.rarity);
            } else {
                g2.setColor(new Color(148, 163, 184, 90));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{4f, 4f}, 0f));
                g2.draw(new RoundRectangle2D.Double(14, 18, 52, 52, 14, 14));
            }

            g2.setFont(Theme.bold(10));
            g2.setColor(Theme.MUTED);
            g2.drawString(title.toUpperCase(), 80, 28);

            g2.setFont(Theme.bold(12));
            g2.setColor(item == null ? Theme.MUTED : Theme.TEXT);
            String name = item == null ? "Empty" : item.name;
            g2.drawString(ellipsize(name, g2.getFontMetrics(), w - 88), 80, 47);

            g2.setFont(Theme.regular(11));
            g2.setColor(item == null ? Theme.MUTED : Theme.GREEN);
            g2.drawString(item == null ? "Nothing equipped" : "Equipped", 80, 64);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ grid

    private void renderGrid() {

        gridHost.removeAll();
        String slot = SLOTS[tabIndex];

        List<Item> list = new ArrayList<>();
        for (Item it : items) {
            if (it.slot.equals(slot) && (it.owned || showLocked.isSelected())) {
                list.add(it);
            }
        }
        // owned first, equipped on top
        list.sort(Comparator
                .comparing((Item it) -> !isEquipped(it))
                .thenComparing(it -> !it.owned)
                .thenComparingInt(it -> rarityRank(it.rarity)));

        long ownedCount = items.stream().filter(it -> it.slot.equals(slot) && it.owned).count();

        if (list.isEmpty()) {
            EmptyState empty = new EmptyState(Icons.Type.GIFT,
                    "No " + SLOT_TITLES[tabIndex].toLowerCase() + " yet",
                    ownedCount == 0
                            ? "Earn rewards in Points & Rewards or Playtime Rewards and they will appear here."
                            : "Nothing to show with the current filter.");
            gridHost.add(empty, BorderLayout.NORTH);
        } else {
            WrapGrid grid = new WrapGrid(196, 226, 14);
            for (Item it : list) {
                grid.add(new RewardCard(it));
            }
            gridHost.add(grid, BorderLayout.NORTH);
        }
        gridHost.revalidate();
        gridHost.repaint();
    }

    private static int rarityRank(String rarity) {
        String r = rarity == null ? "" : rarity.toLowerCase();
        return switch (r) {
            case "legendary" -> 0;
            case "epic" -> 1;
            case "rare" -> 2;
            case "uncommon" -> 3;
            default -> 4;
        };
    }

    /** Fixed-size cards that wrap to as many columns as fit; works from phone width up. */
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

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth()
                    : (getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 700);
            int cols = columns(w);
            int rows = (int) Math.ceil(getComponentCount() / (double) cols);
            return new Dimension(w, rows == 0 ? 0 : rows * ch + (rows - 1) * gap);
        }

        @Override
        public void doLayout() {
            int cols = columns(getWidth());
            int i = 0;
            for (Component c : getComponents()) {
                int col = i % cols;
                int row = i / cols;
                c.setBounds(col * (cw + gap), row * (ch + gap), cw, ch);
                i++;
            }
            if (getPreferredSize().height != getHeight()) {
                SwingUtilities.invokeLater(() -> {
                    revalidate();
                    repaint();
                });
            }
        }
    }

    private final class RewardCard extends JComponent {

        private final Item item;
        private final Anim.Fade hover = new Anim.Fade(this, 0.25f);

        RewardCard(Item item) {
            this.item = item;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(false);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    inspected = item;
                    applyLook();
                    refreshInspector();
                    renderGrid();
                    if (e.getClickCount() == 2 && item.owned && !isEquipped(item)) {
                        onEquipClicked();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);

            float lift = hover.value();
            g2.translate(0, -3 * lift);
            int w = getWidth();
            int h = getHeight() - 4;

            boolean equippedNow = isEquipped(item);
            boolean selected = inspected != null && inspected.id == item.id;
            Color rc = RewardStyle.primary(item.name, item.rarity);

            // shadow + body
            g2.setColor(new Color(0, 0, 0, (int) (40 + 40 * lift)));
            g2.fill(new RoundRectangle2D.Double(3, 6, w - 6, h - 2, 18, 18));
            g2.setColor(Anim.mix(Theme.CARD, Theme.CARD_LIGHT, 0.25f + 0.35f * lift));
            RoundRectangle2D.Double body = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 18, 18);
            g2.fill(body);

            // art well
            Graphics2D art = (Graphics2D) g2.create();
            art.clip(new RoundRectangle2D.Double(0, 0, w - 1, 138, 18, 18));
            art.setPaint(new GradientPaint(0, 0, Anim.mix(Theme.BACKGROUND, rc, 0.20f),
                    0, 138, Theme.BACKGROUND));
            art.fillRect(0, 0, w, 138);
            RewardArt.paintThumb(art, item.slot, 0, 0, w, 138, item.name, item.rarity);
            if (!item.owned) {
                art.setColor(new Color(15, 23, 42, 150));
                art.fillRect(0, 0, w, 138);
                Icons.paint(art, Icons.Type.LOCK, w / 2f - 14, 138 / 2f - 14, 28, new Color(226, 232, 240));
            }
            art.dispose();

            // text
            g2.setFont(Theme.bold(13));
            g2.setColor(Theme.TEXT);
            g2.drawString(ellipsize(item.name, g2.getFontMetrics(), w - 24), 12, 162);

            // rarity pill
            String r = item.rarity == null ? "COMMON" : item.rarity.toUpperCase();
            g2.setFont(Theme.bold(10));
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(r) + 16;
            g2.setColor(RewardArt.alpha(rc, 45));
            g2.fill(new RoundRectangle2D.Double(12, 172, pw, 20, 20, 20));
            g2.setColor(rc);
            g2.drawString(r, 20, 186);

            // status
            g2.setFont(Theme.bold(11));
            String status;
            Color sc;
            if (equippedNow) {
                status = "Equipped";
                sc = Theme.GREEN;
            } else if (item.owned) {
                status = "Owned";
                sc = Theme.MUTED;
            } else {
                status = "Locked • " + item.cost + " pts";
                sc = new Color(251, 191, 36);
            }
            g2.setColor(sc);
            g2.drawString(status, 12, 212);

            // borders: equipped (green glow) / selected (blue)
            if (equippedNow) {
                g2.setColor(RewardArt.alpha(Theme.GREEN, 70));
                g2.setStroke(new BasicStroke(5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, w - 3, h - 3, 18, 18));
                g2.setColor(Theme.GREEN);
                g2.setStroke(new BasicStroke(1.8f));
                g2.draw(body);
                g2.fill(new Ellipse2D.Double(w - 34, 10, 24, 24));
                Icons.paint(g2, Icons.Type.CHECK, w - 31, 13, 18, Color.WHITE);
            } else if (selected) {
                g2.setColor(Theme.BLUE);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(body);
            } else {
                g2.setColor(Anim.mix(Theme.BORDER, rc, 0.55f * lift));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(body);
            }
            g2.dispose();
        }
    }

    private static String ellipsize(String text, FontMetrics fm, int max) {
        if (text == null) {
            return "";
        }
        if (fm.stringWidth(text) <= max) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "...") > max) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }
}
