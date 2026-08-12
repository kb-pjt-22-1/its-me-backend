package site.benepay.domain.recommendation.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantResponseDto;
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
	 * 카테고리/매장 검색 파이프라인(매장 도메인 -> 카드 도메인 -> 추천 도메인 순으로 위임하는
	 * Facade)이 이 메서드를 직접 호출한다고 가정한다. Facade가 유저 정보 + 위치 기반 매장
	 * 리스트 + 카드 도메인의 유저 보유 카드(월별 실적 이력 포함)를 한 번에 모아 넘겨주면,
	 * 매장마다 보유 카드 전체를 모드 3(우선순위 비교 - 결제 이력·지갑 전체 여력을 함께 고려)
	 * 기준으로 평가해 최적 카드를 고른다. 매장을 걸러내지 않고 전달받은 매장 전부를 그대로
	 * 반환하되, 최적 카드의 총 기대 가치(이번 달 확정 + 다음 달 기대)가 0보다 큰 매장만
	 * benefitAvailable=true로 표시한다. 카테고리로 미리 좁히지 않고 전달받은 매장 리스트
	 * 전체를 평가하므로, 검색한 카테고리 밖이라 혜택받을 수 있는 다른 매장을 놓치는 문제가 없다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param heldCards 카드 도메인에서 전달받은 사용자 보유 카드 + 혜택/월별 실적 이력
	 * @param merchants 매장 도메인에서 전달받은 위치 기반 매장 후보 리스트
	 * @return 전달받은 매장 전부(각 매장마다 benefitAvailable로 혜택 사용 가능 여부 표시)
	 */
	List<NearbyMerchantRecommendationResponseDto> recommendMerchants(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<MerchantResponseDto> merchants
	);
}
