package site.benepay.domain.recommendation.dto.response;

import site.benepay.domain.recommendation.model.RecommendedBenefitStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class NearbyBenefitStoreResponseDto {

    private final String baseLocationLabel;
    private final Integer radiusMeters;
    private final Integer totalCount;
    private final List<BenefitStoreDto> stores;

    private NearbyBenefitStoreResponseDto(String baseLocationLabel, Integer radiusMeters,
                                           Integer totalCount, List<BenefitStoreDto> stores) {
        this.baseLocationLabel = baseLocationLabel;
        this.radiusMeters = radiusMeters;
        this.totalCount = totalCount;
        this.stores = stores;
    }

    public static NearbyBenefitStoreResponseDto from(Integer radiusMeters, List<RecommendedBenefitStore> stores) {
        List<BenefitStoreDto> storeDtos = stores.stream()
                .map(BenefitStoreDto::from)
                .collect(Collectors.toList());
        return new NearbyBenefitStoreResponseDto("CURRENT_LOCATION", radiusMeters, storeDtos.size(), storeDtos);
    }

    public String getBaseLocationLabel() {
        return baseLocationLabel;
    }

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public List<BenefitStoreDto> getStores() {
        return stores;
    }

    public static class BenefitStoreDto {
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
        private final RecommendedCardDto recommendedCard;

        private BenefitStoreDto(RecommendedBenefitStore store) {
            this.merchantId = store.getMerchantId();
            this.merchantName = store.getMerchantName();
            this.brandName = store.getBrandName();
            this.categoryCode = store.getCategoryCode();
            this.categoryName = store.getCategoryName();
            this.address = store.getAddress();
            this.latitude = store.getLatitude();
            this.longitude = store.getLongitude();
            this.distanceMeters = store.getDistanceMeters();
            this.rating = store.getRating();
            this.bookmarked = store.isBookmarked();
            this.recommendedCard = RecommendedCardDto.from(store);
        }

        private static BenefitStoreDto from(RecommendedBenefitStore store) {
            return new BenefitStoreDto(store);
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

        public RecommendedCardDto getRecommendedCard() {
            return recommendedCard;
        }
    }

    public static class RecommendedCardDto {
        private final Long cardId;
        private final String cardName;
        private final String cardImageUrl;
        private final Long benefitId;
        private final String benefitName;
        private final String benefitType;
        private final String benefitLabel;
        private final Integer estimatedBenefitScore;

        private RecommendedCardDto(RecommendedBenefitStore store) {
            this.cardId = store.getCardId();
            this.cardName = store.getCardName();
            this.cardImageUrl = store.getCardImageUrl();
            this.benefitId = store.getBenefitId();
            this.benefitName = store.getBenefitName();
            this.benefitType = store.getBenefitType();
            this.benefitLabel = store.getBenefitLabel();
            this.estimatedBenefitScore = store.getEstimatedBenefitScore();
        }

        private static RecommendedCardDto from(RecommendedBenefitStore store) {
            return new RecommendedCardDto(store);
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

        public String getBenefitName() {
            return benefitName;
        }

        public String getBenefitType() {
            return benefitType;
        }

        public String getBenefitLabel() {
            return benefitLabel;
        }

        public Integer getEstimatedBenefitScore() {
            return estimatedBenefitScore;
        }
    }
}
