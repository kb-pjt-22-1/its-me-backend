package site.benepay.domain.merchant.service;

import site.benepay.domain.merchant.dto.MerchantBrandResponseDto;

import java.util.List;

public interface MerchantBrandService {

    List<MerchantBrandResponseDto> getBrandList();
}
