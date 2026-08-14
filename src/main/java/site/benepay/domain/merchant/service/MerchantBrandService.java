package site.benepay.domain.merchant.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantBrandResponseDto;

public interface MerchantBrandService {

	List<MerchantBrandResponseDto> getBrandList();
}