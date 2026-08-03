package site.benepay.domain.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Merchant {
    private Long merchantId;
    @NonNull
    private String categoryCode;
    @NonNull
    private Long brandId;
    @NonNull
    private String merchantCode;
    @NonNull
    private String merchantName;
    @NonNull
    @Setter
    private String address;
    @NonNull
    private BigDecimal latitude;
    @NonNull
    private BigDecimal longitude;
    @Setter
    private String phone;
}
