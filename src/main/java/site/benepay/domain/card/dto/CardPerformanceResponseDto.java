package site.benepay.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardPerformanceResponseDto {

	private Long userCardId;
	private Long cardId;
	private String cardName;

	private String targetYearMonth;

	private Long currentSpendingAmount;
	private Long requiredSpendingAmount;

	private Long remainingAmount;
	private Double achievementRate;
	private Boolean performanceMet;
}
