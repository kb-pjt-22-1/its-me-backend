package site.benepay.auth.domain.user.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "invalid phone number format")
    private String phoneNumber;
}
