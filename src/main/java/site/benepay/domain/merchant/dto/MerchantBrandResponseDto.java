package site.benepay.domain.merchant.dto;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.merchant.vo.MerchantBrand;

@Getter
@Builder
public class MerchantBrandResponseDto {

    private Long brandId;
    private String brandCode;
    private String brandName;
    private String brandLogo;

    public static MerchantBrandResponseDto from(MerchantBrand brand) {
        return MerchantBrandResponseDto.builder()
                .brandId(brand.getBrandId())
                .brandCode(brand.getBrandCode())
                .brandName(brand.getBrandName())
                .brandLogo(brand.getBrandLogo())
                .build();
    }
}
