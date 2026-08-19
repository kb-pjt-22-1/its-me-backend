package site.benepay.domain.recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.common.facade.Facade;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;
	private final Facade facade;

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
	 * 홈 화면 "오늘의 카드 추천": 사용자 현재 위치에서 가까운 매장 후보 중, 지금 당장 혜택받을 수
	 * 있는 가장 가까운 매장의 1순위 카드를 대표로 보여준다.
	 * @param lat 사용자 위도
	 * @param lng 사용자 경도
	 */
	@GetMapping("/today")
	public ResponseEntity<TodayCardRecommendationResponseDto> getTodayCardRecommendation(
		@RequestParam double lat,
		@RequestParam double lng
	) {
		Long userId = getAuthenticatedUserId();

		return ResponseEntity.ok(facade.getTodayCardRecommendation(userId, lat, lng));
	}

	/*
	 * 카테고리/매장 검색 기반 매장 추천(구 GET /categories/{categoryName}/cards, bounds 기반
	 * GET /api/v1/merchants/recommendations, "오늘의 추천" GET /api/v1/merchants/today-recommendation)은
	 * 매장 도메인 -> 카드 도메인 -> 추천 도메인 순으로 위임하는 Facade를 매장 도메인의
	 * MerchantController가 호출하는 구조다 - 매장 후보 조회 자체가 매장 도메인 책임이라서다.
	 * 위 getTodayCardRecommendation도 같은 원칙을 따른다 - 이 컨트롤러(추천 도메인)는
	 * 매장 도메인을 모르므로 후보 조회는 Facade 안에서 하고, 여긴 Facade에만 의존한다.
	 */

	private Long getAuthenticatedUserId() {
		return (Long)SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
	}
}
