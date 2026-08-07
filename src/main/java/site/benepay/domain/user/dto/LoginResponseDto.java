package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
}
