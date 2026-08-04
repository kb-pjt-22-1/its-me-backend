package site.benepay.domain.merchant.service;

import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.vo.RecommendationMerchantCandidate;

import java.util.List;

public interface MerchantService {

    MerchantResponseDto createMerchant(MerchantRequestDto request);

    MerchantResponseDto getMerchant(Long merchantId);

    List<MerchantResponseDto> getMerchantList();

    List<RecommendationMerchantCandidate> getNearbyRecommendationCandidates(
            Long userId,
            Double latitude,
            Double longitude,
            Integer radiusMeters,
            String categoryCode,
            Integer candidateLimit
    );

    RecommendationMerchantCandidate getRecommendationCandidate(
            Long userId,
            Long merchantId,
            Double latitude,
            Double longitude
    );

    MerchantResponseDto updateMerchant(Long merchantId, MerchantRequestDto request);

    void deleteMerchant(Long merchantId);
}
