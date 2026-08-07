package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {

	@NotBlank
	private String currentPassword;

	// 회원가입 때와 동일한 규칙(SignUpRequestDto 참고)
	@NotBlank
	@Size(min = 8, max = 64)
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
		message = "password must contain at least one letter, one digit, and one special character")
	private String newPassword;
}
