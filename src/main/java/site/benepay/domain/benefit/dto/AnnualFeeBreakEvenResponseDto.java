package site.benepay.domain.benefit.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnualFeeBreakEvenResponseDto {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String panLast4;

	/**
	 * 조회 기준 연도
	 */
	private Integer baseYear;

	/**
	 * 카드 연회비
	 */
	private Long annualFee;

	/**
	 * 조회 연도에 받은 누적 혜택 금액
	 */
	private Long accumulatedBenefit;

	/**
	 * 누적 혜택에서 연회비를 차감한 금액
	 */
	private Long netBenefit;

	/**
	 * 연회비 본전까지 남은 금액
	 */
	private Long remainingAmount;

	/**
	 * 연회비 본전 달성 여부
	 */
	private Boolean breakEvenAchieved;

	/**
	 * 누적 혜택이 처음으로 연회비 이상이 된 날짜
	 */
	private LocalDate breakEvenDate;

	/**
	 * 그래프에 사용할 월별 혜택 정보
	 */
	private List<MonthlyBenefitDto> monthlyBenefits;

	@Getter
	@Builder
	public static class MonthlyBenefitDto {

		/**
		 * 혜택 발생 연월
		 * 예: 2026-08
		 */
		private String yearMonth;

		/**
		 * 해당 월에 받은 혜택 금액
		 */
		private Long monthlyBenefitAmount;

		/**
		 * 1월부터 해당 월까지 받은 누적 혜택 금액
		 */
		private Long accumulatedBenefitAmount;
	}
}
