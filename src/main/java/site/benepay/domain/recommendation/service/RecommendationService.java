package site.benepay.domain.recommendation.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

public interface RecommendationService {

	/**
	 * 선택한 매장에서 사용자의 보유 카드별 혜택을 모드 3(우선순위 비교) 기준으로 전부 비교하고,
	 * 그중 total이 가장 큰 한 장만 recommended=true로 표시한다. "오늘의 카드 추천"과 달리
	 * total&lt;=0인 카드도 걸러내지 않고 전부 반환한다 - 이 화면은 "왜 이 카드는 안 되는지"까지
	 * 보여주는 비교 화면이라서다. heldCards는 Facade가 카드 도메인에서 조회해 넘겨준다 - 이
	 * 서비스는 카드 도메인을 직접 알지 못한다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param merchantId 사용자가 선택한 매장 식별자
	 * @param heldCards 카드 도메인에서 전달받은 사용자 보유 카드 + 혜택/월별 실적 이력
	 * @return 선택 매장 정보와 보유 카드 전체의 혜택 비교 결과
	 */
	MerchantCardRecommendationResponseDto getCardRecommendations(
		Long userId,
		Long merchantId,
		List<RecommendationCardCandidateVO> heldCards
	);

	/**
	 * 카테고리/매장 검색 파이프라인(매장 도메인 -> 카드 도메인 -> 추천 도메인 순으로 위임하는
	 * Facade)이 이 메서드를 직접 호출한다고 가정한다. Facade가 유저 정보 + 위치 기반 매장
	 * 리스트 + 카드 도메인의 유저 보유 카드(월별 실적 이력 포함)를 한 번에 모아 넘겨주면,
	 * 매장마다 보유 카드 전체를 모드 3(우선순위 비교 - 결제 이력·지갑 전체 여력을 함께 고려)
	 * 기준으로 평가해 total(이번 달 확정 + 다음 달 기대)이 0보다 큰 카드를 내림차순 상위
	 * 3장까지 recommendedCards에 담는다. 매장을 걸러내지 않고 전달받은 매장 전부를 그대로
	 * 반환하되, 그런 카드가 하나라도 있는 매장만 benefitAvailable=true로 표시한다. 카테고리로
	 * 미리 좁히지 않고 전달받은 매장 리스트 전체를 평가하므로, 검색한 카테고리 밖이라 혜택받을
	 * 수 있는 다른 매장을 놓치는 문제가 없다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param heldCards 카드 도메인에서 전달받은 사용자 보유 카드 + 혜택/월별 실적 이력
	 * @param merchants 매장 도메인에서 전달받은 위치 기반 매장 후보 리스트
	 * @return 전달받은 매장 전부(각 매장마다 benefitAvailable/recommendedCards(최대 3장)로 혜택 사용 가능 여부 표시)
	 */
	List<NearbyMerchantRecommendationResponseDto> recommendMerchants(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<MerchantResponseDto> merchants
	);

	/**
	 * 홈 화면 "오늘의 카드 추천": 특정 매장이 아니라 지갑 전체 기준으로 "오늘 쓸 카드" 한 장을
	 * 고른다. 보유 카드 x 추천 분석 대상 카테고리(16개 대분류) 전체 조합을 모드 3으로 평가해
	 * total이 가장 큰 조합의 카드를 뽑고(카드가 어느 카테고리에서 제일 값어치가 큰지가
	 * categoryName), 그 카드가 실제로 혜택 주는 카테고리에 속하면서 total&gt;0인 근처 매장을
	 * 거리순으로 최대 2곳 붙여준다. 카드 선정 자체는 위치와 무관하므로, 근처에 맞는 매장이 하나도
	 * 없어도(nearbyMerchants가 비어도) 카드 추천 자체는 그대로 반환한다 - 어느 카드로도 어느
	 * 카테고리에서도 이득이 없을 때만(total&gt;0인 조합이 하나도 없을 때만) 완전히 빈 추천이다.
	 *
	 * @param userId 로그인한 사용자 식별자
	 * @param heldCards 카드 도메인에서 전달받은 사용자 보유 카드 + 혜택/월별 실적 이력
	 * @param nearbyMerchantCandidates 매장 도메인에서 전달받은 위치 기반 매장 후보 리스트
	 * @return 지갑 전체 기준 추천 카드 1장 + 그 카드가 통하는 가까운 매장 최대 2곳
	 */
	TodayCardRecommendationResponseDto getTodayCardRecommendation(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<MerchantResponseDto> nearbyMerchantCandidates
	);
}
