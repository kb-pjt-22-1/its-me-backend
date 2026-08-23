package site.benepay.domain.user.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import site.benepay.auth.security.jwt.JwtProperties;
import site.benepay.auth.security.jwt.JwtTokenProvider;
import site.benepay.common.exception.InvalidTokenException;
import site.benepay.common.exception.TokenReuseException;
import site.benepay.common.exception.UserNotFoundException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.TokenPairDto;
import site.benepay.domain.user.event.SessionDisplacedEvent;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.vo.User;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final StringRedisTemplate redisTemplate;
	private final UserMapper userMapper;
	private final ApplicationEventPublisher eventPublisher;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final long gracePeriodMillis;

	public TokenServiceImpl(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties,
		StringRedisTemplate redisTemplate, UserMapper userMapper, ApplicationEventPublisher eventPublisher,
		@Value("${token.refresh-grace-period-ms}") long gracePeriodMillis) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtProperties = jwtProperties;
		this.redisTemplate = redisTemplate;
		this.userMapper = userMapper;
		this.eventPublisher = eventPublisher;
		this.gracePeriodMillis = gracePeriodMillis;
	}

	@Override
	public TokenPairDto issueTokenPair(User user) {
		// 덮어쓰기 전에 먼저 읽어야 "기존에 로그인된 기기가 있었는지"를 알 수 있다 -
		// saveState가 실행되고 나면 이전 세션 정보는 사라진다.
		String previousAccessToken = readPreviousAccessToken(user.getUserId());

		String jti = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user, jti);

		RefreshTokenState state = new RefreshTokenState();
		state.jti = jti;
		state.accessToken = accessToken;
		state.refreshToken = refreshToken;

		saveState(user.getUserId(), state);
		displacePreviousSessionIfAny(user.getUserId(), previousAccessToken);

		return TokenPairDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
	}

	// saveState가 이미 Redis의 refresh 상태를 새 세션 것으로 덮어써서, 이전 기기는 refresh를
	// 더 이상 못 쓴다. 하지만 이전 기기의 access 토큰은 만료 전까지 계속 인증을 통과하므로
	// 별도로 블랙리스트에 넣어야 "로그인 해제"가 즉시 적용된다. 신규 가입 직후 자동 로그인
	// (UserServiceImpl.signUp)처럼 이전 세션이 없는 경우 previousAccessToken이 null이라
	// 아무 일도 일어나지 않는다.
	private void displacePreviousSessionIfAny(Long userId, String previousAccessToken) {
		if (previousAccessToken == null) {
			return;
		}
		try {
			blacklistAccessToken(previousAccessToken);
			eventPublisher.publishEvent(new SessionDisplacedEvent(userId));
		} catch (RuntimeException e) {
			// 로그인 해제 후처리(블랙리스트/알림) 실패가 새 로그인 자체를 막으면 안 된다.
			log.warn("이전 세션 로그인 해제 처리 실패. userId={}", userId, e);
		}
	}

	private String readPreviousAccessToken(Long userId) {
		String raw = redisTemplate.opsForValue().get(RedisKeys.refresh(userId));
		if (raw == null) {
			return null;
		}
		try {
			return readState(raw).accessToken;
		} catch (RuntimeException e) {
			return null;
		}
	}

	@Override
	public TokenPairDto rotateRefreshToken(String presentedRefreshToken) {
		if (!jwtTokenProvider.validateToken(presentedRefreshToken)
			|| !JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.getTokenType(presentedRefreshToken))) {
			throw new InvalidTokenException("refresh token is invalid, expired, or of the wrong type");
		}

		Long userId = jwtTokenProvider.getUserId(presentedRefreshToken);
		String presentedJti = jwtTokenProvider.getJti(presentedRefreshToken);

		String raw = redisTemplate.opsForValue().get(RedisKeys.refresh(userId));
		if (raw == null) {
			throw new InvalidTokenException("no active session for this refresh token");
		}
		RefreshTokenState state = readState(raw);

		if (presentedJti.equals(state.jti)) {
			return rotate(userId, state);
		}

		if (state.previousJti != null && presentedJti.equals(state.previousJti)) {
			if (state.graceExpiresAt != null && System.currentTimeMillis() < state.graceExpiresAt) {
				// NFR-REL-01: concurrent duplicate refresh request using the just-rotated-out token
				return TokenPairDto.builder().accessToken(state.accessToken).refreshToken(state.refreshToken).build();
			}
			// NFR-SEC-02: 같은 회전 체인의 직전 토큰이 유예 기간을 넘겨서 다시 나타났다 - 정상적인
			// 동시 요청으로는 설명이 안 되는 지연 재사용이라 탈취로 간주하고 세션을 통째로 끊는다.
			redisTemplate.delete(RedisKeys.refresh(userId));
			redisTemplate.opsForValue().set(RedisKeys.alert(userId),
				"refresh token reuse detected at " + System.currentTimeMillis(), Duration.ofDays(30));
			log.warn("Refresh token reuse detected for userId={}", userId);
			throw new TokenReuseException("refresh token reuse detected; all sessions revoked");
		}

		// presentedJti가 현재 세션도, 그 직전 세션도 아니다 - 이 회전 체인과 전혀 무관한, 이미
		// 다른 로그인으로 대체된(예: 다른 기기 로그인으로 밀려난) 세션의 토큰이다. 이건 그 토큰
		// 자체가 더 이상 유효하지 않다는 뜻일 뿐, 지금 활성 상태인 다른 세션(state)을 훼손할
		// 근거가 안 된다 - 여기서 현재 세션을 지우면, 밀려난 기기가 재로그인해도 "지울 이전
		// 세션이 없다"고 판단해 새로 로그인한 기기가 상대를 강제 로그아웃시키지 못하는 문제가
		// 생긴다(SessionDisplacedEvent 참고). 그래서 이 경우엔 이 요청만 거부하고 state는 그대로 둔다.
		throw new InvalidTokenException("refresh token does not match any active session");
	}

	@Override
	public void blacklistAccessToken(String accessToken) {
		if (!jwtTokenProvider.validateToken(accessToken)) {
			return;
		}
		String jti = jwtTokenProvider.getJti(accessToken);
		long remainingTtlMillis = jwtTokenProvider.getRemainingTtlMillis(accessToken);
		if (remainingTtlMillis > 0) {
			redisTemplate.opsForValue().set(RedisKeys.blacklist(jti), "logout", Duration.ofMillis(remainingTtlMillis));
		}
	}

	@Override
	public void revokeRefreshToken(Long userId) {
		redisTemplate.delete(RedisKeys.refresh(userId));
	}

	private TokenPairDto rotate(Long userId, RefreshTokenState previousState) {
		User user = userMapper.findByUserId(userId)
			.orElseThrow(() -> new UserNotFoundException("user not found for refresh token"));

		String newJti = UUID.randomUUID().toString();
		String newAccessToken = jwtTokenProvider.generateAccessToken(user);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user, newJti);

		RefreshTokenState newState = new RefreshTokenState();
		newState.jti = newJti;
		newState.accessToken = newAccessToken;
		newState.refreshToken = newRefreshToken;
		newState.previousJti = previousState.jti;
		newState.graceExpiresAt = System.currentTimeMillis() + gracePeriodMillis;

		saveState(userId, newState);
		return TokenPairDto.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
	}

	private void saveState(Long userId, RefreshTokenState state) {
		try {
			String json = objectMapper.writeValueAsString(state);
			redisTemplate.opsForValue().set(RedisKeys.refresh(userId), json,
				Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMillis()));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to serialize refresh token state", e);
		}
	}

	private RefreshTokenState readState(String raw) {
		try {
			return objectMapper.readValue(raw, RefreshTokenState.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to deserialize refresh token state", e);
		}
	}

	public static class RefreshTokenState {
		public String jti;
		public String accessToken;
		public String refreshToken;
		public String previousJti;
		public Long graceExpiresAt;
	}
}
