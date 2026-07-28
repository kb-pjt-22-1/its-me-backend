package site.benepay.domain.recommendation.algorithm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import site.benepay.domain.recommendation.model.BenefitStoreCandidate;
import site.benepay.domain.recommendation.model.RecommendedBenefitCard;
import site.benepay.domain.recommendation.model.RecommendedBenefitStore;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DefaultBenefitRecommendationAlgorithm implements BenefitRecommendationAlgorithm {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<RecommendedBenefitStore> getRecommendedStores(List<BenefitStoreCandidate> candidates) {
        Map<Long, RecommendedBenefitStore> bestByMerchant = new LinkedHashMap<>();

        for (BenefitStoreCandidate candidate : candidates) {
            BenefitScore score = score(candidate);
            RecommendedBenefitStore recommendedStore = new RecommendedBenefitStore(
                    candidate,
                    score.benefitLabel,
                    score.estimatedBenefitScore,
                    score.recommendationScore
            );

            bestByMerchant.merge(
                    candidate.getMerchantId(),
                    recommendedStore,
                    (current, next) -> current.getRecommendationScore() >= next.getRecommendationScore()
                            ? current
                            : next
            );
        }

        return bestByMerchant.values().stream()
                .sorted(Comparator
                        .comparingDouble(RecommendedBenefitStore::getRecommendationScore).reversed()
                        .thenComparingInt(RecommendedBenefitStore::getDistanceMeters))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendedBenefitCard> getRecommendedCards(List<StoreBenefitCardCandidate> candidates) {
        Map<Long, RecommendedBenefitCard> bestByUserCard = new LinkedHashMap<>();

        for (StoreBenefitCardCandidate candidate : candidates) {
            BenefitScore score = score(candidate);
            RecommendedBenefitCard recommendedCard = new RecommendedBenefitCard(
                    candidate,
                    score.benefitLabel,
                    score.estimatedBenefitScore,
                    score.recommendationScore
            );

            bestByUserCard.merge(
                    candidate.getUserCardId(),
                    recommendedCard,
                    (current, next) -> current.getRecommendationScore() >= next.getRecommendationScore()
                            ? current
                            : next
            );
        }

        return bestByUserCard.values().stream()
                .sorted(Comparator
                        .comparing(RecommendedBenefitCard::isRecommended).reversed()
                        .thenComparing(
                                Comparator.comparingDouble(RecommendedBenefitCard::getRecommendationScore).reversed()
                        ))
                .collect(Collectors.toList());
    }

    private BenefitScore score(BenefitStoreCandidate candidate) {
        return score(
                candidate.getBenefitsInfo(),
                candidate.getBenefitName(),
                candidate.getDistanceMeters(),
                candidate.isBookmarked()
        );
    }

    private BenefitScore score(StoreBenefitCardCandidate candidate) {
        if (candidate.getBenefitId() == null) {
            return new BenefitScore("혜택 없음", 0, 0.0);
        }
        return score(candidate.getBenefitsInfo(), candidate.getBenefitName(), BigDecimal.ZERO, false);
    }

    private BenefitScore score(
            String rawBenefitsInfo,
            String benefitName,
            BigDecimal distanceMeters,
            boolean bookmarked
    ) {
        JsonNode benefitsInfo = parseBenefitsInfo(rawBenefitsInfo);
        BigDecimal rate = firstDecimal(benefitsInfo, "discount_rate", "cashback_rate", "point_rate");
        BigDecimal amount = decimalValue(benefitsInfo, "discount_amount");

        int benefitScore;
        String label;
        if (amount != null) {
            benefitScore = amount.intValue();
            label = "최대 " + String.format("%,d", benefitScore) + "원 혜택";
        } else if (rate != null) {
            benefitScore = rate.multiply(BigDecimal.valueOf(100)).intValue();
            label = stripZeros(rate) + "% 혜택";
        } else {
            benefitScore = 10;
            label = benefitName == null ? "카드 혜택" : benefitName;
        }

        double distancePenalty = distanceMeters.doubleValue() / 100.0;
        double bookmarkBoost = bookmarked ? 5.0 : 0.0;
        double recommendationScore = benefitScore + bookmarkBoost - distancePenalty;
        return new BenefitScore(label, benefitScore, recommendationScore);
    }

    private JsonNode parseBenefitsInfo(String benefitsInfo) {
        try {
            return objectMapper.readTree(benefitsInfo);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private BigDecimal firstDecimal(JsonNode node, String... names) {
        for (String name : names) {
            BigDecimal value = decimalValue(node, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal decimalValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isNumber()) {
            return null;
        }
        return value.decimalValue();
    }

    private String stripZeros(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static class BenefitScore {
        private final String benefitLabel;
        private final Integer estimatedBenefitScore;
        private final double recommendationScore;

        private BenefitScore(String benefitLabel, Integer estimatedBenefitScore, double recommendationScore) {
            this.benefitLabel = benefitLabel;
            this.estimatedBenefitScore = estimatedBenefitScore;
            this.recommendationScore = recommendationScore;
        }
    }
}
