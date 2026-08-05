package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.merchant.dto.MerchantBrandResponseDto;
import site.benepay.domain.merchant.mapper.MerchantBrandMapper;
import site.benepay.domain.merchant.vo.MerchantBrand;

@ExtendWith(MockitoExtension.class)
class MerchantBrandServiceImplTest {

	@Mock
	private MerchantBrandMapper merchantBrandMapper;
	private MerchantBrandServiceImpl merchantBrandService;

	@BeforeEach
	void setUp() {
		merchantBrandService = new MerchantBrandServiceImpl(merchantBrandMapper);
	}

	@Test
	void getBrandListMapsEveryBrandToADto() {
		when(merchantBrandMapper.findAll()).thenReturn(List.of(
			MerchantBrand.builder().brandId(1L).brandCode("B001").brandName("브랜드A").brandLogo("logo-a").build(),
			MerchantBrand.builder().brandId(2L).brandCode("B002").brandName("브랜드B").brandLogo("logo-b").build()));

		List<MerchantBrandResponseDto> result = merchantBrandService.getBrandList();

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MerchantBrandResponseDto::getBrandCode).containsExactly("B001", "B002");
		assertThat(result).extracting(MerchantBrandResponseDto::getBrandName).containsExactly("브랜드A", "브랜드B");
	}

	@Test
	void getBrandListReturnsEmptyListWhenNoneExist() {
		when(merchantBrandMapper.findAll()).thenReturn(List.of());

		assertThat(merchantBrandService.getBrandList()).isEmpty();
	}
}
