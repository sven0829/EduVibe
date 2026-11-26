package com.eduvibe.util;

import java.security.SecureRandom;
import java.util.prefs.Preferences;

/**
 * com.eduvibe.util.SessionHandoff  (NEW FILE)
 *
 * Lets the app restart itself (theme switch) without asking the user to sign in again.
 * The old window stores a one-time token that is valid for 30 seconds; the new process
 * receives the same token on its command line, uses it once and it is deleted.
 * No password is ever stored.
 */
public final class SessionHandoff {

    private static final String NODE = "com/eduvibe/handoff";
    private static final long VALID_MS = 30_000;

    private SessionHandoff() {
    }

    /** Creates and stores a one-time token for this user; returns the token. */
    public static String prepare(int userId) {
        byte[] raw = new byte[16];
        new SecureRandom().nextBytes(raw);
        StringBuilder token = new StringBuilder();
        for (byte b : raw) {
            token.append(String.format("%02x", b));
        }
        try {
            Preferences p = Preferences.userRoot().node(NODE);
            p.put("token", token.toString());
            p.putInt("user", userId);
            p.putLong("expires", System.currentTimeMillis() + VALID_MS);
            p.flush();
        } catch (Exception e) {
            return null;
        }
        return token.toString();
    }

    /** @return the user id if the token matches and is still valid (it is then deleted), else -1. */
    public static int consume(String token) {
        if (token == null || token.isBlank()) {
            return -1;
        }
        try {
            Preferences p = Preferences.userRoot().node(NODE);
            String saved = p.get("token", "");
            long expires = p.getLong("expires", 0);
            int user = p.getInt("user", -1);
            p.remove("token");
            p.remove("user");
            p.remove("expires");
            p.flush();
            if (!saved.isEmpty() && saved.equals(token) && System.currentTimeMillis() <= expires) {
                return user;
            }
        } catch (Exception ignored) {
            // fall through to normal sign-in
        }
        return -1;
    }
}
