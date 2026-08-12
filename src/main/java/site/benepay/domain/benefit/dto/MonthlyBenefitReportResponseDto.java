package site.benepay.domain.benefit.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyBenefitReportResponseDto {

	/**
	 * 조회 기준 연월. 예: 2026-08
	 */
	private String yearMonth;

	/**
	 * 이번 달 받은 혜택 총액
	 */
	private Long totalBenefitAmount;

	/**
	 * 지난달 대비 증감액. 음수일 수 있음
	 */
	private Long deltaVsLastMonth;

	/**
	 * 카테고리별 혜택 내역, 금액 내림차순
	 */
	private List<CategoryBenefitDto> categoryBreakdown;

	@Getter
	@Builder
	public static class CategoryBenefitDto {

		private String categoryCode;
		private String categoryName;
		private Long amount;

		/**
		 * 전체 대비 비중(%), 반올림. 이번 달 혜택이 없으면 0
		 */
		private Integer percent;
	}
}
