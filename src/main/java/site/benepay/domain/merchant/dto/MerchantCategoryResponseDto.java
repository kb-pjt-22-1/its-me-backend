package site.benepay.domain.merchant.dto;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.merchant.vo.MerchantCategory;

@Getter
@Builder
public class MerchantCategoryResponseDto {

    private String categoryCode;
    private String categoryName;
    private String categoryIcon;

    public static MerchantCategoryResponseDto from(MerchantCategory category) {
        return MerchantCategoryResponseDto.builder()
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .categoryIcon(category.getCategoryIcon())
                .build();
    }
}
