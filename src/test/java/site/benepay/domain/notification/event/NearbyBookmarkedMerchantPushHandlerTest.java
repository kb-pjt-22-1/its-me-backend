package site.benepay.domain.notification.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.domain.geo.Metrics;

import site.benepay.common.util.RedisKeys;
import site.benepay.domain.bookmark.mapper.BookmarkMapper;
import site.benepay.domain.bookmark.vo.Bookmark;
import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.PushNotificationSender;

@ExtendWith(MockitoExtension.class)
class NearbyBookmarkedMerchantPushHandlerTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 7L;
	private static final double LAT = 37.5;
	private static final double LNG = 127.0;

	@Mock
	private BookmarkMapper bookmarkMapper;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private GeoOperations<String, String> geoOperations;

	@Mock
	private PushNotificationSender pushNotificationSender;

	private NearbyBookmarkedMerchantPushHandler handler;

	@BeforeEach
	void setUp() {
		handler = new NearbyBookmarkedMerchantPushHandler(bookmarkMapper, redisTemplate, pushNotificationSender);
	}

	private Bookmark bookmark(Long merchantId) {
		return Bookmark.builder().bookmarkId(1L).userId(USER_ID).merchantId(merchantId).build();
	}

	@SuppressWarnings("unchecked")
	private GeoResults<GeoLocation<String>> geoResultsWithin(Long... merchantIds) {
		List<GeoResult<GeoLocation<String>>> results = List.of(merchantIds).stream()
			.map(id -> new GeoResult<>(new GeoLocation<>(String.valueOf(id), new Point(LNG, LAT)),
				new Distance(10, Metrics.METERS)))
			.toList();
		return new GeoResults<>((List) results);
	}

	@SuppressWarnings("unchecked")
	private GeoResults<GeoLocation<String>> emptyGeoResults() {
		return new GeoResults<>(List.of());
	}

	private void stubGeoSearch(GeoResults<GeoLocation<String>> result) {
		when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
		when(geoOperations.search(eq(RedisKeys.MERCHANT_GEO_ALL), any(GeoReference.class), any(GeoShape.class),
			any(GeoSearchCommandArgs.class))).thenReturn(result);
	}

	@Test
	void doesNothingWhenUserHasNoBookmarks() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of());

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verifyNoInteractions(redisTemplate, pushNotificationSender);
	}

	@Test
	void sendsPushAndSetsFlagOnFirstArrivalWithinRadius() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(pushNotificationSender).send(any(PushNotificationMessage.class));
		verify(valueOperations).set(eq(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID)), anyString(),
			eq(Duration.ofDays(1)));
	}

	@Test
	void doesNotResendWhenAlreadyFlaggedAsNearby() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(true);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(pushNotificationSender, never()).send(any());
		verify(redisTemplate, never()).opsForValue();
	}

	@Test
	void doesNothingWhenOutsideRadiusAndNoFlagExists() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(emptyGeoResults());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(null);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(pushNotificationSender, never()).send(any());
		verify(redisTemplate, never()).delete(anyString());
	}

	@Test
	void keepsFlagWhenOutsideRadiusButCooldownHasNotElapsed() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(emptyGeoResults());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		String recentFlag = String.valueOf(Instant.now().minus(Duration.ofMinutes(30)).getEpochSecond());
		when(valueOperations.get(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(recentFlag);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(redisTemplate, never()).delete(anyString());
	}

	@Test
	void clearsFlagWhenOutsideRadiusAndCooldownHasElapsed() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(emptyGeoResults());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		String staleFlag = String.valueOf(Instant.now().minus(Duration.ofHours(3)).getEpochSecond());
		when(valueOperations.get(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(staleFlag);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(redisTemplate).delete(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID));
	}

	@Test
	void ignoresNearbyMerchantsThatAreNotBookmarked() {
		Long otherMerchantId = 999L;
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		// GEO 검색 결과엔 북마크하지 않은 매장도 섞여 온다 - 북마크한 것만 걸러야 한다.
		stubGeoSearch(geoResultsWithin(MERCHANT_ID, otherMerchantId));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(pushNotificationSender, times(1)).send(any());
		verify(redisTemplate, never()).hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, otherMerchantId));
	}

	@Test
	void stillSetsFlagAndKeepsProcessingWhenSendFails() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		doThrow(new IllegalStateException("FCM 다운")).when(pushNotificationSender).send(any());

		assertThatCode(() -> handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG)))
			.doesNotThrowAnyException();

		verify(valueOperations).set(eq(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID)), anyString(),
			eq(Duration.ofDays(1)));
	}
}
