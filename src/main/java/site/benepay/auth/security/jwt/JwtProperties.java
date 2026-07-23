package site.benepay.auth.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    private final String secret;
    private final String issuer;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtProperties(@Value("${jwt.secret}") String secret,
                          @Value("${jwt.issuer}") String issuer,
                          @Value("${jwt.access-token-expiration}") long accessTokenExpirationMillis,
                          @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    public String getSecret() {
        return secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public long getAccessTokenExpirationMillis() {
        return accessTokenExpirationMillis;
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }
}
