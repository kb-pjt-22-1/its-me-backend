package site.benepay.domain.recommendation.algorithm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import site.benepay.domain.recommendation.model.BenefitStoreCandidate;
import site.benepay.domain.recommendation.model.RecommendedBenefitCard;
import site.benepay.domain.recommendation.model.RecommendedBenefitStore;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DefaultBenefitRecommendationAlgorithm implements BenefitRecommendationAlgorithm {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<RecommendedBenefitStore> getRecommendedStores(List<BenefitStoreCandidate> candidates) {
        Map<Long, RecommendedBenefitStore> bestByMerchant = new LinkedHashMap<>();

        for (BenefitStoreCandidate candidate : candidates) {
            BenefitScore score = score(candidate);
            if (score.recommendationScore <= 0) {
                continue;
            }
            RecommendedBenefitStore recommendedStore = new RecommendedBenefitStore(
                    candidate,
                    score.benefitId,
                    score.benefitType,
                    score.benefitName,
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
        return getRecommendedCards(candidates, null);
    }

    @Override
    public List<RecommendedBenefitCard> getRecommendedCards(
            List<StoreBenefitCardCandidate> candidates,
            BigDecimal estimatedPaymentAmount
    ) {
        Map<Long, RecommendedBenefitCard> bestByUserCard = new LinkedHashMap<>();

        for (StoreBenefitCardCandidate candidate : candidates) {
            BenefitScore score = score(candidate, estimatedPaymentAmount);
            RecommendedBenefitCard recommendedCard = new RecommendedBenefitCard(
                    candidate,
                    score.benefitId,
                    score.benefitType,
                    score.benefitName,
                    score.benefitDescription,
                    score.benefitLabel,
                    score.estimatedBenefitScore,
                    score.expectedBenefitAmount,
                    score.recommendationScore
            );

            bestByUserCard.merge(
                    candidate.getUserCardId(),
                    recommendedCard,
                    (current, next) -> compareCards(current, next, estimatedPaymentAmount) >= 0
                            ? current
                            : next
            );
        }

        return bestByUserCard.values().stream()
                .sorted(cardComparator(estimatedPaymentAmount))
                .collect(Collectors.toList());
    }

    private BenefitScore score(BenefitStoreCandidate candidate) {
        BenefitScore structuredScore = scoreStructuredBenefit(toStoreBenefitCardCandidate(candidate), null);
        if (structuredScore != null) {
            double distancePenalty = candidate.getDistanceMeters().doubleValue() / 100.0;
            double bookmarkBoost = candidate.isBookmarked() ? 5.0 : 0.0;
            return structuredScore.withRecommendationScore(
                    structuredScore.recommendationScore + bookmarkBoost - distancePenalty
            );
        }
        return score(
                candidate.getBenefitsInfo(),
                candidate.getBenefitName(),
                candidate.getDistanceMeters(),
                candidate.isBookmarked()
        );
    }

    private StoreBenefitCardCandidate toStoreBenefitCardCandidate(BenefitStoreCandidate candidate) {
        StoreBenefitCardCandidate cardCandidate = new StoreBenefitCardCandidate();
        cardCandidate.setMerchantId(candidate.getMerchantId());
        cardCandidate.setMerchantName(candidate.getMerchantName());
        cardCandidate.setBrandCode(candidate.getBrandCode());
        cardCandidate.setBrandName(candidate.getBrandName());
        cardCandidate.setCategoryCode(candidate.getCategoryCode());
        cardCandidate.setCategoryName(candidate.getCategoryName());
        cardCandidate.setAddress(candidate.getAddress());
        cardCandidate.setLatitude(candidate.getLatitude());
        cardCandidate.setLongitude(candidate.getLongitude());
        cardCandidate.setDistanceMeters(candidate.getDistanceMeters());
        cardCandidate.setRating(candidate.getRating());
        cardCandidate.setBookmarked(candidate.isBookmarked());
        cardCandidate.setUserCardId(candidate.getUserCardId());
        cardCandidate.setCardId(candidate.getCardId());
        cardCandidate.setCardName(candidate.getCardName());
        cardCandidate.setCardImageUrl(candidate.getCardImageUrl());
        cardCandidate.setBenefitId(candidate.getBenefitId());
        cardCandidate.setBenefitType(candidate.getBenefitType());
        cardCandidate.setBenefitName(candidate.getBenefitName());
        cardCandidate.setBenefitDescription(candidate.getBenefitDescription());
        cardCandidate.setBenefitsInfo(candidate.getBenefitsInfo());
        cardCandidate.setTotalSpendingAmount(candidate.getTotalSpendingAmount());
        return cardCandidate;
    }

    private BenefitScore score(StoreBenefitCardCandidate candidate, BigDecimal estimatedPaymentAmount) {
        if (candidate.getBenefitId() == null) {
            BenefitScore structuredScore = scoreStructuredBenefit(candidate, estimatedPaymentAmount);
            if (structuredScore != null) {
                return structuredScore;
            }
            return new BenefitScore("혜택 없음", 0, null, 0.0);
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
        return new BenefitScore(label, benefitScore, null, recommendationScore);
    }

    private BenefitScore scoreStructuredBenefit(
            StoreBenefitCardCandidate candidate,
            BigDecimal estimatedPaymentAmount
    ) {
        JsonNode root = parseBenefitsInfo(candidate.getBenefitsInfo());
        JsonNode tier = selectPerformanceTier(root, candidate.getTotalSpendingAmount());
        if (tier == null) {
            return null;
        }

        BenefitScore bestScore = null;
        for (JsonNode benefit : benefitNodes(tier)) {
            if (!matchesBenefit(benefit, candidate)) {
                continue;
            }

            BenefitScore score = scoreMatchedBenefit(benefit, estimatedPaymentAmount);
            if (score == null) {
                continue;
            }
            if (bestScore == null || compareScores(score, bestScore, estimatedPaymentAmount) > 0) {
                bestScore = score;
            }
        }
        return bestScore;
    }

    private JsonNode selectPerformanceTier(JsonNode root, BigDecimal totalSpendingAmount) {
        JsonNode tiers = root.get("performanceTiers");
        if (tiers == null || !tiers.isArray()) {
            return null;
        }

        BigDecimal spendingAmount = totalSpendingAmount == null ? BigDecimal.ZERO : totalSpendingAmount;
        for (JsonNode tier : tiers) {
            BigDecimal minimumSpending = decimalValue(tier, "minimumSpending");
            BigDecimal maximumSpending = decimalValue(tier, "maximumSpending");
            boolean satisfiesMinimum = minimumSpending == null || spendingAmount.compareTo(minimumSpending) >= 0;
            boolean satisfiesMaximum = maximumSpending == null || spendingAmount.compareTo(maximumSpending) < 0;
            if (satisfiesMinimum && satisfiesMaximum) {
                return tier;
            }
        }
        return null;
    }

    private List<JsonNode> benefitNodes(JsonNode tier) {
        JsonNode benefits = tier.get("benefits");
        if (benefits != null && benefits.isArray()) {
            return iterableToList(benefits);
        }
        return List.of(tier);
    }

    private List<JsonNode> iterableToList(JsonNode nodes) {
        return java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .collect(Collectors.toList());
    }

    private boolean matchesBenefit(JsonNode benefit, StoreBenefitCardCandidate candidate) {
        String merchantType = textValue(benefit, "merchantType");
        boolean categoryMatches = matchesCategory(benefit, candidate.getCategoryCode());

        if ("BRAND".equalsIgnoreCase(merchantType)) {
            if (hasCategoryCondition(benefit) && !categoryMatches) {
                return false;
            }
            boolean hasBrandCondition = hasNonEmptyArray(benefit, "brandCodes") || hasNonEmptyArray(benefit, "merchants");
            if (!hasBrandCondition) {
                return categoryMatches || !hasCategoryCondition(benefit);
            }
            return containsText(benefit.get("brandCodes"), candidate.getBrandCode())
                    || containsText(benefit.get("merchants"), candidate.getBrandName());
        }

        return !hasCategoryCondition(benefit) || categoryMatches;
    }

    private boolean matchesCategory(JsonNode benefit, String categoryCode) {
        return equalsText(textValue(benefit, "categoryCode"), categoryCode)
                || containsText(benefit.get("categoryCodes"), categoryCode);
    }

    private boolean hasCategoryCondition(JsonNode benefit) {
        return textValue(benefit, "categoryCode") != null || hasNonEmptyArray(benefit, "categoryCodes");
    }

    private boolean hasNonEmptyArray(JsonNode node, String name) {
        JsonNode array = node.get(name);
        return array != null && array.isArray() && !array.isEmpty();
    }

    private boolean containsText(JsonNode array, String expected) {
        if (array == null || !array.isArray() || expected == null) {
            return false;
        }
        Set<String> values = new HashSet<>();
        for (JsonNode value : array) {
            if (value.isTextual()) {
                values.add(value.asText().trim().toUpperCase());
            }
        }
        return values.contains(expected.trim().toUpperCase());
    }

    private boolean equalsText(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.trim().equalsIgnoreCase(expected.trim());
    }

    private BenefitScore scoreMatchedBenefit(JsonNode benefit, BigDecimal estimatedPaymentAmount) {
        BigDecimal discountRate = firstDecimal(benefit, "discountRate", "discount_rate");
        BigDecimal expectedBenefitAmount = calculateExpectedBenefitAmount(
                benefit,
                estimatedPaymentAmount,
                discountRate
        );

        if (estimatedPaymentAmount != null && expectedBenefitAmount == null) {
            return null;
        }

        int benefitScore;
        String label;
        if (expectedBenefitAmount != null) {
            benefitScore = expectedBenefitAmount.intValue();
            label = String.format("%,d원 혜택", benefitScore);
        } else if (discountRate != null) {
            benefitScore = discountRate.multiply(BigDecimal.valueOf(100)).intValue();
            label = stripZeros(discountRate) + "% 혜택";
        } else {
            benefitScore = 10;
            label = fallbackLabel(benefit);
        }

        String benefitName = firstText(benefit, "categoryName", "benefitName", "name");
        String benefitType = firstText(benefit, "discountMethod", "benefitType");
        String benefitDescription = firstText(benefit, "description");

        return new BenefitScore(
                label,
                benefitScore,
                expectedBenefitAmount,
                benefitScore,
                longValue(benefit, "benefitId"),
                benefitType,
                benefitName,
                benefitDescription
        );
    }

    private BigDecimal calculateExpectedBenefitAmount(
            JsonNode benefit,
            BigDecimal estimatedPaymentAmount,
            BigDecimal discountRate
    ) {
        if (estimatedPaymentAmount == null || discountRate == null) {
            return null;
        }

        BigDecimal applicableAmountMin = decimalValue(benefit, "applicableAmountMin");
        if (applicableAmountMin != null && estimatedPaymentAmount.compareTo(applicableAmountMin) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = estimatedPaymentAmount
                .multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        BigDecimal monthlyBenefitLimit = decimalValue(benefit, "monthlyBenefitLimit");
        if (monthlyBenefitLimit != null && amount.compareTo(monthlyBenefitLimit) > 0) {
            return monthlyBenefitLimit.setScale(0, RoundingMode.DOWN);
        }
        return amount;
    }

    private String fallbackLabel(JsonNode benefit) {
        String name = firstText(benefit, "categoryName", "benefitName", "name", "description");
        return name == null ? "카드 혜택" : name;
    }

    private Comparator<RecommendedBenefitCard> cardComparator(BigDecimal estimatedPaymentAmount) {
        if (estimatedPaymentAmount != null) {
            return Comparator
                    .comparing(RecommendedBenefitCard::isRecommended).reversed()
                    .thenComparing(RecommendedBenefitCard::getExpectedBenefitAmount, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparingDouble(RecommendedBenefitCard::getRecommendationScore).reversed());
        }
        return Comparator
                .comparing(RecommendedBenefitCard::isRecommended).reversed()
                .thenComparing(Comparator.comparingDouble(RecommendedBenefitCard::getRecommendationScore).reversed());
    }

    private int compareCards(
            RecommendedBenefitCard current,
            RecommendedBenefitCard next,
            BigDecimal estimatedPaymentAmount
    ) {
        return cardComparator(estimatedPaymentAmount).compare(current, next) * -1;
    }

    private int compareScores(BenefitScore current, BenefitScore next, BigDecimal estimatedPaymentAmount) {
        if (estimatedPaymentAmount != null) {
            int amountCompare = nullToZero(current.expectedBenefitAmount)
                    .compareTo(nullToZero(next.expectedBenefitAmount));
            if (amountCompare != 0) {
                return amountCompare;
            }
        }
        return Double.compare(current.recommendationScore, next.recommendationScore);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = textValue(node, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String textValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Long longValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.canConvertToLong()) {
            return value.asLong();
        }
        try {
            return Long.valueOf(value.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal decimalValue(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual()) {
            try {
                return new BigDecimal(value.asText());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private String stripZeros(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static class BenefitScore {
        private final String benefitLabel;
        private final Integer estimatedBenefitScore;
        private final BigDecimal expectedBenefitAmount;
        private final double recommendationScore;
        private final Long benefitId;
        private final String benefitType;
        private final String benefitName;
        private final String benefitDescription;

        private BenefitScore(
                String benefitLabel,
                Integer estimatedBenefitScore,
                BigDecimal expectedBenefitAmount,
                double recommendationScore
        ) {
            this(benefitLabel, estimatedBenefitScore, expectedBenefitAmount, recommendationScore,
                    null, null, null, null);
        }

        private BenefitScore(
                String benefitLabel,
                Integer estimatedBenefitScore,
                BigDecimal expectedBenefitAmount,
                double recommendationScore,
                Long benefitId,
                String benefitType,
                String benefitName,
                String benefitDescription
        ) {
            this.benefitLabel = benefitLabel;
            this.estimatedBenefitScore = estimatedBenefitScore;
            this.expectedBenefitAmount = expectedBenefitAmount;
            this.recommendationScore = recommendationScore;
            this.benefitId = benefitId;
            this.benefitType = benefitType;
            this.benefitName = benefitName;
            this.benefitDescription = benefitDescription;
        }

        private BenefitScore withRecommendationScore(double recommendationScore) {
            return new BenefitScore(
                    benefitLabel,
                    estimatedBenefitScore,
                    expectedBenefitAmount,
                    recommendationScore,
                    benefitId,
                    benefitType,
                    benefitName,
                    benefitDescription
            );
        }
    }
}
