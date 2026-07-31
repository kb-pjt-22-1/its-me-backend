package site.benepay.domain.recommendation.service;

import site.benepay.domain.recommendation.dto.request.BenefitStoreDetailRequestDto;
import site.benepay.domain.recommendation.dto.request.NearbyBenefitStoreRequestDto;
import site.benepay.domain.recommendation.dto.response.BenefitStoreDetailResponseDto;
import site.benepay.domain.recommendation.dto.response.NearbyBenefitStoreResponseDto;

public interface RecommendationService {

    NearbyBenefitStoreResponseDto getNearbyBenefitStores(Long userId, NearbyBenefitStoreRequestDto request);

    BenefitStoreDetailResponseDto getBenefitStoreDetail(
            Long userId,
            Long merchantId,
            BenefitStoreDetailRequestDto request
    );
}
