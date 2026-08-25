package site.benepay.domain.merchant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.common.facade.Facade;
import site.benepay.common.util.GeoCoordinateValidator;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

	// 오늘의 추천 후보 풀 크기 - 가까운 순 20곳을 전부 혜택 평가한 뒤 상위 TODAY_RECOMMENDATION_LIMIT개만 추린다.
	private static final int TODAY_RECOMMENDATION_CANDIDATE_POOL = 20;
	private static final int TODAY_RECOMMENDATION_LIMIT = 2;
	// 지도 화면(bounds) 조회 상한 - bounds가 넓어져도(축소된 지도) 응답이 무한정 커지지 않도록
	// centerLat/centerLng 기준 가까운 순 이 개수까지만 자른다. 나머지는 화면에서 마커
	// 클러스터링으로 뭉쳐 보여준다.
	private static final int BOUNDS_SEARCH_LIMIT = 500;
	// GET /api/v1/merchants 기본 상한 - categoryCode 없이 부르면 예전엔 매장 전체(2만 건+)를
	// LIMIT 없이 다 내려줘서, 로그인 직후 App.vue가 이 API를 부를 때마다 500ms+가 걸렸다
	// (지도 자체가 아니라 이 호출이 지도 화면 진입 시점과 겹쳐서 체감 로딩을 늘렸다).
	// "카테고리 전체 검색"(Map.vue searchCategoryAll)이 쓰는 값이라, 웬만한 카테고리는 다
	// 들어오도록 넉넉하게 잡는다 - 이보다 매장이 많은 카테고리는 나머지가 클러스터링으로 뭉쳐
	// 보이던 지도 화면과 달리 안 보이게 되니, 필요해지면 진짜 페이지네이션으로 바꿔야 한다.
	private static final int MERCHANT_LIST_LIMIT = 2000;

	private final MerchantService merchantService;
	private final Facade facade;

	/**
	 * 매장 조회
	 * 지도 화면에서 매장들을 보여주기 위해 매장 리스트를 반환할 때 사용
	 * @param categoryCode 카테고리 코드. 없으면 전체 카테고리
	 */
	@GetMapping
	public ResponseEntity<List<MerchantResponseDto>> getMerchants(
		@RequestParam(required = false) String categoryCode
	) {
		return ResponseEntity.ok(merchantService.getMerchants(categoryCode, MERCHANT_LIST_LIMIT));
	}

	/**
	 * 매장 상세 조회
	 * 지도 화면에서 핀을 클릭했을 때 매장 상세 정보를 보여줄 때 사용
	 * @param merchantId 매장 식별자
	 */
	@GetMapping("/{merchantId}")
	public ResponseEntity<MerchantResponseDto> getMerchant(@PathVariable Long merchantId) {
		return ResponseEntity.ok(merchantService.getMerchant(merchantId));
	}

	/**
	 * 지도 화면(bounds) 안의 매장 후보를 조회해서 Facade에 넘기고, Facade가 사용자 보유 카드로
	 * 지금 당장 혜택을 주는 매장에 recommended=true 표시를 붙여 처리한 결과를 반환한다. bounds가
	 * 넓어져도(축소된 지도) centerLat/centerLng 기준 가까운 순 최대 {@value #BOUNDS_SEARCH_LIMIT}
	 * 개까지만 조회한다.
	 * @param swLat 남서쪽 위도
	 * @param swLng 남서쪽 경도
	 * @param neLat 북동쪽 위도
	 * @param neLng 북동쪽 경도
	 * @param centerLat 거리 정렬/상한 기준 중심 위도
	 * @param centerLng 거리 정렬/상한 기준 중심 경도
	 * @param categoryCode 카테고리 코드. 없으면 전체 카테고리
	 */
	@GetMapping("/recommendations")
	public ResponseEntity<List<NearbyMerchantRecommendationResponseDto>> getRecommendedMerchantsInBounds(
		@AuthenticationPrincipal Long userId,
		@RequestParam double swLat,
		@RequestParam double swLng,
		@RequestParam double neLat,
		@RequestParam double neLng,
		@RequestParam double centerLat,
		@RequestParam double centerLng,
		@RequestParam(required = false) String categoryCode
	) {
		GeoCoordinateValidator.validate(swLat, swLng);
		GeoCoordinateValidator.validate(neLat, neLng);
		GeoCoordinateValidator.validate(centerLat, centerLng);

		List<MerchantResponseDto> merchants = merchantService.getMerchants(swLat, swLng, neLat, neLng, centerLat,
			centerLng, categoryCode, BOUNDS_SEARCH_LIMIT);
		return ResponseEntity.ok(facade.getRecommendedMerchants(userId, merchants));
	}

	/**
	 * 홈 화면 "오늘의 추천": 사용자 현재 위치에서 가까운 매장 후보 중, 지금 당장 보유 카드로
	 * 혜택 받을 수 있는 매장을 우선으로 최대 {@value #TODAY_RECOMMENDATION_LIMIT}곳을 반환한다.
	 * @param lat 사용자 위도
	 * @param lng 사용자 경도
	 * @param categoryCode 카테고리 코드. 없으면 전체 카테고리
	 */
	@GetMapping("/today-recommendation")
	public ResponseEntity<List<NearbyMerchantRecommendationResponseDto>> getTodayRecommendation(
		@AuthenticationPrincipal Long userId,
		@RequestParam double lat,
		@RequestParam double lng,
		@RequestParam(required = false) String categoryCode
	) {
		GeoCoordinateValidator.validate(lat, lng);

		List<MerchantResponseDto> candidates =
			merchantService.getNearbyMerchants(lat, lng, categoryCode, TODAY_RECOMMENDATION_CANDIDATE_POOL);
		return ResponseEntity.ok(facade.getTodayRecommendedMerchants(userId, candidates, TODAY_RECOMMENDATION_LIMIT));
	}
}
