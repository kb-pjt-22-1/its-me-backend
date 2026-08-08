package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

	private static final Long MERCHANT_ID = 7L;

	@Mock
	private MerchantMapper merchantMapper;

	private MerchantServiceImpl merchantService;

	@BeforeEach
	void setUp() {
		merchantService = new MerchantServiceImpl(merchantMapper);
	}

	private Merchant existingMerchant(String merchantCode) {
		return Merchant.builder()
			.merchantId(MERCHANT_ID)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode(merchantCode)
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.build();
	}

	// ---- getMerchants(categoryCode) ----

	@Test
	void getMerchantsMapsEveryMerchantToADto() {
		when(merchantMapper.findAll(null)).thenReturn(List.of(existingMerchant("M001"), existingMerchant("M002")));

		List<MerchantResponseDto> result = merchantService.getMerchants(null);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MerchantResponseDto::getMerchantCode).containsExactly("M001", "M002");
	}

	@Test
	void getMerchantsReturnsEmptyListWhenNoneExist() {
		when(merchantMapper.findAll(null)).thenReturn(List.of());

		assertThat(merchantService.getMerchants(null)).isEmpty();
	}

	@Test
	void getMerchantsPassesCategoryCodeThroughToMapper() {
		when(merchantMapper.findAll("5812")).thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result = merchantService.getMerchants("5812");

		assertThat(result).hasSize(1);
		verify(merchantMapper).findAll("5812");
	}

	// ---- getMerchants(bounds, categoryCode) ----

	@Test
	void getMerchantsWithinBoundsQueriesMapperAndMapsResult() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, null))
			.thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result = merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantCode()).isEqualTo("M001");
		verify(merchantMapper).findWithinBounds(37.4, 127.0, 37.6, 127.2, null);
	}

	@Test
	void getMerchantsWithinBoundsReturnsEmptyListWhenNoneInRange() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, null)).thenReturn(List.of());

		assertThat(merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, null)).isEmpty();
	}

	@Test
	void getMerchantsWithinBoundsPassesCategoryCodeThroughToMapper() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, "5812"))
			.thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result = merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, "5812");

		assertThat(result).hasSize(1);
		verify(merchantMapper).findWithinBounds(37.4, 127.0, 37.6, 127.2, "5812");
	}
}
