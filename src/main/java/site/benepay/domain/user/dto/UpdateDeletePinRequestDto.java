package site.benepay.domain.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeletePinRequestDto {

	@NotBlank
	@Pattern(regexp = "^\\d{6}$", message = "현재 PIN은 정확히 6자리 숫자여야 합니다.")
	private String currentPin;

	/**
	 * null means "delete the PIN"; if present, must be exactly 6 digits.
	 */
	@Pattern(regexp = "^\\d{6}$", message = "새 PIN은 정확히 6자리 숫자여야 합니다.")
	private String newPin;
}
