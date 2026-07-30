package site.benepay.domain.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class MerchantBrand {
	private Long brandId;
	@NonNull
	private String brandCode;
	@NonNull
	private String brandName;
	@Setter
	private String brandLogo;
}