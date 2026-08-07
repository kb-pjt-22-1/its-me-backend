package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.DuplicateMerchantException;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;
import site.benepay.domain.merchant.vo.MerchantNearbyVO;

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

	private MerchantRequestDto requestDto(String merchantCode) {
		return new MerchantRequestDto(
			"5812", 1L, merchantCode, "테스트 식당", "서울시 강남구",
			BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), "02-000-0000");
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

	// ---- createMerchant ----

	@Test
	void createMerchantSucceedsWhenMerchantCodeIsNotDuplicate() {
		MerchantRequestDto request = requestDto("M001");
		when(merchantMapper.existsByMerchantCode("M001")).thenReturn(false);

		MerchantResponseDto response = merchantService.createMerchant(request);

		assertThat(response.getMerchantCode()).isEqualTo("M001");
		assertThat(response.getMerchantName()).isEqualTo("테스트 식당");
		assertThat(response.getCategoryCode()).isEqualTo("5812");
		verify(merchantMapper).insert(any(Merchant.class));
	}

	@Test
	void createMerchantInsertsMerchantWithNullIdSoMapperCanFillAutoIncrement() {
		MerchantRequestDto request = requestDto("M001");
		when(merchantMapper.existsByMerchantCode("M001")).thenReturn(false);

		merchantService.createMerchant(request);

		ArgumentCaptor<Merchant> inserted = ArgumentCaptor.forClass(Merchant.class);
		verify(merchantMapper).insert(inserted.capture());

		Merchant merchant = inserted.getValue();
		assertThat(merchant.getMerchantId()).isNull();
		assertThat(merchant.getMerchantCode()).isEqualTo("M001");
		assertThat(merchant.getBrandId()).isEqualTo(1L);
		assertThat(merchant.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5));
		assertThat(merchant.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(127.0));
	}

	@Test
	void createMerchantWithDuplicateMerchantCodeThrowsAndNeverInserts() {
		MerchantRequestDto request = requestDto("M001");
		when(merchantMapper.existsByMerchantCode("M001")).thenReturn(true);

		assertThatThrownBy(() -> merchantService.createMerchant(request))
			.isInstanceOf(DuplicateMerchantException.class);

		verify(merchantMapper, never()).insert(any());
	}

	// ---- getMerchant ----

	@Test
	void getMerchantReturnsMappedDtoWhenFound() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));

		MerchantResponseDto response = merchantService.getMerchant(MERCHANT_ID);

		assertThat(response.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(response.getMerchantCode()).isEqualTo("M001");
	}

	@Test
	void getMerchantThrowsWhenNotFound() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> merchantService.getMerchant(MERCHANT_ID))
			.isInstanceOf(MerchantNotFoundException.class);
	}

	// ---- getMerchantList ----

	@Test
	void getMerchantListMapsEveryMerchantToADto() {
		when(merchantMapper.findAll()).thenReturn(List.of(existingMerchant("M001"), existingMerchant("M002")));

		List<MerchantResponseDto> result = merchantService.getMerchantList();

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MerchantResponseDto::getMerchantCode).containsExactly("M001", "M002");
	}

	@Test
	void getMerchantListReturnsEmptyListWhenNoneExist() {
		when(merchantMapper.findAll()).thenReturn(List.of());

		assertThat(merchantService.getMerchantList()).isEmpty();
	}

	// ---- getNearbyMerchants ----

	@Test
	void getNearbyMerchantsQueriesMapperAndMapsDistance() {
		MerchantNearbyVO nearby = mock(MerchantNearbyVO.class);
		when(nearby.getMerchantId()).thenReturn(MERCHANT_ID);
		when(nearby.getCategoryCode()).thenReturn("5812");
		when(nearby.getBrandId()).thenReturn(1L);
		when(nearby.getMerchantCode()).thenReturn("M001");
		when(nearby.getMerchantName()).thenReturn("테스트 식당");
		when(nearby.getAddress()).thenReturn("서울시 강남구");
		when(nearby.getLatitude()).thenReturn(BigDecimal.valueOf(37.5));
		when(nearby.getLongitude()).thenReturn(BigDecimal.valueOf(127.0));
		when(nearby.getPhone()).thenReturn("02-000-0000");
		when(nearby.getDistanceMeters()).thenReturn(120.5);
		when(merchantMapper.findWithinRadius(37.5, 127.0, 1000)).thenReturn(List.of(nearby));

		List<NearbyMerchantResponseDto> result = merchantService.getNearbyMerchants(37.5, 127.0, 1000);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.get(0).getDistanceMeters()).isEqualTo(120.5);
		verify(merchantMapper).findWithinRadius(37.5, 127.0, 1000);
	}

	// ---- getMerchantsWithinBounds ----

	@Test
	void getMerchantsWithinBoundsQueriesMapperAndMapsResults() {
		MerchantNearbyVO nearby = mock(MerchantNearbyVO.class);
		when(nearby.getMerchantId()).thenReturn(MERCHANT_ID);
		when(nearby.getCategoryCode()).thenReturn("5812");
		when(nearby.getBrandId()).thenReturn(1L);
		when(nearby.getMerchantCode()).thenReturn("M001");
		when(nearby.getMerchantName()).thenReturn("테스트 식당");
		when(nearby.getAddress()).thenReturn("서울시 강남구");
		when(nearby.getLatitude()).thenReturn(BigDecimal.valueOf(37.5));
		when(nearby.getLongitude()).thenReturn(BigDecimal.valueOf(127.0));
		when(nearby.getPhone()).thenReturn("02-000-0000");
		when(merchantMapper.findWithinBounds(37.4, 126.9, 37.6, 127.1)).thenReturn(List.of(nearby));

		List<NearbyMerchantResponseDto> result = merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.get(0).getMerchantName()).isEqualTo("테스트 식당");
		verify(merchantMapper).findWithinBounds(37.4, 126.9, 37.6, 127.1);
	}

	@Test
	void getMerchantsWithinBoundsThrowsWhenSouthwestIsNotBelowNortheast() {
		assertThatThrownBy(() -> merchantService.getMerchantsWithinBounds(37.6, 126.9, 37.4, 127.1))
			.isInstanceOf(IllegalArgumentException.class);

		verify(merchantMapper, never()).findWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble());
	}

	// ---- updateMerchant ----

	@Test
	void updateMerchantSucceedsWithoutDuplicateCheckWhenMerchantCodeIsUnchanged() {
		MerchantRequestDto request = requestDto("M001");
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));

		merchantService.updateMerchant(MERCHANT_ID, request);

		verify(merchantMapper, never()).existsByMerchantCode(any());
		verify(merchantMapper).update(any(Merchant.class));
	}

	@Test
	void updateMerchantChecksDuplicateOnlyWhenMerchantCodeChanges() {
		MerchantRequestDto request = requestDto("M002");
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));
		when(merchantMapper.existsByMerchantCode("M002")).thenReturn(false);

		MerchantResponseDto response = merchantService.updateMerchant(MERCHANT_ID, request);

		assertThat(response.getMerchantCode()).isEqualTo("M002");
		verify(merchantMapper).existsByMerchantCode("M002");
		verify(merchantMapper).update(any(Merchant.class));
	}

	@Test
	void updateMerchantWithDuplicateNewMerchantCodeThrowsAndNeverUpdates() {
		MerchantRequestDto request = requestDto("M002");
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));
		when(merchantMapper.existsByMerchantCode("M002")).thenReturn(true);

		assertThatThrownBy(() -> merchantService.updateMerchant(MERCHANT_ID, request))
			.isInstanceOf(DuplicateMerchantException.class);

		verify(merchantMapper, never()).update(any());
	}

	@Test
	void updateMerchantForUnknownMerchantThrowsAndNeverUpdates() {
		MerchantRequestDto request = requestDto("M001");
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> merchantService.updateMerchant(MERCHANT_ID, request))
			.isInstanceOf(MerchantNotFoundException.class);

		verify(merchantMapper, never()).existsByMerchantCode(any());
		verify(merchantMapper, never()).update(any());
	}

	// ---- deleteMerchant ----

	@Test
	void deleteMerchantSucceedsWhenMerchantExists() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(existingMerchant("M001")));

		merchantService.deleteMerchant(MERCHANT_ID);

		verify(merchantMapper).deleteByMerchantId(MERCHANT_ID);
	}

	@Test
	void deleteMerchantForUnknownMerchantThrowsAndNeverDeletes() {
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> merchantService.deleteMerchant(MERCHANT_ID))
			.isInstanceOf(MerchantNotFoundException.class);

		verify(merchantMapper, never()).deleteByMerchantId(any());
	}
}
