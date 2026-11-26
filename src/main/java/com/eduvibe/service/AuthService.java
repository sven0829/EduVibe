
package com.eduvibe.service;

import com.eduvibe.config.DatabaseConnection;
import com.eduvibe.dao.UserDAO;
import com.eduvibe.dao.AdminNotificationDAO;
import com.eduvibe.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public int register(String fullName, String username,
                        String email, char[] password)
            throws SQLException {

        validateRegistration(fullName, username, email, password);

        String cleanFullName = fullName.trim();
        String cleanUsername = username.trim();
        String cleanEmail = email.trim();

        if (userDAO.usernameExists(cleanUsername)) {
            throw new IllegalArgumentException(
                    "Username is already taken.");
        }

        if (userDAO.emailExists(cleanEmail)) {
            throw new IllegalArgumentException(
                    "Email is already registered.");
        }

        String passwordHash = BCrypt.hashpw(
                new String(password),
                BCrypt.gensalt(12)
        );

        int newUserId = userDAO.create(
                cleanFullName,
                cleanUsername,
                cleanEmail,
                passwordHash
        );

        // Record account registration in Admin Activity Log.
        recordActivity(
                newUserId,
                "user_registered",
                "New account registered: @" + cleanUsername
        );

        // Notify administrators.
        try {
            AdminNotificationDAO.notifyAdmins(
                    "New user registered: @" + cleanUsername + "."
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return newUserId;
    }

    /**
     * Logs in with either a username or an email address.
     * Usernames cannot contain '@' (see validateRegistration), so the
     * presence of '@' reliably identifies an email.
     */
    public Optional<User> login(String identifier, char[] password)
            throws SQLException {

        if (identifier == null || identifier.isBlank()
                || password == null || password.length == 0) {
            throw new IllegalArgumentException(
                    "Enter your email or username and your password.");
        }

        String cleanIdentifier = identifier.trim();

        Optional<User> user = cleanIdentifier.contains("@")
                ? userDAO.findByEmail(cleanIdentifier)
                : userDAO.findByUsername(cleanIdentifier);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> savedHash =
                userDAO.findPasswordHashById(user.get().getId());

        if (savedHash.isEmpty()) {
            return Optional.empty();
        }

        boolean passwordMatches;

        try {
            passwordMatches = BCrypt.checkpw(
                    new String(password),
                    savedHash.get());
        } catch (IllegalArgumentException invalidHash) {
            // A malformed stored hash must not leak details to the user.
            return Optional.empty();
        }

        if (!passwordMatches) {
            return Optional.empty();
        }

        User account = user.get();

        if (!"active".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException(
                    "Your account is "
                    + account.getStatus()
                    + ". Please contact the administrator.");
        }

        // Award +20 points once per day after a successful login.
        // A reward failure must never block the user from signing in.
        try {
            boolean rewardGiven =
                    userDAO.claimDailyLoginReward(account.getId());

            if (rewardGiven) {
                account.setPoints(account.getPoints() + 20);

                recordActivity(
                        account.getId(),
                        "daily_login_reward",
                        "@" + account.getUsername()
                        + " claimed the +20 daily login reward."
                );

                try {
                    AdminNotificationDAO.notifyAdmins(
                            "@" + account.getUsername()
                            + " claimed the +20 daily login reward."
                    );
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException rewardError) {
            rewardError.printStackTrace();
        }

        return Optional.of(account);
    }

    private void recordActivity(
            int actorId,
            String action,
            String note) {

        String sql =
                "INSERT INTO moderation_logs "
                + "(post_id, admin_id, action, note, created_at) "
                + "VALUES (NULL, ?, ?, ?, NOW())";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, actorId);
            ps.setString(2, action);
            ps.setString(3, note);

            ps.executeUpdate();

        } catch (SQLException e) {
            // Audit logging must not prevent registration or login.
            e.printStackTrace();
        }
    }

    /**
     * Forgot-password: sets a new password when username and email match an
     * active account. Returns true on success, false when nothing matched.
     */
    public boolean resetPassword(String username, String email,
                                 char[] newPassword)
            throws SQLException {

        if (username == null || username.isBlank()
                || email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter your username and email.");
        }

        if (newPassword == null || newPassword.length < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }

        String hash = BCrypt.hashpw(new String(newPassword), BCrypt.gensalt(12));

        return userDAO.resetPasswordIfMatches(
                username.trim(), email.trim(), hash).isPresent();
    }

    private void validateRegistration(
            String fullName,
            String username,
            String email,
            char[] password) {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        if (username == null
                || !username.matches("[a-zA-Z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must contain 3-30 letters, numbers, or underscores.");
        }

        if (email == null
                || !email.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException(
                    "Enter a valid email address.");
        }

        if (password == null || password.length < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }
    }
}
