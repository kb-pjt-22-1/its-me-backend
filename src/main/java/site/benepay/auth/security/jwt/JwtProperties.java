package site.benepay.auth.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class JwtProperties {

	private final String secret;
	private final String issuer;
	private final long accessTokenExpirationMillis;
	private final long refreshTokenExpirationMillis;

	// 파라미터마다 @Value 표현식이 달라 @RequiredArgsConstructor로 대체할 수 없다.
	public JwtProperties(@Value("${jwt.secret}") String secret,
		@Value("${jwt.issuer}") String issuer,
		@Value("${jwt.access-token-expiration}") long accessTokenExpirationMillis,
		@Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis) {
		this.secret = secret;
		this.issuer = issuer;
		this.accessTokenExpirationMillis = accessTokenExpirationMillis;
		this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
	}
}
