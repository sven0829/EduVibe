package com.eduvibe.util;

import java.awt.Color;

/**
 * com.eduvibe.util.RewardStyle  (NEW FILE)
 * Picks display colours for an owned reward from its own name and rarity (no stored colour needed).
 */
public final class RewardStyle {

    private RewardStyle() {
    }

    public static Color rarityColor(String rarity) {
        String r = rarity == null ? "" : rarity.toLowerCase();
        return switch (r) {
            case "legendary" -> new Color(251, 191, 36);
            case "epic" -> new Color(168, 85, 247);
            case "rare" -> new Color(59, 130, 246);
            case "uncommon" -> new Color(34, 197, 94);
            default -> new Color(148, 163, 184);
        };
    }

    /** Main colour: rarity colour nudged by the name so each reward looks a little different. */
    public static Color primary(String name, String rarity) {
        Color base = rarityColor(rarity);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        int h = name == null ? 0 : Math.abs(name.hashCode());
        float hue = (hsb[0] + ((h % 40) - 20) / 360f + 1f) % 1f;
        return Color.getHSBColor(hue, Math.max(0.55f, hsb[1]), Math.max(0.85f, hsb[2]));
    }

    public static Color secondary(String name, String rarity) {
        Color p = primary(name, rarity);
        float[] hsb = Color.RGBtoHSB(p.getRed(), p.getGreen(), p.getBlue(), null);
        return Color.getHSBColor((hsb[0] + 0.12f) % 1f, hsb[1], Math.max(0.5f, hsb[2] - 0.2f));
    }
}
