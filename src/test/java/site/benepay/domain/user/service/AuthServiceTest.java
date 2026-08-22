package site.benepay.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Long USER_ID = 11L;

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenService tokenService;

	@Mock
	private RedisLockoutService redisLockoutService;

	private JwtProperties jwtProperties;
	private AuthService authService;
	private User user;

	@BeforeEach
	void setUp() {
		jwtProperties = new JwtProperties("unit-test-secret-of-at-least-32-bytes-long", "benepay-auth", 600_000L,
			31_536_000_000L);
		authService = new AuthServiceImpl(userMapper, passwordEncoder, jwtProperties, tokenService,
			redisLockoutService);

		user = User.builder()
			.userId(USER_ID)
			.loginId("tester01")
			.loginPasswordHash("hashed-password")
			.role(Role.USER)
			.createdAt(LocalDateTime.now())
			.build();
	}

	@Test
	void loginSucceedsAndClearsFailureStateOnCorrectPassword() {
		LoginRequestDto request = new LoginRequestDto("tester01", "Test1234!");
		when(userMapper.findByLoginId("tester01")).thenReturn(Optional.of(user));
		when(redisLockoutService.isLocked(RedisKeys.loginLock(USER_ID))).thenReturn(false);
		when(passwordEncoder.matches("Test1234!", "hashed-password")).thenReturn(true);
		when(tokenService.issueTokenPair(user)).thenReturn(
			TokenPairDto.builder().accessToken("access-token").refreshToken("refresh-token").build());

		LoginResponseDto response = authService.login(request);

		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		assertThat(response.getExpiresIn()).isEqualTo(600L);
		assertThat(response.getUserId()).isEqualTo(USER_ID);
		assertThat(response.getLoginId()).isEqualTo("tester01");

		verify(redisLockoutService).clearFailuresAndLock(RedisKeys.loginFailure(USER_ID), RedisKeys.loginLock(USER_ID));
	}

	@Test
	void loginWithUnknownLoginIdThrowsWithoutTouchingRedis() {
		when(userMapper.findByLoginId("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequestDto("ghost", "whatever")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(redisLockoutService, never()).isLocked(any());
		verify(redisLockoutService, never()).recordFailureAndMaybeLock(any(), any(), anyInt(), any(), any());
	}

	@Test
	void loginWhileLockedThrowsWithoutCheckingPassword() {
		when(userMapper.findByLoginId("tester01")).thenReturn(Optional.of(user));
		when(redisLockoutService.isLocked(RedisKeys.loginLock(USER_ID))).thenReturn(true);

		assertThatThrownBy(() -> authService.login(new LoginRequestDto("tester01", "Test1234!")))
			.isInstanceOf(AccountLockedException.class);

		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void wrongPasswordRecordsFailureAndThrows() {
		when(userMapper.findByLoginId("tester01")).thenReturn(Optional.of(user));
		when(redisLockoutService.isLocked(RedisKeys.loginLock(USER_ID))).thenReturn(false);
		when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequestDto("tester01", "wrong-password")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(redisLockoutService).recordFailureAndMaybeLock(
			RedisKeys.loginFailure(USER_ID), RedisKeys.loginLock(USER_ID), 5, Duration.ofMinutes(10),
			Duration.ofMinutes(30));
		verify(tokenService, never()).issueTokenPair(any());
	}

	@Test
	void refreshDelegatesToTokenServiceAndWrapsResult() {
		when(tokenService.rotateRefreshToken("old-refresh-token")).thenReturn(
			TokenPairDto.builder().accessToken("new-access").refreshToken("new-refresh").build());

		RefreshResponseDto response = authService.refresh(new RefreshRequestDto("old-refresh-token"));

		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getAccessToken()).isEqualTo("new-access");
		assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
		assertThat(response.getExpiresIn()).isEqualTo(600L);
	}

	@Test
	void logoutBlacklistsAccessTokenAndRevokesRefreshToken() {
		authService.logout("some-access-token", USER_ID);

		verify(tokenService).blacklistAccessToken("some-access-token");
		verify(tokenService).revokeRefreshToken(USER_ID);
	}

}
