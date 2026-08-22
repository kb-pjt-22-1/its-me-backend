package site.benepay.domain.user.service;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.InvalidTokenException;
import site.benepay.common.exception.TokenReuseException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.TokenPairDto;
import site.benepay.domain.user.event.SessionDisplacedEvent;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.auth.security.jwt.JwtProperties;
import site.benepay.auth.security.jwt.JwtTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final long USER_ID = 11L;
    private static final long GRACE_PERIOD_MILLIS = 8000L;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private TokenService tokenService;
    private User user;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        jwtProperties = new JwtProperties(
                "unit-test-secret-of-at-least-32-bytes-long", "benepay-auth", 600_000L, 31_536_000_000L);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        Method init = JwtTokenProvider.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(jwtTokenProvider);

        tokenService = new TokenServiceImpl(
                jwtTokenProvider, jwtProperties, redisTemplate, userMapper, eventPublisher, GRACE_PERIOD_MILLIS);

        user = User.builder()
                .userId(USER_ID)
                .loginId("tester01")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String storedStateJson(String jti, String accessToken, String refreshToken,
                                    String previousJti, Long graceExpiresAt) throws Exception {
        TokenServiceImpl.RefreshTokenState state = new TokenServiceImpl.RefreshTokenState();
        state.jti = jti;
        state.accessToken = accessToken;
        state.refreshToken = refreshToken;
        state.previousJti = previousJti;
        state.graceExpiresAt = graceExpiresAt;
        return objectMapper.writeValueAsString(state);
    }

    @Test
    void issueTokenPairGeneratesTokensAndStoresRefreshStateWithFullTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenPairDto tokens = tokenService.issueTokenPair(user);

        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        assertThat(jwtTokenProvider.getTokenType(tokens.getAccessToken())).isEqualTo(JwtTokenProvider.TOKEN_TYPE_ACCESS);
        assertThat(jwtTokenProvider.getTokenType(tokens.getRefreshToken())).isEqualTo(JwtTokenProvider.TOKEN_TYPE_REFRESH);

        verify(valueOperations).set(
                eq(RedisKeys.refresh(USER_ID)), anyString(), eq(Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMillis())));
        // 첫 로그인(이전 세션 없음)이라 로그인 해제 처리가 전혀 일어나면 안 된다.
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void issueTokenPairBlacklistsPreviousAccessTokenAndPublishesDisplacementEventWhenPriorSessionExists() throws Exception {
        String previousAccessToken = jwtTokenProvider.generateAccessToken(user);
        String previousJti = jwtTokenProvider.getJti(previousAccessToken);
        String previousRefreshToken = jwtTokenProvider.generateRefreshToken(user, "previous-jti");
        String storedJson = storedStateJson("previous-jti", previousAccessToken, previousRefreshToken, null, null);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.refresh(USER_ID))).thenReturn(storedJson);

        tokenService.issueTokenPair(user);

        // 새 세션 저장(1) + 이전 access 토큰 블랙리스트(1) = set이 총 두 번 호출된다.
        verify(valueOperations).set(eq(RedisKeys.blacklist(previousJti)), eq("logout"), any(Duration.class));
        verify(eventPublisher).publishEvent(new SessionDisplacedEvent(USER_ID));
    }

    @Test
    void rotateWithMatchingJtiIssuesNewTokensAndOverwritesStoredState() throws Exception {
        String jti = "current-jti";
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, jti);
        String storedJson = storedStateJson(jti, "old-access", refreshToken, null, null);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.refresh(USER_ID))).thenReturn(storedJson);
        when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        TokenPairDto rotated = tokenService.rotateRefreshToken(refreshToken);

        assertThat(rotated.getRefreshToken()).isNotEqualTo(refreshToken);
        assertThat(jwtTokenProvider.getJti(rotated.getRefreshToken())).isNotEqualTo(jti);

        ArgumentCaptor<String> savedJson = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(RedisKeys.refresh(USER_ID)), savedJson.capture(), any(Duration.class));
        TokenServiceImpl.RefreshTokenState savedState = objectMapper.readValue(savedJson.getValue(), TokenServiceImpl.RefreshTokenState.class);
        assertThat(savedState.previousJti).isEqualTo(jti);
        assertThat(savedState.jti).isEqualTo(jwtTokenProvider.getJti(rotated.getRefreshToken()));
        assertThat(savedState.graceExpiresAt).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void rotateWithMalformedTokenThrowsInvalidTokenException() {
        assertThatThrownBy(() -> tokenService.rotateRefreshToken("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rotateWithAnAccessTokenInsteadOfRefreshTokenThrowsInvalidTokenException() {
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rotateWithNoStoredSessionThrowsInvalidTokenException() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, "some-jti");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.refresh(USER_ID))).thenReturn(null);

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(refreshToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void reusingAPreviousTokenWithinTheGracePeriodReturnsTheCurrentPairWithoutRotating() throws Exception {
        String previousJti = "previous-jti";
        String presentedRefreshToken = jwtTokenProvider.generateRefreshToken(user, previousJti);

        String currentAccessToken = jwtTokenProvider.generateAccessToken(user);
        String currentRefreshToken = jwtTokenProvider.generateRefreshToken(user, "current-jti");
        long futureGraceExpiry = System.currentTimeMillis() + 5000L;
        String storedJson = storedStateJson("current-jti", currentAccessToken, currentRefreshToken, previousJti, futureGraceExpiry);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.refresh(USER_ID))).thenReturn(storedJson);

        TokenPairDto result = tokenService.rotateRefreshToken(presentedRefreshToken);

        assertThat(result.getAccessToken()).isEqualTo(currentAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo(currentRefreshToken);
        verify(userMapper, never()).findByUserId(any());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void reusingAPreviousTokenAfterTheGracePeriodExpiresIsTreatedAsReuseAndRevokesTheSession() throws Exception {
        String previousJti = "previous-jti";
        String presentedRefreshToken = jwtTokenProvider.generateRefreshToken(user, previousJti);

        String currentAccessToken = jwtTokenProvider.generateAccessToken(user);
        String currentRefreshToken = jwtTokenProvider.generateRefreshToken(user, "current-jti");
        long pastGraceExpiry = System.currentTimeMillis() - 1000L;
        String storedJson = storedStateJson("current-jti", currentAccessToken, currentRefreshToken, previousJti, pastGraceExpiry);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.refresh(USER_ID))).thenReturn(storedJson);

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(presentedRefreshToken))
                .isInstanceOf(TokenReuseException.class);

        verify(redisTemplate).delete(RedisKeys.refresh(USER_ID));
        verify(valueOperations).set(eq(RedisKeys.alert(USER_ID)), anyString(), eq(Duration.ofDays(30)));
    }

    @Test
    void reuseWithNoMatchingPreviousJtiRevokesTheSession() throws Exception {
        String presentedRefreshToken = jwtTokenProvider.generateRefreshToken(user, "unknown-jti");
        String storedJson = storedStateJson("current-jti", "access", "refresh", "some-other-previous-jti", null);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.refresh(USER_ID))).thenReturn(storedJson);

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(presentedRefreshToken))
                .isInstanceOf(TokenReuseException.class);

        verify(redisTemplate).delete(RedisKeys.refresh(USER_ID));
    }

    @Test
    void blacklistAccessTokenStoresBlacklistKeyWithRemainingTtl() {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String jti = jwtTokenProvider.getJti(accessToken);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenService.blacklistAccessToken(accessToken);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(RedisKeys.blacklist(jti)), eq("logout"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().toMillis()).isPositive();
        assertThat(ttlCaptor.getValue().toMillis()).isLessThanOrEqualTo(jwtProperties.getAccessTokenExpirationMillis());
    }

    @Test
    void blacklistingAnInvalidTokenDoesNothing() {
        tokenService.blacklistAccessToken("not-a-jwt");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void revokeRefreshTokenDeletesTheRefreshKey() {
        tokenService.revokeRefreshToken(USER_ID);

        verify(redisTemplate).delete(RedisKeys.refresh(USER_ID));
    }
}
