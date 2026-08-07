package site.benepay.domain.recommendation.service;

import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;

public interface RecommendationService {

	/**
	 * 선택한 매장에서 사용자의 보유 카드별 예상 혜택을 비교하고
	 * 최적의 카드를 추천한다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param merchantId 사용자가 선택한 매장 식별자
	 * @return 선택 매장 정보와 카드별 혜택 비교 결과
	 */
	MerchantCardRecommendationResponseDto getCardRecommendations(
		Long userId,
		Long merchantId
	);
}