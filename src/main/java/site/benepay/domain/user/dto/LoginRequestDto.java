package site.benepay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class LoginRequestDto {
    @NotBlank
    private String loginId;

    @NotBlank
    private String password;
}
