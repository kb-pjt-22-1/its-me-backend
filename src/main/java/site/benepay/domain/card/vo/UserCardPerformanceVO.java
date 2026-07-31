package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardPerformanceVO {
	private Long userCardId;
	private Long cardId;
	private String cardName;

	private String targetYearMonth;

	private Long currentSpendingAmount;
	private Long requiredSpendingAmount;
}
