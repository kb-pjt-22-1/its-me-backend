package site.benepay.auth.security.jwt;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-of-at-least-32-bytes-long";
    private static final String ISSUER = "benepay-auth";

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        jwtProperties = new JwtProperties(SECRET, ISSUER, 600_000L, 31_536_000_000L);
        jwtTokenProvider = newInitializedProvider(jwtProperties);

        user = User.builder()
                .userId(11L)
                .loginId("tester01")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private JwtTokenProvider newInitializedProvider(JwtProperties properties) throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        Method init = JwtTokenProvider.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(provider);
        return provider;
    }

    @Test
    void accessTokenIsValidAndCarriesExpectedClaims() {
        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(11L);
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_ACCESS);
        assertThat(jwtTokenProvider.getJti(token)).isNotBlank();
    }

    @Test
    void refreshTokenCarriesTheGivenJtiAndRefreshType() {
        String token = jwtTokenProvider.generateRefreshToken(user, "fixed-jti-value");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_REFRESH);
        assertThat(jwtTokenProvider.getJti(token)).isEqualTo("fixed-jti-value");
    }

    @Test
    void accessAndRefreshTokensForTheSameUserHaveDifferentJtis() {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, "explicit-jti");

        assertThat(jwtTokenProvider.getJti(accessToken)).isNotEqualTo(jwtTokenProvider.getJti(refreshToken));
    }

    @Test
    void getAuthenticationSetsUserIdAsPrincipalAndRolePrefixedAuthority() {
        String token = jwtTokenProvider.generateAccessToken(user);

        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        assertThat(authentication.getPrincipal()).isEqualTo(11L);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void malformedTokenFailsValidation() {
        assertThat(jwtTokenProvider.validateToken("not-a-real-jwt")).isFalse();
    }

    @Test
    void expiredTokenFailsValidation() throws Exception {
        JwtProperties expiredProperties = new JwtProperties(SECRET, ISSUER, -1000L, 31_536_000_000L);
        JwtTokenProvider expiredProvider = newInitializedProvider(expiredProperties);

        String token = expiredProvider.generateAccessToken(user);

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    void tokenSignedWithADifferentIssuerFailsValidation() throws Exception {
        JwtProperties otherIssuerProperties = new JwtProperties(SECRET, "some-other-issuer", 600_000L, 31_536_000_000L);
        JwtTokenProvider otherIssuerProvider = newInitializedProvider(otherIssuerProperties);

        String token = otherIssuerProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void tokenSignedWithADifferentSecretFailsValidation() throws Exception {
        JwtProperties otherSecretProperties = new JwtProperties(
                "a-completely-different-secret-of-at-least-32-bytes", ISSUER, 600_000L, 31_536_000_000L);
        JwtTokenProvider otherSecretProvider = newInitializedProvider(otherSecretProperties);

        String token = otherSecretProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void remainingTtlIsPositiveAndAtMostTheConfiguredExpiration() {
        String token = jwtTokenProvider.generateAccessToken(user);

        long remaining = jwtTokenProvider.getRemainingTtlMillis(token);

        assertThat(remaining).isPositive();
        assertThat(remaining).isLessThanOrEqualTo(jwtProperties.getAccessTokenExpirationMillis());
    }
}
