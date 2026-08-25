package site.benepay.domain.user.service;

import java.time.Duration;

/**
 * 로그인/비밀번호 재확인/PIN 확인이 공유하는 무차별 대입 방어 정책. 세 곳 모두 같은
 * (시도 횟수, 집계 기간)을 쓰지만 잠금 시간은 위협 강도에 맞춰 달리 가져간다 - 로그인과
 * 비밀번호 재확인은 계정 전체를 보호하는 동일 위협이라 같은 잠금 시간을 쓴다(791d877에서
 * 로그인만 30분->5분으로 줄였을 때 비밀번호 재확인 쪽이 갱신되지 않았던 것을 여기서 맞춘다).
 */
final class LoginAttemptPolicy {

	static final int MAX_ATTEMPTS = 5;
	static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);

	static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(5);
	static final Duration PASSWORD_REVERIFY_LOCK_DURATION = LOGIN_LOCK_DURATION;
	static final Duration PIN_REVERIFY_LOCK_DURATION = Duration.ofSeconds(30);

	private LoginAttemptPolicy() {
	}
}
