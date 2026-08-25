package site.benepay.domain.notification.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;
import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.NotificationHistoryStore;
import site.benepay.domain.notification.service.PushNotificationSender;
import site.benepay.domain.notification.vo.NotificationType;

@ExtendWith(MockitoExtension.class)
class NearbyBookmarkedMerchantPushHandlerTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 7L;
	private static final double LAT = 37.5;
	private static final double LNG = 127.0;

	@Mock
	private BookmarkMapper bookmarkMapper;

	@Mock
	private MerchantMapper merchantMapper;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private GeoOperations<String, String> geoOperations;

	@Mock
	private PushNotificationSender pushNotificationSender;

	@Mock
	private NotificationHistoryStore notificationHistoryStore;

	private NearbyBookmarkedMerchantPushHandler handler;

	@BeforeEach
	void setUp() {
		handler = new NearbyBookmarkedMerchantPushHandler(
			bookmarkMapper, merchantMapper, redisTemplate, pushNotificationSender, notificationHistoryStore);
	}

	private Bookmark bookmark(Long merchantId) {
		return Bookmark.builder().bookmarkId(1L).userId(USER_ID).merchantId(merchantId).build();
	}

	private Merchant merchant(Long merchantId, String merchantName) {
		return Merchant.builder()
			.merchantId(merchantId)
			.categoryCode("5311")
			.brandId(1L)
			.merchantCode("M-" + merchantId)
			.merchantName(merchantName)
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(LAT))
			.longitude(BigDecimal.valueOf(LNG))
			.build();
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

		verifyNoInteractions(redisTemplate, pushNotificationSender, notificationHistoryStore);
	}

	@Test
	void sendsPushAndSetsFlagOnFirstArrivalWithinRadius() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		verify(pushNotificationSender).send(any(PushNotificationMessage.class));
		verify(notificationHistoryStore).record(
			eq(USER_ID), eq(NotificationType.NEARBY_MERCHANT), anyString(), anyString(), eq(MERCHANT_ID));
		verify(valueOperations).set(eq(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID)), anyString(),
			eq(Duration.ofDays(1)));
	}

	@Test
	void pushBodyIncludesTheMerchantNameInsteadOfTheGenericPhrase() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(merchant(MERCHANT_ID, "스타벅스 강남점")));

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		ArgumentCaptor<PushNotificationMessage> captor = ArgumentCaptor.forClass(PushNotificationMessage.class);
		verify(pushNotificationSender).send(captor.capture());
		assertThat(captor.getValue().body()).isEqualTo("스타벅스 강남점 근처에 도착했어요. 지금 바로 확인해보세요.");
	}

	@Test
	void pushBodyFallsBackToTheGenericPhraseWhenTheMerchantNameIsUnavailable() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(merchantMapper.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.empty());

		handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG));

		ArgumentCaptor<PushNotificationMessage> captor = ArgumentCaptor.forClass(PushNotificationMessage.class);
		verify(pushNotificationSender).send(captor.capture());
		assertThat(captor.getValue().body()).isEqualTo("저장해둔 매장 근처에 도착했어요. 지금 바로 확인해보세요.");
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

	@Test
	void stillSetsFlagAndKeepsProcessingWhenHistoryStorageFails() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark(MERCHANT_ID)));
		stubGeoSearch(geoResultsWithin(MERCHANT_ID));
		when(redisTemplate.hasKey(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		doThrow(new IllegalStateException("Redis 다운")).when(notificationHistoryStore)
			.record(any(), any(), any(), any(), any());

		assertThatCode(() -> handler.handle(new UserLocationUpdatedEvent(USER_ID, LAT, LNG)))
			.doesNotThrowAnyException();

		verify(pushNotificationSender).send(any(PushNotificationMessage.class));
		verify(valueOperations).set(eq(RedisKeys.nearbyMerchantFlag(USER_ID, MERCHANT_ID)), anyString(),
			eq(Duration.ofDays(1)));
	}
}
