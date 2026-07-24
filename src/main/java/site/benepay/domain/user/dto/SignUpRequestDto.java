package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {

    @NotBlank
    @Size(min = 4, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "loginId must contain only letters, digits, '-' and '_'")
    private String loginId;

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
            message = "password must contain at least one letter, one digit, and one special character")
    private String password;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "invalid phone number format")
    private String phoneNumber;

    @NotBlank
    private String ciHash;

    @NotBlank
    private String diHash;
}
