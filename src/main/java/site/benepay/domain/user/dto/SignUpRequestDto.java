package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * name/phoneNumber/birthDate/ciEncrypted/diHash를 여기서 직접 받지 않는다. 그 값들은
 * POST /api/auth/signup/identity/confirm이 발급한 verificationToken 뒤에 서버(Redis)에만
 * 있고, signUp()이 그 토큰으로 서버 쪽 값을 꺼내 쓴다 - 클라이언트가 개인정보를 다시 보내면서
 * 위조하거나 다른 사람의 인증 결과를 재사용할 여지를 없애기 위함이다.
 *
 * <p>pin(결제 비밀번호)을 로그인ID/비밀번호와 함께 여기서 한 번에 받는 이유: 계정을 먼저
 * 만들고 별도 인증된 API(PATCH /api/users/me/pin)로 PIN을 나중에 등록하게 하면, 그 사이
 * 사용자가 이탈했을 때 "PIN 없이 이미 작동하는 계정"이 남는다. 회원가입 4단계 중 PIN을
 * 필수로 만들려는 의도와 맞지 않아, User 1건을 원자적으로 만드는 이 요청에 포함시켰다.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {

	@NotBlank
	@Size(min = 4, max = 20)
	@Pattern(regexp = "^[a-zA-Z0-9]+$", message = "loginId must contain only letters and digits")
	private String loginId;

	@NotBlank
	@Size(min = 8, max = 20)
	@Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,20}$",
		message = "password must contain at least one uppercase letter, one lowercase letter, "
			+ "one digit, and one special character")
	private String password;

	// 형식(6자리 숫자, 연속/반복 금지)은 PinValidator가 한 번 더 검증한다 - 여기서는 존재
	// 여부만 강제해 형식 오류를 더 구체적인 InvalidPinFormatException으로 알려줄 수 있게 한다.
	@NotBlank
	private String pin;

	@NotBlank
	private String verificationToken;

	@Size(max = 255)
	private String fcmToken;
}
