package site.benepay.domain.user.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

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
			// 잠금 TTL(lockDuration)이 실패 카운트 TTL(failureWindow)보다 짧을 수 있다(예: PIN
			// 30초 vs 10분) - 그대로 두면 잠금은 풀렸는데 카운트는 5 이상으로 남아있어서 다음
			// 실패 한 번에 바로 재잠금된다. 잠금과 동시에 카운트 TTL도 lockDuration으로 맞춰서
			// 둘이 같이 만료되게 한다.
			redisTemplate.expire(failureKey, lockDuration);
		}
	}

	@Override
	public void clearFailuresAndLock(String failureKey, String lockKey) {
		redisTemplate.delete(failureKey);
		redisTemplate.delete(lockKey);
	}
}
