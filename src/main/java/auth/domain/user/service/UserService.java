package site.benepay.auth.domain.user.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.benepay.auth.common.exception.AccountLockedException;
import site.benepay.auth.common.exception.DuplicateUserException;
import site.benepay.auth.common.exception.InvalidCredentialsException;
import site.benepay.auth.common.exception.PinAlreadyRegisteredException;
import site.benepay.auth.common.exception.UserNotFoundException;
import site.benepay.auth.common.exception.WithdrawalNotConfirmedException;
import site.benepay.auth.common.util.RedisKeys;
import site.benepay.auth.domain.user.dto.RegisterPinRequestDto;
import site.benepay.auth.domain.user.dto.SignUpRequestDto;
import site.benepay.auth.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.auth.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.auth.domain.user.dto.UserResponseDto;
import site.benepay.auth.domain.user.entity.Role;
import site.benepay.auth.domain.user.entity.User;
import site.benepay.auth.domain.user.mapper.UserMapper;
import site.benepay.auth.domain.user.validator.PinValidator;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisLockoutService redisLockoutService;
    private final TokenService tokenService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                        RedisLockoutService redisLockoutService, TokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisLockoutService = redisLockoutService;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponseDto signUp(SignUpRequestDto request) {
        if (userMapper.existsByLoginId(request.getLoginId())) {
            throw new DuplicateUserException("login id already in use: " + request.getLoginId());
        }
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("email already in use: " + request.getEmail());
        }
        if (userMapper.existsByDiHash(request.getDiHash())) {
            throw new DuplicateUserException("identity already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .loginId(request.getLoginId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .ciHash(request.getCiHash())
                .di(request.getDiHash())
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        userMapper.insert(user);

        return UserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request) {
        findActiveUser(userId);
        userMapper.updateProfile(userId, request.getEmail(), request.getPhoneNumber());
        return getMyProfile(userId);
    }

    @Transactional
    public void registerPin(Long userId, RegisterPinRequestDto request) {
        User user = findActiveUser(userId);
        if (user.getPinHash() != null) {
            throw new PinAlreadyRegisteredException("PIN already registered; use the update endpoint instead");
        }
        PinValidator.validate(request.getPin());
        userMapper.updatePinHash(userId, passwordEncoder.encode(request.getPin()));
    }

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
