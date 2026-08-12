package site.benepay.domain.benefit.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyBenefitAmountVO {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String panLast4;
	private Long annualFee;

	/**
	 * 혜택이 발생한 결제일
	 */
	private LocalDate benefitDate;

	/**
	 * 해당 날짜에 받은 할인 혜택 합계
	 */
	private BigDecimal dailyBenefitAmount;
}
