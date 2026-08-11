package site.benepay.domain.merchant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

	private final MerchantMapper merchantMapper;

	@Override
	@Transactional(readOnly = true)
	public List<MerchantResponseDto> getMerchants(String categoryCode) {
		return merchantMapper.findAll(categoryCode).stream()
			.map(MerchantResponseDto::from)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MerchantResponseDto> getMerchants(double swLat, double swLng, double neLat, double neLng,
		String categoryCode) {
		return merchantMapper.findWithinBounds(swLat, swLng, neLat, neLng, categoryCode).stream()
			.map(MerchantResponseDto::from)
			.toList();
	}
}
