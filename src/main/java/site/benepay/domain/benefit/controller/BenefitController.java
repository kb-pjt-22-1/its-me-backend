package site.benepay.domain.benefit.controller;

import java.time.Year;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.BenefitCoachResponseDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto;
import site.benepay.domain.benefit.service.BenefitService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/benefits")
public class BenefitController {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

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
			year == null ? Year.now(ZONE).getValue() : year;

		List<AnnualFeeBreakEvenResponseDto> response =
			benefitService.getAnnualFeeBreakEven(
				userId,
				targetYear
			);

		return ResponseEntity.ok(response);
	}

	/**
	 * 이번 달(또는 지정한 달) 받은 혜택 총액과 카테고리별 리포트를 조회한다.
	 * 명세서 #47(총액), #50(카테고리별 리포트)을 한 응답으로 합쳤다 -
	 * 프론트 목업(혜택.vue)에서 총액/증감/카테고리 breakdown이 같은 카드 안에 함께 표시된다.
	 */
	@GetMapping("/report")
	public ResponseEntity<MonthlyBenefitReportResponseDto> getMonthlyBenefitReport(
		@AuthenticationPrincipal Long userId,
		@RequestParam(required = false) String yearMonth
	) {
		MonthlyBenefitReportResponseDto response =
			benefitService.getMonthlyBenefitReport(userId, yearMonth);

		return ResponseEntity.ok(response);
	}

	/**
	 * 최근 3개월 결제 패턴과 보유 카드 혜택을 분석하여
	 * AI 혜택 코칭 결과를 반환한다.
	 */
	@GetMapping("/coaching")
	public ResponseEntity<BenefitCoachResponseDto> getBenefitCoaching(
		@AuthenticationPrincipal Long userId
	) {
		BenefitCoachResponseDto response =
			benefitService.getBenefitCoaching(userId);

		return ResponseEntity.ok(response);
	}
}
