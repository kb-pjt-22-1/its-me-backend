package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendedCardResponseDto {

	private Long userCardId;
	private String cardName;
	private String benefitSummary;
}
