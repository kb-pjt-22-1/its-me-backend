package site.benepay.auth.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    private void init() {
        // HS256 requires a key of at least 256 bits (32 bytes)
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return generateToken(user, TOKEN_TYPE_ACCESS, jwtProperties.getAccessTokenExpirationMillis(), UUID.randomUUID().toString());
    }

    public String generateRefreshToken(User user, String jti) {
        return generateToken(user, TOKEN_TYPE_REFRESH, jwtProperties.getRefreshTokenExpirationMillis(), jti);
    }

    private String generateToken(User user, String tokenType, long expirationMillis, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .id(jti)
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(user.getUserId()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    public long getRemainingTtlMillis(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
