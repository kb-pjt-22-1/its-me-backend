package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendedCardResponseDto {

	private String cardName;
	private String benefitSummary;
}
