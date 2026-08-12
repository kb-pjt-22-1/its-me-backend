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
		Long userId = getAuthenticatedUserId();

		MerchantCardRecommendationResponseDto response =
			recommendationService.getCardRecommendations(userId, merchantId);

		return ResponseEntity.ok(response);
	}

	/*
	 * 카테고리/매장 검색 기반 매장 추천(구 GET /categories/{categoryName}/cards)은 매장 도메인
	 * -> 카드 도메인 -> 추천 도메인 순으로 위임하는 Facade({기능명}ProcessFacade, 다른 팀원
	 * 작업)가 RecommendationService.recommendMerchants(...)를 직접 호출하는 구조로 대체됐다.
	 * 추천 도메인은 더 이상 이 흐름의 HTTP 진입점을 갖지 않는다 - Facade를 호출하는 컨트롤러는
	 * 매장/파이프라인 담당 팀원이 별도로 둔다.
	 */

	private Long getAuthenticatedUserId() {
		return (Long)SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
	}
}
