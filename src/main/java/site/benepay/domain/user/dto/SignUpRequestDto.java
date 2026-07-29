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

    // 컬럼이 CHAR(8)이라 길이가 어긋난 값은 DB가 잘라 버린다. 형식을 여기서 걸러 실패 지점을
    // 검증 단계로 앞당긴다. 컬럼이 NULL을 허용하므로 미입력은 통과시킨다.
    @Pattern(regexp = "^\\d{8}$", message = "birthDate must be 8 digits in YYYYMMDD format")
    private String birthDate;

    // ci_encrypted, di 모두 NOT NULL로 바뀌어 빈 값이면 insert 단계에서 터진다.
    // @Size는 각 컬럼 길이(200/100)와 맞춰 둔 것이다.
    @NotBlank
    @Size(max = 200)
    private String ciEncrypted;

    @NotBlank
    @Size(max = 100)
    private String diHash;

    @Size(max = 255)
    private String fcmToken;
}
