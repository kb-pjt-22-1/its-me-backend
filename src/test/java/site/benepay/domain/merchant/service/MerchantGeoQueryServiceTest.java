package site.benepay.domain.merchant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import site.benepay.common.util.RedisKeys;

@ExtendWith(MockitoExtension.class)
class MerchantGeoQueryServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private GeoOperations<String, String> geoOperations;

	private MerchantGeoQueryService service;

	private static GeoResults<GeoLocation<String>> geoResultsOf(long merchantId, double distanceKm) {
		GeoLocation<String> location = new GeoLocation<>(String.valueOf(merchantId), new Point(127.0, 37.5));
		GeoResult<GeoLocation<String>> result = new GeoResult<>(location, new Distance(distanceKm, Metrics.KILOMETERS));
		return new GeoResults<>(List.of(result));
	}

	private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
		double earthRadiusMeters = 6_371_000;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		service = new MerchantGeoQueryService(redisTemplate);
	}

	@Test
	void searchNearbyReadsFromTheAllMerchantsKeyWhenCategoryCodeIsNull() {
		when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
		when(geoOperations.search(eq(RedisKeys.MERCHANT_GEO_ALL), any(), any(Distance.class), any()))
			.thenReturn(geoResultsOf(1L, 0.123));

		Map<Long, Long> result = service.searchNearby(37.5, 127.0, null, 10);

		assertThat(result).containsExactly(Map.entry(1L, 123L));
	}

	@Test
	void searchNearbyReadsFromTheCategoryKeyWhenCategoryCodeIsGiven() {
		when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
		when(geoOperations.search(eq(RedisKeys.merchantGeoCategory("CE01")), any(), any(Distance.class), any()))
			.thenReturn(geoResultsOf(2L, 1.5));

		Map<Long, Long> result = service.searchNearby(37.5, 127.0, "CE01", 10);

		assertThat(result).containsExactly(Map.entry(2L, 1500L));
	}

	@Test
	void searchReturnsAnEmptyMapWhenRedisReturnsNoResults() {
		when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
		when(geoOperations.search(anyString(), any(), any(Distance.class), any())).thenReturn(null);

		Map<Long, Long> result = service.searchNearby(37.5, 127.0, null, 10);

		assertThat(result).isEmpty();
	}

	@Test
	void searchWithinBoundsApproximatesTheRectangleWithTheFarthestCornerAsRadius() {
		when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
		when(geoOperations.search(eq(RedisKeys.MERCHANT_GEO_ALL), any(), any(Distance.class), any()))
			.thenReturn(geoResultsOf(3L, 0.5));

		double swLat = 37.54;
		double swLng = 127.07;
		double neLat = 37.55;
		double neLng = 127.08;
		double centerLat = 37.545;
		double centerLng = 127.075;

		service.searchWithinBounds(swLat, swLng, neLat, neLng, centerLat, centerLng, null, 10);

		double expectedRadiusMeters = Math.max(
			Math.max(haversineMeters(centerLat, centerLng, swLat, swLng),
				haversineMeters(centerLat, centerLng, swLat, neLng)),
			Math.max(haversineMeters(centerLat, centerLng, neLat, swLng),
				haversineMeters(centerLat, centerLng, neLat, neLng)));

		ArgumentCaptor<Distance> radiusCaptor = ArgumentCaptor.forClass(Distance.class);
		verify(geoOperations).search(eq(RedisKeys.MERCHANT_GEO_ALL), any(), radiusCaptor.capture(), any());
		assertThat(radiusCaptor.getValue().getValue() * 1000).isCloseTo(expectedRadiusMeters, within(0.01));
	}
}
