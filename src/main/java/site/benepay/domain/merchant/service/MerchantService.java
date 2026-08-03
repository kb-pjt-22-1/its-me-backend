package site.benepay.domain.merchant.service;

import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;

import java.util.List;

public interface MerchantService {

    MerchantResponseDto createMerchant(MerchantRequestDto request);

    MerchantResponseDto getMerchant(Long merchantId);

    List<MerchantResponseDto> getMerchantList();

    MerchantResponseDto updateMerchant(Long merchantId, MerchantRequestDto request);

    void deleteMerchant(Long merchantId);
}
