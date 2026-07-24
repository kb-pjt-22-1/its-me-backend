package site.benepay.service;

import site.benepay.config.Database;
import site.benepay.domain.Merchant;
import site.benepay.domain.MonthlyStatus;
import site.benepay.domain.PaymentView;
import site.benepay.domain.UserCard;
import site.benepay.repository.BookmarkRepository;
import site.benepay.repository.CardRepository;
import site.benepay.repository.MonthlyStatusRepository;
import site.benepay.repository.PaymentRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeService {
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private final CardRepository cardRepository = new CardRepository();
    private final MonthlyStatusRepository monthlyStatusRepository = new MonthlyStatusRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final BookmarkRepository bookmarkRepository = new BookmarkRepository();

    public Map<String, Object> load(long userId) {
        try (Connection connection = Database.getConnection()) {
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("userId", userId);
            response.put("userName", findUserName(connection, userId));

            List<UserCard> cards = cardRepository.findActiveCards(connection, userId, false);
            UserCard primary = null;
            for (UserCard card : cards) {
                if (card.isPrimary()) { primary = card; break; }
            }
            if (primary == null && !cards.isEmpty()) primary = cards.get(0);
            response.put("cards", cards);
            response.put("primaryCard", primaryCard(connection, primary));

            List<PaymentView> payments = paymentRepository.findByUser(connection, userId);
            List<PaymentView> recent = new ArrayList<PaymentView>();
            for (int i = 0; i < payments.size() && i < 3; i++) recent.add(payments.get(i));
            response.put("recentPayments", recent);
            response.put("monthlyBenefit", monthlyBenefit(connection, userId));

            List<Merchant> bookmarks = bookmarkRepository.findByUser(connection, userId);
            response.put("bookmarks", bookmarks);
            response.put("quickMerchant", bookmarks.isEmpty() ? null : bookmarks.get(0));
            return response;
        } catch (Exception e) {
            throw new ServiceException(500, "HOME_DATA_FAILED", "홈 화면 데이터 조회에 실패했습니다.", e);
        }
    }

    private String findUserName(Connection connection, long userId) throws Exception {
        String sql = "SELECT name FROM users WHERE user_id = ? AND is_deleted = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : "사용자";
            }
        }
    }

    private Map<String, Object> primaryCard(Connection connection, UserCard card) throws Exception {
        if (card == null) return null;
        MonthlyStatus status = monthlyStatusRepository.find(
                connection, card.getUserCardId(), YearMonth.now().format(YEAR_MONTH));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userCardId", card.getUserCardId());
        result.put("cardName", card.getCardName());
        result.put("cardCompanyName", card.getCardCompanyName());
        result.put("cardLast4", card.getCardLast4());
        result.put("spentAmount", status.getSpentAmount());
        result.put("benefitEligible", status.isBenefitEligible());
        result.put("targetAmount", card.getMinBenefitAmount());
        BigDecimal remaining = card.getMinBenefitAmount().subtract(status.getSpentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
        result.put("remainingAmount", remaining);
        return result;
    }

    private BigDecimal monthlyBenefit(Connection connection, long userId) throws Exception {
        YearMonth month = YearMonth.now();
        String sql = "SELECT COALESCE(SUM(p.discount_amount + COALESCE(p.reward_amount, 0)), 0) AS total " +
                "FROM payments p JOIN user_cards uc ON uc.user_card_id = p.user_card_id " +
                "WHERE uc.user_id = ? AND p.payment_status = 'APPROVED' " +
                "AND p.payment_time >= ? AND p.payment_time < ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(month.atDay(1).atStartOfDay()));
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(month.plusMonths(1).atDay(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        }
    }
}
