package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.MerchantNotFoundException;
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

	// ---- getMerchant(merchantId) ----

	@Test
	void getMerchantReturnsDtoWhenFound() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));

		MerchantResponseDto result = merchantService.getMerchant(MERCHANT_ID);

		assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.getMerchantCode()).isEqualTo("M001");
	}

	@Test
	void getMerchantThrowsWhenNotFound() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> merchantService.getMerchant(MERCHANT_ID))
			.isInstanceOf(MerchantNotFoundException.class);
	}

	// ---- getMerchants(bounds, center, categoryCode, limit) ----

	private MerchantResponseDto nearbyResponse(String merchantCode) {
		return MerchantResponseDto.builder()
			.merchantId(MERCHANT_ID)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode(merchantCode)
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.distanceMeters(50.0)
			.build();
	}

	@Test
	void getMerchantsWithinBoundsQueriesMapperAndReturnsMapperResultDirectly() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500))
			.thenReturn(List.of(nearbyResponse("M001")));

		List<MerchantResponseDto> result =
			merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantCode()).isEqualTo("M001");
		assertThat(result.get(0).getDistanceMeters()).isEqualTo(50.0);
		verify(merchantMapper).findWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500);
	}

	@Test
	void getMerchantsWithinBoundsReturnsEmptyListWhenNoneInRange() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500)).thenReturn(List.of());

		assertThat(merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500)).isEmpty();
	}

	@Test
	void getMerchantsWithinBoundsPassesCategoryCodeAndLimitThroughToMapper() {
		when(merchantMapper.findWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, "5812", 500))
			.thenReturn(List.of(nearbyResponse("M001")));

		List<MerchantResponseDto> result =
			merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, "5812", 500);

		assertThat(result).hasSize(1);
		verify(merchantMapper).findWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, "5812", 500);
	}

	// ---- getNearbyMerchants(lat, lng, categoryCode, limit) ----

	@Test
	void getNearbyMerchantsMapsDistanceMetersThrough() {
		MerchantResponseDto nearby = MerchantResponseDto.builder()
			.merchantId(MERCHANT_ID)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.distanceMeters(123.4)
			.build();
		when(merchantMapper.findNearby(37.5, 127.0, null, 20)).thenReturn(List.of(nearby));

		List<MerchantResponseDto> result = merchantService.getNearbyMerchants(37.5, 127.0, null, 20);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantCode()).isEqualTo("M001");
		assertThat(result.get(0).getDistanceMeters()).isEqualTo(123.4);
	}

	@Test
	void getNearbyMerchantsPassesCategoryCodeAndLimitThroughToMapper() {
		when(merchantMapper.findNearby(37.5, 127.0, "5812", 2)).thenReturn(List.of());

		assertThat(merchantService.getNearbyMerchants(37.5, 127.0, "5812", 2)).isEmpty();
		verify(merchantMapper).findNearby(37.5, 127.0, "5812", 2);
	}
}
