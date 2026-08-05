package site.benepay.domain.user.dto;

import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Deliberately excludes name/loginId: both fields are disabled for editing (FR-MEM-07).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {

	@Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "invalid phone number format")
	private String phoneNumber;
}
