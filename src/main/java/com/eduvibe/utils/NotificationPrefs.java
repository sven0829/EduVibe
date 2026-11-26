package com.eduvibe.util;

import java.util.prefs.Preferences;

/**
 * com.eduvibe.util.NotificationPrefs  (NEW FILE)
 *
 * Which kinds of notifications a person wants to see. Saved per user on this computer
 * (no database change). Everything is on by default. A hidden kind is left out of the
 * Notifications list and the unread badge; the notification itself is still stored.
 */
public final class NotificationPrefs {

    public static final String LIKES = "likes";
    public static final String COMMENTS = "comments";
    public static final String SYSTEM = "system";    // rewards, reminders and other system messages

    private NotificationPrefs() {
    }

    private static Preferences node(int userId) {
        return Preferences.userRoot().node("com/eduvibe/notifications/" + userId);
    }

    public static boolean isEnabled(int userId, String kind) {
        try {
            return node(userId).getBoolean(kind, true);
        } catch (Exception e) {
            return true;
        }
    }

    public static void setEnabled(int userId, String kind, boolean enabled) {
        try {
            Preferences p = node(userId);
            p.putBoolean(kind, enabled);
            p.flush();
        } catch (Exception ignored) {
            // the choice still applies until the app closes
        }
    }

    /** The kind a stored notification type belongs to. */
    public static String kindOf(String type) {
        String t = type == null ? "" : type.toLowerCase();
        if (t.equals("like")) {
            return LIKES;
        }
        if (t.equals("comment")) {
            return COMMENTS;
        }
        return SYSTEM;
    }

    public static boolean shows(int userId, String type) {
        return isEnabled(userId, kindOf(type));
    }
}
