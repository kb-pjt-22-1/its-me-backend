package site.benepay.domain.user.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.InvalidCredentialsException;
import site.benepay.common.exception.InvalidPinFormatException;
import site.benepay.common.exception.PinAlreadyRegisteredException;
import site.benepay.common.exception.UserNotFoundException;
import site.benepay.common.exception.WithdrawalNotConfirmedException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;
import site.benepay.domain.user.mapper.UserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, passwordEncoder, redisLockoutService, tokenService);
    }

    private User activeUser(String pinHash) {
        return User.builder()
                .userId(USER_ID)
                .email("tester01@example.com")
                .loginId("tester01")
                .passwordHash("hashed-password")
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
        SignUpRequestDto request = new SignUpRequestDto(
                "new@example.com", "newuser", "Test1234!", "New User", "010-1111-2222", "ci-hash", "di-hash");
        when(userMapper.existsByLoginId("newuser")).thenReturn(false);
        when(userMapper.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.existsByDiHash("di-hash")).thenReturn(false);
        when(passwordEncoder.encode("Test1234!")).thenReturn("encoded-password");

        UserResponseDto response = userService.signUp(request);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getLoginId()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void signUpWithDuplicateLoginIdThrowsAndNeverInserts() {
        SignUpRequestDto request = new SignUpRequestDto(
                "new@example.com", "existing", "Test1234!", "New User", "010-1111-2222", "ci-hash", "di-hash");
        when(userMapper.existsByLoginId("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(DuplicateUserException.class);

        verify(userMapper, never()).insert(any());
    }

    @Test
    void signUpWithDuplicateEmailThrows() {
        SignUpRequestDto request = new SignUpRequestDto(
                "dup@example.com", "newuser", "Test1234!", "New User", "010-1111-2222", "ci-hash", "di-hash");
        when(userMapper.existsByLoginId("newuser")).thenReturn(false);
        when(userMapper.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(DuplicateUserException.class);

        verify(userMapper, never()).insert(any());
    }

    @Test
    void signUpWithDuplicateDiHashThrows() {
        SignUpRequestDto request = new SignUpRequestDto(
                "new@example.com", "newuser", "Test1234!", "New User", "010-1111-2222", "ci-hash", "dup-di-hash");
        when(userMapper.existsByLoginId("newuser")).thenReturn(false);
        when(userMapper.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.existsByDiHash("dup-di-hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(DuplicateUserException.class);

        verify(userMapper, never()).insert(any());
    }

    // ---- profile ----

    @Test
    void getMyProfileReturnsMappedDtoWhenUserExists() {
        when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));

        UserResponseDto response = userService.getMyProfile(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo("tester01@example.com");
    }

    @Test
    void getMyProfileThrowsWhenUserDoesNotExist() {
        when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(USER_ID)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfileUpdatesEmailAndPhoneNumber() {
        when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser(null)));
        UpdateProfileRequestDto request = new UpdateProfileRequestDto("updated@example.com", "010-2222-3333");

        userService.updateProfile(USER_ID, request);

        verify(userMapper).updateProfile(USER_ID, "updated@example.com", "010-2222-3333");
    }

    @Test
    void updateProfileThrowsWhenUserDoesNotExist() {
        when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.empty());
        UpdateProfileRequestDto request = new UpdateProfileRequestDto("updated@example.com", "010-2222-3333");

        assertThatThrownBy(() -> userService.updateProfile(USER_ID, request)).isInstanceOf(UserNotFoundException.class);

        verify(userMapper, never()).updateProfile(any(), any(), any());
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

        assertThatThrownBy(() -> userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("481027", "592841")))
                .isInstanceOf(AccountLockedException.class);

        verify(userMapper, never()).findByUserId(any());
    }

    @Test
    void updateOrDeletePinWithWrongCurrentPinRecordsFailure() {
        when(redisLockoutService.isLocked(RedisKeys.pinLock(USER_ID))).thenReturn(false);
        when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(activeUser("existing-pin-hash")));
        when(passwordEncoder.matches("000000", "existing-pin-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("000000", "592841")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(redisLockoutService).recordFailureAndMaybeLock(
                RedisKeys.pinFailure(USER_ID), RedisKeys.pinLock(USER_ID), 5, Duration.ofMinutes(10), Duration.ofSeconds(30));
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

        assertThatThrownBy(() -> userService.updateOrDeletePin(USER_ID, new UpdateDeletePinRequestDto("481027", "123456")))
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
