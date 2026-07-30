package site.benepay.domain.user.service;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockoutServiceTest {

    private static final String FAILURE_KEY = "login:failure:1";
    private static final String LOCK_KEY = "login:lock:1";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisLockoutService redisLockoutService;

    @BeforeEach
    void setUp() {
        redisLockoutService = new RedisLockoutServiceImpl(redisTemplate);
    }

    @Test
    void isLockedReflectsWhetherTheLockKeyExists() {
        when(redisTemplate.hasKey(LOCK_KEY)).thenReturn(true);
        assertThat(redisLockoutService.isLocked(LOCK_KEY)).isTrue();

        when(redisTemplate.hasKey(LOCK_KEY)).thenReturn(false);
        assertThat(redisLockoutService.isLocked(LOCK_KEY)).isFalse();
    }

    @Test
    void firstFailureSetsTheFailureWindowExpiryButDoesNotLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(FAILURE_KEY)).thenReturn(1L);

        redisLockoutService.recordFailureAndMaybeLock(
                FAILURE_KEY, LOCK_KEY, 5, Duration.ofMinutes(10), Duration.ofMinutes(30));

        verify(redisTemplate).expire(FAILURE_KEY, Duration.ofMinutes(10));
        verify(valueOperations, never()).set(eq(LOCK_KEY), any(), any(Duration.class));
    }

    @Test
    void subsequentFailuresBelowThresholdDoNotResetExpiryOrLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(FAILURE_KEY)).thenReturn(3L);

        redisLockoutService.recordFailureAndMaybeLock(
                FAILURE_KEY, LOCK_KEY, 5, Duration.ofMinutes(10), Duration.ofMinutes(30));

        verify(redisTemplate, never()).expire(eq(FAILURE_KEY), any(Duration.class));
        verify(valueOperations, never()).set(eq(LOCK_KEY), any(), any(Duration.class));
    }

    @Test
    void reachingMaxAttemptsLocksTheAccount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(FAILURE_KEY)).thenReturn(5L);

        redisLockoutService.recordFailureAndMaybeLock(
                FAILURE_KEY, LOCK_KEY, 5, Duration.ofMinutes(10), Duration.ofMinutes(30));

        verify(valueOperations).set(LOCK_KEY, "locked", Duration.ofMinutes(30));
    }

    @Test
    void clearFailuresAndLockDeletesBothKeys() {
        redisLockoutService.clearFailuresAndLock(FAILURE_KEY, LOCK_KEY);

        verify(redisTemplate).delete(FAILURE_KEY);
        verify(redisTemplate).delete(LOCK_KEY);
    }
}
