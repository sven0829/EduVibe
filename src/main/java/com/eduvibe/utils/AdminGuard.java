package com.eduvibe.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * com.eduvibe.util.AdminGuard  (NEW FILE)
 *
 * Re-checks in the database that the person performing an administrator action really is an
 * active administrator. The screens already hide admin pages from other users; this makes the
 * database operation itself refuse to run for anyone else, even if a screen were opened by mistake.
 */
public final class AdminGuard {

    private AdminGuard() {
    }

    /**
     * @throws SQLException if the user is missing, is not an admin, or the account is not active.
     */
    public static void require(Connection connection, Integer adminId) throws SQLException {
        if (adminId == null) {
            throw new SQLException("Administrator permission is required for this action.");
        }
        String sql = "SELECT role, status FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean ok = rs.next()
                        && "admin".equalsIgnoreCase(rs.getString("role"))
                        && "active".equalsIgnoreCase(rs.getString("status"));
                if (!ok) {
                    throw new SQLException("Administrator permission is required for this action.");
                }
            }
        }
    }
}
