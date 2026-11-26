package com.eduvibe.util;

import java.time.LocalDate;
import java.util.function.LongSupplier;
import java.util.prefs.Preferences;

/**
 * com.eduvibe.util.EntertainmentTimeTracker  (REPLACE)
 *
 * Counts ACTIVE seconds spent on the Entertainment / Reels feeds today.
 * Time only counts while: the user is on a feed, the window is focused, and the
 * user did something (mouse / key / wheel) within the idle limit.
 *
 * One instance is driven by ONE 1-second timer calling {@link #tick()}; enter()/leave()
 * are idempotent, so navigating around can never create a second tracking session.
 * Totals and the reminder schedule are saved per user per day (java.util.prefs),
 * so they survive restarts. Nothing here blocks or restricts any feed.
 */
public final class EntertainmentTimeTracker {

    public static final long IDLE_LIMIT_MS = 5 * 60_000L;
    public static final int FOLLOW_UP_MINUTES = 15;
    private static final long MAX_STEP_MS = 2_000L;   // ignores big jumps (sleep / debugger)

    private final LongSupplier clock;
    private final Preferences prefs;               // null = no persistence

    private long totalMillis = 0;
    private long nextReminderSec = 0;               // 0 = not scheduled yet
    private boolean inFeed = false;
    private boolean windowActive = true;
    private long lastTick = -1;
    private long lastActivity;
    private LocalDate day;
    private long lastSaved = 0;

    /** In-memory tracker (tests, or when no user is known). */
    public EntertainmentTimeTracker() {
        this(System::currentTimeMillis, null);
    }

    /** Tracker whose totals persist for this user. */
    public static EntertainmentTimeTracker forUser(int userId) {
        Preferences p;
        try {
            p = Preferences.userRoot().node("com/eduvibe/entertainment/" + userId);
        } catch (RuntimeException ex) {
            p = null;
        }
        return new EntertainmentTimeTracker(System::currentTimeMillis, p);
    }

    public EntertainmentTimeTracker(LongSupplier clock, Preferences prefs) {
        this.clock = clock;
        this.prefs = prefs;
        this.lastActivity = clock.getAsLong();
        this.day = today();
        load();
    }

    // ---------------- state changes ----------------

    public synchronized void enter() {
        if (!inFeed) {
            inFeed = true;
            lastTick = clock.getAsLong();
            lastActivity = lastTick;
        }
    }

    public synchronized void leave() {
        if (inFeed) {
            accumulate();
            inFeed = false;
            lastTick = -1;
            save(true);
        }
    }

    public synchronized void setWindowActive(boolean active) {
        if (!active) {
            accumulate();
        }
        windowActive = active;
        lastTick = clock.getAsLong();
        if (active) {
            lastActivity = lastTick;
        }
    }

    /** Call on any user input. */
    public synchronized void touch() {
        lastActivity = clock.getAsLong();
    }

    /** Call about once per second. */
    public synchronized void tick() {
        rollDayIfNeeded();
        accumulate();
        lastTick = clock.getAsLong();
        if (lastTick - lastSaved > 10_000L) {
            save(false);
        }
    }

    private void accumulate() {
        long now = clock.getAsLong();
        if (inFeed && windowActive && lastTick >= 0) {
            long end = Math.min(now, lastActivity + IDLE_LIMIT_MS);
            long from = Math.max(lastTick, 0);
            long step = Math.min(end - from, MAX_STEP_MS);
            if (step > 0) {
                totalMillis += step;
            }
        }
        lastTick = now;
    }

    // ---------------- queries ----------------

    public synchronized boolean isRunning() {
        return inFeed && windowActive && clock.getAsLong() - lastActivity <= IDLE_LIMIT_MS;
    }

    public synchronized boolean isInFeed() {
        return inFeed;
    }

    public synchronized long secondsUsed() {
        return totalMillis / 1000L;
    }

    public synchronized int minutesUsed() {
        return (int) (totalMillis / 60_000L);
    }

    /** "MM:SS" below one hour, otherwise "H:MM:SS". */
    public static String format(long seconds) {
        long s = Math.max(0, seconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    // ---------------- reminders ----------------

    /** Seconds at which the next reminder fires (limit-based until first scheduled). */
    public synchronized long nextReminderSeconds(int limitMinutes) {
        if (limitMinutes <= 0) {
            return 0;
        }
        return nextReminderSec > 0 ? nextReminderSec : limitMinutes * 60L;
    }

    /** 0..1 progress toward the next reminder (0 when reminders are off). */
    public synchronized double progress(int limitMinutes) {
        long target = nextReminderSeconds(limitMinutes);
        if (target <= 0) {
            return 0;
        }
        // after a follow-up, the bar measures the follow-up interval only
        long base = nextReminderSec > 0 ? Math.max(0, nextReminderSec - FOLLOW_UP_MINUTES * 60L) : 0;
        if (base == 0 || nextReminderSec <= 0) {
            return Math.min(1.0, secondsUsed() / (double) target);
        }
        return Math.min(1.0, Math.max(0, secondsUsed() - base) / (double) (target - base));
    }

    /**
     * True exactly once per reminder interval. After firing, the next reminder is
     * FOLLOW_UP_MINUTES of additional browsing later; the timer is never reset.
     */
    public synchronized boolean reminderDue(int limitMinutes) {
        if (limitMinutes <= 0 || !inFeed) {
            return false;
        }
        long used = secondsUsed();
        long target = nextReminderSeconds(limitMinutes);
        if (used < target) {
            return false;
        }
        nextReminderSec = used + FOLLOW_UP_MINUTES * 60L;
        save(true);
        return true;
    }

    /** The user changed the interval / switched reminders on or off. */
    public synchronized void limitChanged(int limitMinutes) {
        long used = secondsUsed();
        if (limitMinutes <= 0) {
            nextReminderSec = 0;
        } else {
            long target = limitMinutes * 60L;
            nextReminderSec = used < target ? 0 : used + FOLLOW_UP_MINUTES * 60L;
        }
        save(true);
    }

    // ---------------- persistence ----------------

    /** Saves now (call on logout / window close). */
    public synchronized void flush() {
        accumulate();
        save(true);
    }

    private LocalDate today() {
        return java.time.Instant.ofEpochMilli(clock.getAsLong())
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private void rollDayIfNeeded() {
        LocalDate now = today();
        if (!now.equals(day)) {
            day = now;
            totalMillis = 0;
            nextReminderSec = 0;
            save(true);
        }
    }

    private void load() {
        if (prefs == null) {
            return;
        }
        try {
            if (day.toString().equals(prefs.get("day", ""))) {
                totalMillis = Math.max(0, prefs.getLong("seconds", 0)) * 1000L;
                nextReminderSec = Math.max(0, prefs.getLong("next", 0));
            }
        } catch (RuntimeException ignored) {
            // start fresh
        }
    }

    private void save(boolean force) {
        lastSaved = clock.getAsLong();
        if (prefs == null) {
            return;
        }
        try {
            prefs.put("day", day.toString());
            prefs.putLong("seconds", totalMillis / 1000L);
            prefs.putLong("next", nextReminderSec);
            if (force) {
                prefs.flush();
            }
        } catch (Exception ignored) {
            // persistence is best-effort
        }
    }
}
