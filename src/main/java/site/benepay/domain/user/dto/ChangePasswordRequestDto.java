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
	@Size(min = 8, max = 20)
	@Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,20}$",
		message = "password must contain at least one uppercase letter, one lowercase letter, "
			+ "one digit, and one special character")
	private String newPassword;
}
