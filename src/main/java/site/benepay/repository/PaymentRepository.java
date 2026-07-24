package site.benepay.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import site.benepay.domain.BenefitResult;
import site.benepay.domain.PaymentView;

public class PaymentRepository {
	public long insert(Connection connection, long userCardId, long merchantId, long tokenId,
		String posRequestId, BigDecimal originalAmount, BenefitResult benefit,
		String status, String approvalCode, String failureReason) throws SQLException {
		String sql = "INSERT INTO payments " +
			"(user_card_id, merchant_id, payment_token_id, pos_request_id, payment_time, original_amount, " +
			"discount_amount, reward_amount, final_amount, payment_status, approval_code, benefit_id, " +
			"benefit_description, failure_reason) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setLong(1, userCardId);
			ps.setLong(2, merchantId);
			ps.setLong(3, tokenId);
			ps.setString(4, posRequestId);
			ps.setBigDecimal(5, originalAmount);
			ps.setBigDecimal(6, benefit.getDiscountAmount());
			ps.setBigDecimal(7, benefit.getRewardAmount());
			ps.setBigDecimal(8, benefit.getFinalAmount());
			ps.setString(9, status);
			ps.setString(10, approvalCode);
			if (benefit.getBenefitId() == null)
				ps.setNull(11, Types.BIGINT);
			else
				ps.setLong(11, benefit.getBenefitId());
			ps.setString(12, benefit.getDescription());
			ps.setString(13, failureReason);
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				if (!keys.next())
					throw new SQLException("Payment ID was not generated");
				return keys.getLong(1);
			}
		}
	}

	public PaymentView findByPosRequestId(Connection connection, String posRequestId) throws SQLException {
		return findOne(connection, baseSelect() + " WHERE p.pos_request_id = ?", posRequestId);
	}

	public PaymentView findById(Connection connection, long paymentId) throws SQLException {
		return findOne(connection, baseSelect() + " WHERE p.payment_id = ?", paymentId);
	}

	public PaymentView findByTokenId(Connection connection, long tokenId, long userId) throws SQLException {
		return findOne(connection, baseSelect() + " WHERE p.payment_token_id = ? AND uc.user_id = ?", tokenId, userId);
	}

	public List<PaymentView> findByUser(Connection connection, long userId) throws SQLException {
		String sql = baseSelect() + " WHERE uc.user_id = ? ORDER BY p.payment_time DESC, p.payment_id DESC";
		List<PaymentView> result = new ArrayList<PaymentView>();
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.add(map(rs));
			}
		}
		return result;
	}

	private PaymentView findOne(Connection connection, String sql, Object... values) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			for (int i = 0; i < values.length; i++)
				ps.setObject(i + 1, values[i]);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		}
	}

	private String baseSelect() {
		return
			"SELECT p.payment_id,p.user_card_id,p.merchant_id,p.pos_request_id,p.payment_time,p.canceled_at,p.cancellation_reason,"
				+
				"p.original_amount,p.discount_amount,COALESCE(p.reward_amount,0) reward_amount,p.final_amount,p.payment_status,"
				+
				"p.approval_code,p.benefit_description,p.failure_reason,m.merchant_name,c.card_company_name,c.card_name,uc.card_last4 "
				+
				"FROM payments p JOIN merchants m ON m.merchant_id=p.merchant_id JOIN user_cards uc ON uc.user_card_id=p.user_card_id JOIN cards c ON c.card_id=uc.card_id";
	}

	private PaymentView map(ResultSet rs) throws SQLException {
		PaymentView x = new PaymentView();
		x.setPaymentId(rs.getLong("payment_id"));
		x.setUserCardId(rs.getLong("user_card_id"));
		x.setMerchantId(rs.getLong("merchant_id"));
		x.setPosRequestId(rs.getString("pos_request_id"));
		x.setPaymentTime(rs.getTimestamp("payment_time").toLocalDateTime());
		Timestamp canceled = rs.getTimestamp("canceled_at");
		x.setCanceledAt(canceled == null ? null : canceled.toLocalDateTime());
		x.setCancellationReason(rs.getString("cancellation_reason"));
		x.setMerchantName(rs.getString("merchant_name"));
		x.setCardCompanyName(rs.getString("card_company_name"));
		x.setCardName(rs.getString("card_name"));
		x.setCardLast4(rs.getString("card_last4"));
		x.setOriginalAmount(rs.getBigDecimal("original_amount"));
		x.setDiscountAmount(rs.getBigDecimal("discount_amount"));
		x.setRewardAmount(rs.getBigDecimal("reward_amount"));
		x.setFinalAmount(rs.getBigDecimal("final_amount"));
		x.setPaymentStatus(rs.getString("payment_status"));
		x.setApprovalCode(rs.getString("approval_code"));
		x.setBenefitDescription(rs.getString("benefit_description"));
		x.setFailureReason(rs.getString("failure_reason"));
		return x;
	}
}
