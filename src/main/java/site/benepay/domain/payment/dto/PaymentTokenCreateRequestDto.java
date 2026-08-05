package site.benepay.domain.payment.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTokenCreateRequestDto {

	@NotNull(message = "결제에 사용할 카드는 필수입니다.")
	private Long userCardId;

	@NotNull(message = "가맹점 아이디는 필수입니다.")
	private Long merchantId;

	@NotNull(message = "결제 금액은 필수입니다.")
	@DecimalMin(value = "0", inclusive = false, message = "결제 금액은 0보다 커야 합니다.")
	private BigDecimal originalAmount;

	// 미전달 시 0으로 처리
	@DecimalMin(value = "0", message = "할인 금액은 0 이상이어야 합니다.")
	private BigDecimal discountAmount;
}
