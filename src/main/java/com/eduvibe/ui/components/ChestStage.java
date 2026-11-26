package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;
import com.eduvibe.util.RewardStyle;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * com.eduvibe.ui.components.ChestStage  (NEW FILE)
 *
 * The animated treasure chest for the Playtime Rewards page.
 *
 *   IDLE    - chest bobs, soft glow, drifting sparkles
 *   CHARGE  - (while the roll is running) the chest shakes harder and harder, energy sparks orbit
 *             faster, light leaks from the lid, and the glow slowly turns the colour of the reward
 *             that is coming (a hint of its rarity)
 *   OPEN    - flash, lid flies open, a beam of light, shockwave rings and a burst of particles
 *             whose amount and colour depend on the rarity
 *   REVEAL  - the reward rises out of the chest on a card with its own animated effects;
 *             click the stage to continue
 *
 * Each rarity has its own opening:
 *   common    - quick, calm pop
 *   uncommon  - green swirl of sparkles
 *   rare      - blue lightning bolts and a double flash
 *   epic      - purple screen shake, spinning rune rings, long burst
 *   legendary - slow build-up, long gold flash, gold rain, rarity banner
 *
 * Everything is drawn with Java2D; no image files. Purely visual: the reward is already saved
 * before open() is called.
 */
public final class ChestStage extends JComponent {

    private enum State { IDLE, CHARGE, OPEN, REVEAL }

    private static final class Particle {
        double x, y, vx, vy, life, max, size, rot, vr;
        Color color;
        boolean confetti;
    }

    private static final class Ring {
        double age, max, x, y;
        Color color;
    }

    private final Timer timer = new Timer(16, e -> frame());
    private final List<Particle> particles = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();

    private State state = State.IDLE;
    private long lastNanos = System.nanoTime();
    private double time;          // global seconds
    private double stateT;        // seconds in current state
    private double flash;
    private double flashDecay = 1.9;
    private boolean pendingBurst;   // legendary: the burst waits for the build-up
    private double shakeKick;     // extra shake after the burst

    // charge tease
    private Color glow = new Color(99, 102, 241);
    private Color glowTarget = new Color(99, 102, 241);

    // result
    private String name = "";
    private String rarity = "common";
    private String kind = "other";     // frame / banner / theme / badge / other
    private int tier;
    private Color rarityColor = new Color(148, 163, 184);
    private double closing = -1;       // >= 0 while the card is leaving
    private Runnable onDismiss = () -> { };

