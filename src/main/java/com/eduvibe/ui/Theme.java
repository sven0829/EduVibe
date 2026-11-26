package com.eduvibe.ui;

import com.eduvibe.ui.components.DarkScrollBarUI;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.util.prefs.Preferences;

/**
 * NEW FILE - com.eduvibe.ui.Theme
 *
 * Single source of truth for EduVibe colors, fonts and global Swing defaults.
 * Call Theme.install() once, before any window is created.
 */
public final class Theme {

    private Theme() {
    }

    // ---- Light / dark choice ----
    // Read once when the class loads (so a change applies after restart).
    // Override for testing:  -Deduvibe.theme=light
    private static final boolean LIGHT = loadLight();

    /** The dark style: "midnight" (default), "ocean" or "plum". Also read once, applied after a restart. */
    private static final String STYLE = loadStyle();

    private static String loadStyle() {
        String forced = System.getProperty("eduvibe.style");
        if (forced != null) {
            return forced.toLowerCase();
        }
        try {
            return Preferences.userRoot().node("eduvibe/ui").get("style", "midnight");
        } catch (Exception e) {
            return "midnight";
        }
    }

    public static String style() {
        return LIGHT ? "midnight" : STYLE;
    }

    /** Saves the dark style; it takes effect the next time EduVibe starts. */
    public static void saveStyle(String style) {
        try {
            Preferences p = Preferences.userRoot().node("eduvibe/ui");
            p.put("style", style);
            p.flush();
        } catch (Exception ignored) {
            // keep running with the current look
        }
    }

    /** Picks a dark colour for the current style. */
    private static Color dk(Color midnight, Color ocean, Color plum) {
        return "ocean".equals(STYLE) ? ocean : "plum".equals(STYLE) ? plum : midnight;
    }

