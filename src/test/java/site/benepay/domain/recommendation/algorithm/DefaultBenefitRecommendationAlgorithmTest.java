package site.benepay.domain.recommendation.algorithm;

import org.junit.jupiter.api.Test;
import site.benepay.domain.recommendation.model.BenefitStoreCandidate;
import site.benepay.domain.recommendation.model.RecommendedBenefitCard;
import site.benepay.domain.recommendation.model.RecommendedBenefitStore;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBenefitRecommendationAlgorithmTest {

    private final DefaultBenefitRecommendationAlgorithm algorithm = new DefaultBenefitRecommendationAlgorithm();

    @Test
    void getRecommendedStoresKeepsBestBenefitPerMerchant() {
        BenefitStoreCandidate lowBenefit = candidate(1L, 1L, "Low Card", "{\"discount_rate\":5}", 80);
        BenefitStoreCandidate highBenefit = candidate(1L, 2L, "High Card", "{\"discount_rate\":10}", 120);

        List<RecommendedBenefitStore> result = algorithm.getRecommendedStores(List.of(lowBenefit, highBenefit));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCardId()).isEqualTo(2L);
        assertThat(result.get(0).getBenefitLabel()).isEqualTo("10% 혜택");
    }

    @Test
    void getRecommendedCardsIncludesCardsWithoutBenefit() {
        StoreBenefitCardCandidate benefitCard = cardCandidate(1L, "Benefit Card", 1L, "{\"discount_rate\":10}");
        StoreBenefitCardCandidate noBenefitCard = cardCandidate(2L, "No Benefit Card", null, null);

        List<RecommendedBenefitCard> result = algorithm.getRecommendedCards(List.of(noBenefitCard, benefitCard));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserCardId()).isEqualTo(1L);
        assertThat(result.get(0).isRecommended()).isTrue();
        assertThat(result.get(1).getBenefitLabel()).isEqualTo("혜택 없음");
    }

    private BenefitStoreCandidate candidate(Long merchantId, Long cardId, String cardName,
                                             String benefitsInfo, int distanceMeters) {
        BenefitStoreCandidate candidate = new BenefitStoreCandidate();
        candidate.setMerchantId(merchantId);
        candidate.setMerchantName("테스트 매장");
        candidate.setCategoryCode("FOOD");
        candidate.setCategoryName("음식");
        candidate.setAddress("서울시 테스트로 1");
        candidate.setLatitude(BigDecimal.valueOf(37.498));
        candidate.setLongitude(BigDecimal.valueOf(127.0276));
        candidate.setDistanceMeters(BigDecimal.valueOf(distanceMeters));
        candidate.setRating(BigDecimal.valueOf(4.6));
        candidate.setCardId(cardId);
        candidate.setCardName(cardName);
        candidate.setBenefitId(cardId);
        candidate.setBenefitName("할인");
        candidate.setBenefitType("청구");
        candidate.setBenefitsInfo(benefitsInfo);
        return candidate;
    }

    private StoreBenefitCardCandidate cardCandidate(
            Long userCardId,
            String cardName,
            Long benefitId,
            String benefitsInfo
    ) {
        StoreBenefitCardCandidate candidate = new StoreBenefitCardCandidate();
        candidate.setUserCardId(userCardId);
        candidate.setCardId(userCardId);
        candidate.setCardName(cardName);
        candidate.setBenefitId(benefitId);
        candidate.setBenefitName("할인");
        candidate.setBenefitType("청구");
        candidate.setBenefitsInfo(benefitsInfo);
        return candidate;
    }
}
