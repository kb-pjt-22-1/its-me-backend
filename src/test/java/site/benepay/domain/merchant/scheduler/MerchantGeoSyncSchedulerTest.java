package site.benepay.domain.merchant.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import site.benepay.common.util.RedisKeys;
import site.benepay.domain.merchant.mapper.MerchantMapper;
import site.benepay.domain.merchant.vo.Merchant;

@ExtendWith(MockitoExtension.class)
class MerchantGeoSyncSchedulerTest {

	@Mock
	private MerchantMapper merchantMapper;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private GeoOperations<String, String> geoOperations;

	private MerchantGeoSyncScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new MerchantGeoSyncScheduler(merchantMapper, redisTemplate);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	private static Merchant merchant(long id, String categoryCode) {
		return Merchant.builder()
			.merchantId(id)
			.categoryCode(categoryCode)
			.brandId(1L)
			.merchantCode("MC-" + id)
			.merchantName("가맹점" + id)
			.address("서울시 어딘가")
			.latitude(new BigDecimal("37.5000000"))
			.longitude(new BigDecimal("127.0000000"))
			.build();
	}

	@Test
	void syncOnStartupRunsTheSameLockedSync() {
		when(valueOperations.setIfAbsent(eq(RedisKeys.MERCHANT_GEO_SYNC_LOCK), anyString(), any(Duration.class)))
			.thenReturn(true);
		when(merchantMapper.findAll(null)).thenReturn(List.of());

		scheduler.syncOnStartup();

		// 배치 실행 여부는 락 획득으로 판단한다 - syncOnStartup()이 별도 로직 없이
		// syncMerchantGeoIndex()를 그대로 태우는지만 확인하면 된다.
		verify(valueOperations).setIfAbsent(eq(RedisKeys.MERCHANT_GEO_SYNC_LOCK), anyString(), any(Duration.class));
		verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(RedisKeys.MERCHANT_GEO_SYNC_LOCK)), any());
	}

	@Test
	void skipsSyncWhenAnotherInstanceAlreadyHoldsTheLock() {
		when(valueOperations.setIfAbsent(eq(RedisKeys.MERCHANT_GEO_SYNC_LOCK), anyString(), any(Duration.class)))
			.thenReturn(false);

		scheduler.syncMerchantGeoIndex();

		verify(merchantMapper, never()).findAll(any());
		verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
	}

	@Test
	void rebuildsPerCategoryAndAllIndexesThenSwapsInAtomicallyAndReleasesTheLock() {
		when(valueOperations.setIfAbsent(eq(RedisKeys.MERCHANT_GEO_SYNC_LOCK), anyString(), any(Duration.class)))
			.thenReturn(true);
		when(merchantMapper.findAll(null))
			.thenReturn(List.of(merchant(1L, "CE01"), merchant(2L, "CE01"), merchant(3L, "CE02")));
		when(redisTemplate.opsForGeo()).thenReturn(geoOperations);

		scheduler.syncMerchantGeoIndex();

		verify(geoOperations).add(eq("merchants:geo:all:staging"), any(Point.class), eq("1"));
		verify(geoOperations).add(eq("merchants:geo:all:staging"), any(Point.class), eq("2"));
		verify(geoOperations).add(eq("merchants:geo:all:staging"), any(Point.class), eq("3"));
		verify(geoOperations).add(eq("merchants:geo:CE01:staging"), any(Point.class), eq("1"));
		verify(geoOperations).add(eq("merchants:geo:CE01:staging"), any(Point.class), eq("2"));
		verify(geoOperations).add(eq("merchants:geo:CE02:staging"), any(Point.class), eq("3"));

		verify(redisTemplate).rename("merchants:geo:all:staging", RedisKeys.MERCHANT_GEO_ALL);
		verify(redisTemplate).rename("merchants:geo:CE01:staging", RedisKeys.merchantGeoCategory("CE01"));
		verify(redisTemplate).rename("merchants:geo:CE02:staging", RedisKeys.merchantGeoCategory("CE02"));

		// 시작 전에 이전 실패 배치의 잔여 staging 키를 지운다.
		verify(redisTemplate).delete(argThat((List<String> keys) -> keys.containsAll(
			List.of("merchants:geo:all:staging", "merchants:geo:CE01:staging", "merchants:geo:CE02:staging"))));

		verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(RedisKeys.MERCHANT_GEO_SYNC_LOCK)), any());
	}

	@Test
	void doesNothingButStillReleasesTheLockWhenThereAreNoMerchants() {
		when(valueOperations.setIfAbsent(eq(RedisKeys.MERCHANT_GEO_SYNC_LOCK), anyString(), any(Duration.class)))
			.thenReturn(true);
		when(merchantMapper.findAll(null)).thenReturn(List.of());

		scheduler.syncMerchantGeoIndex();

		verify(redisTemplate, never()).opsForGeo();
		verify(redisTemplate, never()).rename(anyString(), anyString());
		verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(RedisKeys.MERCHANT_GEO_SYNC_LOCK)), any());
	}

	@Test
	void releasesTheLockEvenWhenTheRebuildFails() {
		when(valueOperations.setIfAbsent(eq(RedisKeys.MERCHANT_GEO_SYNC_LOCK), anyString(), any(Duration.class)))
			.thenReturn(true);
		when(merchantMapper.findAll(null)).thenThrow(new RuntimeException("DB down"));

		assertThatThrownBy(() -> scheduler.syncMerchantGeoIndex())
			.isInstanceOf(RuntimeException.class)
			.hasMessage("DB down");

		verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(RedisKeys.MERCHANT_GEO_SYNC_LOCK)), any());
	}
}
