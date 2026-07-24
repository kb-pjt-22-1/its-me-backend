package site.benepay.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import site.benepay.domain.CardBenefit;
import site.benepay.domain.Merchant;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BenefitRepository {
    public List<CardBenefit> findApplicable(Connection connection, long userCardId, long cardId, Merchant merchant)
            throws SQLException {
        String sql = "SELECT cb.benefit_id, cb.card_id, cb.benefit_type, cb.benefit_name, cb.description, " +
                "cb.benefits_info FROM card_benefits cb " +
                "JOIN benefit_per_user_card bpu ON bpu.benefit_id = cb.benefit_id " +
                "WHERE bpu.user_card_id = ? AND cb.card_id = ? ORDER BY cb.benefit_id";
        List<CardBenefit> benefits = new ArrayList<CardBenefit>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userCardId);
            ps.setLong(2, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CardBenefit benefit = map(rs);
                    if (isApplicable(benefit, merchant)) benefits.add(benefit);
                }
            }
        }
        return benefits;
    }

    private CardBenefit map(ResultSet rs) throws SQLException {
        CardBenefit benefit = new CardBenefit();
        benefit.setBenefitId(rs.getLong("benefit_id"));
        benefit.setCardId(rs.getLong("card_id"));
        benefit.setBenefitName(rs.getString("benefit_name"));
        benefit.setDescription(rs.getString("description"));

        String rawType = rs.getString("benefit_type");
        String jsonText = rs.getString("benefits_info");
        try {
            JsonObject info = JsonParser.parseString(jsonText).getAsJsonObject();
            benefit.setCategoryCode(stringValue(info, "category"));
            benefit.setMonthlyLimit(decimalValue(info, "monthly_limit"));
            benefit.setMonthlyCount(intValue(info, "monthly_count"));

            if (info.has("point_rate")) {
                benefit.setBenefitType("POINT");
                benefit.setRatePercent(decimalValue(info, "point_rate"));
            } else if (info.has("cashback_rate")) {
                benefit.setBenefitType("CASHBACK");
                benefit.setRatePercent(decimalValue(info, "cashback_rate"));
            } else if (info.has("discount_rate")) {
                benefit.setBenefitType("DISCOUNT");
                benefit.setRatePercent(decimalValue(info, "discount_rate"));
            } else if (info.has("discount_amount")) {
                benefit.setBenefitType("DISCOUNT");
                benefit.setFixedAmount(decimalValue(info, "discount_amount"));
            } else {
                benefit.setBenefitType(normalizeType(rawType));
            }
        } catch (RuntimeException e) {
            benefit.setBenefitType(normalizeType(rawType));
        }
        return benefit;
    }

    private boolean isApplicable(CardBenefit benefit, Merchant merchant) {
        String category = upper(benefit.getCategoryCode());
        if (category.isEmpty()) return false;
        if ("ALL".equals(category)) return true;
        if (category.equals(upper(merchant.getCategoryCode()))) return true;

        if ("CAFE".equals(category) && "FOOD".equals(upper(merchant.getCategoryCode()))) {
            String text = upper(nullToEmpty(merchant.getBrandName()) + " " + nullToEmpty(merchant.getMerchantName()));
            return text.contains("스타벅스") || text.contains("투썸") || text.contains("이디야") ||
                    text.contains("카페") || text.contains("커피") || text.contains("COFFEE") || text.contains("CAFE");
        }
        return false;
    }

    private String normalizeType(String type) {
        if (type == null) return "DISCOUNT";
        if (type.contains("환급")) return "CASHBACK";
        return "DISCOUNT";
    }

    private String stringValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private BigDecimal decimalValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsBigDecimal() : BigDecimal.ZERO;
    }

    private int intValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
