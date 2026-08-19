package site.benepay.domain.benefit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class BenefitCoachDataDto {

	private BenefitCoachDataDto() {
	}

	/**
	 * 최근 3개월 결제 내역 조회 결과
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PaymentData {

		private Long paymentId;
		private Long userCardId;
		private String categoryCode;
		private String categoryName;
		private String merchantName;
		private Long brandId;
		private BigDecimal originalAmount;
		private LocalDateTime paymentTime;
	}

	/**
	 * 혜택 코칭의 평가 대상이 되는 사용자 보유 카드
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class CardData {

		private Long userCardId;
		private Long cardId;
		private String cardName;
		private String benefitsInfo;

		/**
		 * 전월 카드 사용 실적
		 */
		private Long previousMonthSpendingAmount;
	}

	/**
	 * 이번 달 카테고리별로 이미 사용한 혜택 정보
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class MonthlyUsageData {

		private Long userCardId;
		private String categoryCode;
		private BigDecimal usedBenefitAmount;
		private Integer usageCount;
	}

	/**
	 * 최근 3개월 결제를 카테고리와 요일 기준으로 집계한 소비 패턴
	 */
	@Getter
	@Builder
	public static class SpendingPatternData {

		private String categoryCode;
		private String categoryName;
		private String usualDayOfWeek;
		private BigDecimal averageAmount;
		private Integer paymentCount;
		private String usualMerchantName;
		private Long usualUserCardId;
	}

	/**
	 * Java 백엔드가 카드 혜택을 계산한 결과
	 *
	 * OpenAI는 이 값을 변경하거나 다시 계산하지 않고
	 * 사용자용 문장으로만 변환한다.
	 */
	@Getter
	@Builder
	public static class CalculatedCoachingData {

		private String categoryCode;
		private String categoryName;
		private String usualDayOfWeek;
		private BigDecimal averageAmount;
		private Integer paymentCount;

		private Long userCardId;
		private String recommendedCardName;
		private Long expectedSavingAmount;
		private Long previousMonthSpendingAmount;
		private String appliedBenefitCondition;
		private String reason;
		private String usualMerchantName;
		private String strategyType;
		private String nextRecommendedCardName;
		private Integer remainingUsageCount;
		private Long expectedAdditionalSavingAmount;
	}
}