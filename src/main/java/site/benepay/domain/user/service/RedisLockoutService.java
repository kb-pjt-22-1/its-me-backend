package site.benepay.domain.user.service;

import java.time.Duration;

public interface RedisLockoutService {

    boolean isLocked(String lockKey);

    void recordFailureAndMaybeLock(String failureKey, String lockKey, int maxAttempts,
                                    Duration failureWindow, Duration lockDuration);

    void clearFailuresAndLock(String failureKey, String lockKey);
}
