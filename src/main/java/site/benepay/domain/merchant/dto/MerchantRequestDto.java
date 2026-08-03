package site.benepay.domain.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRequestDto {
    @NotBlank
    @Pattern(regexp = "^\\d{4}$")
    private String categoryCode;
    @NotNull
    private Long brandId;
    @NotBlank
    @Size(max = 50)
    private String merchantCode;
    @NotBlank
    @Size(max = 100)
    private String merchantName;
    @NotBlank
    @Size(max = 255)
    private String address;
    @NotNull
    private BigDecimal latitude;
    @NotNull
    private BigDecimal longitude;
    @Pattern(regexp = "^0\\d{1,2}-?\\d{3,4}-?\\d{4}$", message = "잘못된 전화번호 형식입니다.")
    @Size(max = 20)
    private String phone;
}
