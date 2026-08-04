package site.benepay.domain.merchant.vo;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class MerchantNearbyVO {

	private Long merchantId;
	private Long brandId;
	private String categoryCode;
	private String merchantCode;
	private String merchantName;
	private String address;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private String phone;
	private Double distanceMeters;
}
