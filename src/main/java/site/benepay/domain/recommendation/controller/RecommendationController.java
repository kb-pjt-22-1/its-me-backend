package site.benepay.domain.recommendation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.recommendation.dto.CategoryCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;

	/**
	 * 주변 매장 후보 중 사용자의 보유 카드 혜택을 받을 수 있는 매장만 반환한다.
	 *
	 * @param latitude 사용자 현재 위도
	 * @param longitude 사용자 현재 경도
	 * @param radiusMeters 조회 반경(m), 기본 1000m
	 * @return 카드 혜택을 받을 수 있는 주변 매장 목록
	 */
	@GetMapping("/merchants")
	public ResponseEntity<List<NearbyMerchantRecommendationResponseDto>> getNearbyMerchants(
		@RequestParam double latitude,
		@RequestParam double longitude,
		@RequestParam(defaultValue = "1000") double radiusMeters
	) {
		Long userId = getAuthenticatedUserId();

		List<NearbyMerchantRecommendationResponseDto> response =
			recommendationService.getNearbyMerchants(
				userId,
				latitude,
				longitude,
				radiusMeters
			);

		return ResponseEntity.ok(response);
	}

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

	/**
	 * 대분류(카테고리명)를 검색해 보유 카드를 모드 1(즉시 할인) 기준으로 순위 매긴다.
	 *
	 * @param categoryName merchant_categories.category_name과 일치하는 대분류명(예: "카페")
	 * @return 상태별로 그룹핑되고 그룹 내 할인율 내림차순으로 정렬된 카드 목록
	 */
	@GetMapping("/categories/{categoryName}/cards")
	public ResponseEntity<CategoryCardRecommendationResponseDto> getCardRecommendationsByCategory(
		@PathVariable String categoryName
	) {
		Long userId = getAuthenticatedUserId();

		CategoryCardRecommendationResponseDto response =
			recommendationService.getCardRecommendationsByCategory(userId, categoryName);

		return ResponseEntity.ok(response);
	}

	private Long getAuthenticatedUserId() {
		return (Long)SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
	}
}
