package site.benepay.domain.notification.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.NotificationNotFoundException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.notification.vo.NotificationHistoryVO;
import site.benepay.domain.notification.vo.NotificationType;

/**
 * 알림 이력의 Redis 저장소. PaymentTokenStore와 동일하게 로컬 ObjectMapper로 JSON 문자열을
 * 직접 다룬다.
 *
 * <p>정렬용 Sorted Set({@code notifications:history:{userId}}, score=생성시각 epoch초)과
 * 본문용 Hash({@code notifications:items:{userId}}, field=notificationId)를 분리했다 - Sorted
 * Set 멤버는 점수만 바꿔치기할 수 있을 뿐 값 자체를 부분 수정할 수 없어서, 읽음 처리처럼 본문
 * 일부만 바꾸는 연산에 맞지 않는다. 정렬은 Sorted Set의 id 목록으로, 본문 조회/수정은 Hash에서
 * id로 바로 하는 식으로 역할을 나눴다.
 *
 * <p>실제 보관 기간(7일)은 기록/조회 시점마다 두 키 모두 함께 정리해서 강제한다. SAFETY_TTL은
 * 그 정리 로직이 한 번도 안 도는 유저(다시는 알림도 안 받고 이 API도 안 여는 경우)를 대비한
 * 안전망일 뿐이다 - NearbyBookmarkedMerchantPushHandler.SAFETY_TTL과 동일한 패턴.
 */
@Service
public class NotificationHistoryStore {

	private static final Duration RETENTION = Duration.ofDays(7);
	private static final Duration SAFETY_TTL = Duration.ofDays(8);
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public NotificationHistoryStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void record(Long userId, NotificationType type, String title, String body, Long relatedId) {
		String notificationId = UUID.randomUUID().toString();
		LocalDateTime now = LocalDateTime.now(ZONE);
		NotificationHistoryVO history = new NotificationHistoryVO(
			notificationId, type.name(), title, body, relatedId, now.toString(), false);

		String historyKey = RedisKeys.notificationHistory(userId);
		String itemsKey = RedisKeys.notificationItems(userId);

		hashOps().put(itemsKey, notificationId, serialize(history));
		zSetOps().add(historyKey, notificationId, now.atZone(ZONE).toEpochSecond());

		pruneExpired(userId);

		redisTemplate.expire(historyKey, SAFETY_TTL);
		redisTemplate.expire(itemsKey, SAFETY_TTL);
	}

	// 최신순(최근 생성 -> 과거)으로 돌려준다.
	public List<NotificationHistoryVO> findRecent(Long userId) {
		pruneExpired(userId);

		Set<String> orderedIds = zSetOps().reverseRange(RedisKeys.notificationHistory(userId), 0, -1);
		if (orderedIds == null || orderedIds.isEmpty()) {
			return List.of();
		}

		List<String> rawItems = hashOps().multiGet(RedisKeys.notificationItems(userId), new ArrayList<>(orderedIds));

		return rawItems.stream()
			.filter(Objects::nonNull)
			.map(this::deserialize)
			.collect(Collectors.toList());
	}

	public void markRead(Long userId, String notificationId) {
		String itemsKey = RedisKeys.notificationItems(userId);
		String raw = hashOps().get(itemsKey, notificationId);
		if (raw == null) {
			throw new NotificationNotFoundException("알림을 찾을 수 없습니다.");
		}

		NotificationHistoryVO history = deserialize(raw);
		history.setRead(true);
		hashOps().put(itemsKey, notificationId, serialize(history));
	}

	// 스케줄러가 아니라 기록/조회 시점에 지연 정리(lazy cleanup)하는 방식 - MerchantGeoSyncScheduler처럼
	// 별도 배치를 새로 두지 않고, 이 저장소를 실제로 쓰는 시점에만 비용을 들인다.
	private void pruneExpired(Long userId) {
		String historyKey = RedisKeys.notificationHistory(userId);
		String itemsKey = RedisKeys.notificationItems(userId);
		double cutoffScore = LocalDateTime.now(ZONE).minus(RETENTION).atZone(ZONE).toEpochSecond();

		Set<String> expiredIds = zSetOps().rangeByScore(historyKey, 0, cutoffScore);
		if (expiredIds == null || expiredIds.isEmpty()) {
			return;
		}

		zSetOps().removeRangeByScore(historyKey, 0, cutoffScore);
		hashOps().delete(itemsKey, expiredIds.toArray());
	}

	private HashOperations<String, String, String> hashOps() {
		return redisTemplate.opsForHash();
	}

	private ZSetOperations<String, String> zSetOps() {
		return redisTemplate.opsForZSet();
	}

	private String serialize(NotificationHistoryVO history) {
		try {
			return objectMapper.writeValueAsString(history);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("알림 이력 직렬화에 실패했습니다.", e);
		}
	}

	private NotificationHistoryVO deserialize(String raw) {
		try {
			return objectMapper.readValue(raw, NotificationHistoryVO.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("알림 이력 역직렬화에 실패했습니다.", e);
		}
	}
}
