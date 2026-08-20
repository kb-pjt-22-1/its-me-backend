package site.benepay.domain.merchant.scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;

/**
 * MySQL merchants 테이블(정본)의 좌표를 Redis GEO 인덱스로 매일 새벽 복제한다.
 *
 * <p>조회 경로(findNearby/findWithinBounds)가 매번 Haversine으로 전체 테이블을 스캔하는
 * 대신, 이 배치가 미리 만들어 둔 GEO 인덱스에서 O(log N) 반경 검색을 쓰게 하는 것이 목적이다.
 *
 * <p>인스턴스가 여러 대로 늘어나도 같은 새벽 시간에 배치가 중복 실행되지 않도록 Redis
 * SET NX 락을 쓴다 - 지금은 인스턴스가 1대뿐이라 당장 충돌은 없지만, 나중에 인스턴스가
 * 늘어난 뒤에 이 문제를 알아채는 것보다 미리 넣어두는 쪽이 싸다.
 *
 * <p>서버가 뜰 때도 한 번 동기화한다({@link #syncOnStartup()}) - Redis 볼륨을 새로 띄우거나
 * 로컬 개발 환경처럼 새벽 배치를 한 번도 못 맞은 상태에서는 GEO 인덱스가 비어 있어 조회 결과가
 * 계속 비게 된다. 같은 락을 그대로 재사용하므로, 여러 인스턴스가 동시에 뜨더라도 실제 적재는
 * 한 대만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantGeoSyncScheduler {

	// 배치가 이 시간 안에 못 끝내면 락을 손 놓은 것으로 보고 다음 실행이 다시 잡을 수 있게 한다 -
	// 락을 영구히 쥐고 있으면 그 인스턴스가 죽었을 때 GEO 인덱스가 영원히 갱신되지 않는다.
	private static final Duration LOCK_TTL = Duration.ofMinutes(30);

	// GET한 값이 내가 SET한 값과 같을 때만 DEL한다 - 배치가 LOCK_TTL을 넘겨서 다른 인스턴스가
	// 이미 새 락을 잡은 뒤에, 내가 뒤늦게 끝내면서 그 새 락을 지워버리는 사고를 막는다.
	private static final RedisScript<Long> RELEASE_LOCK_IF_OWNED = RedisScript.of(
		"if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
		Long.class);

	private final MerchantMapper merchantMapper;
	private final StringRedisTemplate redisTemplate;

	// 컴포넌트 초기화 시점에 1번 실행 - 빈 생성자 주입이 끝난 뒤에 호출되도록 @PostConstruct를
	// 쓴다(생성자 안에서는 필드가 아직 다 안 채워졌을 수 있다).
	@PostConstruct
	public void syncOnStartup() {
		syncMerchantGeoIndex();
	}

	@Scheduled(cron = "${merchant.geo-sync.cron:0 0 4 * * *}", zone = "Asia/Seoul")
	public void syncMerchantGeoIndex() {
		String lockValue = UUID.randomUUID().toString();
		Boolean acquired = redisTemplate.opsForValue()
			.setIfAbsent(RedisKeys.MERCHANT_GEO_SYNC_LOCK, lockValue, LOCK_TTL);

		if (!Boolean.TRUE.equals(acquired)) {
			log.info("매장 GEO 인덱스 동기화 - 다른 인스턴스가 이미 실행 중이라 이번 실행은 건너뜀");
			return;
		}

		try {
			rebuild();
		} catch (RuntimeException e) {
			log.error("매장 GEO 인덱스 동기화 실패", e);
			throw e;
		} finally {
			redisTemplate.execute(RELEASE_LOCK_IF_OWNED, List.of(RedisKeys.MERCHANT_GEO_SYNC_LOCK), lockValue);
		}
	}

	private void rebuild() {
		List<Merchant> merchants = merchantMapper.findAll(null);
		if (merchants.isEmpty()) {
			log.warn("매장 GEO 인덱스 동기화 - merchants 테이블이 비어 있어 건너뜀");
			return;
		}

		// staging 키에 다 채운 뒤 RENAME으로 한 번에 갈아치운다 - DEL 후 재적재하면 배치 도중
		// 짧게라도 "빈 인덱스" 구간이 생겨 그 사이 요청이 빈 결과를 받는다. RENAME은 원자적이라
		// 그 틈이 없다.
		String allStagingKey = stagingKey(RedisKeys.MERCHANT_GEO_ALL);
		Map<String, String> categoryStagingKeys = new HashMap<>();
		for (Merchant merchant : merchants) {
			categoryStagingKeys.computeIfAbsent(merchant.getCategoryCode(),
				categoryCode -> stagingKey(RedisKeys.merchantGeoCategory(categoryCode)));
		}

		// 이전에 중간에 실패한 배치가 남긴 staging 키가 있으면, 그 위에 이어 쓰지 않고 지운 뒤
		// 새로 채운다 - 안 그러면 지난 배치의 잔여 좌표와 이번 배치 좌표가 섞인다.
		List<String> staleStagingKeys = new ArrayList<>(categoryStagingKeys.values());
		staleStagingKeys.add(allStagingKey);
		redisTemplate.delete(staleStagingKeys);

		GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
		for (Merchant merchant : merchants) {
			Point point = new Point(merchant.getLongitude().doubleValue(), merchant.getLatitude().doubleValue());
			String memberId = String.valueOf(merchant.getMerchantId());
			geoOps.add(allStagingKey, point, memberId);
			geoOps.add(categoryStagingKeys.get(merchant.getCategoryCode()), point, memberId);
		}

		redisTemplate.rename(allStagingKey, RedisKeys.MERCHANT_GEO_ALL);
		categoryStagingKeys.forEach((categoryCode, stagingKey) ->
			redisTemplate.rename(stagingKey, RedisKeys.merchantGeoCategory(categoryCode)));

		log.info("매장 GEO 인덱스 동기화 완료. merchantCount={}, categoryCount={}",
			merchants.size(), categoryStagingKeys.size());
	}

	private String stagingKey(String key) {
		return key + ":staging";
	}
}
