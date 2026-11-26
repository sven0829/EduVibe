package com.eduvibe.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntertainmentTimeTrackerTest {

    private final AtomicLong now = new AtomicLong(1_700_000_000_000L);
    private final EntertainmentTimeTracker t = new EntertainmentTimeTracker(now::get, null);

    /** Simulates `seconds` of wall time with a tick every second, optionally with user activity. */
    private void run(int seconds, boolean activity) {
        for (int i = 0; i < seconds; i++) {
            now.addAndGet(1000);
            if (activity) {
                t.touch();
            }
            t.tick();
        }
    }

    @Test
    void startsAtZeroAndTracksRunningState() {
        assertEquals(0, t.minutesUsed());
        assertFalse(t.isRunning());
        t.enter();
        assertTrue(t.isRunning());
        t.leave();
        assertFalse(t.isRunning());
        assertEquals(0, t.minutesUsed());
    }

    @Test
    void countsOnlyWhileInFeed() {
        t.enter();
        run(10, true);
        t.leave();
        run(30, true);
        assertEquals(10, t.secondsUsed());
        t.enter();
        run(5, true);
        assertEquals(15, t.secondsUsed());
    }

    @Test
    void enterIsIdempotent() {
        t.enter();
        t.enter();
        t.enter();
        run(10, true);
        assertEquals(10, t.secondsUsed());
    }

    @Test
    void doesNotCountWhileWindowInBackground() {
        t.enter();
        run(10, true);
        t.setWindowActive(false);
        run(60, true);
        t.setWindowActive(true);
        run(5, true);
        assertEquals(15, t.secondsUsed());
    }

    @Test
    void stopsCountingAfterIdleLimit() {
        t.enter();
        run(60, true);                                   // 60 s active
        run((int) (EntertainmentTimeTracker.IDLE_LIMIT_MS / 1000) + 120, false);
        long used = t.secondsUsed();
        assertEquals(60 + EntertainmentTimeTracker.IDLE_LIMIT_MS / 1000, used, 2);
        assertFalse(t.isRunning());
        t.touch();                                       // user returns
        assertTrue(t.isRunning());
    }

    @Test
    void ignoresHugeClockJumps() {
        t.enter();
        run(5, true);
        now.addAndGet(3_600_000L);                       // laptop slept for an hour
        t.touch();
        t.tick();
        assertEquals(7, t.secondsUsed(), 2);
    }

    @Test
    void reminderFiresOncePerIntervalThenAfterFifteenMore() {
        t.enter();
        run(29 * 60, true);
        assertFalse(t.reminderDue(30));
        run(60, true);
        assertTrue(t.reminderDue(30));
        assertFalse(t.reminderDue(30));                  // not repeated
        run(14 * 60, true);
        assertFalse(t.reminderDue(30));
        run(61, true);
        assertTrue(t.reminderDue(30));                   // +15 min later
        assertTrue(t.secondsUsed() >= 45 * 60);          // timer never reset
    }

    @Test
    void remindersOffNeverFire() {
        t.enter();
        run(100 * 60, true);
        assertFalse(t.reminderDue(0));
    }

    @Test
    void changingLimitDoesNotFireInstantlyWhenAlreadyPast() {
        t.enter();
        run(40 * 60, true);
        t.limitChanged(30);
        assertFalse(t.reminderDue(30));
        t.limitChanged(60);
        assertEquals(60 * 60, t.nextReminderSeconds(60));
    }

    @Test
    void formatsTime() {
        assertEquals("00:00", EntertainmentTimeTracker.format(0));
        assertEquals("05:07", EntertainmentTimeTracker.format(307));
        assertEquals("1:02:03", EntertainmentTimeTracker.format(3723));
    }
}
