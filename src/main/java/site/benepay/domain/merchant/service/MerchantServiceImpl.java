package site.benepay.domain.merchant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.DuplicateMerchantException;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    @Override
    @Transactional
    public MerchantResponseDto createMerchant(MerchantRequestDto request) {
        if (merchantMapper.existsByMerchantCode(request.getMerchantCode())) {
            throw new DuplicateMerchantException("이미 등록된 가맹점: " + request.getMerchantCode());
        }

        Merchant merchant = buildMerchant(null, request);
        merchantMapper.insert(merchant);

        return MerchantResponseDto.from(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponseDto getMerchant(Long merchantId) {
        return MerchantResponseDto.from(findActiveMerchant(merchantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantResponseDto> getMerchantList() {
        return merchantMapper.findAll().stream()
                .map(MerchantResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyMerchantResponseDto> getNearbyMerchants(double lat, double lng, double radiusMeters) {
        return merchantMapper.findWithinRadius(lat, lng, radiusMeters).stream()
                .map(NearbyMerchantResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MerchantResponseDto updateMerchant(Long merchantId, MerchantRequestDto request) {
        Merchant existing = findActiveMerchant(merchantId);

        boolean merchantCodeChanged = !existing.getMerchantCode().equals(request.getMerchantCode());
        if (merchantCodeChanged && merchantMapper.existsByMerchantCode(request.getMerchantCode())) {
            throw new DuplicateMerchantException("이미 등록된 가맹점: " + request.getMerchantCode());
        }

        Merchant merchant = buildMerchant(merchantId, request);
        merchantMapper.update(merchant);

        return MerchantResponseDto.from(merchant);
    }

    @Override
    @Transactional
    public void deleteMerchant(Long merchantId) {
        findActiveMerchant(merchantId);
        merchantMapper.deleteByMerchantId(merchantId);
    }

    private Merchant findActiveMerchant(Long merchantId) {
        return merchantMapper.findByMerchantId(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("가맹점을 찾을 수 없음: " + merchantId));
    }

    private Merchant buildMerchant(Long merchantId, MerchantRequestDto request) {
        return Merchant.builder()
                .merchantId(merchantId)
                .categoryCode(request.getCategoryCode())
                .brandId(request.getBrandId())
                .merchantCode(request.getMerchantCode())
                .merchantName(request.getMerchantName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phone(request.getPhone())
                .build();
    }
}
