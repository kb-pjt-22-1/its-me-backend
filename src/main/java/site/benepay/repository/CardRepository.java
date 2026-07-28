package site.benepay.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import site.benepay.domain.UserCard;

public class CardRepository {
	private static final String BASE_SELECT =
		"SELECT uc.user_card_id, uc.user_id, uc.card_id, uc.card_last4, uc.is_primary, " +
			"uc.recommendation_enabled, uc.card_status, c.card_company_name, c.card_name, c.card_type, " +
			"COALESCE(c.min_benefit_amount, 0) AS min_benefit_amount " +
			"FROM user_cards uc JOIN cards c ON c.card_id = uc.card_id ";

	public List<UserCard> findActiveCards(Connection connection, long userId, boolean recommendationOnly) throws
		SQLException {
		String sql = BASE_SELECT +
			"WHERE uc.user_id = ? AND uc.is_deleted = FALSE AND uc.card_status = 'ACTIVE' AND c.is_supported = TRUE " +
			(recommendationOnly ? "AND uc.recommendation_enabled = TRUE " : "") +
			"ORDER BY uc.is_primary DESC, uc.user_card_id";
		List<UserCard> cards = new ArrayList<UserCard>();
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					cards.add(map(rs));
			}
		}
		return cards;
	}

	public UserCard findById(Connection connection, long userId, long userCardId) throws SQLException {
		String sql = BASE_SELECT +
			"WHERE uc.user_id = ? AND uc.user_card_id = ? AND uc.is_deleted = FALSE " +
			"AND uc.card_status = 'ACTIVE' AND c.is_supported = TRUE";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setLong(1, userId);
			ps.setLong(2, userCardId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		}
	}

	private UserCard map(ResultSet rs) throws SQLException {
		UserCard card = new UserCard();
		card.setUserCardId(rs.getLong("user_card_id"));
		card.setUserId(rs.getLong("user_id"));
		card.setCardId(rs.getLong("card_id"));
		card.setCardLast4(rs.getString("card_last4"));
		card.setPrimary(rs.getBoolean("is_primary"));
		card.setRecommendationEnabled(rs.getBoolean("recommendation_enabled"));
		card.setCardStatus(rs.getString("card_status"));
		card.setCardCompanyName(rs.getString("card_company_name"));
		card.setCardName(rs.getString("card_name"));
		card.setCardType(rs.getString("card_type"));
		card.setMinBenefitAmount(rs.getBigDecimal("min_benefit_amount"));
		return card;
	}
}
