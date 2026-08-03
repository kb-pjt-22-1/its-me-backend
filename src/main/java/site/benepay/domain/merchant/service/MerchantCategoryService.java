package site.benepay.domain.merchant.service;

import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;

import java.util.List;

public interface MerchantCategoryService {

    List<MerchantCategoryResponseDto> getCategoryList();
}
