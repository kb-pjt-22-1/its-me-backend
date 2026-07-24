package site.benepay.repository;

import site.benepay.domain.MonthlyStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;

public class MonthlyStatusRepository {
    public MonthlyStatus find(Connection connection, long userCardId, String targetYearMonth) throws SQLException {
        MonthlyStatus status = new MonthlyStatus();
        String statusSql = "SELECT total_spending_amount, is_benefit_eligible FROM card_monthly_status " +
                "WHERE user_card_id = ? AND target_year_month = ?";
        try (PreparedStatement ps = connection.prepareStatement(statusSql)) {
            ps.setLong(1, userCardId);
            ps.setString(2, targetYearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    status.setSpentAmount(zero(rs.getBigDecimal("total_spending_amount")));
                    status.setBenefitEligible(rs.getBoolean("is_benefit_eligible"));
                }
            }
        }

        YearMonth month = YearMonth.of(
                Integer.parseInt(targetYearMonth.substring(0, 4)),
                Integer.parseInt(targetYearMonth.substring(4, 6)));
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        String benefitSql = "SELECT COALESCE(SUM(discount_amount), 0) AS discount_used, " +
                "COALESCE(SUM(reward_amount), 0) AS reward_used FROM payments " +
                "WHERE user_card_id = ? AND payment_status = 'APPROVED' " +
                "AND payment_time >= ? AND payment_time < ?";
        try (PreparedStatement ps = connection.prepareStatement(benefitSql)) {
            ps.setLong(1, userCardId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    status.setDiscountUsed(zero(rs.getBigDecimal("discount_used")));
                    status.setRewardUsed(zero(rs.getBigDecimal("reward_used")));
                }
            }
        }
        return status;
    }

    public void addPayment(Connection connection, long userCardId, String targetYearMonth,
                           BigDecimal spent, BigDecimal discount, BigDecimal reward) throws SQLException {
        BigDecimal current = BigDecimal.ZERO;
        boolean exists = false;
        String selectSql = "SELECT total_spending_amount FROM card_monthly_status " +
                "WHERE user_card_id = ? AND target_year_month = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setLong(1, userCardId);
            ps.setString(2, targetYearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    current = zero(rs.getBigDecimal("total_spending_amount"));
                    exists = true;
                }
            }
        }

        BigDecimal next = current.add(zero(spent));
        if (next.compareTo(BigDecimal.ZERO) < 0) next = BigDecimal.ZERO;
        BigDecimal threshold = findMinimumBenefitAmount(connection, userCardId);
        boolean eligible = threshold.compareTo(BigDecimal.ZERO) <= 0 || next.compareTo(threshold) >= 0;

        if (exists) {
            String sql = "UPDATE card_monthly_status SET total_spending_amount = ?, is_benefit_eligible = ?, " +
                    "updated_at = CURRENT_TIMESTAMP WHERE user_card_id = ? AND target_year_month = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setBigDecimal(1, next);
                ps.setBoolean(2, eligible);
                ps.setLong(3, userCardId);
                ps.setString(4, targetYearMonth);
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO card_monthly_status " +
                    "(user_card_id, total_spending_amount, is_benefit_eligible, updated_at, target_year_month) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userCardId);
                ps.setBigDecimal(2, next);
                ps.setBoolean(3, eligible);
                ps.setString(4, targetYearMonth);
                ps.executeUpdate();
            }
        }
    }

    public void reversePayment(Connection connection, long userCardId, String targetYearMonth,
                               BigDecimal spent, BigDecimal discount, BigDecimal reward) throws SQLException {
        addPayment(connection, userCardId, targetYearMonth,
                zero(spent).negate(), zero(discount).negate(), zero(reward).negate());
    }

    private BigDecimal findMinimumBenefitAmount(Connection connection, long userCardId) throws SQLException {
        String sql = "SELECT COALESCE(c.min_benefit_amount, 0) AS min_benefit_amount " +
                "FROM user_cards uc JOIN cards c ON c.card_id = uc.card_id WHERE uc.user_card_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userCardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? zero(rs.getBigDecimal("min_benefit_amount")) : BigDecimal.ZERO;
            }
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
