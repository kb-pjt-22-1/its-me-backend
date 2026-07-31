package site.benepay.domain.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.benepay.domain.recommendation.algorithm.BenefitRecommendationAlgorithm;
<<<<<<< Updated upstream
=======
import site.benepay.domain.recommendation.dto.request.BenefitStoreDetailRequestDto;
>>>>>>> Stashed changes
import site.benepay.domain.recommendation.dto.request.NearbyBenefitStoreRequestDto;
import site.benepay.domain.recommendation.dto.request.RecommendationSort;
import site.benepay.domain.recommendation.dto.response.BenefitStoreDetailResponseDto;
import site.benepay.domain.recommendation.dto.response.NearbyBenefitStoreResponseDto;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
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
<<<<<<< Updated upstream
=======
    private static final String DEFAULT_PERFORMANCE_MONTH = "202606";
>>>>>>> Stashed changes

    private final RecommendationMapper recommendationMapper;
    private final BenefitRecommendationAlgorithm recommendationAlgorithm;

    public RecommendationServiceImpl(RecommendationMapper recommendationMapper,
                                      BenefitRecommendationAlgorithm recommendationAlgorithm) {
        this.recommendationMapper = recommendationMapper;
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

        List<BenefitStoreCandidate> candidates = recommendationMapper.getNearbyBenefitStoreCandidates(
                userId,
                request.getLatitude(),
                request.getLongitude(),
                radiusMeters,
                categoryCode,
                limit * 5
        );

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
<<<<<<< Updated upstream
            NearbyBenefitStoreRequestDto request
=======
            BenefitStoreDetailRequestDto request
>>>>>>> Stashed changes
    ) {
        if (merchantId == null) {
            throw new IllegalArgumentException("merchantId is required");
        }
        validateNullableLocation(request.getLatitude(), request.getLongitude());

        List<StoreBenefitCardCandidate> candidates = recommendationMapper.getBenefitStoreCardCandidates(
                userId,
                merchantId,
<<<<<<< Updated upstream
=======
                resolvePerformanceMonth(),
>>>>>>> Stashed changes
                request.getLatitude(),
                request.getLongitude()
        );
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("benefit store not found");
        }

        return BenefitStoreDetailResponseDto.from(
                candidates.get(0),
<<<<<<< Updated upstream
                recommendationAlgorithm.getRecommendedCards(candidates)
        );
    }

=======
                recommendationAlgorithm.getRecommendedCards(candidates, request.getEstimatedPaymentAmount())
        );
    }

    private String resolvePerformanceMonth() {
        return DEFAULT_PERFORMANCE_MONTH;
    }

>>>>>>> Stashed changes
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
