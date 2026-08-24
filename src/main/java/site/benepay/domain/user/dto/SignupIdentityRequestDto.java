package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 1단계(휴대폰 본인인증) - 인증번호 발송 요청. 여기서 받은 이름/생년월일/
 * 휴대폰번호는 SignupIdentityServiceImpl이 내부 중복 체크 + KB Mock Server 회원 확인에
 * 쓴 뒤, 인증번호 검증(SignupIdentityConfirmRequestDto)이 성공해야만 회원가입에 실제로
 * 반영된다 - 이 요청 자체는 아직 아무것도 확정하지 않는다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupIdentityRequestDto {

	@NotBlank
	@Size(max = 50)
	private String name;

	@NotBlank
	@Pattern(regexp = "^\\d{8}$", message = "birthDate must be 8 digits in YYYYMMDD format")
	private String birthDate;

	@NotBlank
	@Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "invalid phone number format")
	private String phoneNumber;
}
