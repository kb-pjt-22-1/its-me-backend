package site.benepay.auth.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.benepay.auth.common.exception.InvalidTokenException;
import site.benepay.auth.common.exception.TokenReuseException;
import site.benepay.auth.common.exception.UserNotFoundException;
import site.benepay.auth.common.util.RedisKeys;
import site.benepay.auth.domain.user.dto.TokenPairDto;
import site.benepay.auth.domain.user.entity.User;
import site.benepay.auth.domain.user.mapper.UserMapper;
import site.benepay.auth.security.jwt.JwtProperties;
import site.benepay.auth.security.jwt.JwtTokenProvider;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long gracePeriodMillis;

    public TokenService(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties,
                         StringRedisTemplate redisTemplate, UserMapper userMapper,
                         @Value("${token.refresh-grace-period-ms}") long gracePeriodMillis) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
        this.gracePeriodMillis = gracePeriodMillis;
    }

    public TokenPairDto issueTokenPair(User user) {
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, jti);

        RefreshTokenState state = new RefreshTokenState();
        state.jti = jti;
        state.accessToken = accessToken;
        state.refreshToken = refreshToken;

        saveState(user.getUserId(), state);
        return TokenPairDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

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

        if (state.previousJti != null && presentedJti.equals(state.previousJti)
                && state.graceExpiresAt != null && System.currentTimeMillis() < state.graceExpiresAt) {
            // NFR-REL-01: concurrent duplicate refresh request using the just-rotated-out token
            return TokenPairDto.builder().accessToken(state.accessToken).refreshToken(state.refreshToken).build();
        }

        // NFR-SEC-02: JTI mismatch outside the grace window -> suspected token theft, kill the whole session
        redisTemplate.delete(RedisKeys.refresh(userId));
        redisTemplate.opsForValue().set(RedisKeys.alert(userId),
                "refresh token reuse detected at " + System.currentTimeMillis(), Duration.ofDays(30));
        log.warn("Refresh token reuse detected for userId={}", userId);
        throw new TokenReuseException("refresh token reuse detected; all sessions revoked");
    }

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
