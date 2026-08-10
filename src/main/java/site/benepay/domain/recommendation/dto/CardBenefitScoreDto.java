package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 카드 한 장에 대한 모드 1(즉시 할인) + 모드 2(실적 채우기) 평가 결과.
 */
@Getter
@Builder
public class CardBenefitScoreDto {

	private Long userCardId;
	private String cardName;
	private String cardImageUrl;

	// 모드 1 - 지금 이 결제에서 확정된 혜택.
	private String status;
	private double discountRate;
	private double nominalDiscountRate;
	private boolean capped;
	private long discountAmount;
	private long evaluatedAmount;
	private String note;

	// 모드 2 - 다음 구간까지 확률 × 기대값. 정렬 기준은 모드 1 그대로이고, 이 값들은 참고 정보다.
	private String buildStatus;
	private double reachProbability;
	private long expectedValue;
	private long gapAmount;
	private long gainAmount;
	private String buildNote;
}
