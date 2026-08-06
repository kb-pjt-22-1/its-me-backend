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
import site.benepay.common.exception.DevLoginDisabledException;
import site.benepay.common.exception.InvalidCredentialsException;
import site.benepay.common.exception.UserNotFoundException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.DevLoginRequestDto;
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
		authService = authServiceWithDevLogin(false);

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

	// ---- dev login ----

	private AuthService authServiceWithDevLogin(boolean enabled) {
		return new AuthServiceImpl(userMapper, passwordEncoder, jwtProperties, tokenService,
			redisLockoutService, enabled, 5);
	}

	private User devUser(Long userId, String loginId) {
		return User.builder()
			.userId(userId)
			.loginId(loginId)
			.loginPasswordHash("!DEV-ACCOUNT-NO-PASSWORD-LOGIN!")
			.role(Role.USER)
			.createdAt(LocalDateTime.now())
			.build();
	}

	@Test
	void devLoginIsRejectedWhenTheFlagIsOff() {
		assertThatThrownBy(() -> authService.devLogin(new DevLoginRequestDto(1)))
			.isInstanceOf(DevLoginDisabledException.class);

		verify(userMapper, never()).findByLoginId(any());
		verify(tokenService, never()).issueTokenPair(any());
	}

	@Test
	void devLoginIssuesTokensForTheRequestedSlot() {
		AuthService devAuthService = authServiceWithDevLogin(true);
		when(userMapper.findByLoginId("dev2")).thenReturn(Optional.of(devUser(22L, "dev2")));
		when(tokenService.issueTokenPair(any())).thenReturn(
			TokenPairDto.builder().accessToken("dev-access").refreshToken("dev-refresh").build());

		LoginResponseDto response = devAuthService.devLogin(new DevLoginRequestDto(2));

		assertThat(response.getLoginId()).isEqualTo("dev2");
		assertThat(response.getUserId()).isEqualTo(22L);
		assertThat(response.getAccessToken()).isEqualTo("dev-access");
		assertThat(response.getTokenType()).isEqualTo("Bearer");
	}

	@Test
	void devLoginFallsBackToTheFirstSlotWhenNoneIsGiven() {
		AuthService devAuthService = authServiceWithDevLogin(true);
		when(userMapper.findByLoginId("dev1")).thenReturn(Optional.of(devUser(21L, "dev1")));
		when(tokenService.issueTokenPair(any())).thenReturn(
			TokenPairDto.builder().accessToken("dev-access").refreshToken("dev-refresh").build());

		assertThat(devAuthService.devLogin(new DevLoginRequestDto()).getLoginId()).isEqualTo("dev1");
	}

	@Test
	void devLoginNeverChecksAPassword() {
		AuthService devAuthService = authServiceWithDevLogin(true);
		when(userMapper.findByLoginId("dev1")).thenReturn(Optional.of(devUser(21L, "dev1")));
		when(tokenService.issueTokenPair(any())).thenReturn(
			TokenPairDto.builder().accessToken("dev-access").refreshToken("dev-refresh").build());

		devAuthService.devLogin(new DevLoginRequestDto(1));

		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void devLoginRejectsSlotsOutsideTheConfiguredRange() {
		AuthService devAuthService = authServiceWithDevLogin(true);

		assertThatThrownBy(() -> devAuthService.devLogin(new DevLoginRequestDto(0)))
			.isInstanceOf(InvalidCredentialsException.class);
		assertThatThrownBy(() -> devAuthService.devLogin(new DevLoginRequestDto(6)))
			.isInstanceOf(InvalidCredentialsException.class);
		assertThatThrownBy(() -> devAuthService.devLogin(new DevLoginRequestDto(-1)))
			.isInstanceOf(InvalidCredentialsException.class);

		// 슬롯 검증이 조회보다 먼저 끝나야 임의의 loginId를 만들어 볼 여지가 없다.
		verify(userMapper, never()).findByLoginId(any());
	}

	@Test
	void devLoginFailsWhenTheSlotAccountIsNotSeeded() {
		AuthService devAuthService = authServiceWithDevLogin(true);
		when(userMapper.findByLoginId("dev3")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> devAuthService.devLogin(new DevLoginRequestDto(3)))
			.isInstanceOf(UserNotFoundException.class);

		verify(tokenService, never()).issueTokenPair(any());
	}
}
