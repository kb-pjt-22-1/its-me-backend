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

	// 지금 당장(즉시할인) 혜택을 쓸 수 있는 매장인지 - true일 때만 benefitSummary/recommendedCardName이 채워진다.
	private boolean benefitAvailable;
	private String benefitSummary;
	private String recommendedCardName;
}