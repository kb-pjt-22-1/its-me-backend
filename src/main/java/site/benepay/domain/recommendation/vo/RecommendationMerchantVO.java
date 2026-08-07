package site.benepay.domain.recommendation.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationMerchantVO {

	private Long merchantId;
	private String merchantName;
	private String categoryCode;
	private Long brandId;
}