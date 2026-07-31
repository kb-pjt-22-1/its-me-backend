package site.benepay.domain.recommendation.model;

<<<<<<< Updated upstream
=======
import java.math.BigDecimal;

>>>>>>> Stashed changes
public class RecommendedBenefitCard {

    private final Long userCardId;
    private final Long cardId;
    private final String cardName;
    private final String cardImageUrl;
    private final String cardLast4;
    private final boolean primary;
    private final Long benefitId;
    private final String benefitType;
    private final String benefitName;
    private final String benefitDescription;
    private final String benefitLabel;
    private final Integer estimatedBenefitScore;
<<<<<<< Updated upstream
=======
    private final BigDecimal expectedBenefitAmount;
>>>>>>> Stashed changes
    private final double recommendationScore;

    public RecommendedBenefitCard(
            StoreBenefitCardCandidate candidate,
            String benefitLabel,
            Integer estimatedBenefitScore,
<<<<<<< Updated upstream
=======
            BigDecimal expectedBenefitAmount,
            double recommendationScore
    ) {
        this(candidate, candidate.getBenefitId(), candidate.getBenefitType(), candidate.getBenefitName(),
                candidate.getBenefitDescription(), benefitLabel, estimatedBenefitScore, expectedBenefitAmount,
                recommendationScore);
    }

    public RecommendedBenefitCard(
            StoreBenefitCardCandidate candidate,
            Long benefitId,
            String benefitType,
            String benefitName,
            String benefitDescription,
            String benefitLabel,
            Integer estimatedBenefitScore,
            BigDecimal expectedBenefitAmount,
>>>>>>> Stashed changes
            double recommendationScore
    ) {
        this.userCardId = candidate.getUserCardId();
        this.cardId = candidate.getCardId();
        this.cardName = candidate.getCardName();
        this.cardImageUrl = candidate.getCardImageUrl();
        this.cardLast4 = candidate.getCardLast4();
        this.primary = candidate.isPrimary();
<<<<<<< Updated upstream
        this.benefitId = candidate.getBenefitId();
        this.benefitType = candidate.getBenefitType();
        this.benefitName = candidate.getBenefitName();
        this.benefitDescription = candidate.getBenefitDescription();
        this.benefitLabel = benefitLabel;
        this.estimatedBenefitScore = estimatedBenefitScore;
=======
        this.benefitId = benefitId;
        this.benefitType = benefitType;
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
        this.benefitLabel = benefitLabel;
        this.estimatedBenefitScore = estimatedBenefitScore;
        this.expectedBenefitAmount = expectedBenefitAmount;
>>>>>>> Stashed changes
        this.recommendationScore = recommendationScore;
    }

    public Long getUserCardId() {
        return userCardId;
    }

    public Long getCardId() {
        return cardId;
    }

    public String getCardName() {
        return cardName;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Long getBenefitId() {
        return benefitId;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public String getBenefitDescription() {
        return benefitDescription;
    }

    public String getBenefitLabel() {
        return benefitLabel;
    }

    public Integer getEstimatedBenefitScore() {
        return estimatedBenefitScore;
    }

<<<<<<< Updated upstream
=======
    public BigDecimal getExpectedBenefitAmount() {
        return expectedBenefitAmount;
    }

>>>>>>> Stashed changes
    public double getRecommendationScore() {
        return recommendationScore;
    }

    public boolean isRecommended() {
        return recommendationScore > 0;
    }
}
