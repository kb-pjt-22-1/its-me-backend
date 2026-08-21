package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.domain.geo.Metrics;

import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

	private static final Long MERCHANT_ID = 7L;

	@Mock
	private MerchantMapper merchantMapper;

	@Mock
	private MerchantGeoQueryService merchantGeoQueryService;

	private MerchantServiceImpl merchantService;

	@BeforeEach
	void setUp() {
		merchantService = new MerchantServiceImpl(merchantMapper, merchantGeoQueryService);
	}

	private Merchant existingMerchant(Long merchantId, String merchantCode) {
		return Merchant.builder()
			.merchantId(merchantId)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode(merchantCode)
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();
	}

	private Merchant existingMerchant(String merchantCode) {
		return existingMerchant(MERCHANT_ID, merchantCode);
	}

	@SuppressWarnings("unchecked")
	private GeoResults<GeoLocation<String>> geoResultsOf(long merchantId, double distanceMeters) {
		GeoLocation<String> location = new GeoLocation<>(String.valueOf(merchantId), new Point(127.0, 37.5));
		GeoResult<GeoLocation<String>> result = new GeoResult<>(location, new Distance(distanceMeters, Metrics.METERS));
		return new GeoResults<>(List.of(result));
	}

	@SuppressWarnings("unchecked")
	private GeoResults<GeoLocation<String>> emptyGeoResults() {
		return new GeoResults<>(List.of());
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

	@Test
	void getMerchantsWithinBoundsQueriesGeoServiceThenFetchesDetailsFromMapper() {
		when(merchantGeoQueryService.searchWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500))
			.thenReturn(new LinkedHashMap<>(Map.of(MERCHANT_ID, 50L)));
		when(merchantMapper.findByIds(List.of(MERCHANT_ID))).thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result =
			merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantCode()).isEqualTo("M001");
		assertThat(result.get(0).getDistanceMeters()).isEqualTo(50L);
	}

	@Test
	void getMerchantsWithinBoundsReturnsEmptyListWhenNoneInRange() {
		when(merchantGeoQueryService.searchWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500))
			.thenReturn(Map.of());

		assertThat(merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500)).isEmpty();
		verify(merchantMapper, never()).findByIds(any());
	}

	@Test
	void getMerchantsWithinBoundsSkipsIdsMissingFromMapperLookup() {
		// GEO 인덱스 동기화 이후 매장이 삭제됐다면 Redis엔 남아 있어도 MySQL 조회에선 빠진다 -
		// 그런 id는 조용히 건너뛰어야 한다(NPE 없이).
		when(merchantGeoQueryService.searchWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500))
			.thenReturn(new LinkedHashMap<>(Map.of(MERCHANT_ID, 50L)));
		when(merchantMapper.findByIds(List.of(MERCHANT_ID))).thenReturn(List.of());

		assertThat(merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, null, 500)).isEmpty();
	}

	@Test
	void getMerchantsWithinBoundsPassesCategoryCodeAndLimitThroughToGeoService() {
		when(merchantGeoQueryService.searchWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, "5812", 500))
			.thenReturn(new LinkedHashMap<>(Map.of(MERCHANT_ID, 50L)));
		when(merchantMapper.findByIds(List.of(MERCHANT_ID))).thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result =
			merchantService.getMerchants(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, "5812", 500);

		assertThat(result).hasSize(1);
		verify(merchantGeoQueryService).searchWithinBounds(37.4, 127.0, 37.6, 127.2, 37.5, 127.1, "5812", 500);
	}

	// ---- getNearbyMerchants(lat, lng, categoryCode, limit) - Redis GEO 검색 경로 ----

	@Test
	void getNearbyMerchantsMapsDistanceMetersThrough() {
		when(merchantGeoQueryService.searchNearby(37.5, 127.0, null, 20))
			.thenReturn(new LinkedHashMap<>(Map.of(MERCHANT_ID, 123L)));
		when(merchantMapper.findByIds(List.of(MERCHANT_ID))).thenReturn(List.of(existingMerchant("M001")));

		List<MerchantResponseDto> result = merchantService.getNearbyMerchants(37.5, 127.0, null, 20);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantCode()).isEqualTo("M001");
		assertThat(result.get(0).getDistanceMeters()).isEqualTo(123L);
	}

	@Test
	void getNearbyMerchantsPassesCategoryCodeAndLimitThroughToGeoService() {
		when(merchantGeoQueryService.searchNearby(37.5, 127.0, "5812", 2)).thenReturn(Map.of());

		assertThat(merchantService.getNearbyMerchants(37.5, 127.0, "5812", 2)).isEmpty();
		verify(merchantGeoQueryService).searchNearby(37.5, 127.0, "5812", 2);
	}
}
