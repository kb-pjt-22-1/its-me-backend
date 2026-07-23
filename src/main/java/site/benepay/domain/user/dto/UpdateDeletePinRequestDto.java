package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeletePinRequestDto {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "current PIN must be exactly 6 digits")
    private String currentPin;

    /**
     * null means "delete the PIN"; if present, must be exactly 6 digits.
     */
    @Pattern(regexp = "^\\d{6}$", message = "new PIN must be exactly 6 digits")
    private String newPin;
}
