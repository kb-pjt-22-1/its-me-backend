package site.benepay.domain.recommendation.model;

import java.math.BigDecimal;

public class RecommendedBenefitStore {

    private final Long merchantId;
    private final String merchantName;
    private final String brandName;
    private final String categoryCode;
    private final String categoryName;
    private final String address;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final Integer distanceMeters;
    private final BigDecimal rating;
    private final boolean bookmarked;
    private final Long userCardId;
    private final Long cardId;
    private final String cardName;
    private final String cardImageUrl;
    private final Long benefitId;
    private final String benefitType;
    private final String benefitName;
    private final String benefitLabel;
    private final Integer estimatedBenefitScore;
    private final double recommendationScore;

    public RecommendedBenefitStore(BenefitStoreCandidate candidate, String benefitLabel,
                                    Integer estimatedBenefitScore, double recommendationScore) {
        this.merchantId = candidate.getMerchantId();
        this.merchantName = candidate.getMerchantName();
        this.brandName = candidate.getBrandName();
        this.categoryCode = candidate.getCategoryCode();
        this.categoryName = candidate.getCategoryName();
        this.address = candidate.getAddress();
        this.latitude = candidate.getLatitude();
        this.longitude = candidate.getLongitude();
        this.distanceMeters = candidate.getDistanceMeters().setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        this.rating = candidate.getRating();
        this.bookmarked = candidate.isBookmarked();
        this.userCardId = candidate.getUserCardId();
        this.cardId = candidate.getCardId();
        this.cardName = candidate.getCardName();
        this.cardImageUrl = candidate.getCardImageUrl();
        this.benefitId = candidate.getBenefitId();
        this.benefitType = candidate.getBenefitType();
        this.benefitName = candidate.getBenefitName();
        this.benefitLabel = benefitLabel;
        this.estimatedBenefitScore = estimatedBenefitScore;
        this.recommendationScore = recommendationScore;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public boolean isBookmarked() {
        return bookmarked;
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

    public Long getBenefitId() {
        return benefitId;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public String getBenefitName() {
        return benefitName;
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
}
