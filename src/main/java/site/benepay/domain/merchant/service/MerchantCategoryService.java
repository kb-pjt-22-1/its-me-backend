package site.benepay.domain.merchant.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;

public interface MerchantCategoryService {

	List<MerchantCategoryResponseDto> getCategoryList();
}
