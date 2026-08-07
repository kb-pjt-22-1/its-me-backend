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
	 * 지도 화면 bounds(bbox) 안의 매장 중, 사용자 보유 카드로 골랐을 때 최적 카드가 지금 당장
	 * 혜택을 주는 매장만 반환한다(recommendedCardName에 그 카드명이 채워진다).
	 *
	 * @param swLat 지도 화면 남서쪽 위도
	 * @param swLng 지도 화면 남서쪽 경도
	 * @param neLat 지도 화면 북동쪽 위도
	 * @param neLng 지도 화면 북동쪽 경도
	 * @return 최적 카드가 즉시할인을 주는 매장 목록
	 */
	@GetMapping("/merchants")
	public ResponseEntity<List<NearbyMerchantRecommendationResponseDto>> getNearbyMerchants(
		@RequestParam double swLat,
		@RequestParam double swLng,
		@RequestParam double neLat,
		@RequestParam double neLng
	) {
		Long userId = getAuthenticatedUserId();

		List<NearbyMerchantRecommendationResponseDto> response =
			recommendationService.getNearbyMerchants(
				userId,
				swLat,
				swLng,
				neLat,
				neLng
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
