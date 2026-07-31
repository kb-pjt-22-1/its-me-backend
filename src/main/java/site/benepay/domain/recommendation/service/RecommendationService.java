package site.benepay.domain.recommendation.service;

<<<<<<< Updated upstream
=======
import site.benepay.domain.recommendation.dto.request.BenefitStoreDetailRequestDto;
>>>>>>> Stashed changes
import site.benepay.domain.recommendation.dto.request.NearbyBenefitStoreRequestDto;
import site.benepay.domain.recommendation.dto.response.BenefitStoreDetailResponseDto;
import site.benepay.domain.recommendation.dto.response.NearbyBenefitStoreResponseDto;

public interface RecommendationService {

    NearbyBenefitStoreResponseDto getNearbyBenefitStores(Long userId, NearbyBenefitStoreRequestDto request);

    BenefitStoreDetailResponseDto getBenefitStoreDetail(
            Long userId,
            Long merchantId,
<<<<<<< Updated upstream
            NearbyBenefitStoreRequestDto request
=======
            BenefitStoreDetailRequestDto request
>>>>>>> Stashed changes
    );
}
