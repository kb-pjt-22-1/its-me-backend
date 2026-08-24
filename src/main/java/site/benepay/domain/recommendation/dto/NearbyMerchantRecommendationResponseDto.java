package site.benepay.domain.recommendation.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.merchant.dto.MerchantResponseDto;

@Getter
@Builder
public class NearbyMerchantRecommendationResponseDto {

	private Long merchantId;
	private String categoryCode;
	private String categoryName;
	private Long brandId;
	private String merchantCode;
	private String merchantName;
	private String address;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private Long distanceMeters;
	// 지금 당장(즉시할인) 혜택을 쓸 수 있는 매장인지 - true일 때만 recommendedCards가 채워진다.
	private boolean benefitAvailable;
	// 총 기대 가치(now + beta*future) 내림차순 상위 3장까지 - benefitAvailable=false면 빈 리스트다.
	private List<RecommendedCardResponseDto> recommendedCards;
	// recommendedCards 계산에 쓰인 기준 결제액(ticket) - 프론트가 "n원 기준"으로 안내할 때 쓴다.
	// recommendedCards가 비어 있으면(benefitAvailable=false) null이다.
	private Long typicalPaymentAmount;

	public static NearbyMerchantRecommendationResponseDto from(
		MerchantResponseDto merchant
	) {
		return NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.merchantName(merchant.getMerchantName())
			.latitude(merchant.getLatitude())
			.longitude(merchant.getLongitude())
			.benefitAvailable(false)
			.recommendedCards(Collections.emptyList())
			.build();
	}
}
