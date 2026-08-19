package site.benepay.common.facade;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.service.CardService;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.RecommendedCardResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@Component
@RequiredArgsConstructor
public class Facade {

	// "오늘의 추천"(MerchantController)과 동일한 후보 풀 크기 - 가까운 순 20곳을 전부 혜택 평가한다.
	private static final int TODAY_CARD_CANDIDATE_POOL = 20;
	// "가까운 혜택 매장" 목록에 대표 매장 외 몇 곳을 더 보여줄지.
	private static final int TODAY_CARD_NEARBY_LIMIT = 2;

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
						Comparator.nullsLast(Double::compareTo))
			)
			.limit(limit)
			.toList();
	}

	/**
	 * 홈 화면 "오늘의 카드 추천": 사용자 현재 위치에서 가까운 매장 후보를 이 Facade가 직접
	 * 조회한다 - RecommendationController(추천 도메인)는 매장 도메인을 모르므로, 매장 조회는
	 * 여기(도메인 간 조율 계층)에서 하고 컨트롤러는 Facade만 의존한다. 후보 중 지금 당장
	 * 혜택받을 수 있는(benefitAvailable=true) 매장만 남겨 가장 가까운 곳을 대표로 삼고, 그 매장의
	 * 1순위 카드를 추천 카드로 보여준다. 대표 매장 다음으로 가까운 혜택 매장을 최대
	 * {@value #TODAY_CARD_NEARBY_LIMIT}곳까지 "가까운 혜택 매장"으로 함께 반환한다. 혜택 매장이
	 * 하나도 없으면 필드가 전부 비어있는 빈 추천을 반환한다 - "오늘의 추천"과 달리 혜택 없는
	 * 매장으로 자리를 채우지 않는다(이 위젯은 "혜택"이 핵심이라 채워봐야 의미가 없다).
	 */
	public TodayCardRecommendationResponseDto getTodayCardRecommendation(Long userId, double lat, double lng) {
		List<MerchantResponseDto> candidates =
			merchantService.getNearbyMerchants(lat, lng, null, TODAY_CARD_CANDIDATE_POOL);

		List<NearbyMerchantRecommendationResponseDto> benefitMerchants = getRecommendedMerchants(userId, candidates)
			.stream()
			.filter(NearbyMerchantRecommendationResponseDto::isBenefitAvailable)
			.sorted(Comparator.comparing(NearbyMerchantRecommendationResponseDto::getDistanceMeters,
				Comparator.nullsLast(Double::compareTo)))
			.toList();

		if (benefitMerchants.isEmpty()) {
			return TodayCardRecommendationResponseDto.empty();
		}

		NearbyMerchantRecommendationResponseDto featured = benefitMerchants.get(0);
		RecommendedCardResponseDto topCard = featured.getRecommendedCards().get(0);

		List<TodayCardRecommendationResponseDto.NearbyMerchant> nearby = benefitMerchants.stream()
			.skip(1)
			.limit(TODAY_CARD_NEARBY_LIMIT)
			.map(merchant -> TodayCardRecommendationResponseDto.NearbyMerchant.builder()
				.merchantId(merchant.getMerchantId())
				.merchantName(merchant.getMerchantName())
				.distanceMeters(merchant.getDistanceMeters())
				.benefitLabel(merchant.getRecommendedCards().get(0).getBenefitSummary())
				.build())
			.toList();

		return TodayCardRecommendationResponseDto.builder()
			.userCardId(topCard.getUserCardId())
			.cardName(topCard.getCardName())
			.categoryName(featured.getCategoryName())
			.benefitLabel(topCard.getBenefitSummary())
			.nearbyMerchants(nearby)
			.build();
	}
}