    private static boolean loadLight() {
        String forced = System.getProperty("eduvibe.theme");
        if (forced != null) {
            return "light".equalsIgnoreCase(forced);
        }
        try {
            return "light".equals(
                    Preferences.userRoot().node("eduvibe/ui").get("theme", "dark"));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isLight() {
        return LIGHT;
    }

    /** Saves the choice; it takes effect the next time EduVibe starts. */
    public static void saveLight(boolean light) {
        try {
            Preferences p = Preferences.userRoot().node("eduvibe/ui");
            p.put("theme", light ? "light" : "dark");
            p.flush();
        } catch (Exception ignored) {
            // keep running with the current theme
        }
    }

    // ---- Palette: deep midnight navy with indigo/violet energy ----
    public static final Color BACKGROUND  = LIGHT ? new Color(243, 245, 252) : dk(new Color(9, 14, 30), new Color(5, 18, 26), new Color(19, 9, 27));
    public static final Color SIDEBAR     = LIGHT ? new Color(255, 255, 255) : dk(new Color(13, 20, 41), new Color(8, 27, 37), new Color(27, 13, 39));
    public static final Color CARD        = LIGHT ? new Color(255, 255, 255) : dk(new Color(21, 30, 56), new Color(13, 37, 51), new Color(39, 21, 57));
    public static final Color CARD_LIGHT  = LIGHT ? new Color(232, 236, 248) : dk(new Color(32, 44, 80), new Color(21, 55, 75), new Color(57, 33, 81));
    public static final Color INPUT       = LIGHT ? new Color(243, 245, 252) : dk(new Color(14, 21, 42), new Color(8, 25, 35), new Color(25, 13, 37));
    public static final Color BORDER      = LIGHT ? new Color(212, 219, 238) : dk(new Color(46, 60, 104), new Color(29, 71, 97), new Color(85, 51, 119));

    public static final Color BLUE        = new Color(59, 130, 246);
    public static final Color BLUE_HOVER  = new Color(37, 99, 235);
    public static final Color BLUE_SOFT   = LIGHT ? new Color(222, 233, 255) : new Color(30, 52, 112);
    public static final Color BLUE_LIGHT  = LIGHT ? new Color(37, 99, 235) : new Color(124, 177, 255);
    /** Second brand colour: gradients run from BLUE to this violet. */
    public static final Color VIOLET      = new Color(124, 92, 250);

    public static final Color TEXT        = LIGHT ? new Color(16, 22, 46) : new Color(233, 238, 253);
    public static final Color MUTED       = LIGHT ? new Color(94, 108, 144) : new Color(139, 154, 192);

    public static final Color GREEN       = LIGHT ? new Color(22, 163, 106) : new Color(52, 211, 153);
    public static final Color RED         = LIGHT ? new Color(225, 45, 70) : new Color(251, 99, 118);
    public static final Color YELLOW      = LIGHT ? new Color(202, 138, 4) : new Color(251, 191, 36);
    public static final Color PURPLE      = LIGHT ? new Color(124, 58, 237) : new Color(176, 150, 255);
    public static final Color GOLD        = LIGHT ? new Color(202, 138, 4) : new Color(251, 191, 36);

    /** Brand gradient across a rectangle (BLUE to VIOLET), slightly brighter when lit. */
    public static Paint brand(float x1, float y1, float x2, float y2, float lift) {
        Color a = lift == 0 ? BLUE : lift > 0 ? mixWhite(BLUE, lift) : mixBlack(BLUE, -lift);
        Color b = lift == 0 ? VIOLET : lift > 0 ? mixWhite(VIOLET, lift) : mixBlack(VIOLET, -lift);
        return new GradientPaint(x1, y1, a, x2, y2, b);
    }

    private static Color mixWhite(Color c, float t) {
        return new Color(Math.round(c.getRed() + (255 - c.getRed()) * t),
                Math.round(c.getGreen() + (255 - c.getGreen()) * t),
                Math.round(c.getBlue() + (255 - c.getBlue()) * t), c.getAlpha());
    }

    private static Color mixBlack(Color c, float t) {
        return new Color(Math.round(c.getRed() * (1 - t)), Math.round(c.getGreen() * (1 - t)),
                Math.round(c.getBlue() * (1 - t)), c.getAlpha());
    }

    // ---- Fonts ----
    // Inter for text, Poppins Bold for big headings. Loaded from the jar (src/main/resources/fonts);
    // if anything is missing the app falls back to the best system font, so it never fails to start.
    private static final Font INTER_REGULAR = loadFont("/fonts/Inter-Regular.otf");
    private static final Font INTER_BOLD = loadFont("/fonts/Inter-SemiBold.otf");
    private static final Font DISPLAY = loadFont("/fonts/Poppins-Bold.ttf");
    private static final String FAMILY = resolveFamily();

    private static Font loadFont(String resource) {
        try (java.io.InputStream in = Theme.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f;
        } catch (Exception | Error e) {
            return null;
        }
    }

    private static String resolveFamily() {
        String[] preferred = {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String wanted : preferred) {
            for (String name : available) {
                if (name.equalsIgnoreCase(wanted)) {
                    return name;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        boolean bold = (style & Font.BOLD) != 0;
        Font base = bold ? (size >= 22 && DISPLAY != null ? DISPLAY : INTER_BOLD) : INTER_REGULAR;
        if (base == null) {
            return new Font(FAMILY, style, 12).deriveFont(style, size);
        }
        return base.deriveFont((style & Font.ITALIC) != 0 ? Font.ITALIC : Font.PLAIN, size);
    }

    public static Font regular(float size) {
        return font(Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return font(Font.BOLD, size);
    }

    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ---- Global defaults ----

    public static void install() {

        System.setProperty("swing.boldMetal", "false");
        com.eduvibe.util.WindowFit.install();

        // Videos are decoded and drawn by JavaFX. On a screen set to 125% / 150% it would draw 1.5x as many
        // pixels in each direction (2.25x the work) only for Swing to scale them again. Drawing at normal
        // size is much lighter on a small laptop. Must be set before JavaFX starts, hence here.
        if (System.getProperty("prism.allowhidpi") == null) {
            System.setProperty("prism.allowhidpi", "false");
        }
        // Optional: JavaFX can draw video with the processor only ("software"), which on some laptops is
        // smoother inside a Swing window than the default graphics path. Chosen with the V key on Reels.
        try {
            if (System.getProperty("prism.order") == null
                    && java.util.prefs.Preferences.userRoot().node("eduvibe/ui").getBoolean("videoSoftware", false)) {
                System.setProperty("prism.order", "sw");
            }
        } catch (Exception ignored) {
            // the default is used
        }

        try {
            // Metal respects setBackground/setForeground on every component,
            // so legacy panels render dark instead of falling back to system gray.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Base font for every component.
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(regular(13)));
            }
        }

        ColorUIResource bg = new ColorUIResource(BACKGROUND);
        ColorUIResource card = new ColorUIResource(CARD);
        ColorUIResource input = new ColorUIResource(INPUT);
        ColorUIResource text = new ColorUIResource(TEXT);
        ColorUIResource muted = new ColorUIResource(MUTED);
        ColorUIResource blue = new ColorUIResource(BLUE);
        ColorUIResource border = new ColorUIResource(BORDER);

        // Remove the Ocean gradients so buttons, boxes and bars are flat.
        for (String key : new String[]{
                "Button.gradient", "ToggleButton.gradient", "CheckBox.gradient",
                "RadioButton.gradient", "ScrollBar.gradient", "Slider.gradient",
                "MenuBar.gradient", "InternalFrame.activeTitleGradient"}) {
            UIManager.put(key, null);
        }

        UIManager.put("PanelUI", "com.eduvibe.ui.components.AuroraPanelUI");
        UIManager.put("ViewportUI", "com.eduvibe.ui.components.AuroraViewportUI");
        UIManager.put("Panel.background", bg);
        UIManager.put("Panel.foreground", text);
        UIManager.put("Label.foreground", text);
        UIManager.put("Label.background", bg);
        UIManager.put("Viewport.background", bg);
        UIManager.put("ScrollPane.background", bg);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Separator.foreground", border);
        UIManager.put("Separator.background", bg);

        UIManager.put("Button.background", new ColorUIResource(CARD_LIGHT));
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.select", new ColorUIResource(BLUE_HOVER));
        UIManager.put("Button.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("ToggleButton.background", new ColorUIResource(CARD_LIGHT));
        UIManager.put("ToggleButton.foreground", text);
        UIManager.put("ToggleButton.select", new ColorUIResource(BLUE_HOVER));
        UIManager.put("ToggleButton.focus", new ColorUIResource(new Color(0, 0, 0, 0)));

        UIManager.put("CheckBox.background", bg);
        UIManager.put("CheckBox.foreground", muted);
        UIManager.put("CheckBox.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("RadioButton.background", bg);
        UIManager.put("RadioButton.foreground", muted);

        UIManager.put("TextField.background", input);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.caretForeground", text);
        UIManager.put("TextField.selectionBackground", blue);
        UIManager.put("TextField.selectionForeground", text);
        UIManager.put("TextField.inactiveForeground", muted);
        UIManager.put("PasswordField.background", input);
        UIManager.put("PasswordField.foreground", text);
        UIManager.put("PasswordField.caretForeground", text);
        UIManager.put("PasswordField.selectionBackground", blue);
        UIManager.put("TextArea.background", input);
        UIManager.put("TextArea.foreground", text);
        UIManager.put("TextArea.caretForeground", text);
        UIManager.put("TextArea.selectionBackground", blue);
        UIManager.put("TextArea.selectionForeground", text);
        UIManager.put("TextPane.background", input);
        UIManager.put("TextPane.foreground", text);

        UIManager.put("ComboBox.background", input);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.selectionBackground", blue);
        UIManager.put("ComboBox.selectionForeground", text);
        UIManager.put("ComboBox.buttonBackground", new ColorUIResource(CARD_LIGHT));
        UIManager.put("ComboBox.disabledBackground", input);
        UIManager.put("ComboBox.disabledForeground", muted);
        UIManager.put("List.background", input);
        UIManager.put("List.foreground", text);
        UIManager.put("List.selectionBackground", blue);
        UIManager.put("List.selectionForeground", text);

        UIManager.put("Table.background", card);
        UIManager.put("Table.foreground", text);
        UIManager.put("Table.gridColor", border);
        UIManager.put("Table.selectionBackground", new ColorUIResource(BLUE_SOFT));
        UIManager.put("Table.selectionForeground", text);
        UIManager.put("TableHeader.background", new ColorUIResource(SIDEBAR));
        UIManager.put("TableHeader.foreground", text);

        UIManager.put("TabbedPane.background", bg);
        UIManager.put("TabbedPane.foreground", text);
        UIManager.put("TabbedPane.selected", card);
        UIManager.put("TabbedPane.contentAreaColor", bg);
        UIManager.put("TabbedPane.light", border);
        UIManager.put("TabbedPane.highlight", border);
        UIManager.put("TabbedPane.shadow", bg);
        UIManager.put("TabbedPane.darkShadow", bg);
        UIManager.put("TabbedPane.focus", blue);
        UIManager.put("TabbedPane.borderHightlightColor", border);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));

        // ---- Flat buttons / drop-downs / borders for every stock widget ----
        UIManager.put("ButtonUI", com.eduvibe.ui.components.FlatButtonUI.class.getName());
        UIManager.put("ComboBoxUI", com.eduvibe.ui.components.FlatComboBoxUI.class.getName());
        UIManager.put("Button.disabledText", new ColorUIResource(MUTED));
        UIManager.put("Button.border", new javax.swing.plaf.BorderUIResource(BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        javax.swing.border.Border fieldBorder = new javax.swing.plaf.BorderUIResource.CompoundBorderUIResource(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
        UIManager.put("TextField.border", fieldBorder);
        UIManager.put("PasswordField.border", fieldBorder);
        UIManager.put("TextArea.border", new javax.swing.plaf.BorderUIResource(BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        UIManager.put("OptionPane.buttonAreaBorder", new javax.swing.plaf.BorderUIResource(BorderFactory.createEmptyBorder(8, 16, 16, 16)));
        UIManager.put("OptionPane.border", new javax.swing.plaf.BorderUIResource(BorderFactory.createEmptyBorder(16, 20, 4, 20)));
        UIManager.put("OptionPane.informationIcon", com.eduvibe.ui.components.Icons.icon(com.eduvibe.ui.components.Icons.Type.INFO, 32, BLUE));
        UIManager.put("OptionPane.questionIcon", com.eduvibe.ui.components.Icons.icon(com.eduvibe.ui.components.Icons.Type.INFO, 32, BLUE));
        UIManager.put("OptionPane.warningIcon", com.eduvibe.ui.components.Icons.icon(com.eduvibe.ui.components.Icons.Type.ALERT, 32, YELLOW));
        UIManager.put("OptionPane.errorIcon", com.eduvibe.ui.components.Icons.icon(com.eduvibe.ui.components.Icons.Type.ALERT, 32, RED));
        UIManager.put("TableHeader.cellBorder", new javax.swing.plaf.BorderUIResource(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10))));
        UIManager.put("TableHeader.font", new javax.swing.plaf.FontUIResource(bold(12)));
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.focusCellHighlightBorder", new javax.swing.plaf.BorderUIResource(
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        UIManager.put("TabbedPane.unselectedBackground", new ColorUIResource(CARD));
        UIManager.put("TabbedPane.selectedForeground", new ColorUIResource(TEXT));
        UIManager.put("TabbedPane.tabAreaBackground", bg);
        UIManager.put("TabbedPane.borderColor", border);

        UIManager.put("OptionPane.background", bg);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("OptionPane.foreground", text);
        UIManager.put("ToolTip.background", new ColorUIResource(CARD_LIGHT));
        UIManager.put("ToolTip.foreground", text);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("PopupMenu.background", card);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("MenuItem.background", card);
        UIManager.put("MenuItem.foreground", text);
        UIManager.put("MenuItem.selectionBackground", blue);
        UIManager.put("MenuItem.selectionForeground", text);

        UIManager.put("ProgressBar.background", border);
        UIManager.put("ProgressBar.foreground", blue);
        UIManager.put("ProgressBar.selectionForeground", text);
        UIManager.put("ProgressBar.selectionBackground", text);

        // Slim dark scroll bars everywhere (feeds, tables, dialogs).
        UIManager.put("ScrollBarUI", DarkScrollBarUI.class.getName());
        UIManager.put("ScrollBar.width", 10);
    }
}
