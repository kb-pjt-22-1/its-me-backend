package site.benepay.domain.recommendation.model;

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
    private final double recommendationScore;

    public RecommendedBenefitCard(
            StoreBenefitCardCandidate candidate,
            String benefitLabel,
            Integer estimatedBenefitScore,
            double recommendationScore
    ) {
        this.userCardId = candidate.getUserCardId();
        this.cardId = candidate.getCardId();
        this.cardName = candidate.getCardName();
        this.cardImageUrl = candidate.getCardImageUrl();
        this.cardLast4 = candidate.getCardLast4();
        this.primary = candidate.isPrimary();
        this.benefitId = candidate.getBenefitId();
        this.benefitType = candidate.getBenefitType();
        this.benefitName = candidate.getBenefitName();
        this.benefitDescription = candidate.getBenefitDescription();
        this.benefitLabel = benefitLabel;
        this.estimatedBenefitScore = estimatedBenefitScore;
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

    public double getRecommendationScore() {
        return recommendationScore;
    }

    public boolean isRecommended() {
        return recommendationScore > 0;
    }
}
