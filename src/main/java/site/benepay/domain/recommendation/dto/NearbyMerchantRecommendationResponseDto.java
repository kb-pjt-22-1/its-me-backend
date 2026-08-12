package site.benepay.domain.recommendation.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.merchant.dto.MerchantResponseDto;

@Getter
@Builder
public class NearbyMerchantRecommendationResponseDto {

	private Long merchantId;
	private String merchantName;
	private String categoryName;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private Double distanceMeters;

	// 지금 당장(즉시할인) 혜택을 쓸 수 있는 매장인지 - true일 때만 benefitSummary/recommendedCardName이 채워진다.
	private boolean benefitAvailable;
	private String benefitSummary;
	private String recommendedCardName;

	public static NearbyMerchantRecommendationResponseDto from(
		MerchantResponseDto merchant
	) {
		return NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.merchantName(merchant.getMerchantName())
			.latitude(merchant.getLatitude())
			.longitude(merchant.getLongitude())
			.benefitAvailable(false)
			.benefitSummary(null)
			.recommendedCardName(null)
			.build();
	}
}
