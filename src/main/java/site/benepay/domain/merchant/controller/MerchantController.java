package site.benepay.domain.merchant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

	private final MerchantService merchantService;

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
	 * 지도 화면(bounds) 안에 있는 매장만 조회
	 * 남서쪽(sw)/북동쪽(ne) 좌표로 이루어진 사각형 범위 안의 매장만 반환한다.
	 * @param swLat 남서쪽 위도
	 * @param swLng 남서쪽 경도
	 * @param neLat 북동쪽 위도
	 * @param neLng 북동쪽 경도
	 * @param categoryCode 카테고리 코드. 없으면 전체 카테고리
	 */
	@GetMapping("/within-bounds")
	public ResponseEntity<List<MerchantResponseDto>> getMerchants(
		@RequestParam double swLat,
		@RequestParam double swLng,
		@RequestParam double neLat,
		@RequestParam double neLng,
		@RequestParam(required = false) String categoryCode
	) {
		return ResponseEntity.ok(merchantService.getMerchants(swLat, swLng, neLat, neLng, categoryCode));
	}
}
