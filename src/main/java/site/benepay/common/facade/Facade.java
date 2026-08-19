package site.benepay.common.facade;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.service.CardService;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@Component
@RequiredArgsConstructor
public class Facade {

	// "오늘의 추천"(MerchantController)과 동일한 후보 풀 크기 - 가까운 순 20곳을 전부 혜택 평가한다.
	private static final int TODAY_CARD_CANDIDATE_POOL = 20;

	private final CardService cardService;
	private final MerchantService merchantService;
	private final RecommendationService recommendationService;

	public List<NearbyMerchantRecommendationResponseDto> getRecommendedMerchants(Long userId,
		List<MerchantResponseDto> merchants) {
		List<RecommendationCardCandidateVO> heldCards =
			cardService.getRecommendationCandidates(userId);

		return recommendationService.recommendMerchants(
			userId,
			heldCards,
			merchants
		);
	}

	/**
	 * 매장 상세 "이 매장 추천 카드": 보유 카드 전체를 이 매장 카테고리 기준으로 비교한다.
	 * RecommendationController(추천 도메인)는 카드 도메인을 모르므로, 보유 카드 조회는 여기서
	 * 하고 컨트롤러는 Facade만 의존한다.
	 */
	public MerchantCardRecommendationResponseDto getCardRecommendations(Long userId, Long merchantId) {
		List<RecommendationCardCandidateVO> heldCards =
			cardService.getRecommendationCandidates(userId);

		return recommendationService.getCardRecommendations(userId, merchantId, heldCards);
	}

	/**
	 * 홈 화면 "오늘의 추천": 후보 매장(candidates, 이미 가까운 순으로 정렬돼 들어옴) 전부를 평가한 뒤
	 * 지금 당장 혜택 받을 수 있는 매장(benefitAvailable=true)을 우선으로, 그다음 가까운 순으로
	 * 정렬해 최대 limit개만 반환한다. 혜택 매장이 부족하면 남은 자리는 그냥 가까운 매장으로
	 * 채운다(화면이 비어 보이지 않도록) - 필터링이 아니라 정렬 우선순위다.
	 */
	public List<NearbyMerchantRecommendationResponseDto> getTodayRecommendedMerchants(Long userId,
		List<MerchantResponseDto> candidates, int limit) {
		return getRecommendedMerchants(userId, candidates).stream()
			.sorted(
				Comparator.comparing(NearbyMerchantRecommendationResponseDto::isBenefitAvailable).reversed()
					.thenComparing(NearbyMerchantRecommendationResponseDto::getDistanceMeters,
						Comparator.nullsLast(Long::compareTo))
			)
			.limit(limit)
			.toList();
	}

	/**
	 * 홈 화면 "오늘의 카드 추천": 사용자 현재 위치에서 가까운 매장 후보와 보유 카드를 이
	 * Facade가 모아서 recommendationService에 넘긴다 - RecommendationController(추천 도메인)는
	 * 카드/매장 도메인을 모르므로, 두 도메인 조회는 여기(도메인 간 조율 계층)에서 하고 실제
	 * "오늘 쓸 카드" 산정(지갑 전체 기준, 매장과 무관)은 recommendationService가 맡는다.
	 */
	public TodayCardRecommendationResponseDto getTodayCardRecommendation(Long userId, double lat, double lng) {
		List<RecommendationCardCandidateVO> heldCards =
			cardService.getRecommendationCandidates(userId);
		List<MerchantResponseDto> candidates =
			merchantService.getNearbyMerchants(lat, lng, null, TODAY_CARD_CANDIDATE_POOL);

		return recommendationService.getTodayCardRecommendation(userId, heldCards, candidates);
	}
}
