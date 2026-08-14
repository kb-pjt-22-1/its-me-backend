package site.benepay.domain.merchant.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/** 위치 기반 최근접 매장 조회(findNearby) 결과 - Merchant 필드 + 계산된 실거리(distanceMeters, m). */
@Getter
@Builder
public class NearbyMerchantVO {
	private Long merchantId;
	private String categoryCode;
	private Long brandId;
	private String merchantCode;
	private String merchantName;
	private String address;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private String phone;
	private Double distanceMeters;
}
