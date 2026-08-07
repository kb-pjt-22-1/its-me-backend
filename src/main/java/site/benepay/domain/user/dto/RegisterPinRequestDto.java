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
	@Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
	private String pin;
}
