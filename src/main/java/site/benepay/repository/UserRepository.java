package site.benepay.repository;

import java.sql.*;
import java.time.LocalDateTime;

public class UserRepository {
    public String findPinHash(Connection connection, long userId) throws SQLException {
        String sql = "SELECT pin_hash FROM users WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("pin_hash") : null;
            }
        }
    }

    public int findPinFailCount(Connection connection, long userId) throws SQLException {
        String sql = "SELECT pin_fail_count FROM users WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("pin_fail_count") : 0;
            }
        }
    }

    public LocalDateTime findPinLockedUntil(Connection connection, long userId) throws SQLException {
        String sql = "SELECT pin_locked_until FROM users WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Timestamp timestamp = rs.getTimestamp("pin_locked_until");
                return timestamp == null ? null : timestamp.toLocalDateTime();
            }
        }
    }

    public void recordPinFailure(Connection connection, long userId, int failCount, LocalDateTime lockedUntil) throws SQLException {
        String sql = "UPDATE users SET pin_fail_count = ?, pin_locked_until = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, failCount);
            if (lockedUntil == null) ps.setNull(2, Types.TIMESTAMP);
            else ps.setTimestamp(2, Timestamp.valueOf(lockedUntil));
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void resetPinFailures(Connection connection, long userId) throws SQLException {
        String sql = "UPDATE users SET pin_fail_count = 0, pin_locked_until = NULL WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}
