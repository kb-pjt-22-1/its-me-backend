package site.benepay.domain.merchant.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.BoundingBox;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

	// GEOSEARCH는 BYRADIUS/BYBOX 중 하나를 반드시 지정해야 해서 "반경 제한 없이 가장 가까운
	// N개"를 표현할 방법이 없다. 대신 지구 반대편까지도 다 들어오는 큰 반경을 줘서 사실상
	// 무제한처럼 동작하게 한다(Redis GEO가 지원하는 최대 반경이 대략 이 수준이다).
	private static final double EFFECTIVELY_UNBOUNDED_RADIUS_METERS = 20_000_000;

	// 위도 1도의 대략적인 거리(m). 경도 1도 거리는 위도에 따라 달라지므로 cos(centerLat)를
	// 곱해서 보정한다 - bounds 폭/높이를 GEOSEARCH BYBOX가 요구하는 미터 단위로 바꾸는 용도라
	// 지도 화면 스케일에서는 이 정도 근사로 충분하다(예전 Haversine 방식도 구면 근사였다).
	private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

	private final MerchantMapper merchantMapper;
	private final StringRedisTemplate redisTemplate;

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
	public List<MerchantResponseDto> getMerchants(double swLat, double swLng, double neLat, double neLng,
		double centerLat, double centerLng, String categoryCode, int limit) {

		// centerLat/centerLng가 bounds의 정중앙이라고 가정한다 - 지도 SDK의 뷰포트 중심이
		// 보통 그 값이다. BYBOX는 FROMLONLAT 지점을 중심으로 폭/높이만큼 대칭으로 뻗는
		// 사각형이라, 이 가정이 깨지면(중심이 한쪽으로 치우치면) 원래의 sw~ne 사각형과
		// 완전히 일치하지는 않는다 - 다만 "중심에서 가까운 순 정렬 + limit" 의미 자체는 그대로다.
		double heightMeters = Math.abs(neLat - swLat) * METERS_PER_DEGREE_LATITUDE;
		double widthMeters = Math.abs(neLng - swLng) * METERS_PER_DEGREE_LATITUDE
			* Math.cos(Math.toRadians(centerLat));

		GeoReference<String> reference = GeoReference.fromCoordinate(centerLng, centerLat);
		GeoShape shape = GeoShape.byBox(new BoundingBox(
			new Distance(widthMeters, Metrics.METERS),
			new Distance(heightMeters, Metrics.METERS)));

		return searchGeoIndex(reference, shape, categoryCode, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MerchantResponseDto> getNearbyMerchants(double lat, double lng, String categoryCode, int limit) {
		GeoReference<String> reference = GeoReference.fromCoordinate(lng, lat);
		GeoShape shape = GeoShape.byRadius(new Distance(EFFECTIVELY_UNBOUNDED_RADIUS_METERS, Metrics.METERS));

		return searchGeoIndex(reference, shape, categoryCode, limit);
	}

	/**
	 * Redis GEO 인덱스(MerchantGeoSyncScheduler가 매일 새벽 MySQL에서 재적재)에서 좌표순
	 * merchant_id + 거리를 뽑은 뒤, 그 PK로 MySQL에서 나머지 컬럼을 채워 응답 DTO로 조립한다.
	 * MySQL은 IN절 순서를 보장하지 않으므로 Redis가 준 순서로 다시 맞춘다.
	 */
	private List<MerchantResponseDto> searchGeoIndex(GeoReference<String> reference, GeoShape shape,
		String categoryCode, int limit) {

		String geoKey = categoryCode == null ? RedisKeys.MERCHANT_GEO_ALL : RedisKeys.merchantGeoCategory(categoryCode);
		GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
			.includeDistance()
			.sortAscending()
			.limit(limit);

		GeoResults<GeoLocation<String>> geoResults = redisTemplate.opsForGeo().search(geoKey, reference, shape, args);
		List<GeoResult<GeoLocation<String>>> content = geoResults.getContent();
		if (content.isEmpty()) {
			return List.of();
		}

		List<Long> orderedIds = new ArrayList<>(content.size());
		Map<Long, Long> distanceMetersByMerchantId = new LinkedHashMap<>();
		for (GeoResult<GeoLocation<String>> result : content) {
			Long merchantId = Long.valueOf(result.getContent().getName());
			long distanceMeters = Math.round(result.getDistance().in(Metrics.METERS).getValue());
			orderedIds.add(merchantId);
			distanceMetersByMerchantId.put(merchantId, distanceMeters);
		}

		Map<Long, Merchant> merchantsById = merchantMapper.findByIds(orderedIds).stream()
			.collect(Collectors.toMap(Merchant::getMerchantId, merchant -> merchant));

		return orderedIds.stream()
			.map(merchantsById::get)
			// 새벽 배치 이후 매장이 삭제되는 등, Redis 인덱스와 MySQL이 아주 잠깐 어긋나 있는
			// 사이에 조회가 들어오면 merchantsById에 없는 id가 섞일 수 있다 - 조용히 건너뛴다.
			.filter(Objects::nonNull)
			.map(merchant -> MerchantResponseDto.from(merchant, distanceMetersByMerchantId.get(merchant.getMerchantId())))
			.toList();
	}
}
