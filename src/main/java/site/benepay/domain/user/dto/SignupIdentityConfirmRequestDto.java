package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 1단계 - 인증번호 검증 요청. phoneNumber는 SignupIdentityRequestDto로 발송을
 * 요청했던 그 번호와 같아야 Redis에 저장해 둔 인증번호를 찾을 수 있다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupIdentityConfirmRequestDto {

	@NotBlank
	@Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "invalid phone number format")
	private String phoneNumber;

	@NotBlank
	@Pattern(regexp = "^\\d{6}$", message = "code must be exactly 6 digits")
	private String code;
}
