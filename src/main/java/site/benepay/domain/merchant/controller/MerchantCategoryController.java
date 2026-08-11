package site.benepay.domain.merchant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchant-categories")
public class MerchantCategoryController {

	private final MerchantCategoryService merchantCategoryService;

	/**
	 * 매장 카테고리 조회. 지도 필터, 매장 등록 폼에서 목록을 채울 때 사용.
	 */
	@GetMapping
	public ResponseEntity<List<MerchantCategoryResponseDto>> getCategoryList() {
		return ResponseEntity.ok(merchantCategoryService.getCategoryList());
	}
}
