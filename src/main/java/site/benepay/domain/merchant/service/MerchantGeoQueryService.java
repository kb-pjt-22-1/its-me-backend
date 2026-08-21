package site.benepay.domain.merchant.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.common.util.RedisKeys;

/**
 * MerchantGeoSyncScheduler가 채워 둔 Redis GEO 인덱스에서 반경 검색으로 매장 후보를 찾는다.
 *
 * <p>MerchantMapper.findNearby/findWithinBounds가 매번 전체 테이블에 Haversine을 계산해
 * 정렬하는 대신, 여기서는 Redis 정렬 셋의 O(log N) 반경 검색으로 "가까운 순 상위 limit개
 * merchantId"만 빠르게 뽑는다. 실제 매장 상세 정보는 이 결과로 추려낸 ID만 MySQL에 PK
 * IN 조회하면 되므로(MerchantMapper.findByIds), MySQL이 매 요청마다 전체 스캔할 필요가 없다.
 */
@Component
@RequiredArgsConstructor
public class MerchantGeoQueryService {

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	// "가장 가까운 limit개"처럼 반경 제한이 없는 검색용 - 지구 반대편 매장까지도 후보에 넣을 수
	// 있을 만큼 넉넉한 반경(지구 둘레의 절반보다 조금 크게)을 준다. GEOSEARCH는 도형(반경/사각형)
	// 없이는 검색이 안 되므로 반경 제한이 "없다"는 걸 이렇게 표현한다.
	private static final double UNBOUNDED_SEARCH_RADIUS_METERS = 20_040_000;

	private final StringRedisTemplate redisTemplate;

	/**
	 * bounds(SW/NE 사각형)를 감싸는 원으로 근사해서 검색한다. centerLat/centerLng가 사각형의
	 * 정확한 중심이 아닐 수 있어(호출부가 지도 화면 중심을 그대로 넘김) 사각형과 완전히 같은
	 * 도형은 못 만들지만, 지도 화면 마커 조회는 대각선 모서리 근처의 약간의 오차를 허용해도
	 * 되는 용도라 반경 검색으로 충분하다.
	 * @return merchantId → 거리(m), Redis가 반환한 순서(가까운 순) 그대로 보존
	 */
	public Map<Long, Long> searchWithinBounds(double swLat, double swLng, double neLat, double neLng,
		double centerLat, double centerLng, String categoryCode, int limit) {
		double radiusMeters = Math.max(
			Math.max(haversineMeters(centerLat, centerLng, swLat, swLng),
				haversineMeters(centerLat, centerLng, swLat, neLng)),
			Math.max(haversineMeters(centerLat, centerLng, neLat, swLng),
				haversineMeters(centerLat, centerLng, neLat, neLng)));
		return search(centerLat, centerLng, radiusMeters, categoryCode, limit);
	}

	/**
	 * 기준 좌표에서 가까운 순 상위 limit개를 반경 제한 없이 찾는다.
	 * @return merchantId → 거리(m), Redis가 반환한 순서(가까운 순) 그대로 보존
	 */
	public Map<Long, Long> searchNearby(double lat, double lng, String categoryCode, int limit) {
		return search(lat, lng, UNBOUNDED_SEARCH_RADIUS_METERS, categoryCode, limit);
	}

	private Map<Long, Long> search(double lat, double lng, double radiusMeters, String categoryCode, int limit) {
		String key = categoryCode == null ? RedisKeys.MERCHANT_GEO_ALL : RedisKeys.merchantGeoCategory(categoryCode);
		GeoReference<String> reference = GeoReference.fromCoordinate(new Point(lng, lat));
		// Metrics에는 METERS가 없어(KILOMETERS/MILES/NEUTRAL뿐) km로 바꿔서 넘긴다.
		Distance radius = new Distance(radiusMeters / 1000, Metrics.KILOMETERS);
		GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
			.includeDistance()
			.sortAscending()
			.limit(limit);

		GeoResults<GeoLocation<String>> results = redisTemplate.opsForGeo().search(key, reference, radius, args);

		Map<Long, Long> distanceByMerchantId = new LinkedHashMap<>();
		if (results == null) {
			return distanceByMerchantId;
		}
		for (GeoResult<GeoLocation<String>> result : results) {
			Long merchantId = Long.valueOf(result.getContent().getName());
			// 검색을 km 단위로 요청했으니(Metrics.KILOMETERS) 반환되는 거리도 km라 m로 되돌린다.
			distanceByMerchantId.put(merchantId, Math.round(result.getDistance().getValue() * 1000));
		}
		return distanceByMerchantId;
	}

	private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}
}
