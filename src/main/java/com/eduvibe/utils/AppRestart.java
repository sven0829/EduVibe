package com.eduvibe.util;

import java.util.ArrayList;
import java.util.List;

/**
 * com.eduvibe.util.AppRestart  (NEW FILE)
 *
 * Starts a fresh copy of EduVibe with the same Java command and then closes this one,
 * so the user never has to close and reopen the app by hand (used for the Dark/Light switch).
 */
public final class AppRestart {

    private AppRestart() {
    }

    /** @return false if the app could not start a new copy (nothing is closed in that case). */
    public static boolean relaunch() {
        return relaunch(-1);
    }

    /** Same, but the new copy opens straight into this user's session (no sign-in screen). */
    public static boolean relaunch(int keepUserId) {
        try {
            ProcessHandle.Info info = ProcessHandle.current().info();
            String command = info.command().orElse(null);
            if (command == null) {
                return false;
            }
            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            if (keepUserId > 0) {
                String token = SessionHandoff.prepare(keepUserId);
                if (token != null) {
                    cmd.add("-Deduvibe.resume=" + token);
                }
            }
            String[] args = info.arguments().orElse(null);
            if (args == null || args.length == 0) {
                return false;
            }
            for (String a : args) {
                cmd.add(a);
            }
            new ProcessBuilder(cmd).inheritIO().start();
            Runtime.getRuntime().halt(0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
