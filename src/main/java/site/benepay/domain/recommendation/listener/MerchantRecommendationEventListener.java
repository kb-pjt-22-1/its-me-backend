package site.benepay.domain.recommendation.listener;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.common.event.MerchantRecommendationRequestedEvent;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;

/**
 * 매장 도메인이 발행했다고 가정하는 {@link MerchantRecommendationRequestedEvent}의 리스너.
 * 유저 보유 카드 + 위치 기반 매장 리스트를 이벤트로 전달받아 추천 계산만 수행하고,
 * 그 결과를 이벤트에 채워 넣는다(발행 쪽이 이어서 프론트로 반환).
 */
@Component
@RequiredArgsConstructor
public class MerchantRecommendationEventListener {

	private final RecommendationService recommendationService;

	@EventListener
	public void handleMerchantRecommendationRequested(MerchantRecommendationRequestedEvent event) {
		List<NearbyMerchantRecommendationResponseDto> recommendedMerchants =
			recommendationService.recommendMerchants(
				event.getUserId(),
				event.getHeldCards(),
				event.getMerchants()
			);

		event.completeWith(recommendedMerchants);
	}
}
