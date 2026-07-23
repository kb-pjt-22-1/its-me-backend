package site.benepay.auth.domain.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockoutService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockoutService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    public void recordFailureAndMaybeLock(String failureKey, String lockKey, int maxAttempts,
                                           Duration failureWindow, Duration lockDuration) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        Long count = ops.increment(failureKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failureKey, failureWindow);
        }
        if (count != null && count >= maxAttempts) {
            ops.set(lockKey, "locked", lockDuration);
        }
    }

    public void clearFailuresAndLock(String failureKey, String lockKey) {
        redisTemplate.delete(failureKey);
        redisTemplate.delete(lockKey);
    }
}
