package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * name/phoneNumber/birthDate/ciEncrypted/diHash를 여기서 직접 받지 않는다. 그 값들은
 * POST /api/auth/portone/verify가 발급한 verificationToken 뒤에 서버(Redis)에만 있고,
 * signUp()이 그 토큰으로 서버 쪽 값을 꺼내 쓴다 - 클라이언트가 개인정보를 다시 보내면서
 * 위조하거나 다른 사람의 인증 결과를 재사용할 여지를 없애기 위함이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {

	@NotBlank
	@Size(min = 4, max = 20)
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "loginId must contain only letters, digits, '-' and '_'")
	private String loginId;

	@NotBlank
	@Size(min = 8, max = 64)
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
		message = "password must contain at least one letter, one digit, and one special character")
	private String password;

	@NotBlank
	private String verificationToken;

	@Size(max = 255)
	private String fcmToken;
}
