package site.benepay.domain.merchant.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecommendationMerchantCandidate {

    private Long merchantId;
    private String merchantName;
    private String brandCode;
    private String brandName;
    private String categoryCode;
    private String categoryName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal distanceMeters;
    private BigDecimal rating;
    private boolean bookmarked;
}
