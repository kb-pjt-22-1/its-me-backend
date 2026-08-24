package site.benepay.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import site.benepay.auth.security.jwt.JwtProperties;
import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.InvalidCredentialsException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.LoginRequestDto;
import site.benepay.domain.user.dto.LoginResponseDto;
import site.benepay.domain.user.dto.RefreshRequestDto;
import site.benepay.domain.user.dto.RefreshResponseDto;
import site.benepay.domain.user.dto.TokenPairDto;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.vo.User;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtProperties jwtProperties;
	private final TokenService tokenService;
	private final RedisLockoutService redisLockoutService;

	public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtProperties jwtProperties,
		TokenService tokenService, RedisLockoutService redisLockoutService) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.jwtProperties = jwtProperties;
		this.tokenService = tokenService;
		this.redisLockoutService = redisLockoutService;
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponseDto login(LoginRequestDto request) {
		User user = userMapper.findByLoginId(request.getLoginId())
			.orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

		Long userId = user.getUserId();
		String failureKey = RedisKeys.loginFailure(userId);
		String lockKey = RedisKeys.loginLock(userId);

		if (redisLockoutService.isLocked(lockKey)) {
			throw new AccountLockedException("로그인 실패가 반복되어 계정이 일시적으로 잠겼습니다.");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getLoginPasswordHash())) {
			redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, LoginAttemptPolicy.MAX_ATTEMPTS,
				LoginAttemptPolicy.FAILURE_WINDOW, LoginAttemptPolicy.LOGIN_LOCK_DURATION);
			throw new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
		}

		redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
		return issueTokensFor(user);
	}

	@Override
	public RefreshResponseDto refresh(RefreshRequestDto request) {
		TokenPairDto tokens = tokenService.rotateRefreshToken(request.getRefreshToken());
		return RefreshResponseDto.builder()
			.tokenType("Bearer")
			.accessToken(tokens.getAccessToken())
			.refreshToken(tokens.getRefreshToken())
			.expiresIn(jwtProperties.getAccessTokenExpirationMillis() / 1000)
			.build();
	}

	@Override
	public void logout(String accessToken, Long userId) {
		tokenService.blacklistAccessToken(accessToken);
		tokenService.revokeRefreshToken(userId);
	}

	private LoginResponseDto issueTokensFor(User user) {
		TokenPairDto tokens = tokenService.issueTokenPair(user);
		return LoginResponseDto.of(user, tokens, jwtProperties.getAccessTokenExpirationMillis() / 1000);
	}
}
