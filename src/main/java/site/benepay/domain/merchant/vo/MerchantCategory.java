package site.benepay.domain.merchant.vo;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor
@RequiredArgsConstructor
public class MerchantCategory {
	@NonNull
	@Pattern(regexp = "^\\d{4}$")
	private String categoryCode;
	@NonNull
	private String categoryName;
	@NonNull
	private String categoryIcon;
}