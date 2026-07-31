package site.benepay.domain.card.vo;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardMonthlyStatusVO {

	private Long cardMonthlyStatusId;
	private Long userCardId;
	private String targetYearMonth;
	private Long totalSpendingAmount;
	private LocalDateTime updatedAt;
}
