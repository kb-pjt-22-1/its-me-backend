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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.InvalidCredentialsException;
import site.benepay.common.exception.InvalidPinFormatException;
import site.benepay.common.exception.InvalidTokenException;
import site.benepay.common.exception.PinAlreadyRegisteredException;
import site.benepay.common.exception.UserNotFoundException;
import site.benepay.common.exception.WithdrawalNotConfirmedException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.ChangePasswordRequestDto;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.dto.VerifyPasswordRequestDto;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final Long USER_ID = 11L;

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RedisLockoutService redisLockoutService;

	@Mock
	private TokenService tokenService;

	@Mock
	private SignupVerificationStore signupVerificationStore;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserServiceImpl(userMapper, passwordEncoder, redisLockoutService, tokenService,
			signupVerificationStore);
	}

	private SignupVerificationStore.VerifiedIdentity verifiedIdentity() {
		return new SignupVerificationStore.VerifiedIdentity(
			"New User", "010-1111-2222", "19900101", "ci-hash", "ci-encrypted", "di-hash");
	}

	private User activeUser(String pinHash) {
		return User.builder()
			.userId(USER_ID)
			.loginId("tester01")
			.loginPasswordHash("hashed-password")
			.pinHash(pinHash)
			.name("Tester")
			.phoneNumber("010-9999-0001")
			.role(Role.USER)
			.createdAt(LocalDateTime.now())
			.deleted(false)
			.build();
	}

	// ---- signUp ----

	@Test
	void signUpSucceedsWhenNothingIsDuplicate() {
		SignUpRequestDto request = new SignUpRequestDto("newuser", "Test1234!", "valid-token", "fcm-token");
		when(userMapper.existsByLoginId("newuser")).thenReturn(false);
		when(signupVerificationStore.redeem("valid-token")).thenReturn(Optional.of(verifiedIdentity()));
		when(userMapper.existsByDiHash("di-hash")).thenReturn(false);
		when(passwordEncoder.encode("Test1234!")).thenReturn("encoded-password");

		UserResponseDto response = userService.signUp(request);

		assertThat(response.getLoginId()).isEqualTo("newuser");
		assertThat(response.getBirthDate()).isEqualTo("19900101");
		assertThat(response.getRole()).isEqualTo(Role.USER);
		verify(userMapper).insert(any(User.class));
	}

	@Test
	void signUpMapsVerifiedIdentityAndRequestFieldsOntoTheInsertedUser() {
		SignUpRequestDto request = new SignUpRequestDto("newuser", "Test1234!", "valid-token", "fcm-token");
		when(userMapper.existsByLoginId("newuser")).thenReturn(false);
		when(signupVerificationStore.redeem("valid-token")).thenReturn(Optional.of(verifiedIdentity()));
		when(userMapper.existsByDiHash("di-hash")).thenReturn(false);
		when(passwordEncoder.encode("Test1234!")).thenReturn("encoded-password");

		userService.signUp(request);

		ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class);
		verify(userMapper).insert(inserted.capture());

		User user = inserted.getValue();
		// loginId/password/fcmToken은 요청에서, name/phoneNumber/birthDate/di/ciEncrypted는
		// 토큰으로 복원한 검증 결과에서 온다 - 클라이언트가 개인정보를 직접 못 정하는 게 핵심이다.
		assertThat(user.getLoginId()).isEqualTo("newuser");
		assertThat(user.getLoginPasswordHash()).isEqualTo("encoded-password");
		assertThat(user.getFcmToken()).isEqualTo("fcm-token");
		assertThat(user.getName()).isEqualTo("New User");
		assertThat(user.getPhoneNumber()).isEqualTo("010-1111-2222");
		assertThat(user.getBirthDate()).isEqualTo("19900101");
		assertThat(user.getDi()).isEqualTo("di-hash");
		assertThat(user.getCiHash()).isEqualTo("ci-hash");
		assertThat(user.getCiEncrypted()).isEqualTo("ci-encrypted");
		assertThat(user.getRole()).isEqualTo(Role.USER);
		assertThat(user.isDeleted()).isFalse();
		// user_id는 AUTO_INCREMENT라 insert 시점에는 비어 있고 MyBatis가 채워 넣는다.
		assertThat(user.getUserId()).isNull();
	}

	@Test
	void signUpWithDuplicateLoginIdThrowsAndNeverTouchesTheVerificationToken() {
		SignUpRequestDto request = new SignUpRequestDto("existing", "Test1234!", "valid-token", "fcm-token");
		when(userMapper.existsByLoginId("existing")).thenReturn(true);

		assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(DuplicateUserException.class);

		// 아이디 중복은 토큰이 유효한지와 무관한 실패라, 토큰을 소모(redeem)하기 전에 끝나야
		// 한다 - 그래야 아이디만 다시 골라 같은 토큰으로 재시도할 수 있다.
		verify(signupVerificationStore, never()).redeem(any());
		verify(userMapper, never()).insert(any());
	}

	@Test
	void signUpWithInvalidOrExpiredTokenThrowsAndNeverInserts() {
		SignUpRequestDto request = new SignUpRequestDto("newuser", "Test1234!", "stale-token", "fcm-token");
		when(userMapper.existsByLoginId("newuser")).thenReturn(false);
		when(signupVerificationStore.redeem("stale-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(InvalidTokenException.class);

		verify(userMapper, never()).insert(any());
	}

	@Test
	void signUpWithDuplicateDiHashThrowsAfterRedeemingTheToken() {
		SignUpRequestDto request = new SignUpRequestDto("newuser", "Test1234!", "valid-token", "fcm-token");
		when(userMapper.existsByLoginId("newuser")).thenReturn(false);
		when(signupVerificationStore.redeem("valid-token")).thenReturn(Optional.of(verifiedIdentity()));
		when(userMapper.existsByDiHash("di-hash")).thenReturn(true);

		assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(DuplicateUserException.class);

		verify(userMapper, never()).insert(any());
	}

	@Test
	void signUpWithDuplicateCiHashThrowsAfterRedeemingTheToken() {
		SignUpRequestDto request = new SignUpRequestDto("newuser", "Test1234!", "valid-token", "fcm-token");
		when(userMapper.existsByLoginId("newuser")).thenReturn(false);
		when(signupVerificationStore.redeem("valid-token")).thenReturn(Optional.of(verifiedIdentity()));
		when(userMapper.existsByDiHash("di-hash")).thenReturn(false);
		when(userMapper.existsByCiHash("ci-hash")).thenReturn(true);

		assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(DuplicateUserException.class);

		verify(userMapper, never()).insert(any());
	}

	// ---- password verification (재인증 게이트) ----

	@Test
	void verifyPasswordSucceedsAndClearsFailuresWhenCorrect() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		when(passwordEncoder.matches("Test1234!", "hashed-password")).thenReturn(true);

		userService.verifyPassword(USER_ID, new VerifyPasswordRequestDto("Test1234!"));

		verify(redisLockoutService).clearFailuresAndLock(RedisKeys.passwordFailure(USER_ID),
			RedisKeys.passwordLock(USER_ID));
	}

	@Test
	void verifyPasswordWithWrongPasswordRecordsFailure() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

		assertThatThrownBy(() -> userService.verifyPassword(USER_ID, new VerifyPasswordRequestDto("wrong-password")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(redisLockoutService).recordFailureAndMaybeLock(
			RedisKeys.passwordFailure(USER_ID), RedisKeys.passwordLock(USER_ID), 5, Duration.ofMinutes(10),
			Duration.ofMinutes(30));
	}

	@Test
	void verifyPasswordFailsFastWhenLockedWithoutTouchingTheDatabase() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(true);

		assertThatThrownBy(() -> userService.verifyPassword(USER_ID, new VerifyPasswordRequestDto("Test1234!")))
			.isInstanceOf(AccountLockedException.class);

		verify(userMapper, never()).findByUserId(any());
	}

	// password change와 verifyPassword가 같은 잠금 카운터를 공유하므로, 검증에서 쌓인 실패가
	// 변경 시도에도 그대로 이어지는지 확인한다.
	@Test
	void verifyPasswordAndChangePasswordShareTheSameLockoutCounter() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

		assertThatThrownBy(() -> userService.verifyPassword(USER_ID, new VerifyPasswordRequestDto("wrong-password")))
			.isInstanceOf(InvalidCredentialsException.class);
		assertThatThrownBy(() -> userService.changePassword(USER_ID,
			new ChangePasswordRequestDto("wrong-password", "NewPass1!")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(redisLockoutService, org.mockito.Mockito.times(2)).recordFailureAndMaybeLock(
			RedisKeys.passwordFailure(USER_ID), RedisKeys.passwordLock(USER_ID), 5, Duration.ofMinutes(10),
			Duration.ofMinutes(30));
	}

	// ---- password change ----

	@Test
	void changePasswordFailsFastWhenLockedWithoutTouchingTheDatabase() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(true);

		assertThatThrownBy(() -> userService.changePassword(USER_ID,
			new ChangePasswordRequestDto("Test1234!", "NewPass1!")))
			.isInstanceOf(AccountLockedException.class);

		verify(userMapper, never()).findByUserId(any());
	}

	@Test
	void changePasswordWithWrongCurrentPasswordRecordsFailure() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

		assertThatThrownBy(() -> userService.changePassword(USER_ID,
			new ChangePasswordRequestDto("wrong-password", "NewPass1!")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(redisLockoutService).recordFailureAndMaybeLock(
			RedisKeys.passwordFailure(USER_ID), RedisKeys.passwordLock(USER_ID), 5, Duration.ofMinutes(10),
			Duration.ofMinutes(30));
		verify(userMapper, never()).updatePasswordHash(any(), any());
	}

	@Test
	void changePasswordWithCorrectCurrentPasswordUpdatesAndRevokesSessions() {
		when(redisLockoutService.isLocked(RedisKeys.passwordLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		when(passwordEncoder.matches("Test1234!", "hashed-password")).thenReturn(true);
		when(passwordEncoder.encode("NewPass1!")).thenReturn("new-encoded-password");

		userService.changePassword(USER_ID, new ChangePasswordRequestDto("Test1234!", "NewPass1!"));

		verify(redisLockoutService).clearFailuresAndLock(RedisKeys.passwordFailure(USER_ID),
			RedisKeys.passwordLock(USER_ID));
		verify(userMapper).updatePasswordHash(USER_ID, "new-encoded-password");
		// 비밀번호가 바뀌면 예전 비밀번호로 떠 있던 세션은 무효화돼야 한다.
		verify(tokenService).revokeRefreshToken(USER_ID);
	}

	// ---- profile ----

	@Test
	void getMyProfileReturnsMappedDtoWhenUserExists() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));

		UserResponseDto response = userService.getMyProfile(USER_ID);

		assertThat(response.getUserId()).isEqualTo(USER_ID);
	}

	@Test
	void getMyProfileThrowsWhenUserDoesNotExist() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getMyProfile(USER_ID)).isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void updateProfileUpdatesPhoneNumber() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		UpdateProfileRequestDto request = new UpdateProfileRequestDto("010-2222-3333");

		userService.updateProfile(USER_ID, request);

		verify(userMapper).updateProfile(USER_ID, "010-2222-3333");
	}

	@Test
	void updateProfileThrowsWhenUserDoesNotExist() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.empty());
		UpdateProfileRequestDto request = new UpdateProfileRequestDto("010-2222-3333");

		assertThatThrownBy(() -> userService.updateProfile(USER_ID, request)).isInstanceOf(UserNotFoundException.class);

		verify(userMapper, never()).updateProfile(any(), any());
	}

	// ---- PIN registration ----

	@Test
	void registerPinSucceedsWhenNoPinExistsYet() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
		when(passwordEncoder.encode("481027")).thenReturn("encoded-pin");

		userService.registerPin(USER_ID, new RegisterPinRequestDto("481027"));

		verify(userMapper).updatePinHash(USER_ID, "encoded-pin");
	}

	@Test
	void registerPinThrowsWhenAlreadyRegistered() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser("existing-pin-hash")));

		assertThatThrownBy(() -> userService.registerPin(USER_ID, new RegisterPinRequestDto("481027")))
			.isInstanceOf(PinAlreadyRegisteredException.class);

		verify(userMapper, never()).updatePinHash(any(), any());
	}

	@Test
	void registerPinWithInvalidFormatThrowsAndNeverPersists() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));

		assertThatThrownBy(() -> userService.registerPin(USER_ID, new RegisterPinRequestDto("123456")))
			.isInstanceOf(InvalidPinFormatException.class);

		verify(userMapper, never()).updatePinHash(any(), any());
	}

	// ---- PIN update/delete ----

	@Test
	void updateOrDeletePinFailsFastWhenLockedWithoutTouchingTheDatabase() {
		when(redisLockoutService.isLocked(RedisKeys.pinLock(USER_ID))).thenReturn(true);

		assertThatThrownBy(
			() -> userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("481027", "592841")))
			.isInstanceOf(AccountLockedException.class);

		verify(userMapper, never()).findByUserId(any());
	}

	@Test
	void updateOrDeletePinWithWrongCurrentPinRecordsFailure() {
		when(redisLockoutService.isLocked(RedisKeys.pinLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser("existing-pin-hash")));
		when(passwordEncoder.matches("000000", "existing-pin-hash")).thenReturn(false);

		assertThatThrownBy(
			() -> userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("000000", "592841")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(redisLockoutService).recordFailureAndMaybeLock(
			RedisKeys.pinFailure(USER_ID), RedisKeys.pinLock(USER_ID), 5, Duration.ofMinutes(10),
			Duration.ofSeconds(30));
		verify(userMapper, never()).updatePinHash(any(), any());
	}

	@Test
	void updateOrDeletePinWithCorrectCurrentPinAndValidNewPinUpdates() {
		when(redisLockoutService.isLocked(RedisKeys.pinLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser("existing-pin-hash")));
		when(passwordEncoder.matches("481027", "existing-pin-hash")).thenReturn(true);
		when(passwordEncoder.encode("592841")).thenReturn("new-encoded-pin");

		userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("481027", "592841"));

		verify(redisLockoutService).clearFailuresAndLock(RedisKeys.pinFailure(USER_ID), RedisKeys.pinLock(USER_ID));
		verify(userMapper).updatePinHash(USER_ID, "new-encoded-pin");
	}

	@Test
	void updateOrDeletePinWithNullNewPinDeletesIt() {
		when(redisLockoutService.isLocked(RedisKeys.pinLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser("existing-pin-hash")));
		when(passwordEncoder.matches("481027", "existing-pin-hash")).thenReturn(true);

		userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("481027", null));

		verify(userMapper).updatePinHash(eq(USER_ID), isNull());
	}

	@Test
	void updateOrDeletePinWithInvalidNewPinThrowsAfterCurrentPinIsVerified() {
		when(redisLockoutService.isLocked(RedisKeys.pinLock(USER_ID))).thenReturn(false);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser("existing-pin-hash")));
		when(passwordEncoder.matches("481027", "existing-pin-hash")).thenReturn(true);

		assertThatThrownBy(
			() -> userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("481027", "123456")))
			.isInstanceOf(InvalidPinFormatException.class);

		verify(redisLockoutService).clearFailuresAndLock(RedisKeys.pinFailure(USER_ID), RedisKeys.pinLock(USER_ID));
		verify(userMapper, never()).updatePinHash(any(), any());
	}

	// ---- withdrawal ----

	@Test
	void withdrawWithoutConfirmationThrows() {
		assertThatThrownBy(() -> userService.withdraw(USER_ID, false))
			.isInstanceOf(WithdrawalNotConfirmedException.class);

		verify(userMapper, never()).softDeleteAndAnonymize(any());
	}

	@Test
	void withdrawWithConfirmationSoftDeletesAndRevokesSession() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));

		userService.withdraw(USER_ID, true);

		verify(userMapper).softDeleteAndAnonymize(USER_ID);
		verify(tokenService).revokeRefreshToken(USER_ID);
	}

	@Test
	void withdrawForUnknownUserThrowsAndNeverDeletes() {
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.withdraw(USER_ID, true)).isInstanceOf(UserNotFoundException.class);

		verify(userMapper, never()).softDeleteAndAnonymize(any());
	}
}
