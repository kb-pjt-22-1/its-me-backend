package site.benepay.domain.merchant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantBrandResponseDto;
import site.benepay.domain.merchant.service.MerchantBrandService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchant-brands")
public class MerchantBrandController {
	private final MerchantBrandService merchantBrandService;

	/**
	 * 브랜드 조회. 매장 등록 폼에서 목록을 채울 때 사용.
	 */
	@GetMapping
	public ResponseEntity<List<MerchantBrandResponseDto>> getBrandList() {
		return ResponseEntity.ok(merchantBrandService.getBrandList());
	}
}
