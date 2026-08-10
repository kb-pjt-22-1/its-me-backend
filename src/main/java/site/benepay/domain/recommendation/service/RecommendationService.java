package site.benepay.domain.recommendation.service;

import java.util.List;

import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

public interface RecommendationService {

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

	/**
	 * 매장 도메인이 (유저 아이디 + 위치정보) 진입점에서 카드 도메인의 유저 보유 카드와 자신이
	 * 조회한 위치 기반 매장 리스트를 모아 전달하면(가정 - 실제로는
	 * {@link site.benepay.common.event.MerchantRecommendationRequestedEvent}로 전달됨), 매장마다
	 * 보유 카드 전체를 모드 1(즉시 할인) 기준으로 평가해 최적 카드를 고르고, 그 카드가 지금 당장
	 * 혜택을 주는 매장만 추려서 반환한다. 카테고리로 미리 좁히지 않고 전달받은 매장 리스트 전체를
	 * 평가하므로, 검색한 카테고리 밖이라 혜택받을 수 있는 다른 매장을 놓치는 문제가 없다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param heldCards 카드 도메인에서 전달받은 사용자 보유 카드 + 혜택 정보
	 * @param merchants 매장 도메인에서 전달받은 위치 기반 매장 후보 리스트
	 * @return 최적 카드가 즉시할인을 주는 매장 목록(recommendedCardName 포함)
	 */
	List<NearbyMerchantRecommendationResponseDto> recommendMerchants(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<NearbyMerchantResponseDto> merchants
	);
}