package site.benepay.domain.merchant.service;

import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;

import java.util.List;

public interface MerchantService {

    MerchantResponseDto createMerchant(MerchantRequestDto request);

    MerchantResponseDto getMerchant(Long merchantId);

    List<MerchantResponseDto> getMerchantList();

    List<NearbyMerchantResponseDto> getNearbyMerchants(double lat, double lng, double radiusMeters);

    List<NearbyMerchantResponseDto> getMerchantsWithinBounds(double swLat, double swLng, double neLat, double neLng);

    MerchantResponseDto updateMerchant(Long merchantId, MerchantRequestDto request);

    void deleteMerchant(Long merchantId);
}
