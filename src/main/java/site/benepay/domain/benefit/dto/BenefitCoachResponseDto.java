package site.benepay.domain.benefit.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BenefitCoachResponseDto {

	/**
	 * AI가 작성한 전체 코칭 요약
	 */
	private String summary;

	/**
	 * 소비 패턴별 카드 사용 코칭 결과
	 */
	private List<BenefitCoachItemDto> items;

	@Getter
	@Builder
	public static class BenefitCoachItemDto {

		private String title;
		private String message;

		/**
		 * Java 백엔드가 계산한 추천 카드 이름
		 */
		private String recommendedCardName;

		/**
		 * Java 백엔드가 계산한 예상 절약 금액
		 */
		private Long expectedSavingAmount;

		/**
		 * 카드 추천 및 혜택 적용 근거
		 */
		private String reason;

		/**
		 * 카드 사용 전략
		 */
		private String strategyType;

		/**
		 * 현재 카드 사용 후 전환할 카드
		 */
		private String nextRecommendedCardName;

		/**
		 * 현재 카드 혜택을 추가로 사용할 수 있는 횟수
		 */
		private Integer remainingUsageCount;

		/**
		 * 기존 사용 방법과 비교한 추가 절약 예상 금액
		 */
		private Long expectedAdditionalSavingAmount;
	}
}