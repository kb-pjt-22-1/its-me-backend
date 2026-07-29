package site.benepay.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import site.benepay.domain.user.vo.User;

import java.time.Duration;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEV_LOGIN_ID_PREFIX = "dev";
    private static final int DEFAULT_DEV_LOGIN_SLOT = 1;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;
    private final RedisLockoutService redisLockoutService;
    private final boolean devLoginEnabled;
    private final int devLoginAccountCount;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtProperties jwtProperties,
                            TokenService tokenService, RedisLockoutService redisLockoutService,
                            @Value("${dev-login.enabled}") boolean devLoginEnabled,
                            @Value("${dev-login.account-count}") int devLoginAccountCount) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.tokenService = tokenService;
        this.redisLockoutService = redisLockoutService;
        this.devLoginEnabled = devLoginEnabled;
        this.devLoginAccountCount = devLoginAccountCount;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userMapper.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("invalid login id or password"));

        Long userId = user.getUserId();
        String failureKey = RedisKeys.loginFailure(userId);
        String lockKey = RedisKeys.loginLock(userId);

        if (redisLockoutService.isLocked(lockKey)) {
            throw new AccountLockedException("account temporarily locked due to repeated login failures");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getLoginPasswordHash())) {
            redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, 5, Duration.ofMinutes(10), Duration.ofMinutes(30));
            throw new InvalidCredentialsException("invalid login id or password");
        }

        redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
        return issueTokensFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto devLogin(DevLoginRequestDto request) {
        if (!devLoginEnabled) {
            throw new DevLoginDisabledException("dev login is disabled");
        }

        int slot = request.getSlot() == null ? DEFAULT_DEV_LOGIN_SLOT : request.getSlot();
        if (slot < 1 || slot > devLoginAccountCount) {
            throw new InvalidCredentialsException(
                    "dev login slot must be between 1 and " + devLoginAccountCount);
        }

        String loginId = DEV_LOGIN_ID_PREFIX + slot;
        User user = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("dev account is not seeded: " + loginId));

        // 켜진 채로 배포되는 것이 이 기능의 유일한 위험이다. 로그를 남겨 두면 운영 로그만 훑어도
        // 바로 눈에 띈다.
        log.warn("DEV LOGIN used for loginId={}. This endpoint must stay disabled outside development.", loginId);

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
        return LoginResponseDto.builder()
                .tokenType("Bearer")
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(jwtProperties.getAccessTokenExpirationMillis() / 1000)
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .build();
    }
}
