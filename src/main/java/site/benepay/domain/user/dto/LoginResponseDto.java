package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.user.vo.User;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDto {

	private String tokenType;
	private String accessToken;
	private String refreshToken;
	private long expiresIn;
	private Long userId;
	private String loginId;

	// login/signUp이 전부 "토큰 한 쌍 + 유저 식별자"를 같은 모양으로 조립해야 해서
	// 중복을 피하려고 여기 모아 둔다 - AuthServiceImpl.issueTokensFor와 UserServiceImpl.signUp
	// 양쪽에서 재사용한다.
	public static LoginResponseDto of(User user, TokenPairDto tokens, long expiresInSeconds) {
		return LoginResponseDto.builder()
			.tokenType("Bearer")
			.accessToken(tokens.getAccessToken())
			.refreshToken(tokens.getRefreshToken())
			.expiresIn(expiresInSeconds)
			.userId(user.getUserId())
			.loginId(user.getLoginId())
			.build();
	}
}