    public ChestStage() {
        setOpaque(false);
        setPreferredSize(new Dimension(420, 230));
        setCursor(Cursor.getDefaultCursor());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (state == State.REVEAL && closing < 0) {
                    closing = 0;
                }
            }
        });
        timer.start();
    }

    // ------------------------------------------------------------------ control

    public boolean isBusy() {
        return state == State.CHARGE || state == State.OPEN || (state == State.REVEAL && closing < 0 && false);
    }

    /** True while the reward card is showing (waiting for a click). */
    public boolean isRevealing() {
        return state == State.REVEAL || state == State.OPEN;
    }

    public void setOnDismiss(Runnable r) {
        this.onDismiss = r == null ? () -> { } : r;
    }

    public void startCharging() {
        pendingBurst = false;
        particles.clear();
        rings.clear();
        flash = 0;
        closing = -1;
        glow = new Color(99, 102, 241);
        glowTarget = glow;
        enter(State.CHARGE);
    }

    /** A hint of what is coming: the glow slowly turns the reward's colour. */
    public void tease(String rarityName) {
        glowTarget = RewardStyle.rarityColor(rarityName);
    }

    /** Stops the animation without a reveal (e.g. when the roll failed). */
    public void cancel() {
        closing = -1;
        pendingBurst = false;
        enter(State.IDLE);
    }

    /** Opens the chest and reveals the reward. {@code type} is the reward_type from the database. */
    public void open(String rewardName, String rewardRarity, String rewardType) {
        name = rewardName == null ? "Reward" : rewardName;
        rarity = rewardRarity == null ? "common" : rewardRarity;
        tier = RewardFx.tier(rarity);
        rarityColor = RewardStyle.primary(name, rarity);
        kind = kindOf(rewardType);
        closing = -1;
        enter(State.OPEN);
        // tier feel: weak/short for common, big and slow for legendary
        flashDecay = new double[] {3.2, 2.6, 2.0, 1.5, 0.8}[tier];
        pendingBurst = true;
        if (hold() <= 0) {
            fireBurst();
        }
    }

    /** Seconds of build-up before the chest bursts (legendary only). */
    private double hold() {
        return tier == 4 ? 1.0 : 0;
    }

    private boolean holding() {
        return state == State.OPEN && pendingBurst;
    }

    /** Seconds since the burst (0 while still building up). */
    private double ot() {
        return Math.max(0, stateT - hold());
    }

    private void fireBurst() {
        pendingBurst = false;
        flash = new double[] {0.45, 0.7, 1.0, 1.0, 1.0}[tier];
        shakeKick = new double[] {0.0, 0.4, 0.8, 1.6, 2.0}[tier];
        burst();
    }

    public static String kindOf(String type) {
        String t = type == null ? "" : type.toLowerCase();
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
        return "other";
    }

    private void enter(State s) {
        state = s;
        stateT = 0;
    }

    // ------------------------------------------------------------------ simulation

    private void frame() {
        if (!isShowing()) {
            lastNanos = System.nanoTime();
            return;
        }
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;
        time += dt;
        stateT += dt;
        flash = Math.max(0, flash - dt * flashDecay);
        shakeKick = Math.max(0, shakeKick - dt * 2.4);

        if (holding()) {
            // legendary build-up: the glow turns gold and energy rushes into the chest
            glow = Anim.mix(glow, new Color(255, 214, 90), (float) Math.min(1, dt * 3));
            if (ThreadLocalRandom.current().nextDouble() < 0.9) {
                spark(true);
            }
            if (stateT >= hold()) {
                fireBurst();
            }
        } else if (state == State.CHARGE) {
            glow = Anim.mix(glow, glowTarget, (float) Math.min(1, dt * 0.9));
            // energy sparks flying into the chest
            if (ThreadLocalRandom.current().nextDouble() < 0.5 + stateT * 0.25) {
                spark(false);
            }
        } else if (state == State.OPEN) {
            if (ot() > new double[] {1.0, 1.15, 1.3, 1.5, 1.8}[tier]) {
                enter(State.REVEAL);
            }
        } else if (state == State.REVEAL) {
            if (tier >= 2 && ThreadLocalRandom.current().nextDouble() < 0.18 + tier * 0.06) {
                fountain();
            }
            if (tier == 4 && ThreadLocalRandom.current().nextDouble() < 0.7) {
                goldRain();
            }
            if (closing >= 0) {
                closing += dt;
                if (closing > 0.28) {
                    closing = -1;
                    enter(State.IDLE);
                    onDismiss.run();
                }
            }
        } else {
            glow = Anim.mix(glow, new Color(99, 102, 241), (float) Math.min(1, dt * 2));
        }

        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.life += dt;
            if (p.life >= p.max) {
                it.remove();
                continue;
            }
            p.vy += (p.confetti ? 260 : 340) * dt;
            if (p.confetti) {
                p.vx *= 1 - dt * 1.4;
                p.vy *= 1 - dt * 0.9;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += p.vr * dt;
        }
        for (Iterator<Ring> it = rings.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            r.age += dt;
            if (r.age >= r.max) {
                it.remove();
            }
        }
        repaint();
    }

    private double cx() {
        return getWidth() / 2.0;
    }

    private double chestY() {
        return getHeight() * 0.64;
    }

    private void spark(boolean outward) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble() * Math.PI * 2;
        double d = 120 + r.nextDouble() * 70;
        Particle p = new Particle();
        p.x = cx() + Math.cos(a) * d * 1.4;
        p.y = chestY() - 20 + Math.sin(a) * d * 0.7;
        double sp = 150 + r.nextDouble() * 120;
        p.vx = (cx() - p.x) / d * sp * 0.55;
        p.vy = (chestY() - 20 - p.y) / d * sp * 0.55 - 10;
        p.life = 0;
        p.max = 0.55 + r.nextDouble() * 0.35;
        p.size = 1.5 + r.nextDouble() * 2.2;
        p.color = Anim.mix(glow, Color.WHITE, 0.35f);
        particles.add(p);
    }

    private void fountain() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Particle p = new Particle();
        p.x = cx() + (r.nextDouble() - 0.5) * 120;
        p.y = chestY() - 36;
        p.vx = (r.nextDouble() - 0.5) * 120;
        p.vy = -190 - r.nextDouble() * 160;
        p.life = 0;
        p.max = 0.9 + r.nextDouble() * 0.7;
        p.size = 1.6 + r.nextDouble() * 2.4;
        p.color = r.nextBoolean() ? rarityColor : Color.WHITE;
        particles.add(p);
    }

    private void goldRain() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Particle p = new Particle();
        p.x = r.nextDouble() * getWidth();
        p.y = -6;
        p.vx = (r.nextDouble() - 0.5) * 30;
        p.vy = 30 + r.nextDouble() * 70;
        p.life = 0;
        p.max = 1.6 + r.nextDouble() * 0.8;
        p.size = 1.6 + r.nextDouble() * 2.2;
        p.confetti = r.nextInt(3) == 0;
        p.rot = r.nextDouble() * 6.28;
        p.vr = (r.nextDouble() - 0.5) * 8;
        p.color = r.nextBoolean() ? new Color(255, 214, 90) : new Color(255, 244, 190);
        particles.add(p);
    }

    private void burst() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int[] counts = {38, 56, 84, 124, 190};
        int n = counts[tier];
        Color a = rarityColor;
        Color b = RewardStyle.secondary(name, rarity);
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            p.x = cx();
            p.y = chestY() - 34;
            double ang = -Math.PI / 2 + (r.nextDouble() - 0.5) * Math.PI * 1.25;
            double sp = 140 + r.nextDouble() * (260 + tier * 55);
            p.vx = Math.cos(ang) * sp;
            p.vy = Math.sin(ang) * sp;
            p.life = 0;
            p.max = 0.9 + r.nextDouble() * 1.1;
            p.size = 1.8 + r.nextDouble() * (2.2 + tier * 0.6);
            p.confetti = tier >= 1 && i % 3 == 0;
            p.rot = r.nextDouble() * 6.28;
            p.vr = (r.nextDouble() - 0.5) * 14;
            int pick = r.nextInt(4);
            p.color = pick == 0 ? Color.WHITE : (pick == 1 ? b : a);
            particles.add(p);
        }
        for (int i = 0; i < 2 + tier; i++) {
            Ring ring = new Ring();
            ring.x = cx();
            ring.y = chestY() - 30;
            ring.age = -i * 0.12;
            ring.max = 0.9 + i * 0.12;
            ring.color = i % 2 == 0 ? a : Color.WHITE;
            rings.add(ring);
        }
    }

    // ------------------------------------------------------------------ easing

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double easeOutCubic(double t) {
        t = clamp01(t);
        return 1 - Math.pow(1 - t, 3);
    }

    private static double easeOutBack(double t) {
        t = clamp01(t);
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        RewardFx.quality(g2);

        int w = getWidth();
        int h = getHeight();
        double cx = cx();
        double cy = chestY();

        // panel
        Shape panel = new RoundRectangle2D.Double(0, 0, w, h, 18, 18);
        g2.setColor(Theme.isLight() ? new Color(226, 232, 240) : new Color(16, 24, 48));
        g2.fill(panel);
        g2.clip(panel);

        Color tint = state == State.OPEN || state == State.REVEAL ? Anim.mix(glow, rarityColor, (float) easeOutCubic(ot() * 2)) : glow;

        // vignette tinted by the glow
        float breathe = 0.5f + 0.5f * (float) Math.sin(time * (state == State.CHARGE ? 7 : 1.6));
        g2.setPaint(new RadialGradientPaint((float) cx, (float) (cy - 30), (float) (w * 0.62),
                new float[] {0f, 1f},
                new Color[] {RewardFx.alpha(tint, (int) (70 + 60 * breathe * (state == State.CHARGE ? 1 : 0.5))),
                             RewardFx.alpha(tint, 0)}));
        g2.fillRect(0, 0, w, h);

        // god rays when the chest is open (strength follows rarity)
        if ((state == State.OPEN && !holding()) || state == State.REVEAL) {
            double k = easeOutCubic(ot() * 1.6) * (closing >= 0 ? 1 - closing / 0.28 : 1);
            RewardFx.rays(g2, cx, cy - 36, w * 0.9, tier == 4 ? new Color(255, 236, 160) : tint,
                    tier >= 3 ? 14 : 9, (int) ((22 + tier * 14) * k));
        }

        // shake
        double shake = 0;
        if (state == State.CHARGE) {
            shake = Math.min(1, stateT / 2.0) * 3.6 + 0.4;
        } else if (holding()) {
            shake = 1.5 + 5 * (stateT / hold());
        }
        shake += shakeKick * 4;
        double dx = shake > 0 ? Math.sin(time * 61) * shake + Math.sin(time * 97) * shake * 0.4 : 0;
        double dy = state == State.IDLE || state == State.REVEAL ? Math.sin(time * 1.5) * 2.5
                : (shake > 0 ? Math.cos(time * 73) * shake * 0.45 : 0);

        // sparks orbiting the chest while charging
        if (state == State.CHARGE || holding()) {
            int n = 8 + (int) Math.min(14, stateT * 6);
            double speed = 1.4 + stateT * 1.1;
            for (int i = 0; i < n; i++) {
                double a = time * speed + i * Math.PI * 2 / n;
                double rr = 78 + 14 * Math.sin(time * 3 + i);
                RewardFx.glowDot(g2, cx + Math.cos(a) * rr * 1.25, cy - 22 + Math.sin(a) * rr * 0.5,
                        2.6 + (i % 3), Anim.mix(tint, Color.WHITE, 0.4f), 210);
            }
        } else if (state == State.IDLE) {
            RewardFx.motes(g2, cx, cy - 20, 110, 70, 9, new Color(165, 180, 252), 1, 7, false, 2.2);
        }

        // chest
        double open = state == State.OPEN ? easeOutCubic(ot() / 0.45)
                : state == State.REVEAL ? 1 : 0;
        if (closing >= 0) {
            open = 1 - easeOutCubic(closing / 0.28);
        }
        // epic+: spinning rune rings behind everything else
        if (tier >= 3 && state == State.REVEAL || (tier >= 3 && state == State.OPEN && !holding())) {
            paintRunes(g2, cx, getHeight() * 0.53, tint, easeOutCubic(ot() * 1.5));
        }
        paintChest(g2, cx + dx, cy + dy, open, tint);
        if (state == State.OPEN && !holding()) {
            if (tier == 1) {
                paintSwirl(g2, cx, cy - 34, ot());
            } else if (tier == 2) {
                paintBolts(g2, cx, cy - 34, ot());
            }
        }
        // legendary build-up: the screen dims while the light grows
        if (holding()) {
            double k = stateT / hold();
            g2.setColor(new Color(0, 0, 0, (int) (90 * k)));
            g2.fillRect(0, 0, w, h);
            RewardFx.halo(g2, cx, cy - 30, 40 + 130 * k, new Color(255, 214, 90), 4);
        }

        // beam of light
        if (open > 0) {
            double bw = 30 + 70 * open;
            double fade = state == State.REVEAL ? 0.45 : 1.0;
            g2.setPaint(new LinearGradientPaint((float) cx, (float) (cy - 40), (float) cx, 0,
                    new float[] {0f, 1f},
                    new Color[] {RewardFx.alpha(Color.WHITE, (int) (150 * open * fade)), RewardFx.alpha(tint, 0)}));
            Path2D beam = new Path2D.Double();
            beam.moveTo(cx - bw * 0.35, cy - 40);
            beam.lineTo(cx - bw, 0);
            beam.lineTo(cx + bw, 0);
            beam.lineTo(cx + bw * 0.35, cy - 40);
            beam.closePath();
            g2.fill(beam);
        }

        // shock rings
        for (Ring r : rings) {
            if (r.age < 0) {
                continue;
            }
            double p = r.age / r.max;
            double rad = 20 + p * (w * 0.55);
            g2.setStroke(new BasicStroke((float) (5 * (1 - p) + 1)));
            g2.setColor(RewardFx.alpha(r.color, (int) (200 * (1 - p))));
            g2.draw(new Ellipse2D.Double(r.x - rad, r.y - rad * 0.55, rad * 2, rad * 1.1));
        }

        // reward card
        if ((state == State.OPEN && !holding()) || state == State.REVEAL) {
            double appear = easeOutBack((ot() - 0.4) / 0.75);
            double leave = closing >= 0 ? 1 - easeOutCubic(closing / 0.28) : 1;
            if (ot() > 0.4) {
                paintCard(g2, cx, cy, appear * leave, leave);
            }
        }

        // particles
        for (Particle p : particles) {
            double f = 1 - p.life / p.max;
            int a = (int) (255 * Math.min(1, f * 1.6));
            if (p.confetti) {
                Graphics2D c = (Graphics2D) g2.create();
                c.translate(p.x, p.y);
                c.rotate(p.rot);
                c.setColor(RewardFx.alpha(p.color, a));
                double sz = p.size * 1.6;
                c.fill(new Rectangle2D.Double(-sz, -sz * 0.45, sz * 2, sz * 0.9));
                c.dispose();
            } else {
                RewardFx.glowDot(g2, p.x, p.y, p.size * (1.2 + f), p.color, a);
            }
        }

        // flash
        if (flash > 0) {
            Color fc = new Color[] {Color.WHITE, new Color(225, 255, 235), new Color(215, 235, 255),
                    new Color(238, 220, 255), new Color(255, 240, 190)}[tier];
            double f = flash;
            if (tier == 2) {
                // rare: a double blink
                f = flash * (0.55 + 0.45 * Math.abs(Math.sin(flash * 14)));
            }
            g2.setColor(new Color(fc.getRed(), fc.getGreen(), fc.getBlue(), (int) (235 * Math.min(1, f))));
            g2.fillRect(0, 0, w, h);
        }

        g2.setClip(null);

        // RNG tag
        g2.setFont(Theme.bold(12));
        FontMetrics fm = g2.getFontMetrics();
        boolean shown = (state == State.OPEN && !holding() && ot() > 0.3) || state == State.REVEAL;
        String tag = shown && tier >= 2 ? rarity.toUpperCase() : "RNG";
        int tw = fm.stringWidth(tag) + 24;
        g2.setColor(shown && tier >= 2 ? rarityColor : Theme.BLUE);
        g2.fillRoundRect((int) cx - tw / 2, 12, tw, 24, 12, 12);
        g2.setColor(shown && tier >= 2 ? new Color(15, 23, 42) : Color.WHITE);
        g2.drawString(tag, (int) cx - fm.stringWidth(tag) / 2, 29);

        if (state == State.REVEAL && closing < 0) {
            String hint = "Click to continue";
            g2.setFont(Theme.regular(11));
            fm = g2.getFontMetrics();
            g2.setColor(RewardFx.alpha(Color.WHITE, (int) (90 + 90 * RewardFx.pulse(1400))));
            g2.drawString(hint, (int) (cx - fm.stringWidth(hint) / 2.0), h - 10);
        }
        g2.dispose();
    }

    /** Uncommon: two arms of green sparkles spiral out of the chest. */
    private void paintSwirl(Graphics2D g, double cx, double cy, double t) {
        if (t > 1.1) {
            return;
        }
        double fade = 1 - t / 1.1;
        for (int arm = 0; arm < 2; arm++) {
            for (int j = 0; j < 22; j++) {
                double ang = j * 0.42 + t * 7 + arm * Math.PI;
                double rad = 6 + j * 4.2 + t * 70;
                double x = cx + Math.cos(ang) * rad * 1.3;
                double y = cy + Math.sin(ang) * rad * 0.55 - t * 30;
                RewardFx.glowDot(g, x, y, 2.4 + (22 - j) * 0.08, Anim.mix(rarityColor, Color.WHITE, 0.3f),
                        (int) (230 * fade * (1 - j / 26.0)));
            }
        }
    }

    /** Rare: jagged blue lightning bolts that flicker for a moment. */
    private void paintBolts(Graphics2D g0, double cx, double cy, double t) {
        if (t > 0.6) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        double fade = 1 - t / 0.6;
        long flicker = (long) (t * 22);
        for (int i = 0; i < 6; i++) {
            java.util.Random r = new java.util.Random(i * 7919L + flicker * 31L);
            double ang = -Math.PI / 2 + (i - 2.5) * 0.46;
            double len = 120 + r.nextInt(70);
            Path2D bolt = new Path2D.Double();
            double x = cx;
            double y = cy;
            bolt.moveTo(x, y);
            int seg = 7;
            for (int s = 1; s <= seg; s++) {
                double d = len * s / seg;
                double jitter = (r.nextDouble() - 0.5) * 26;
                x = cx + Math.cos(ang) * d + Math.cos(ang + Math.PI / 2) * jitter;
                y = cy + Math.sin(ang) * d * 0.9 + Math.sin(ang + Math.PI / 2) * jitter;
                bolt.lineTo(x, y);
            }
            g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(RewardFx.alpha(rarityColor, (int) (90 * fade)));
            g.draw(bolt);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(RewardFx.alpha(Color.WHITE, (int) (240 * fade)));
            g.draw(bolt);
        }
        g.dispose();
    }

    /** Epic and legendary: two dashed rings spin in opposite directions behind the card. */
    private void paintRunes(Graphics2D g0, double cx, double cy, Color tint, double k) {
        Graphics2D g = (Graphics2D) g0.create();
        double leave = closing >= 0 ? 1 - closing / 0.28 : 1;
        int a = (int) (170 * k * leave);
        if (a <= 0) {
            g.dispose();
            return;
        }
        Color c = tier == 4 ? new Color(255, 214, 90) : tint;
        for (int ring = 0; ring < 2; ring++) {
            double rad = (ring == 0 ? 104 : 120) * (0.6 + 0.4 * k);
            g.setStroke(new BasicStroke(ring == 0 ? 2.2f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[] {ring == 0 ? 14f : 4f, ring == 0 ? 10f : 9f}, (float) (((time * (ring == 0 ? 30 : -22)) % 40 + 40) % 40)));
            g.setColor(RewardFx.alpha(c, ring == 0 ? a : a / 2));
            g.draw(new Ellipse2D.Double(cx - rad, cy - rad, rad * 2, rad * 2));
        }
        g.dispose();
    }

    private void paintChest(Graphics2D g0, double cx, double cy, double open, Color tint) {

        Graphics2D g = (Graphics2D) g0.create();
        g.translate(cx, cy);

        double scale = 0.92;
        g.scale(scale, scale);

        Color gold = new Color(250, 204, 21);
        Color body1 = new Color(79, 70, 229);
        Color body2 = new Color(49, 46, 129);

        // soft ground shadow
        g.setPaint(new RadialGradientPaint(0, 72, 90, new float[] {0f, 1f},
                new Color[] {new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(-90, 56, 180, 34));

        // body
        Shape body = new RoundRectangle2D.Double(-70, 0, 140, 66, 14, 14);
        g.setPaint(new GradientPaint(0, 0, body1, 0, 66, body2));
        g.fill(body);
        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(-69.3, 0.7, 138.6, 64.6, 13, 13));

        // interior glow where the lid was
        if (open > 0.02) {
            double iw = 128;
            double ih = 22 * open;
            g.setPaint(new RadialGradientPaint(0f, -4f, (float) (iw * 0.62), new float[] {0f, 1f},
                    new Color[] {RewardFx.alpha(Color.WHITE, (int) (255 * open)), RewardFx.alpha(tint, (int) (150 * open))}));
            g.fill(new Ellipse2D.Double(-iw / 2, -4 - ih / 2, iw, ih));
        }

        // light leaking from the lid seam while charging
        if (state == State.CHARGE) {
            double leak = Math.min(1, stateT / 1.6) * (0.6 + 0.4 * Math.sin(time * 18));
            g.setColor(RewardFx.alpha(Color.WHITE, (int) (200 * leak)));
            g.fill(new RoundRectangle2D.Double(-62, -7, 124, 3 + 3 * leak, 3, 3));
            g.setPaint(new RadialGradientPaint(0, -6, 110, new float[] {0f, 1f},
                    new Color[] {RewardFx.alpha(tint, (int) (110 * leak)), RewardFx.alpha(tint, 0)}));
            g.fill(new Ellipse2D.Double(-110, -64, 220, 116));
        }

        // lid: flattens and lifts as it opens (seen from the front)
        double sy = 1 - 0.88 * open;
        double lift = open * 34;
        Graphics2D l = (Graphics2D) g.create();
        l.translate(0, -4 - lift);
        l.scale(1 + open * 0.04, sy);
        l.translate(0, 4);
        Path2D lid = new Path2D.Double();
        lid.moveTo(-72, -4);
        lid.curveTo(-72, -52, 72, -52, 72, -4);
        lid.closePath();
        l.setPaint(new GradientPaint(0, -50, new Color(129, 140, 248), 0, 0, body1));
        l.fill(lid);
        l.setColor(gold);
        l.fill(new RoundRectangle2D.Double(-72, -6, 144, 10, 5, 5));
        l.fill(new RoundRectangle2D.Double(-48, -44, 10, 40, 4, 4));
        l.fill(new RoundRectangle2D.Double(38, -44, 10, 40, 4, 4));
        l.dispose();

        // bands + lock on the body
        g.setColor(gold);
        g.fill(new RoundRectangle2D.Double(-48, 4, 10, 62, 4, 4));
        g.fill(new RoundRectangle2D.Double(38, 4, 10, 62, 4, 4));
        if (open < 0.6) {
            Graphics2D k = (Graphics2D) g.create();
            k.setComposite(AlphaComposite.SrcOver.derive((float) (1 - open / 0.6)));
            k.setColor(gold);
            k.fill(new RoundRectangle2D.Double(-13, -8, 26, 30, 7, 7));
            k.setColor(body2);
            k.fill(new Ellipse2D.Double(-4, 2, 8, 8));
            k.fill(new RoundRectangle2D.Double(-2, 7, 4, 10, 2, 2));
            k.dispose();
        }

        // glossy highlight
        g.setPaint(new LinearGradientPaint(-70, 0, 70, 60, new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, 38), new Color(255, 255, 255, 0)}));
        g.fill(body);
        g.dispose();
    }

    private void paintCard(Graphics2D g0, double cx, double cy, double scale, double alpha) {

        if (scale <= 0.01) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        RewardFx.quality(g);

        int cw = 176;
        int ch = 158;
        double rise = easeOutCubic((ot() - 0.4) / 0.9);
        double targetY = getHeight() * 0.53;
        double startY = cy - 40;
        double y = startY + (targetY - startY) * rise + (state == State.REVEAL ? Math.sin(time * 1.8) * 2.5 : 0);

        g.translate(cx, y);
        g.scale(scale, scale);
        g.setComposite(AlphaComposite.SrcOver.derive((float) clamp01(alpha)));

        double x0 = -cw / 2.0;
        double y0 = -ch / 2.0;

        // aura behind the card
        RewardFx.halo(g, 0, 0, 150 + tier * 12, rarityColor, Math.max(1, tier));

        // card body
        Shape card = new RoundRectangle2D.Double(x0, y0, cw, ch, 20, 20);
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new RoundRectangle2D.Double(x0 + 3, y0 + 6, cw, ch, 20, 20));
        g.setPaint(new GradientPaint(0, (float) y0, Anim.mix(new Color(30, 41, 59), rarityColor, 0.22f),
                0, (float) (y0 + ch), new Color(20, 28, 48)));
        g.fill(card);

        // art
        int ax = (int) x0 + 10;
        int ay = (int) y0 + 10;
        int aw = cw - 20;
        int ah = 86;
        Graphics2D a = (Graphics2D) g.create();
        a.clip(new RoundRectangle2D.Double(ax, ay, aw, ah, 14, 14));
        a.setColor(new Color(0, 0, 0, 70));
        a.fill(new Rectangle2D.Double(ax, ay, aw, ah));
        if (!"other".equals(kind)) {
            RewardArt.paintThumb(a, kind, ax, ay, aw, ah, name, rarity);
        } else {
            RewardFx.halo(a, ax + aw / 2.0, ay + ah / 2.0, 54, rarityColor, Math.max(1, tier));
            Icons.paint(a, Icons.Type.GIFT, ax + aw / 2f - 24, ay + ah / 2f - 24, 48, rarityColor);
            RewardFx.motes(a, ax + aw / 2.0, ay + ah / 2.0, 60, 40, 8, rarityColor, tier, 11, true, 2.2);
        }
        a.dispose();

        // name (shrinks to fit)
        float size = 14f;
        g.setFont(Theme.bold(size));
        FontMetrics fm = g.getFontMetrics();
        while (size > 10f && fm.stringWidth(name) > cw - 20) {
            size -= 1f;
            g.setFont(Theme.bold(size));
            fm = g.getFontMetrics();
        }
        g.setColor(Color.WHITE);
        g.drawString(name, (int) (-fm.stringWidth(name) / 2.0), (int) y0 + ah + 36);

        // rarity pill
        String label = rarity.substring(0, 1).toUpperCase() + rarity.substring(1).toLowerCase();
        g.setFont(Theme.bold(11));
        fm = g.getFontMetrics();
        int pw = fm.stringWidth(label) + 26;
        int py = (int) y0 + ah + 46;
        g.setColor(RewardFx.alpha(rarityColor, 50));
        g.fillRoundRect(-pw / 2, py, pw, 22, 22, 22);
        g.setColor(RewardFx.alpha(rarityColor, 190));
        g.drawRoundRect(-pw / 2, py, pw, 22, 22, 22);
        g.setColor(rarityColor);
        g.drawString(label, -fm.stringWidth(label) / 2, py + 15);

        // glossy edge + shimmer across the card
        Graphics2D s = (Graphics2D) g.create();
        s.clip(card);
        RewardFx.gloss(s, x0, y0, cw, ch, Math.max(1, tier));
        s.dispose();
        RewardFx.edgeGlow(g, x0, y0, cw, ch, 20, rarityColor, Math.max(1, tier));
        g.setStroke(new BasicStroke(2f));
        g.setColor(RewardFx.alpha(rarityColor, 220));
        g.draw(new RoundRectangle2D.Double(x0 + 1, y0 + 1, cw - 2, ch - 2, 20, 20));

        // sparkles around the card for epic / legendary
        if (tier >= 2) {
            RewardFx.motes(g, 0, 0, cw * 0.62, ch * 0.62, 6 + tier * 3, rarityColor, tier, 5, true, 2.6);
        }
        if (tier == 4) {
            RewardFx.flare(g, x0 + cw * (0.15 + 0.7 * RewardFx.phase(3400)), y0 + 26, 14,
                    (int) (120 + 100 * RewardFx.pulse(900)));
        }

        // "NEW" tag
        g.setFont(Theme.bold(10));
        fm = g.getFontMetrics();
        g.setColor(new Color(34, 197, 94));
        g.fillRoundRect((int) x0 + cw - 44, (int) y0 - 8, 44, 18, 18, 18);
        g.setColor(Color.WHITE);
        g.drawString("NEW!", (int) x0 + cw - 44 + (44 - fm.stringWidth("NEW!")) / 2, (int) y0 + 5);
        g.dispose();
    }
}
