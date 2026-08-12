package site.benepay.domain.benefit.controller;

import java.time.Year;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.service.BenefitService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/benefits")
public class BenefitController {

	private final BenefitService benefitService;

	/**
	 * 혜택 탭에서 사용할 카드별 연회비 본전 정보를 조회한다.
	 *
	 * 보유 카드 전체를 한 번에 반환하고,
	 * 프론트에서는 응답 배열을 슬라이드 형태로 표시한다.
	 */
	@GetMapping("/annual-fee-break-even")
	public ResponseEntity<List<AnnualFeeBreakEvenResponseDto>>
	getAnnualFeeBreakEven(
		@AuthenticationPrincipal Long userId,
		@RequestParam(required = false) Integer year
	) {
		int targetYear =
			year == null
				? Year.now().getValue()
				: year;

		List<AnnualFeeBreakEvenResponseDto> response =
			benefitService.getAnnualFeeBreakEven(
				userId,
				targetYear
			);

		return ResponseEntity.ok(response);
	}
}
