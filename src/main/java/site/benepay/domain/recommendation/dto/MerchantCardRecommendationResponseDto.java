package site.benepay.domain.recommendation.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MerchantCardRecommendationResponseDto {

	private Long merchantId;
	private String merchantName;
	private String categoryCode;
	private Long brandId;
	private List<CardBenefitComparisonResponseDto> cards;
}
