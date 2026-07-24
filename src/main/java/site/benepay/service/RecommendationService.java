package site.benepay.service;

import site.benepay.config.Database;
import site.benepay.domain.BenefitResult;
import site.benepay.domain.Merchant;
import site.benepay.domain.UserCard;
import site.benepay.repository.CardRepository;
import site.benepay.repository.MerchantRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecommendationService {
    private final CardRepository cardRepository = new CardRepository();
    private final MerchantRepository merchantRepository = new MerchantRepository();
    private final BenefitCalculationService benefitService = new BenefitCalculationService();

    public Map<String, Object> recommend(long userId, long merchantId, BigDecimal amount) {
        validateAmount(amount);
        try (Connection connection = Database.getConnection()) {
            Merchant merchant = merchantRepository.findById(connection, merchantId);
            if (merchant == null) throw new ServiceException(404, "MERCHANT_NOT_FOUND", "가맹점을 찾을 수 없습니다.");

            List<UserCard> cards = cardRepository.findActiveCards(connection, userId, true);
            if (cards.isEmpty()) throw new ServiceException(404, "NO_AVAILABLE_CARD", "추천 가능한 등록 카드가 없습니다.");

            List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();
            for (UserCard card : cards) {
                BenefitResult benefit = benefitService.calculate(connection, card, merchant, amount);
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("userCardId", card.getUserCardId());
                item.put("cardCompanyName", card.getCardCompanyName());
                item.put("cardName", card.getCardName());
                item.put("cardLast4", card.getCardLast4());
                item.put("primary", card.isPrimary());
                item.put("benefitType", benefit.getBenefitType());
                item.put("benefitName", benefit.getBenefitName());
                item.put("benefitDescription", benefit.getDescription());
                item.put("expectedBenefit", benefit.getBenefitAmount());
                item.put("expectedDiscount", benefit.getDiscountAmount());
                item.put("expectedReward", benefit.getRewardAmount());
                item.put("finalAmount", benefit.getFinalAmount());
                candidates.add(item);
            }

            candidates.sort(new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    BigDecimal av = (BigDecimal) a.get("expectedBenefit");
                    BigDecimal bv = (BigDecimal) b.get("expectedBenefit");
                    int benefitCompare = bv.compareTo(av);
                    if (benefitCompare != 0) return benefitCompare;
                    boolean ap = Boolean.TRUE.equals(a.get("primary"));
                    boolean bp = Boolean.TRUE.equals(b.get("primary"));
                    return Boolean.compare(bp, ap);
                }
            });

            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("merchant", merchant);
            response.put("amount", amount);
            response.put("recommendedCardId", candidates.get(0).get("userCardId"));
            response.put("cards", candidates);
            return response;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "RECOMMENDATION_FAILED", "추천 카드 계산에 실패했습니다.", e);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new ServiceException(400, "INVALID_AMOUNT", "결제 금액은 1원 이상이어야 합니다.");
        }
        if (amount.compareTo(new BigDecimal("10000000")) > 0) {
            throw new ServiceException(400, "INVALID_AMOUNT", "결제 금액은 1,000만원 이하여야 합니다.");
        }
    }
}
