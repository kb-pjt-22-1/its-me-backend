package site.benepay.common.facade;

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

	/*
	 * TODO: 추천 로직이 연동되면 사용자 보유 카드로 지금 당장 혜택을 주는 매장만
	 * recommended=true로 표시하도록 교체한다. 지금은 후보 매장을 그대로 반환한다(전부 recommended=false).
	 */

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
}
