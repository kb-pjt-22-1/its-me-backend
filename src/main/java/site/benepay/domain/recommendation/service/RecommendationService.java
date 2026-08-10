package site.benepay.domain.recommendation.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;

public interface RecommendationService {

	/**
	 * 매장 후보 목록을 받아, 사용자 보유 카드 기준으로 지금 당장 혜택을 주는 매장을
	 * recommended=true로 표시해 그대로 돌려준다.
	 */
	List<MerchantRecommendationResponseDto> markRecommendedMerchants(
		Long userId, List<MerchantRecommendationResponseDto> candidates);
}
