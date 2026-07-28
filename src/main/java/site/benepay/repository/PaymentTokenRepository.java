package site.benepay.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

import site.benepay.domain.PaymentTokenRecord;

public class PaymentTokenRepository {
	public long insert(Connection c, long userId, long userCardId, Long merchantId, BigDecimal amount, String hash,
		LocalDateTime expires) throws SQLException {
		String sql = "INSERT INTO payment_tokens(user_id,user_card_id,merchant_id,expected_amount,token_hash,token_status,expires_at) VALUES(?,?,?,?,?,'ACTIVE',?)";
		try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setLong(1, userId);
			ps.setLong(2, userCardId);
			if (merchantId == null)
				ps.setNull(3, Types.BIGINT);
			else
				ps.setLong(3, merchantId);
			if (amount == null)
				ps.setNull(4, Types.DECIMAL);
			else
				ps.setBigDecimal(4, amount);
			ps.setString(5, hash);
			ps.setTimestamp(6, Timestamp.valueOf(expires));
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (!rs.next())
					throw new SQLException("Token ID not generated");
				return rs.getLong(1);
			}
		}
	}

	public PaymentTokenRecord findByHashForUpdate(Connection c, String hash) throws SQLException {
		return one(c,
			"SELECT payment_token_id,user_id,user_card_id,merchant_id,expected_amount,token_status,expires_at,used_at FROM payment_tokens WHERE token_hash=? FOR UPDATE",
			hash);
	}

	public PaymentTokenRecord findById(Connection c, long id, long userId) throws SQLException {
		return one(c,
			"SELECT payment_token_id,user_id,user_card_id,merchant_id,expected_amount,token_status,expires_at,used_at FROM payment_tokens WHERE payment_token_id=? AND user_id=?",
			id, userId);
	}

	public int revokeActive(Connection c, long userId, long cardId) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
			"UPDATE payment_tokens SET token_status='REVOKED' WHERE user_id=? AND user_card_id=? AND token_status='ACTIVE'")) {
			ps.setLong(1, userId);
			ps.setLong(2, cardId);
			return ps.executeUpdate();
		}
	}

	public void markUsed(Connection c, long id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
			"UPDATE payment_tokens SET token_status='USED',used_at=CURRENT_TIMESTAMP WHERE payment_token_id=?")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}

	public void markExpired(Connection c, long id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
			"UPDATE payment_tokens SET token_status='EXPIRED' WHERE payment_token_id=? AND token_status='ACTIVE'")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}

	private PaymentTokenRecord one(Connection c, String sql, Object... values) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < values.length; i++)
				ps.setObject(i + 1, values[i]);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		}
	}

	private PaymentTokenRecord map(ResultSet rs) throws SQLException {
		PaymentTokenRecord x = new PaymentTokenRecord();
		x.setPaymentTokenId(rs.getLong("payment_token_id"));
		x.setUserId(rs.getLong("user_id"));
		x.setUserCardId(rs.getLong("user_card_id"));
		Object m = rs.getObject("merchant_id");
		x.setMerchantId(m == null ? null : ((Number)m).longValue());
		x.setExpectedAmount(rs.getBigDecimal("expected_amount"));
		x.setTokenStatus(rs.getString("token_status"));
		x.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
		Timestamp u = rs.getTimestamp("used_at");
		x.setUsedAt(u == null ? null : u.toLocalDateTime());
		return x;
	}
}
