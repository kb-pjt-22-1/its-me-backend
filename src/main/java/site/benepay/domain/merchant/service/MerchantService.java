package site.benepay.domain.merchant.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantResponseDto;

public interface MerchantService {

	List<MerchantResponseDto> getMerchants(String categoryCode);

	List<MerchantResponseDto> getMerchants(double swLat, double swLng, double neLat, double neLng, String categoryCode);
}
