package site.benepay.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.InvalidCredentialsException;
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
import site.benepay.domain.user.validator.PinValidator;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisLockoutService redisLockoutService;
    private final TokenService tokenService;
    private final SignupVerificationStore signupVerificationStore;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                            RedisLockoutService redisLockoutService, TokenService tokenService,
                            SignupVerificationStore signupVerificationStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisLockoutService = redisLockoutService;
        this.tokenService = tokenService;
        this.signupVerificationStore = signupVerificationStore;
    }

    @Override
    @Transactional
    public UserResponseDto signUp(SignUpRequestDto request) {
        if (userMapper.existsByLoginId(request.getLoginId())) {
            throw new DuplicateUserException("login id already in use: " + request.getLoginId());
        }

        SignupVerificationStore.VerifiedIdentity identity = signupVerificationStore
                .redeem(request.getVerificationToken())
                .orElseThrow(() -> new InvalidTokenException("identity verification token is invalid or expired"));

        // PortOne 인증 시점에 이미 한 번 걸렀지만, 그 사이 다른 요청이 같은 DI로 먼저
        // 가입했을 수 있어 여기서 한 번 더 확인한다. 최종 방어선은 어차피 users.di UNIQUE다.
        if (userMapper.existsByDiHash(identity.diHash)) {
            throw new DuplicateUserException("identity already registered");
        }

        User user = User.builder()
                .loginId(request.getLoginId())
                .loginPasswordHash(passwordEncoder.encode(request.getPassword()))
                .name(identity.name)
                .phoneNumber(identity.phoneNumber)
                .birthDate(identity.birthDate)
                .role(Role.USER)
                .di(identity.diHash)
                .ciEncrypted(identity.ciEncrypted)
                // 컬럼에 DEFAULT CURRENT_TIMESTAMP가 붙었지만, insert 후 재조회하지 않고
                // 이 객체를 그대로 응답으로 내보내므로 값을 직접 채워 둔다.
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .fcmToken(request.getFcmToken())
                .build();

        userMapper.insert(user);

        return UserResponseDto.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserResponseDto.from(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request) {
        findActiveUser(userId);
        userMapper.updateProfile(userId, request.getPhoneNumber());
        return getMyProfile(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyPassword(Long userId, VerifyPasswordRequestDto request) {
        // 실패하면 verifyCurrentPasswordOrThrow가 예외를 던진다 - 성공은 반환값 자체가 증거라
        // 여기서 더 할 일이 없다. 개인정보 수정 페이지 진입 전 재인증 게이트 용도.
        verifyCurrentPasswordOrThrow(userId, request.getPassword());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto request) {
        verifyCurrentPasswordOrThrow(userId, request.getCurrentPassword());

        userMapper.updatePasswordHash(userId, passwordEncoder.encode(request.getNewPassword()));
        // 비밀번호가 바뀌면 이전 비밀번호로 발급된 세션은 더 이상 유효할 이유가 없다 -
        // 다른 곳에서 이미 로그인돼 있던 사람이 그대로 남아있게 두지 않는다.
        tokenService.revokeRefreshToken(userId);
    }

    // verifyPassword(재인증 게이트)와 changePassword가 "현재 비밀번호가 맞는지" 확인하는
    // 로직과 잠금 카운터를 그대로 공유한다 - 둘 다 막으려는 위협이 같다(비밀번호 무차별 대입).
    private User verifyCurrentPasswordOrThrow(Long userId, String currentPassword) {
        String failureKey = RedisKeys.passwordFailure(userId);
        String lockKey = RedisKeys.passwordLock(userId);

        if (redisLockoutService.isLocked(lockKey)) {
            throw new AccountLockedException("password verification is temporarily locked");
        }

        User user = findActiveUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getLoginPasswordHash())) {
            redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, 5, Duration.ofMinutes(10), Duration.ofMinutes(30));
            throw new InvalidCredentialsException("current password is incorrect");
        }
        redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
        return user;
    }

    @Override
    @Transactional
    public void registerPin(Long userId, RegisterPinRequestDto request) {
        User user = findActiveUser(userId);
        if (user.getPinHash() != null) {
            throw new PinAlreadyRegisteredException("PIN already registered; use the update endpoint instead");
        }
        PinValidator.validate(request.getPin());
        userMapper.updatePinHash(userId, passwordEncoder.encode(request.getPin()));
    }

    @Override
    @Transactional
    public void updateOrDeletePin(Long userId, UpdateDeletePinRequestDto request) {
        String failureKey = RedisKeys.pinFailure(userId);
        String lockKey = RedisKeys.pinLock(userId);

        if (redisLockoutService.isLocked(lockKey)) {
            throw new AccountLockedException("PIN verification is temporarily locked");
        }

        User user = findActiveUser(userId);
        if (user.getPinHash() == null || !passwordEncoder.matches(request.getCurrentPin(), user.getPinHash())) {
            redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, 5, Duration.ofMinutes(10), Duration.ofSeconds(30));
            throw new InvalidCredentialsException("current PIN is incorrect");
        }
        redisLockoutService.clearFailuresAndLock(failureKey, lockKey);

        if (request.getNewPin() == null) {
            userMapper.updatePinHash(userId, null);
        } else {
            PinValidator.validate(request.getNewPin());
            userMapper.updatePinHash(userId, passwordEncoder.encode(request.getNewPin()));
        }
    }

    @Override
    @Transactional
    public void withdraw(Long userId, boolean confirmed) {
        if (!confirmed) {
            throw new WithdrawalNotConfirmedException("withdrawal confirmation flag is required");
        }
        findActiveUser(userId);
        userMapper.softDeleteAndAnonymize(userId);
        tokenService.revokeRefreshToken(userId);
    }

    private User findActiveUser(Long userId) {
        return userMapper.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("user not found"));
    }
}
