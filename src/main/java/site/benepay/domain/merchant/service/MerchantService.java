package site.benepay.domain.merchant.service;

import java.util.List;

import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;

public interface MerchantService {

	List<MerchantResponseDto> getMerchants(String categoryCode);

	MerchantResponseDto getMerchant(Long merchantId);

	List<MerchantRecommendationResponseDto> getRecommendedMerchantsInBounds(
		Long userId, double swLat, double swLng, double neLat, double neLng, String categoryCode);
}
