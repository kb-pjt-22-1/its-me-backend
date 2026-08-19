package site.benepay.domain.merchant.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.merchant.vo.Merchant;

@Getter
@Builder
public class MerchantResponseDto {
	private Long merchantId;
	private String categoryCode;
	private Long brandId;
	private String merchantCode;
	private String merchantName;
	private String address;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private String phone;
	// 위치 기반 조회(findNearby)에서만 채워진다. 그 외 조회 경로에서는 null.
	// 소수점 없이 반올림된 값(SQL에서 ROUND 처리) - 응답에 "123.0m" 같은 소수점이 노출되지 않게 Long으로 받는다.
	private Long distanceMeters;

	public static MerchantResponseDto from(Merchant merchant) {
		return MerchantResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.categoryCode(merchant.getCategoryCode())
			.brandId(merchant.getBrandId())
			.merchantCode(merchant.getMerchantCode())
			.merchantName(merchant.getMerchantName())
			.address(merchant.getAddress())
			.latitude(merchant.getLatitude())
			.longitude(merchant.getLongitude())
			.phone(merchant.getPhone())
			.build();
	}
}
