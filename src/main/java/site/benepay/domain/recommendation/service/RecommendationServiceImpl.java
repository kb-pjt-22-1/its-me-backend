package site.benepay.domain.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.benepay.domain.card.service.CardService;
import site.benepay.domain.card.vo.RecommendationUserCardCandidate;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.merchant.vo.RecommendationMerchantCandidate;
import site.benepay.domain.recommendation.algorithm.BenefitRecommendationAlgorithm;
import site.benepay.domain.recommendation.dto.request.BenefitStoreDetailRequestDto;
import site.benepay.domain.recommendation.dto.request.NearbyBenefitStoreRequestDto;
import site.benepay.domain.recommendation.dto.request.RecommendationSort;
import site.benepay.domain.recommendation.dto.response.BenefitStoreDetailResponseDto;
import site.benepay.domain.recommendation.dto.response.NearbyBenefitStoreResponseDto;
import site.benepay.domain.recommendation.model.BenefitStoreCandidate;
import site.benepay.domain.recommendation.model.RecommendedBenefitStore;
import site.benepay.domain.recommendation.model.StoreBenefitCardCandidate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_RADIUS_METERS = 1000;
    private static final int MAX_RADIUS_METERS = 5000;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final String DEFAULT_PERFORMANCE_MONTH = "202606";

    private final MerchantService merchantService;
    private final CardService cardService;
    private final BenefitRecommendationAlgorithm recommendationAlgorithm;

    public RecommendationServiceImpl(MerchantService merchantService,
                                      CardService cardService,
                                      BenefitRecommendationAlgorithm recommendationAlgorithm) {
        this.merchantService = merchantService;
        this.cardService = cardService;
        this.recommendationAlgorithm = recommendationAlgorithm;
    }

    @Override
    @Transactional(readOnly = true)
    public NearbyBenefitStoreResponseDto getNearbyBenefitStores(Long userId, NearbyBenefitStoreRequestDto request) {
        validateLocation(request.getLatitude(), request.getLongitude());

        int radiusMeters = normalizeRadius(request.getRadiusMeters());
        int limit = normalizeLimit(request.getLimit());
        String categoryCode = normalizeCategoryCode(request.getCategoryCode());
        RecommendationSort sort = request.getSort() == null ? RecommendationSort.DISTANCE : request.getSort();

        List<RecommendationMerchantCandidate> merchants = merchantService.getNearbyRecommendationCandidates(
                userId,
                request.getLatitude(),
                request.getLongitude(),
                radiusMeters,
                categoryCode,
                limit * 5
        );
        List<RecommendationUserCardCandidate> cards = cardService.getRecommendationEnabledCards(
                userId,
                resolvePerformanceMonth()
        );
        List<BenefitStoreCandidate> candidates = buildBenefitStoreCandidates(merchants, cards);

        List<RecommendedBenefitStore> stores = recommendationAlgorithm.getRecommendedStores(candidates).stream()
                .sorted(comparator(sort))
                .limit(limit)
                .collect(Collectors.toList());

        return NearbyBenefitStoreResponseDto.from(radiusMeters, stores);
    }

    @Override
    @Transactional(readOnly = true)
    public BenefitStoreDetailResponseDto getBenefitStoreDetail(
            Long userId,
            Long merchantId,
            BenefitStoreDetailRequestDto request
    ) {
        if (merchantId == null) {
            throw new IllegalArgumentException("merchantId is required");
        }
        validateNullableLocation(request.getLatitude(), request.getLongitude());

        RecommendationMerchantCandidate merchant = merchantService.getRecommendationCandidate(
                userId,
                merchantId,
                request.getLatitude(),
                request.getLongitude()
        );
        List<RecommendationUserCardCandidate> cards = cardService.getRecommendationEnabledCards(
                userId,
                resolvePerformanceMonth()
        );
        List<StoreBenefitCardCandidate> candidates = buildStoreBenefitCardCandidates(merchant, cards);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("benefit store not found");
        }

        return BenefitStoreDetailResponseDto.from(
                candidates.get(0),
                recommendationAlgorithm.getRecommendedCards(candidates, request.getEstimatedPaymentAmount())
        );
    }

    private String resolvePerformanceMonth() {
        return DEFAULT_PERFORMANCE_MONTH;
    }

    private List<BenefitStoreCandidate> buildBenefitStoreCandidates(
            List<RecommendationMerchantCandidate> merchants,
            List<RecommendationUserCardCandidate> cards
    ) {
        return merchants.stream()
                .flatMap(merchant -> cards.stream().map(card -> toBenefitStoreCandidate(merchant, card)))
                .collect(Collectors.toList());
    }

    private List<StoreBenefitCardCandidate> buildStoreBenefitCardCandidates(
            RecommendationMerchantCandidate merchant,
            List<RecommendationUserCardCandidate> cards
    ) {
        return cards.stream()
                .map(card -> toStoreBenefitCardCandidate(merchant, card))
                .collect(Collectors.toList());
    }

    private BenefitStoreCandidate toBenefitStoreCandidate(
            RecommendationMerchantCandidate merchant,
            RecommendationUserCardCandidate card
    ) {
        BenefitStoreCandidate candidate = new BenefitStoreCandidate();
        applyMerchant(candidate, merchant);
        candidate.setUserCardId(card.getUserCardId());
        candidate.setCardId(card.getCardId());
        candidate.setCardName(card.getCardName());
        candidate.setCardImageUrl(card.getCardImageUrl());
        candidate.setBenefitType(card.getBenefitType());
        candidate.setBenefitName(card.getBenefitName());
        candidate.setBenefitDescription(card.getBenefitDescription());
        candidate.setBenefitsInfo(card.getBenefitsInfo());
        candidate.setTotalSpendingAmount(card.getTotalSpendingAmount());
        return candidate;
    }

    private StoreBenefitCardCandidate toStoreBenefitCardCandidate(
            RecommendationMerchantCandidate merchant,
            RecommendationUserCardCandidate card
    ) {
        StoreBenefitCardCandidate candidate = new StoreBenefitCardCandidate();
        candidate.setMerchantId(merchant.getMerchantId());
        candidate.setMerchantName(merchant.getMerchantName());
        candidate.setBrandCode(merchant.getBrandCode());
        candidate.setBrandName(merchant.getBrandName());
        candidate.setCategoryCode(merchant.getCategoryCode());
        candidate.setCategoryName(merchant.getCategoryName());
        candidate.setAddress(merchant.getAddress());
        candidate.setLatitude(merchant.getLatitude());
        candidate.setLongitude(merchant.getLongitude());
        candidate.setDistanceMeters(merchant.getDistanceMeters());
        candidate.setRating(merchant.getRating());
        candidate.setBookmarked(merchant.isBookmarked());
        candidate.setUserCardId(card.getUserCardId());
        candidate.setCardId(card.getCardId());
        candidate.setCardName(card.getCardName());
        candidate.setCardImageUrl(card.getCardImageUrl());
        candidate.setCardLast4(card.getCardLast4());
        candidate.setPrimary(card.isPrimary());
        candidate.setBenefitType(card.getBenefitType());
        candidate.setBenefitName(card.getBenefitName());
        candidate.setBenefitDescription(card.getBenefitDescription());
        candidate.setBenefitsInfo(card.getBenefitsInfo());
        candidate.setTotalSpendingAmount(card.getTotalSpendingAmount());
        return candidate;
    }

    private void applyMerchant(BenefitStoreCandidate candidate, RecommendationMerchantCandidate merchant) {
        candidate.setMerchantId(merchant.getMerchantId());
        candidate.setMerchantName(merchant.getMerchantName());
        candidate.setBrandCode(merchant.getBrandCode());
        candidate.setBrandName(merchant.getBrandName());
        candidate.setCategoryCode(merchant.getCategoryCode());
        candidate.setCategoryName(merchant.getCategoryName());
        candidate.setAddress(merchant.getAddress());
        candidate.setLatitude(merchant.getLatitude());
        candidate.setLongitude(merchant.getLongitude());
        candidate.setDistanceMeters(merchant.getDistanceMeters());
        candidate.setRating(merchant.getRating());
        candidate.setBookmarked(merchant.isBookmarked());
    }

    private void validateLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }

    private void validateNullableLocation(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        validateLocation(latitude, longitude);
    }

    private int normalizeRadius(Integer radiusMeters) {
        if (radiusMeters == null) {
            return DEFAULT_RADIUS_METERS;
        }
        if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and " + MAX_RADIUS_METERS);
        }
        return radiusMeters;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private String normalizeCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank() || "ALL".equalsIgnoreCase(categoryCode)) {
            return null;
        }
        return categoryCode.trim().toUpperCase(Locale.ROOT);
    }

    private Comparator<RecommendedBenefitStore> comparator(RecommendationSort sort) {
        if (sort == RecommendationSort.BENEFIT) {
            return Comparator
                    .comparingDouble(RecommendedBenefitStore::getRecommendationScore).reversed()
                    .thenComparingInt(RecommendedBenefitStore::getDistanceMeters);
        }
        return Comparator
                .comparingInt(RecommendedBenefitStore::getDistanceMeters)
                .thenComparing(Comparator.comparingDouble(RecommendedBenefitStore::getRecommendationScore).reversed());
    }
}
