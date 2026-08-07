package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 카드 한 장에 대한 모드 1(즉시 할인) 평가 결과.
 */
@Getter
@Builder
public class CardBenefitScoreDto {

	private Long userCardId;
	private String cardName;
	private String cardImageUrl;
	private String status;
	private double discountRate;
	private double nominalDiscountRate;
	private boolean capped;
	private long discountAmount;
	private long evaluatedAmount;
	private String note;
}
