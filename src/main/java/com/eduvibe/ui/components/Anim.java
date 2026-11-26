package com.eduvibe.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * NEW FILE - tiny animation helpers shared by the components.
 * Fade = a 0..1 value that eases toward a target (used for hover / press fades).
 */
public final class Anim {

    private Anim() {
    }

    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    /** Eases a value toward 0 or 1 and repaints its component every frame. */
    public static final class Fade {

        private final JComponent owner;
        private final float speed;
        private final Timer timer;
        private float value;
        private float target;

        public Fade(JComponent owner) {
            this(owner, 0.22f);
        }

        public Fade(JComponent owner, float speed) {
            this.owner = owner;
            this.speed = speed;
            this.timer = new Timer(15, e -> step());
            this.timer.setCoalesce(true);
        }

        public void to(boolean on) {
            target = on ? 1f : 0f;
            if (!timer.isRunning() && value != target) {
                timer.start();
            }
        }

        public float value() {
            return value;
        }

        private void step() {
            if (!owner.isShowing()) {
                value = target;
                timer.stop();
                return;
            }
            value += (target - value) * speed + Math.signum(target - value) * 0.01f;
            if (Math.abs(target - value) < 0.02f) {
                value = target;
                timer.stop();
            }
            owner.repaint();
        }
    }
}
