package site.benepay.domain.merchant.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
	private final MerchantGeoQueryService merchantGeoQueryService;

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
		Map<Long, Long> distanceByMerchantId = merchantGeoQueryService.searchWithinBounds(
			swLat, swLng, neLat, neLng, centerLat, centerLng, categoryCode, limit);
		return toResponseDtos(distanceByMerchantId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MerchantResponseDto> getNearbyMerchants(double lat, double lng, String categoryCode, int limit) {
		Map<Long, Long> distanceByMerchantId = merchantGeoQueryService.searchNearby(lat, lng, categoryCode, limit);
		return toResponseDtos(distanceByMerchantId);
	}

	// Redis GEO 검색 결과(merchantId → 거리, 가까운 순)에 대해 상세 정보만 MySQL에서 PK IN
	// 조회로 채워 넣는다. MySQL IN 조회는 순서를 보장하지 않으므로, Redis가 이미 정해 둔
	// 가까운 순서를 기준으로 다시 조립한다. 동기화 배치 이후 삭제된 매장은 조용히 건너뛴다.
	private List<MerchantResponseDto> toResponseDtos(Map<Long, Long> distanceByMerchantId) {
		if (distanceByMerchantId.isEmpty()) {
			return List.of();
		}
		Map<Long, Merchant> merchantsById = merchantMapper.findByIds(List.copyOf(distanceByMerchantId.keySet()))
			.stream()
			.collect(Collectors.toMap(Merchant::getMerchantId, Function.identity()));

		return distanceByMerchantId.entrySet().stream()
			.filter(entry -> merchantsById.containsKey(entry.getKey()))
			.map(entry -> MerchantResponseDto.from(merchantsById.get(entry.getKey()), entry.getValue()))
			.toList();
	}
}
