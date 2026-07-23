package site.benepay.domain.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockoutServiceImpl implements RedisLockoutService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockoutServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    @Override
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

    @Override
    public void clearFailuresAndLock(String failureKey, String lockKey) {
        redisTemplate.delete(failureKey);
        redisTemplate.delete(lockKey);
    }
}
