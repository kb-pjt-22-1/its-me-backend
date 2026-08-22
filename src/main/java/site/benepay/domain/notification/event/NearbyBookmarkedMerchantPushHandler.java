package site.benepay.domain.notification.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.bookmark.mapper.BookmarkMapper;
import site.benepay.domain.bookmark.vo.Bookmark;
import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.NotificationHistoryStore;
import site.benepay.domain.notification.service.PushNotificationSender;
import site.benepay.domain.notification.vo.NotificationType;

/**
 * 유저 위치가 저장(북마크)한 매장 반경 안에 들어오면 푸시 알림을 보낸다.
 *
 * <p>거리 계산은 MerchantGeoSyncScheduler가 매일 새벽 채워두는 {@code merchants:geo:all}
 * GEO 인덱스를 그대로 재사용한다 - 여기서 새로 계산식을 만들지 않는다.
 *
 * <p>"가까워졌다"는 반경 밖 -&gt; 안으로의 전환을 의미한다. 반경 안에 계속 머무는 동안은
 * 재알림하지 않고, Redis에 "지금 가까움" 플래그를 남겨 판단한다. 이 플래그는 반경을 벗어난
 * 뒤 {@link #RENOTIFY_COOLDOWN}이 지나야 지워진다 - 경계선 근처에서 GPS 오차로 반경 안팎을
 * 오갈 때 알림이 반복 발사되는 것을 막기 위한 유예 기간이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NearbyBookmarkedMerchantPushHandler {

	private static final Distance NEARBY_RADIUS = new Distance(50, Metrics.METERS);
	private static final Duration RENOTIFY_COOLDOWN = Duration.ofHours(2);
	// 플래그가 영원히 안 지워지는 상황(유저가 다시는 근처에 안 오는 경우)에 대비한 안전망.
	// 정상적인 경우 이 TTL이 실제로 만료를 트리거하는 일은 없다 - 반경을 벗어난 게 관측되면
	// 쿨다운 경과 시 명시적으로 지운다.
	private static final Duration SAFETY_TTL = Duration.ofDays(1);

	private final BookmarkMapper bookmarkMapper;
	private final StringRedisTemplate redisTemplate;
	private final PushNotificationSender pushNotificationSender;
	private final NotificationHistoryStore notificationHistoryStore;

	@EventListener
	public void handle(UserLocationUpdatedEvent event) {
		List<Bookmark> bookmarks = bookmarkMapper.findActiveByUserId(event.userId());
		if (bookmarks.isEmpty()) {
			return;
		}

		Set<Long> nearbyMerchantIds = findNearbyMerchantIds(event, bookmarks);

		for (Bookmark bookmark : bookmarks) {
			Long merchantId = bookmark.getMerchantId();
			String flagKey = RedisKeys.nearbyMerchantFlag(event.userId(), merchantId);

			if (nearbyMerchantIds.contains(merchantId)) {
				handleWithinRadius(event.userId(), merchantId, flagKey);
			} else {
				clearFlagIfCooldownElapsed(flagKey);
			}
		}
	}

	private Set<Long> findNearbyMerchantIds(UserLocationUpdatedEvent event, List<Bookmark> bookmarks) {
		Set<String> bookmarkedMerchantIds = bookmarks.stream()
			.map(bookmark -> String.valueOf(bookmark.getMerchantId()))
			.collect(Collectors.toSet());

		GeoReference<String> reference = GeoReference.fromCoordinate(event.longitude(), event.latitude());
		GeoShape shape = GeoShape.byRadius(NEARBY_RADIUS);
		GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs();

		GeoResults<GeoLocation<String>> geoResults =
			redisTemplate.opsForGeo().search(RedisKeys.MERCHANT_GEO_ALL, reference, shape, args);

		return geoResults.getContent().stream()
			.map(GeoResult::getContent)
			.map(GeoLocation::getName)
			// 북마크가 아닌 매장까지 반경 검색에 걸릴 수 있으니, 유저의 북마크로만 좁힌다.
			.filter(bookmarkedMerchantIds::contains)
			.map(Long::valueOf)
			.collect(Collectors.toSet());
	}

	private void handleWithinRadius(Long userId, Long merchantId, String flagKey) {
		if (Boolean.TRUE.equals(redisTemplate.hasKey(flagKey))) {
			// 이미 이번 방문에서 알림을 보냈다.
			return;
		}

		String title = "저장한 매장이 근처에 있어요";
		String body = "저장해둔 매장 근처에 도착했어요. 지금 바로 확인해보세요.";

		try {
			pushNotificationSender.send(new PushNotificationMessage(
				userId,
				title,
				body,
				Map.of("merchantId", String.valueOf(merchantId))
			));
		} catch (RuntimeException e) {
			log.warn("근처 매장 푸시 알림 전송 실패. userId={}, merchantId={}", userId, merchantId, e);
		}

		try {
			notificationHistoryStore.record(userId, NotificationType.NEARBY_MERCHANT, title, body, merchantId);
		} catch (RuntimeException e) {
			// 이력 저장 실패가 푸시 전송이나 위치 이벤트 처리 결과에 영향을 주면 안 된다 - 위와 같은 격리 원칙.
			log.warn("근처 매장 알림 이력 저장 실패. userId={}, merchantId={}", userId, merchantId, e);
		}

		redisTemplate.opsForValue().set(flagKey, String.valueOf(Instant.now().getEpochSecond()), SAFETY_TTL);
	}

	private void clearFlagIfCooldownElapsed(String flagKey) {
		String flaggedAtRaw = redisTemplate.opsForValue().get(flagKey);
		if (flaggedAtRaw == null) {
			return;
		}

		Instant flaggedAt = Instant.ofEpochSecond(Long.parseLong(flaggedAtRaw));
		if (Duration.between(flaggedAt, Instant.now()).compareTo(RENOTIFY_COOLDOWN) >= 0) {
			redisTemplate.delete(flagKey);
		}
	}
}
