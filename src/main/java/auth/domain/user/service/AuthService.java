package site.benepay.auth.domain.user.service;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.benepay.auth.common.exception.AccountLockedException;
import site.benepay.auth.common.exception.InvalidCredentialsException;
import site.benepay.auth.common.util.RedisKeys;
import site.benepay.auth.domain.user.dto.LoginRequestDto;
import site.benepay.auth.domain.user.dto.LoginResponseDto;
import site.benepay.auth.domain.user.dto.RefreshRequestDto;
import site.benepay.auth.domain.user.dto.RefreshResponseDto;
import site.benepay.auth.domain.user.dto.TokenPairDto;
import site.benepay.auth.domain.user.entity.User;
import site.benepay.auth.domain.user.mapper.UserMapper;
import site.benepay.auth.security.jwt.JwtProperties;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;
    private final RedisLockoutService redisLockoutService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtProperties jwtProperties,
                        TokenService tokenService, RedisLockoutService redisLockoutService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.tokenService = tokenService;
        this.redisLockoutService = redisLockoutService;
    }

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

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, 5, Duration.ofMinutes(10), Duration.ofMinutes(30));
            throw new InvalidCredentialsException("invalid login id or password");
        }

        redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
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

    public RefreshResponseDto refresh(RefreshRequestDto request) {
        TokenPairDto tokens = tokenService.rotateRefreshToken(request.getRefreshToken());
        return RefreshResponseDto.builder()
                .tokenType("Bearer")
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(jwtProperties.getAccessTokenExpirationMillis() / 1000)
                .build();
    }

    public void logout(String accessToken, Long userId) {
        tokenService.blacklistAccessToken(accessToken);
        tokenService.revokeRefreshToken(userId);
    }
}
