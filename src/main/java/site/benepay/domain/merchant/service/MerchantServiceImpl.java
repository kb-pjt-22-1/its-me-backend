package site.benepay.domain.merchant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.recommendation.service.RecommendationService;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

	private final MerchantMapper merchantMapper;
	private final RecommendationService recommendationService;

	@Override
	@Transactional(readOnly = true)
	public List<MerchantResponseDto> getMerchants(String categoryCode) {
		return merchantMapper.findAll(categoryCode).stream()
			.map(MerchantResponseDto::from)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public MerchantResponseDto getMerchant(Long merchantId) {
		return merchantMapper.findByMerchantId(merchantId)
			.map(MerchantResponseDto::from)
			.orElseThrow(() -> new MerchantNotFoundException("존재하지 않는 매장입니다: " + merchantId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MerchantRecommendationResponseDto> getRecommendedMerchantsInBounds(
		Long userId, double swLat, double swLng, double neLat, double neLng, String categoryCode) {

		List<MerchantRecommendationResponseDto> candidates = merchantMapper
			.findWithinBounds(swLat, swLng, neLat, neLng, categoryCode).stream()
			.map(MerchantRecommendationResponseDto::from)
			.toList();

		if (candidates.isEmpty()) {
			return candidates;
		}

		return recommendationService.markRecommendedMerchants(userId, candidates);
	}
}
