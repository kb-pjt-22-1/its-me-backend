package site.benepay.domain.recommendation.dto.response;

import site.benepay.domain.recommendation.model.RecommendedBenefitCard;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

public class BenefitStoreDetailResponseDto {

    private final StoreDto store;
    private final List<CardBenefitDto> cards;
    private final PaymentEntryDto paymentEntry;

    private BenefitStoreDetailResponseDto(
            StoreDto store,
            List<CardBenefitDto> cards,
            PaymentEntryDto paymentEntry
    ) {
        this.store = store;
        this.cards = cards;
        this.paymentEntry = paymentEntry;
    }

    public static BenefitStoreDetailResponseDto from(
            StoreBenefitCardCandidate store,
            List<RecommendedBenefitCard> cards
    ) {
        List<CardBenefitDto> cardDtos = cards.stream()
                .map(CardBenefitDto::from)
                .collect(Collectors.toList());
        Long selectedUserCardId = cardDtos.stream()
                .filter(CardBenefitDto::isRecommended)
                .findFirst()
                .map(CardBenefitDto::getUserCardId)
                .orElse(null);

        return new BenefitStoreDetailResponseDto(
                StoreDto.from(store),
                cardDtos,
                new PaymentEntryDto(store.getMerchantId(), selectedUserCardId)
        );
    }

    public StoreDto getStore() {
        return store;
    }

    public List<CardBenefitDto> getCards() {
        return cards;
    }

    public PaymentEntryDto getPaymentEntry() {
        return paymentEntry;
    }

    public static class StoreDto {
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

        private StoreDto(StoreBenefitCardCandidate store) {
            this.merchantId = store.getMerchantId();
            this.merchantName = store.getMerchantName();
            this.brandName = store.getBrandName();
            this.categoryCode = store.getCategoryCode();
            this.categoryName = store.getCategoryName();
            this.address = store.getAddress();
            this.latitude = store.getLatitude();
            this.longitude = store.getLongitude();
            this.distanceMeters = getDistanceMeters(store.getDistanceMeters());
            this.rating = store.getRating();
            this.bookmarked = store.isBookmarked();
        }

        private static StoreDto from(StoreBenefitCardCandidate store) {
            return new StoreDto(store);
        }

        private Integer getDistanceMeters(BigDecimal distanceMeters) {
            if (distanceMeters == null) {
                return null;
            }
            return distanceMeters.setScale(0, RoundingMode.HALF_UP).intValue();
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
    }

    public static class CardBenefitDto {
        private final Long userCardId;
        private final Long cardId;
        private final String cardName;
        private final String cardImageUrl;
        private final String cardLast4;
        private final boolean primary;
        private final boolean recommended;
        private final Long benefitId;
        private final String benefitType;
        private final String benefitName;
        private final String benefitDescription;
        private final String benefitLabel;
        private final Integer estimatedBenefitScore;

        private CardBenefitDto(RecommendedBenefitCard card) {
            this.userCardId = card.getUserCardId();
            this.cardId = card.getCardId();
            this.cardName = card.getCardName();
            this.cardImageUrl = card.getCardImageUrl();
            this.cardLast4 = card.getCardLast4();
            this.primary = card.isPrimary();
            this.recommended = card.isRecommended();
            this.benefitId = card.getBenefitId();
            this.benefitType = card.getBenefitType();
            this.benefitName = card.getBenefitName();
            this.benefitDescription = card.getBenefitDescription();
            this.benefitLabel = card.getBenefitLabel();
            this.estimatedBenefitScore = card.getEstimatedBenefitScore();
        }

        private static CardBenefitDto from(RecommendedBenefitCard card) {
            return new CardBenefitDto(card);
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

        public boolean isRecommended() {
            return recommended;
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
    }

    public static class PaymentEntryDto {
        private final Long merchantId;
        private final Long selectedUserCardId;

        private PaymentEntryDto(Long merchantId, Long selectedUserCardId) {
            this.merchantId = merchantId;
            this.selectedUserCardId = selectedUserCardId;
        }

        public Long getMerchantId() {
            return merchantId;
        }

        public Long getSelectedUserCardId() {
            return selectedUserCardId;
        }
    }
}
