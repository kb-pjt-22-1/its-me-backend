package site.benepay.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import site.benepay.common.exception.NotificationNotFoundException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.notification.vo.NotificationHistoryVO;
import site.benepay.domain.notification.vo.NotificationType;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryStoreTest {

	private static final Long USER_ID = 1L;
	private static final String HISTORY_KEY = RedisKeys.notificationHistory(USER_ID);
	private static final String ITEMS_KEY = RedisKeys.notificationItems(USER_ID);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private HashOperations<String, String, String> hashOperations;

	@Mock
	private ZSetOperations<String, String> zSetOperations;

	private NotificationHistoryStore store;

	@BeforeEach
	void setUp() {
		// 테스트마다 hash/zset 둘 다 쓰는 건 아니라(예: markRead는 zset을 안 건드림) lenient로 둔다.
		lenient().when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
		lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		store = new NotificationHistoryStore(redisTemplate);
	}

	@Test
	void recordSavesTheItemUnderARandomIdAndRefreshesTheSafetyTtl() {
		when(zSetOperations.rangeByScore(eq(HISTORY_KEY), anyDouble(), anyDouble())).thenReturn(Set.of());

		store.record(USER_ID, NotificationType.PAYMENT_APPROVED, "결제가 완료됐어요",
			"스타벅스 강남점에서 15,000원 결제했어요.", 100L);

		ArgumentCaptor<String> hashIdCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(hashOperations).put(eq(ITEMS_KEY), hashIdCaptor.capture(), jsonCaptor.capture());
		assertThat(jsonCaptor.getValue()).contains("결제가 완료됐어요", "스타벅스 강남점에서 15,000원 결제했어요.",
			"\"relatedId\":100", "\"read\":false", "PAYMENT_APPROVED");

		ArgumentCaptor<String> zSetIdCaptor = ArgumentCaptor.forClass(String.class);
		verify(zSetOperations).add(eq(HISTORY_KEY), zSetIdCaptor.capture(), anyDouble());
		assertThat(zSetIdCaptor.getValue()).isEqualTo(hashIdCaptor.getValue());

		verify(redisTemplate).expire(HISTORY_KEY, Duration.ofDays(8));
		verify(redisTemplate).expire(ITEMS_KEY, Duration.ofDays(8));
	}

	@Test
	void recordPrunesEntriesOlderThanTheRetentionWindow() {
		when(zSetOperations.rangeByScore(eq(HISTORY_KEY), anyDouble(), anyDouble())).thenReturn(Set.of("old-id"));

		store.record(USER_ID, NotificationType.NEARBY_MERCHANT, "제목", "본문", 1L);

		verify(zSetOperations).removeRangeByScore(eq(HISTORY_KEY), anyDouble(), anyDouble());
		verify(hashOperations).delete(ITEMS_KEY, "old-id");
	}

	@Test
	void findRecentReturnsHistoryInTheSortedSetsOrder() {
		when(zSetOperations.rangeByScore(eq(HISTORY_KEY), anyDouble(), anyDouble())).thenReturn(Set.of());
		Set<String> orderedIds = new LinkedHashSet<>(List.of("id-2", "id-1"));
		when(zSetOperations.reverseRange(HISTORY_KEY, 0, -1)).thenReturn(orderedIds);
		when(hashOperations.multiGet(ITEMS_KEY, List.of("id-2", "id-1"))).thenReturn(List.of(
			json("id-2", 200L),
			json("id-1", 100L)
		));

		List<NotificationHistoryVO> result = store.findRecent(USER_ID);

		assertThat(result).extracting(NotificationHistoryVO::getNotificationId).containsExactly("id-2", "id-1");
	}

	@Test
	void findRecentReturnsAnEmptyListWhenThereIsNoHistory() {
		when(zSetOperations.rangeByScore(eq(HISTORY_KEY), anyDouble(), anyDouble())).thenReturn(Set.of());
		when(zSetOperations.reverseRange(HISTORY_KEY, 0, -1)).thenReturn(Set.of());

		List<NotificationHistoryVO> result = store.findRecent(USER_ID);

		assertThat(result).isEmpty();
		verify(hashOperations, never()).multiGet(anyString(), org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void markReadFlipsTheStoredItemsReadFlag() {
		when(hashOperations.get(ITEMS_KEY, "id-1")).thenReturn(json("id-1", 100L));

		store.markRead(USER_ID, "id-1");

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(hashOperations).put(eq(ITEMS_KEY), eq("id-1"), jsonCaptor.capture());
		assertThat(jsonCaptor.getValue()).contains("\"read\":true");
	}

	@Test
	void markReadThrowsWhenTheNotificationDoesNotExist() {
		when(hashOperations.get(ITEMS_KEY, "unknown")).thenReturn(null);

		assertThatThrownBy(() -> store.markRead(USER_ID, "unknown"))
			.isInstanceOf(NotificationNotFoundException.class);
	}

	private String json(String notificationId, Long relatedId) {
		return "{\"notificationId\":\"" + notificationId + "\",\"type\":\"PAYMENT_APPROVED\","
			+ "\"title\":\"결제가 완료됐어요\",\"body\":\"본문\",\"relatedId\":" + relatedId + ","
			+ "\"createdAt\":\"2026-08-21T10:00:00\",\"read\":false}";
	}
}
