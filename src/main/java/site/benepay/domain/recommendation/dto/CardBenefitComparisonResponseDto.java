package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBenefitComparisonResponseDto {

	private Long userCardId;
	private String cardName;
	private String cardImageUrl;
	private String benefitDescription;
	private boolean benefitApplicable;
	private boolean performanceMet;
	private Long minimumPaymentAmount;
	private String reason;
	private boolean recommended;
}

