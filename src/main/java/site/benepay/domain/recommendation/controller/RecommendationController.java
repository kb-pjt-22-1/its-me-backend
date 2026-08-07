package site.benepay.domain.recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;

	/**
	 * 선택한 매장에서 사용자의 보유 카드별 혜택을 비교하고
	 * 가장 적합한 카드를 추천한다.
	 *
	 * @param merchantId 사용자가 선택한 매장 식별자
	 * @return 선택 매장 정보와 카드별 혜택 비교 결과
	 */
	@GetMapping("/merchants/{merchantId}/cards")
	public ResponseEntity<MerchantCardRecommendationResponseDto> getCardRecommendations(
		@PathVariable Long merchantId
	) {
		Long userId = (Long)SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();

		MerchantCardRecommendationResponseDto response =
			recommendationService.getCardRecommendations(userId, merchantId);

		return ResponseEntity.ok(response);
	}
}