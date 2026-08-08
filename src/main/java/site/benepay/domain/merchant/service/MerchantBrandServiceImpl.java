package site.benepay.domain.merchant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantBrandResponseDto;
import site.benepay.domain.merchant.mapper.MerchantBrandMapper;

@Service
@RequiredArgsConstructor
public class MerchantBrandServiceImpl implements MerchantBrandService {

	private final MerchantBrandMapper merchantBrandMapper;

	@Override
	@Transactional(readOnly = true)
	public List<MerchantBrandResponseDto> getBrandList() {
		return merchantBrandMapper.findAll().stream()
			.map(MerchantBrandResponseDto::from)
			.toList();
	}
}
