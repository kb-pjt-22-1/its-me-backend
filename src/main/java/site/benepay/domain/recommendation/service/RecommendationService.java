package site.benepay.domain.recommendation.service;

import java.util.List;

import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;

public interface RecommendationService {

	/**
	 * 주변 매장 후보 중 사용자의 카드 혜택을 받을 수 있는 매장만 조회한다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param latitude 사용자 현재 위도
	 * @param longitude 사용자 현재 경도
	 * @param radiusMeters 조회 반경(m)
	 * @return 카드 혜택을 받을 수 있는 주변 매장 목록
	 */
	List<NearbyMerchantRecommendationResponseDto> getNearbyMerchants(
		Long userId,
		double latitude,
		double longitude,
		double radiusMeters
	);

	/**
	 * 선택한 매장에서 사용자의 보유 카드별 혜택을 비교하고
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