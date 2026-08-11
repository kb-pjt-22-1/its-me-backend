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
import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

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
		return ResponseEntity.ok(merchantService.getMerchants(categoryCode));
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
	 * 지금 당장 혜택을 주는 매장에 recommended=true 표시를 붙여 처리한 결과를 반환한다.
	 * @param swLat 남서쪽 위도
	 * @param swLng 남서쪽 경도
	 * @param neLat 북동쪽 위도
	 * @param neLng 북동쪽 경도
	 * @param categoryCode 카테고리 코드. 없으면 전체 카테고리
	 */
	@GetMapping("/recommendations")
	public ResponseEntity<List<MerchantRecommendationResponseDto>> getRecommendedMerchantsInBounds(
		@AuthenticationPrincipal Long userId,
		@RequestParam double swLat,
		@RequestParam double swLng,
		@RequestParam double neLat,
		@RequestParam double neLng,
		@RequestParam(required = false) String categoryCode
	) {
		List<MerchantResponseDto> merchants = merchantService.getMerchants(swLat, swLng, neLat, neLng, categoryCode);
		return ResponseEntity.ok(facade.getRecommendedMerchants(userId, merchants));
	}
}
