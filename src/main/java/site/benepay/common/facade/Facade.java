package site.benepay.common.facade;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.service.CardService;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@Component
@RequiredArgsConstructor
public class Facade {

	private final CardService cardService;
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
}
