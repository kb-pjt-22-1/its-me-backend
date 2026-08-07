package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NearbyMerchantRecommendationResponseDto {

	private Long merchantId;
	private String merchantName;
	private String categoryName;
	private Double latitude;
	private Double longitude;
	private Double distanceMeters;
	private String benefitSummary;
	private String recommendedCardName;
}