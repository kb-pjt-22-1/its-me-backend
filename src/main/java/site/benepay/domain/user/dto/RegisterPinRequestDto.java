package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPinRequestDto {

	@NotBlank
	@Pattern(regexp = "^\\d{6}$", message = "PIN은 정확히 6자리 숫자여야 합니다.")
	private String pin;
}
