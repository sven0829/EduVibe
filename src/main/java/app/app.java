package app;

import com.eduvibe.ui.LoginFrame;
import com.eduvibe.ui.Theme;
import javax.swing.SwingUtilities;

public class app {

    public static void main(String[] args) {

        // Must be set before any Swing/AWT class is used. Uses the plain
        // Windows GDI pipeline, which avoids blank/half-drawn windows seen on
        // some graphics drivers and newer JDKs (e.g. JDK 26).
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.noddraw", "true");

        // Print any error that happens while drawing, instead of hiding it.
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            System.err.println("Uncaught error in " + thread.getName() + ":");
            error.printStackTrace();
        });

        final int resumeUser = com.eduvibe.util.SessionHandoff.consume(
                System.getProperty("eduvibe.resume"));

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            if (resumeUser > 0) {
                resumeSession(resumeUser);
            } else {
                new LoginFrame().setVisible(true);
            }
        });
    }

    /** Reopens straight into the dashboard after the app restarted itself (e.g. theme switch). */
    private static void resumeSession(int userId) {
        new javax.swing.SwingWorker<java.util.Optional<com.eduvibe.model.User>, Void>() {
            @Override
            protected java.util.Optional<com.eduvibe.model.User> doInBackground() throws Exception {
                return new com.eduvibe.dao.UserDAO().findById(userId);
            }

            @Override
            protected void done() {
                try {
                    java.util.Optional<com.eduvibe.model.User> user = get();
                    if (user.isPresent() && "active".equalsIgnoreCase(user.get().getStatus())) {
                        com.eduvibe.ui.DashboardFrame dashboard = new com.eduvibe.ui.DashboardFrame(user.get());
                        dashboard.setVisible(true);
                        dashboard.showPage("SETTINGS");
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                new LoginFrame().setVisible(true);
            }
        }.execute();
    }
}
