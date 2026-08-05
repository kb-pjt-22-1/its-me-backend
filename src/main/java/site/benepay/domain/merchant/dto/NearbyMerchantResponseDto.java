package site.benepay.domain.merchant.dto;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.merchant.vo.MerchantNearbyVO;

import java.math.BigDecimal;

@Getter
@Builder
public class NearbyMerchantResponseDto {

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

    public static NearbyMerchantResponseDto from(MerchantNearbyVO merchant) {
        return NearbyMerchantResponseDto.builder()
                .merchantId(merchant.getMerchantId())
                .categoryCode(merchant.getCategoryCode())
                .brandId(merchant.getBrandId())
                .merchantCode(merchant.getMerchantCode())
                .merchantName(merchant.getMerchantName())
                .address(merchant.getAddress())
                .latitude(merchant.getLatitude())
                .longitude(merchant.getLongitude())
                .phone(merchant.getPhone())
                .distanceMeters(merchant.getDistanceMeters())
                .build();
    }
}
