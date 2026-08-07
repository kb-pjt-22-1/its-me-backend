package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RefreshResponseDto {

	private String tokenType;
	private String accessToken;
	private String refreshToken;
	private long expiresIn;
}
